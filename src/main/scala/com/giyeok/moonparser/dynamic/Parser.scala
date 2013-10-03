package com.giyeok.moonparser.dynamic

import scala.Option.option2Iterable
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListMap
import scala.collection.mutable.Queue
import com.giyeok.moonparser.CharInput
import com.giyeok.moonparser.CharacterInput
import com.giyeok.moonparser.GrElem
import com.giyeok.moonparser.EOF
import com.giyeok.moonparser.EmptySymbol
import com.giyeok.moonparser.Except
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Input
import com.giyeok.moonparser.LookaheadExcept
import com.giyeok.moonparser.NontermSymbol
import com.giyeok.moonparser.Nonterminal
import com.giyeok.moonparser.OneOf
import com.giyeok.moonparser.ParserInput
import com.giyeok.moonparser.Repeat
import com.giyeok.moonparser.RepeatRangeFrom
import com.giyeok.moonparser.RepeatRangeTo
import com.giyeok.moonparser.Sequence
import com.giyeok.moonparser.ParsedSymbol
import com.giyeok.moonparser.StartSymbol
import com.giyeok.moonparser.StringInput
import com.giyeok.moonparser.TermSymbol
import com.giyeok.moonparser.TokenInput
import com.giyeok.moonparser.VirtInput
import com.giyeok.moonparser.VirtualInput
import com.giyeok.moonparser.InputElem

class ParseResult(val messages: List[ParsePossibility]) {
    def add(p: ParsePossibility) =
        new ParseResult(p :: messages)

    val succeeds = (messages map (_ match {
        case x: ParseSuccess => Some(x)
        case _ => None
    })).flatten
    val ambiguous = succeeds.length > 1
    val succeed = succeeds.length == 1
    val parsed: Option[ParsedSymbol] = if (succeed) Some(succeeds.head.parsed) else None
}
sealed abstract class ParsePossibility
case class ParseFailed(reason: String, location: Int) extends ParsePossibility
case class ParseSuccess(parsed: ParsedSymbol) extends ParsePossibility

trait BlackboxParser {
    def parse(input: ParserInput): ParseResult
    def parse(input: String): ParseResult
}
class BasicBlackboxParser(val grammar: Grammar) extends BlackboxParser {
    // === parser ===
    def parse(input: ParserInput): ParseResult = {
        val parser = new Parser(grammar, input)
        parser.parseAll()
        parser.result
    }
    def parse(input: String): ParseResult = parse(ParserInput.fromString(input))
}
class Parser(val grammar: Grammar, val input: ParserInput) {
    protected val starter = new StackEntry(null, StartSymbol, 0, null, null) {
        def _items = List(new StackEntryItem(defItemToState(Nonterminal(grammar.startSymbol)), null, Nil))
    }
    protected val stack = new OctopusStack(starter)

    // === stack ===
    class OctopusStack(val bottom: StackEntry) {
        import scala.collection.mutable.Queue

        private val tops = Queue[StackEntry](bottom)

        def add(entry: StackEntry) = tops += entry
        def addAll(entries: Seq[StackEntry]) = for (entry <- entries) add(entry)
        def hasNext = !tops.isEmpty
        def top = tops.front
        def pop() = tops.dequeue()
        def iterator = tops.iterator
    }

    // === parser ===
    private var _result: ParseResult = new ParseResult(Nil)
    def result = _result

    private var _finished = false
    def finished = _finished

    protected def shifted(newentry: StackEntry): Unit = stack add newentry
    protected def reduced(newentry: StackEntry): Unit = stack add newentry
    protected def done(): Unit = ()
    def parseStep() =
        if (stack hasNext) {
            val entry = stack.pop()
            val pointer = entry.pointer
            val fin = entry finished
            val term = TermSymbol(input at pointer, pointer)

            def pushFinished(f: List[entry.StackEntryItem]): List[StackEntry] =
                f flatMap ((x) =>
                    if (x.item finishable) {
                        if (x.generationPoint == null) {
                            // Parsing finished
                            if ((input at pointer) == EOF) {
                                _result = _result add ParseSuccess(NontermSymbol(x.item)) // Successfully parsed
                            } else {
                                _result = _result add ParseFailed("type 1", pointer) // Error while parsing
                            }
                            Nil
                        } else {
                            (x.generationPoint proceed (NontermSymbol(x.item), pointer, x.belonged, x)) match {
                                case Some(newentry) => List(newentry)
                                case _ => Nil
                            }
                        }
                    } else Nil)

            val newentries = pushFinished(fin)
            newentries foreach (reduced(_))

            (entry proceed (term, pointer + 1, entry, null)) match {
                case Some(newentry) => shifted(newentry)
                case _ =>
                    if (fin isEmpty) {
                        // and if the entry has no child
                        // println(s"${entry.id} is vaporized")
                    }
            }
            true
        } else {
            _finished = true
            done()
            false
        }
    def parseAll() = {
        while (parseStep()) ()
    }

    // === nullable checking ===
    object Nullable {
        // nonterminal to nullable boolean map
        private val map = new collection.mutable.HashMap[String, Boolean]()
    }
    implicit class Nullable(item: GrElem) {
        def isNullable(item: GrElem): Boolean = {
            item match {
                case Nonterminal(name) => (Nullable.map get name) match {
                    case Some(v) => v
                    case None => {
                        Nullable.map += name -> false
                        val rhs = (grammar.rules get name)
                        if (rhs isEmpty) throw new Exception("Unknown nonterminal: " + name)
                        val temp = rhs.head exists { isNullable(_) }
                        Nullable.map(name) = temp; temp
                    }
                }
                case StringInput(string) => string.length == 0
                case _: CharacterInput | _: VirtualInput => false
                case Sequence(seq, whitespace) => {
                    def AND(items: List[GrElem]): Boolean = items match {
                        case Nil => true
                        case x :: xs => if (isNullable(x)) AND(xs) else false
                    }
                    AND(seq.toList)
                }
                case OneOf(items) => items exists { isNullable(_) }
                case Except(item, except) => isNullable(item)
                case LookaheadExcept(except) => false
                case Repeat(item, range) => range match {
                    case RepeatRangeFrom(from) => if (from == 0) true else isNullable(item)
                    case RepeatRangeTo(from, to) => if (from == 0) true else isNullable(item)
                }
            }
        }

        val nullable = isNullable(item)
    }

    // === Stack Entry ============================================================================
    object StackEntry {
        private var unique: Int = 0
        private def nextId = { unique += 1; unique }
    }
    abstract class StackEntry(
        val parent: StackEntry,
        val symbol: ParsedSymbol,
        val pointer: Int,
        val generatedFrom: StackEntry,
        val generatedFromItem: StackEntry#StackEntryItem) {
        def _items: List[this.StackEntryItem]

        def finished: List[this.StackEntryItem] = items filter (_ finishable)
        val kernels = _items
        val id = StackEntry.nextId

        def proceed(n: ParsedSymbol, p: Int, from: StackEntry, fromItem: StackEntry#StackEntryItem): Option[StackEntry] = {
            // TODO this is temporary, rewrite this
            val newentry = new StackEntry(this, n, p, from, fromItem) {
                def _items = parent.items flatMap ((x) => (x proceed (n, this)))
            }
            if (newentry isEmpty) None else Some(newentry)
        }
        val isEmpty = kernels isEmpty

        // NOTE: Not all entries of 'kernels' may not be included in 'items' (because of derivedFrom)
        val items = {
            import scala.collection.mutable.ListMap
            var _items = kernels
            val derivation = new ListMap[ParsingItem, StackEntryItem]

            kernels foreach ((i) => derivation(i.item) = i)

            def derive(left: List[StackEntryItem]): Unit = {
                left match {
                    case x :: xs =>
                        var added: List[StackEntryItem] = List()
                        (x.item.derived.distinct) foreach ((d) => {
                            val newitem = (derivation get d) match {
                                case Some(q) =>
                                    // assert(!q.derivedFrom.contains(x))
                                    val nq = new StackEntryItem(q.item, this, x :: q.derivedFrom, q.id)
                                    _items = _items.updated(_items.indexOf(q), nq)
                                    nq
                                case None =>
                                    val nq = new StackEntryItem(d, this, List(x))
                                    _items = _items ++ List(nq)
                                    added :+= nq
                                    nq
                            }
                            derivation(d) = newitem
                        })
                        derive(xs ++ added)
                    case _ =>
                }
            }
            derive(kernels)
            _items
        }

        // ==== StackEntryItem ====================================================================
        object StackEntryItem {
            private var unique: Int = 0
            private def nextId = { unique += 1; unique }
        }
        class StackEntryItem(val item: ParsingItem, val generationPoint: StackEntry, val derivedFrom: List[StackEntry#StackEntryItem], _id: Int = StackEntryItem.nextId) {
            // derivedFrom: List[StackEntry.this.StackEntryItem]

            val id = _id
            val belonged = StackEntry.this

            override def equals(other: Any) = other match {
                case that: StackEntryItem => (that canEqual this) && that.item == item && that.generationPoint == generationPoint
                case _ => false
            }
            def canEqual(other: Any) = other.isInstanceOf[StackEntryItem]

            val finishable: Boolean = item finishable

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.StackEntryItem] =
                (item proceed (next, belonged)) map (new belonged.StackEntryItem(_, generationPoint, Nil))

            override def toString = "" + id
        }

        // ==== ParsingItems ======================================================================
        def defItemToState(i: GrElem) = i match {
            case j: CharacterInput => ParsingCharacterInput(j)
            case j: StringInput => ParsingStringInput(j)
            case j: VirtualInput => ParsingVirtualInput(j)
            case j: Nonterminal => ParsingNonterminal(j)
            case j: OneOf => ParsingOneOf(j)
            case j: Repeat => ParsingRepeat(j)
            case j: Sequence => new ParsingSequence(j)
            case j: Except => ParsingExcept(j)
            case j: LookaheadExcept => ParsingLookaheadExcept(j)
        }
        abstract class ParsingItem(val item: GrElem) {
            val finishable: Boolean
            val children: List[ParsedSymbol]

            def derived: List[ParsingItem]

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingItem]

            val enclosingEntry = StackEntry.this

            override def equals(other: Any) = other match {
                case that: ParsingItem => (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item)
                case _ => false
            }
            def canEqual(other: Any) = other.isInstanceOf[ParsingItem]
        }
        abstract class ParsingInput(override val item: InputElem) extends ParsingItem(item) {
            val done: Boolean
            def derived: List[ParsingItem] = Nil
        }
        case class ParsingCharacterInput(override val item: CharacterInput, char: ParsedSymbol = null) extends ParsingInput(item) {
            val done = (char != null)
            val finishable = done
            val children = if (!done) List() else List(char)

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingCharacterInput] = next match {
                case TermSymbol(CharInput(c), _) if (!done && (item acceptable c)) =>
                    List(belonged.ParsingCharacterInput(item, next))
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingCharacterInput =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingCharacterInput]
        }
        case class ParsingStringInput(override val item: StringInput, str: List[ParsedSymbol] = Nil, val pointer: Int = 0) extends ParsingInput(item) {
            val done = pointer >= item.string.length()
            val finishable = (!str.isEmpty) && done
            val children = str

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingStringInput] = if (finishable) Nil else next match {
                case TermSymbol(CharInput(char), _) if (item.string.charAt(pointer) == char) =>
                    List(belonged.ParsingStringInput(item, str ++ List(next), pointer + 1))
                case TermSymbol(TokenInput(token), _) if (str.isEmpty && (token compat item)) =>
                    List(belonged.ParsingStringInput(item, str ++ List(next), item.string.length()))
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingStringInput =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingStringInput]
        }
        case class ParsingVirtualInput(override val item: VirtualInput, virt: ParsedSymbol = null) extends ParsingInput(item) {
            val done = (virt != null)
            val finishable = done
            val children = if (!done) List() else List(virt)

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingVirtualInput] = next match {
                case TermSymbol(_@ VirtInput(v), _) if (!done && v == item.name) =>
                    List(belonged.ParsingVirtualInput(item, next))
                case TermSymbol(TokenInput(token), _) if (!done && (token compat item)) =>
                    List(belonged.ParsingVirtualInput(item, next))
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingVirtualInput =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingVirtualInput]
        }
        case class ParsingNonterminal(override val item: Nonterminal, nonterm: ParsedSymbol = null) extends ParsingItem(item) {
            val done = (nonterm != null)
            val finishable = done
            val children = if (!done) List() else List(nonterm)

            def derived = (if (!done) (grammar.rules(item.name)) else List()) map (defItemToState _)

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingNonterminal] = next match {
                case NontermSymbol(rhs) if (!done && (grammar.rules(item.name) contains rhs.item)) =>
                    List(belonged.ParsingNonterminal(item, next))
                case TermSymbol(TokenInput(token), _) if (!done && (token compat item)) =>
                    List(belonged.ParsingNonterminal(item, next))
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingNonterminal =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingNonterminal]
        }
        case class ParsingOneOf(override val item: OneOf, chosen: ParsedSymbol = null) extends ParsingItem(item) {
            val done = (chosen != null)
            val finishable = done
            val children = if (!done) List() else List(chosen)

            def derived = (if (!done) item.items.toList else List()) map (defItemToState _)

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingOneOf] = next match {
                case NontermSymbol(s) if (!done && (item.items contains s.item)) =>
                    List(belonged.ParsingOneOf(item, next))
                case TermSymbol(TokenInput(token), _) if (!done && (token compat item)) =>
                    List(belonged.ParsingOneOf(item, next))
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingOneOf =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingOneOf]
        }
        case class ParsingRepeat(override val item: Repeat, repeated: List[ParsedSymbol] = Nil) extends ParsingItem(item) {
            val count = repeated.length
            val finishable = (!repeated.isEmpty) && (item.range contains count)
            val children = repeated

            def derived = {
                var _derived: List[GrElem] = List()
                if (item.range canProceed count) _derived :+= item.item

                _derived map (defItemToState _)
            }

            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingRepeat] = next match {
                case NontermSymbol(s) if (item.item == s.item && (item.range canProceed count)) =>
                    List(belonged.ParsingRepeat(item, repeated ++ List(next)))
                // NOTE needs token proceed?
                case _ => Nil
            }

            override def equals(other: Any) = other match {
                case that: ParsingRepeat =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.count == count)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingRepeat]
        }
        case class ParsingSequence(override val item: Sequence, _children: List[ParsedSymbol], nonWS: List[(Int, Int)], pointer: Int) extends ParsingItem(item) {
            // nonWS: index of _children(without whitespace) -> index of children(without whitespace)
            def this(item: Sequence) = this(item, Nil, Nil, 0)
            private val allNullables = {
                def all(l: List[GrElem]): Boolean = l match {
                    case x :: xs => if (!(x nullable)) false else all(xs)
                    case _ => true
                }
                all(item.seq drop pointer)
            }
            val finishable = (!_children.isEmpty) && allNullables && (!nonWS.isEmpty && nonWS.last._1 + 1 == _children.length)

            val children = {
                def pick(indices: List[(Int, Int)], i: Int = 0): List[ParsedSymbol] = {
                    def placeholders(i: Int, c: Int): List[ParsedSymbol] =
                        if (i < c) (EmptySymbol(item.seq(i)) :: placeholders(i + 1, c)) else Nil
                    indices match {
                        case x :: xs =>
                            placeholders(i, x._2) ++ List(_children(x._1)) ++ pick(xs, x._2 + 1)
                        case Nil =>
                            placeholders(i, item.seq.length)
                    }
                }
                pick(nonWS)
            }
            val childrenWithWS = _children
            lazy val indexNonWS = nonWS map (_._1)

            def derived = (if (_children.isEmpty) proceedables else (proceedables ++ item.whitespace)) map (defItemToState _)

            private val proceedables: List[GrElem] = {
                def checkLookaheadNot(except: List[GrElem]): Boolean = {
                    val g = new CompositeGrammar(except)
                    val parser = new Parser(g, input.subinput(enclosingEntry.pointer))
                    def rec: Boolean =
                        if (parser parseStep) {
                            if (!(parser.stack hasNext)) true else {
                                val i = parser.stack.top.kernels
                                // if "parser.stackTop" is like $* (finishing start symbol), it returns false
                                if (i.length == 1 && ((i.head.item) match {
                                    // case n @ ParsingNonterminal(Nonterminal(g.startSymbol), _) if (n.done) => true
                                    // the following "weird" case is 
                                    case n if (n.isInstanceOf[ParsingNonterminal] && {
                                        val pn = n.asInstanceOf[ParsingNonterminal]; (pn.item match {
                                            case Nonterminal(g.startSymbol) => true
                                            case _ => false
                                        }) && pn.done
                                    }) => true
                                    case _ => false
                                })) false
                                else rec
                            }
                        } else true
                    rec
                }
                def propagate(l: List[GrElem]): List[GrElem] = l match {
                    case (x @ LookaheadExcept(except)) :: xs => if (checkLookaheadNot(except)) x :: propagate(xs) else List()
                    case x :: xs => if (x nullable) x :: propagate(xs) else List(x)
                    case Nil => List()
                }
                propagate(item.seq drop pointer).distinct
            }
            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingSequence] =
                next match {
                    case NontermSymbol(x) =>
                        val k = proceedables indexOf x.item
                        if (k >= 0) {
                            List(belonged.ParsingSequence(item, _children ++ List(next), nonWS ++ List((_children.length, pointer + k)), pointer + k + 1))
                        } else {
                            val input = x.item
                            val Sequence(_, midWS) = item

                            if (!_children.isEmpty && (midWS contains input))
                                List(belonged.ParsingSequence(item, _children ++ List(next), nonWS, pointer))
                            else Nil
                        }
                    // NOTE needs token proceed?
                    case _ => Nil
                }

            override def equals(other: Any) = other match {
                case that: ParsingSequence =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingSequence]
        }
        case class ParsingExcept(override val item: Except, child: ParsedSymbol = null) extends ParsingItem(item) {
            private lazy val inputPointer = StackEntry.this.pointer

            val passed = child != null
            val finishable: Boolean = passed
            def proceed(next: ParsedSymbol, belonged: StackEntry): List[belonged.ParsingItem] = next match {
                // check in proceed
                case NontermSymbol(x) if (x.item == item.item) =>
                    // check input is not in item.except
                    val test = new BasicBlackboxParser(new CompositeGrammar(item.except)).parse(ParserInput.fromList(next.source))
                    if (!(test succeed)) List(belonged.ParsingExcept(item, next)) else Nil
                case _ => Nil
            }
            def derived: List[ParsingItem] = if (!passed) List(defItemToState(item.item)) else List()
            val children = List(child)

            override def equals(other: Any) = other match {
                case that: ParsingExcept =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.passed == passed)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingExcept]
        }
        case class ParsingLookaheadExcept(override val item: LookaheadExcept) extends ParsingItem(item) {
            // "lookahead except" items are processed in sequence, so this is just dummy
            val finishable: Boolean = false
            def proceed(next: ParsedSymbol, belonged: StackEntry) = Nil
            def derived: List[ParsingItem] = List()
            val children = List()

            override def equals(other: Any) = other match {
                case that: ParsingLookaheadExcept =>
                    (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[ParsingLookaheadExcept]
        }
    }

    class CompositeGrammar(starting: List[GrElem]) extends Grammar {
        val name: String = "Except"
        val startSymbol: String = {
            def nextSymbol(x: String): String =
                if ((grammar.rules get x) isDefined) nextSymbol(x + "$")
                else x
            nextSymbol("$")
        }
        val rules: Map[String, List[GrElem]] = grammar.rules + ((startSymbol, starting))
    }
}
