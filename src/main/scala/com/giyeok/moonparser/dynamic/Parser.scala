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

    implicit class SetToMapSet[A, B](set: Set[(A, B)]) {
        def toMapSet = set groupBy { _._1 } map { p => (p._1, p._2 map { _._2 }) }
    }

    def logln(str: => String): Unit = if (log) println(str)

    protected val starter =
        EntryGroup(0, Set(Entry(grammar.n(grammar.startSymbol).toParsingItem, None)), Set())
    protected val stack = new OctopusStack(starter)

    type ParseStep = (EntryGroup, Set[ParsedSymbol]) => Boolean

    def proceed(entry: EntryGroup, syms: Set[ParsedSymbol], nextPointer: Int)(failed: ParseStep): Boolean = {
        def lift(lifting: Set[Entry], lifted: Set[Entry]): Set[Entry] = {
            val fin = lifting flatMap { _.finish }
            val pro = (fin.toMapSet flatMap {
                case (Some(genpoint), syms) => genpoint proceed syms
                case _ => Set[Entry]()
            }).toSet
            val next = lifted ++ pro
            if (next != lifted) lift(pro, next) else next
        }
        entry proceed syms match {
            case proceeded if !proceeded.isEmpty =>
                stack.add(entry, EntryGroup(nextPointer, proceeded, lift(proceeded, Set())))
                true
            case _ => failed(entry, syms)
        }
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
                    val proceeded = genpoint proceed reduced
                    if (proceeded.isEmpty) logln(s"assertion: $reduced at ${System.identityHashCode(genpoint).toHexString}")
                    assert(!proceeded.isEmpty)
                    stack.add(genpoint, EntryGroup(entry.pointer, proceeded, Set()))
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
            pointer: Int,
            kernels: Set[Entry],
            liftedKernels: Set[Entry]) {

        lazy val (members, lifted): (Set[Entry], Set[Entry]) = {
            def stabilize(members: Set[Entry]): Set[Entry] = {
                val subs = members ++ (members flatMap { _.subs(this) })
                if (subs == members) subs else stabilize(subs)
            }
            (stabilize(kernels), stabilize(liftedKernels))
        }
        assert(kernels subsetOf members)

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
            kernels foreach printItem
            logln(s"Members: ${members.size}")
            (members -- kernels) foreach printItem
            logln(s"Lifted: ${lifted.size}")
            lifted foreach printItem
            logln("================================")
        }
        if (log) printMe()

        def proceed(sym: Set[ParsedSymbol]): Set[Entry] = {
            logln(s"Proceed ${System.identityHashCode(this).toHexString} $sym")
            ((members ++ lifted) flatMap { _.proceed(sym) }).toSet
        }
        // mems: members.partition(!_._1.item.isInstanceOf[ParsingBackup])
        private lazy val finmems: (Set[Entry], Set[Entry]) =
            members.foldLeft((Set[Entry](), Set[Entry]())) { (acc, entry) =>
                if (entry.item.isInstanceOf[ParsingBackup]) (acc._1 + entry, acc._2)
                else (acc._1, acc._2 + entry)
            }
        lazy val finish: Set[(Option[EntryGroup], ParsedSymbol)] = finmems._1 flatMap { _.finish }
        lazy val finishBackup: Set[(Option[EntryGroup], ParsedSymbol)] = finmems._2 flatMap { _.finish }

        override lazy val hashCode = (pointer, kernels, liftedKernels).hashCode
    }
    case class Entry(item: ParsingItem, genpoint: Option[EntryGroup]) {
        def subs(genpoint: EntryGroup): Set[Entry] =
            item.subs map { Entry(_, Some(genpoint)) }
        def proceed(sym: Set[ParsedSymbol]): Set[Entry] = {
            val proceeded: Set[ParsingItem] = item proceed sym
            if (proceeded.isEmpty) Set()
            else {
                logln(s"proceeded: $proceeded")
                (proceeded map { Entry(_, genpoint) }).toSet
            }
        }
        lazy val finish: Option[(Option[EntryGroup], ParsedSymbol)] = item.finish match {
            case Some(fin) => Some(genpoint, fin)
            case None => None
        }

        override lazy val hashCode = (item, genpoint).hashCode
    }
}
