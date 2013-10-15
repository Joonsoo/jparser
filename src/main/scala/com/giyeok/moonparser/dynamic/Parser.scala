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
            lazy val failed = resultOpt match { case Some(_: Failed) => true case _ => false }
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
        EntryGroup(Set(EntryInfo(grammar.n(grammar.startSymbol).toParsingItem, None)), 0)
    protected val stack = new OctopusStack(starter)

    type ParseStep = (EntryGroup, Set[ParsedSymbol]) => Boolean

    def proceed(entry: EntryGroup, syms: Set[ParsedSymbol], nextPointer: Int)(failed: ParseStep): Boolean =
        // TODO lifting the items that are finished after proceed
        entry.proceed(syms, nextPointer) match {
            case Some(proceeded) => { stack.add(entry, proceeded); true }
            case None => failed(entry, syms)
        }
    def proceed(entry: EntryGroup, syms: Set[ParsedSymbol])(failed: ParseStep): Boolean =
        proceed(entry, syms, entry.pointer)(failed)

    def defaultFailedHandler(entry: EntryGroup, syms: Set[ParsedSymbol]): Boolean = {
        _result += Parser.Failed(Parser.FailedReason.UnexpectedInput, entry.pointer)
        false
    }
    def defaultAmbiguousHandler(entry: EntryGroup, sym: Set[ParsedSymbol]): Boolean = {
        throw AmbiguousGrammarException(s"ambiguous at ${entry.pointer}")
        false
    }
    def finish(entry: EntryGroup, syms: Set[ParsedSymbol])(failed: ParseStep = defaultFailedHandler, ambiguous: ParseStep = defaultAmbiguousHandler): Boolean = {
        logln(s"Finish ${System.identityHashCode(entry).toHexString}")
        def finishItem(genpoint: Option[EntryGroup], reduced: Set[ParsedSymbol]): Boolean =
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
                    if (input finishedAt entry.pointer) _result ++= reduced map { Parser.Succeed(_) }
                    else _result += Parser.Failed(Parser.FailedReason.UnexpectedEndOfFile, entry.pointer)
                    true
            }
        // TODO modify following to support multiple reductions
        // many possibilities for same GrElem is not allowed => real ambiguity!
        implicit class SetToMapSet[A, B](set: Set[(A, B)]) {
            def toMapSet = set groupBy { _._1 } map { p => (p._1, p._2 map { _._2 }) }
        }
        val finish = entry.finish.toMapSet
        finish.toSeq match {
            case Seq(finitem) => finishItem(finitem._1, finitem._2)
            case Nil =>
                // trying backup items
                val finishBackup = entry.finishBackup.toMapSet
                finishBackup.toSeq match {
                    case Seq(backupitem) => finishItem(backupitem._1, backupitem._2)
                    case Nil => failed(entry, syms)
                    case many => ambiguous(entry, syms)
                }
            case many =>
                // the grammar seems to be ambiguous
                // Generally, this is a grammar error
                // but the user may want to process it in different way
                ambiguous(entry, syms)
        }
    }

    def defaultParseStep(entry: EntryGroup, syms: Set[ParsedSymbol], nextPointer: Int): Boolean =
        proceed(entry, syms, nextPointer)(finish(_, _)())

    def parseStep() =
        if (stack hasNext) {
            val entry = stack.pop()
            val sym = TermSymbol(input at entry.pointer, entry.pointer)

            // There could be several variations here
            // `proceed` and `finish` dual
            // `proceed`, and `finish` if failed

            logln(s"TermSymbol $sym ${input finishedAt sym.pointer}")
            defaultParseStep(entry, Set(sym), input nextPointer entry.pointer)
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
            kernels: Set[EntryInfo],
            pointer: Int) {

        lazy val members: Map[EntryInfo, Entry] = {
            def stabilize(news: Set[EntryInfo], premems: Map[EntryInfo, Entry]): Map[EntryInfo, Entry] = {
                val newmems = news.foldLeft(premems)((mems, info) =>
                    if (mems contains info) mems
                    else mems + (info -> info.contextual(this)))
                val subs = (news flatMap { newmems(_).subs(this) }) -- newmems.keySet
                if (subs.isEmpty) newmems else stabilize(subs, newmems)
            }
            stabilize(kernels, Map())
        }
        assert(kernels subsetOf members.keySet)

        def printMe() = {
            logln(s"=== new EntryGroup ${System.identityHashCode(this).toHexString} ===")
            logln(s"Pointer: $pointer")
            def printItem(e: Entry) =
                logln(s"${e.item}: ${
                    e.genpoint match {
                        case Some(x) => System.identityHashCode(x).toHexString
                        case None => "root"
                    }
                } ${e.finish.isDefined}")
            logln(s"Kernels: ${kernels.size}")
            kernels foreach { k => printItem(members(k)) }
            logln(s"Members: ${members.size}")
            (members.keySet -- kernels) foreach { m => printItem(members(m)) }
            logln("================================")
        }
        if (log) printMe()

        def proceed(sym: Set[ParsedSymbol], pointer: Int): Option[EntryGroup] = {
            logln(s"Proceed ${System.identityHashCode(this).toHexString} $sym")
            val proceeded = (members.values flatMap { _.proceed(sym) }).toSet
            logln(proceeded.toString)
            if (proceeded.isEmpty) None else Some(EntryGroup(proceeded, pointer))
        }
        // mems: members.partition(!_._1.item.isInstanceOf[ParsingBackup])
        private lazy val mems: (Set[Entry], Set[Entry]) =
            members.foldLeft((Set[Entry](), Set[Entry]())) { (acc, entry) =>
                if (entry._1.item.isInstanceOf[ParsingBackup]) (acc._1 + entry._2, acc._2)
                else (acc._1, acc._2 + entry._2)
            }
        lazy val finish: Set[(Option[EntryGroup], ParsedSymbol)] = mems._1 flatMap { _.finish }
        lazy val finishBackup: Set[(Option[EntryGroup], ParsedSymbol)] = mems._2 flatMap { _.finish }

        override lazy val hashCode = (kernels, pointer).hashCode
        override def equals(other: Any) = other match {
            case that: EntryGroup => (that canEqual this) && (kernels == that.kernels) &&
                (pointer == that.pointer)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[EntryGroup]
    }
    case class EntryInfo(item: ParsingItem, genpoint: Option[EntryGroup]) {
        def contextual(context: EntryGroup) = new Entry(item.contextual(context), genpoint)

        override lazy val hashCode = (item, genpoint).hashCode
    }
    class Entry(val item: ContextualItem, val genpoint: Option[EntryGroup]) {
        def subs(genpoint: EntryGroup): Set[EntryInfo] =
            item.subs map { EntryInfo(_, Some(genpoint)) }
        def proceed(sym: Set[ParsedSymbol]): Set[EntryInfo] = {
            val proceeded: Set[ParsingItem] = item proceed sym
            if (proceeded.isEmpty) Set()
            else {
                logln(s"proceeded: $proceeded")
                (proceeded map { EntryInfo(_, genpoint) }).toSet
            }
        }
        lazy val finish: Option[(Option[EntryGroup], ParsedSymbol)] = item.finish match {
            case Some(fin) => Some(genpoint, fin)
            case None => None
        }
    }
}
