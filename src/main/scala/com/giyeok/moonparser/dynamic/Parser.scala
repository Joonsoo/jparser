package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.ParserInputs._
import com.giyeok.moonparser.InputPieces._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.AmbiguousGrammarException

trait BlackboxParser {
    def parse(input: ParserInput): Parser.Result
    def parse(input: String): Parser.Result
}
object Parser {
    type Result = Set[ParsePossibility]
    object Result {
        implicit class ResultHelper(set: Result) {
            lazy val resultOpt = if (set.size == 1) Some(set.toSeq.head) else None
            lazy val parsedOpt = resultOpt match {
                case Some(Succeed(parsed)) => Some(parsed)
                case _ => None
            }
            lazy val succeed = parsedOpt.isDefined
            lazy val ambiguous = set.size > 1

            def textEq(text: String) = this.parsedOpt match {
                case Some(cs: ConcreteSymbol) if cs.text == text => true
                case _ => false
            }
        }
    }
    sealed abstract class ParsePossibility
    case class Failed(reason: FailedReason.Value, location: Int, message: String = "") extends ParsePossibility {
        override lazy val hashCode = (reason, location, message).hashCode
        override def equals(other: Any) = other match {
            case that: Failed =>
                (that canEqual this) && (reason == that.reason) && (location == that.location) && (message == that.message)
            case _ => false
        }
        def canEqual(other: Any) = other.isInstanceOf[Failed]
    }
    case class Succeed(parsed: ParsedSymbol) extends ParsePossibility {
        override lazy val hashCode = parsed.hashCode
        override def equals(other: Any) = other match {
            case that: Succeed => (that canEqual this) && (parsed == that.parsed)
            case _ => false
        }
        def canEqual(other: Any) = other.isInstanceOf[Succeed]
    }

    object FailedReason extends Enumeration {
        val UnexpectedInput, UnexpectedEndOfFile = Value
    }
}
class BasicBlackboxParser(val grammar: Grammar) extends BlackboxParser {
    // === parser ===
    def parse(input: ParserInput): Parser.Result = {
        val parser = new Parser(grammar, input)
        parser.parseAll
    }
    def parse(input: String): Parser.Result = parse(ParserInput.fromString(input))
}
class Parser(val grammar: Grammar, val input: ParserInput, val log: Boolean = false)
        extends ParsingItems with IsNullable with OctopusStacks {
    private var _result: Parser.Result = Set()
    def result = _result

    def logln(str: => String): Unit = if (log) println(str)

    protected val starter =
        EntryGroup(Set(Entry(grammar.n(grammar.startSymbol).toParsingItem, None)), StartSymbol, 0)
    protected val stack = new OctopusStack(starter)

    type ParseStep = (EntryGroup, ParsedSymbol) => Boolean

    def proceed(entry: EntryGroup, sym: ParsedSymbol, nextPointer: Int)(failed: ParseStep): Boolean =
        entry.proceed(sym, nextPointer) match {
            case Some(proceeded) => { stack.add(entry, proceeded); true }
            case None => failed(entry, sym)
        }
    def proceed(entry: EntryGroup, sym: ParsedSymbol)(failed: ParseStep): Boolean =
        proceed(entry, sym, entry.pointer)(failed)

    def defaultFailedHandler(entry: EntryGroup, sym: ParsedSymbol): Boolean = {
        _result += Parser.Failed(Parser.FailedReason.UnexpectedInput, entry.pointer)
        false
    }
    def defaultAmbiguousHandler(entry: EntryGroup, sym: ParsedSymbol): Boolean = {
        throw AmbiguousGrammarException(s"ambiguous at ${entry.pointer}")
        false
    }
    def finish(entry: EntryGroup, sym: ParsedSymbol)(failed: ParseStep = defaultFailedHandler, ambiguous: ParseStep = defaultAmbiguousHandler): Boolean = {
        logln(s"Finish ${System.identityHashCode(entry).toHexString}")
        def finishItem(genpoint: Option[EntryGroup], reduced: ParsedSymbol): Boolean =
            genpoint match {
                case Some(genpoint) =>
                    logln(s"Genpoint: ${System.identityHashCode(genpoint).toHexString}")
                    val proceeded = genpoint.proceed(reduced, entry.pointer)
                    if (proceeded.isEmpty) logln(s"assertion: $reduced at ${System.identityHashCode(genpoint).toHexString}")
                    assert(proceeded.isDefined)
                    stack.add(genpoint, proceeded.get)
                    true
                case None =>
                    // parsing may be finished
                    // it succeed if entry reaches to the end of input, and failed otherwise
                    logln(s"Trying to finish parsing as $reduced")
                    if (input finishedAt entry.pointer) _result += Parser.Succeed(reduced)
                    else _result += Parser.Failed(Parser.FailedReason.UnexpectedEndOfFile, entry.pointer)
                    true
            }
        entry.finish.toSeq match {
            case Seq(finitem) => finishItem(finitem._1, finitem._2)
            case Nil =>
                // trying backup items
                entry.finishBackup.toSeq match {
                    case Seq(backupitem) => finishItem(backupitem._1, backupitem._2)
                    case Nil => failed(entry, sym)
                    case many => ambiguous(entry, sym)
                }
            case many =>
                // the grammar seems to be ambiguous
                // Generally, this is a grammar error
                // but the user may want to process it in different way
                ambiguous(entry, sym)
        }
    }

    def defaultParseStep(entry: EntryGroup, sym: ParsedSymbol, nextPointer: Int): Boolean =
        proceed(entry, sym, nextPointer)((entry, sym) =>
            finish(entry, sym)((entry, _) =>
                throw AmbiguousGrammarException(s"ambiguous at ${entry.pointer}")))

    def parseStep() =
        if (stack hasNext) {
            val entry = stack.pop()
            val sym = TermSymbol(input at entry.pointer, entry.pointer)

            // There could be several variations here
            // `proceed` and `finish` dual
            // `proceed`, and `finish` if failed

            logln(s"TermSymbol $sym ${input finishedAt sym.pointer}")
            defaultParseStep(entry, sym, input nextPointer entry.pointer)
        } else {
            false
        }
    def parseAll() = {
        while (parseStep()) ()
        result
    }

    case class EntryGroup(
            // parent is not Entry's problem, but it's OctopusStack's thing
            // genpoint will be changed if the item is created by `subs` of ParsingItem
            // and preserved if the item is created by `proceed` of ParsingItem
            kernels: Set[Entry],
            symbol: ParsedSymbol,
            pointer: Int) {

        lazy val members: Set[Entry] = {
            def stabilize(members: Set[Entry]): Set[Entry] = {
                val subs = members ++ (members flatMap { _.subs(this) })
                if (subs == members) subs else stabilize(subs)
            }
            stabilize(kernels)
        }

        def printMe() = {
            logln(s"=== new EntryGroup ${System.identityHashCode(this).toHexString} ===")
            symbol match {
                case cs: ConcreteSymbol => logln(s"Symbol: ${cs.text}")
                case _ =>
            }
            logln(s"Pointer: $pointer")
            def printItem(e: Entry) =
                logln(s"${e.item}: ${
                    e.genpoint match {
                        case Some(x) => System.identityHashCode(x).toHexString
                        case None => "root"
                    }
                } ${e.finish.isDefined}")
            logln(s"Kernels: ${kernels.size}")
            kernels foreach printItem
            logln(s"Members: ${members.size}")
            (members -- kernels) foreach printItem
            logln("================================")
        }
        if (log) printMe()

        def proceed(sym: ParsedSymbol, pointer: Int): Option[EntryGroup] = {
            logln(s"Proceed ${System.identityHashCode(this).toHexString} $sym")
            val proceeded = members flatMap { _.proceed(sym) }
            logln(proceeded.toString)
            if (proceeded.isEmpty) None else Some(EntryGroup(proceeded, sym, pointer))
        }
        private lazy val mems = members partition { !_.item.isInstanceOf[ParsingBackup] }
        lazy val finish: Set[(Option[EntryGroup], ParsedSymbol)] = mems._1 flatMap { _.finish }
        lazy val finishBackup: Set[(Option[EntryGroup], ParsedSymbol)] = mems._2 flatMap { _.finish }

        override lazy val hashCode = (kernels, symbol, pointer).hashCode
        override def equals(other: Any) = other match {
            case that: EntryGroup => (that canEqual this) && (kernels == that.kernels) &&
                (symbol == that.symbol) && (pointer == that.pointer)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[EntryGroup]
    }
    case class Entry(item: ParsingItem, genpoint: Option[EntryGroup]) {
        def subs(genpoint: EntryGroup): Set[Entry] = item.subs map { Entry(_, Some(genpoint)) }
        def proceed(sym: ParsedSymbol): Option[Entry] = {
            (item proceed sym) match {
                case Some(proceeded) =>
                    logln(s"proceeded: $proceeded")
                    Some(Entry(proceeded, genpoint))
                case None => None
            }
        }
        lazy val finish: Option[(Option[EntryGroup], ParsedSymbol)] = item.finish match {
            case Some(fin) => Some(genpoint, fin)
            case None => None
        }

        override lazy val hashCode = (item, genpoint).hashCode
        override def equals(other: Any) = other match {
            case that: Entry => (that canEqual this) && (item == that.item) && (genpoint == that.genpoint)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Entry]
    }
}
