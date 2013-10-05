package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.ParserInputs._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.GrElems._

class ParseResult(val messages: List[ParsePossibility]) {
    def add(p: ParsePossibility) =
        new ParseResult(p :: messages)

    val succeeds = (messages map (_ match {
        case x: ParseSuccess => Some(x)
        case _ => None
    })).flatten
    val ambiguous = succeeds.length > 1
    val succeed = succeeds.length == 1
    val parsed: Option[ConcreteSymbol] = if (succeed) Some(succeeds.head.parsed) else None
}
sealed abstract class ParsePossibility
case class ParseFailed(reason: String, location: Int) extends ParsePossibility
case class ParseSuccess(parsed: ConcreteSymbol) extends ParsePossibility

trait BlackboxParser {
    def parse(input: ParserInput): ParseResult
    def parse(input: String): ParseResult
}
class BasicBlackboxParser(val grammar: Grammar) extends BlackboxParser {
    // === parser ===
    def parse(input: ParserInput): ParseResult = {
        val parser = new Parser(grammar, input)
        parser.parseAll
    }
    def parse(input: String): ParseResult = parse(ParserInput.fromString(input))
}
class Parser(val grammar: Grammar, val input: ParserInput)
        extends ParsingItems with OctopusStacks {
    private var _result: ParseResult = new ParseResult(Nil)
    def result = _result

    protected val starter = None
    protected val stack = new OctopusStack(starter)

    def parseStep() =
        if (stack hasNext) {
            /*
            val entry = stack.pop()
            val pointer = entry.pointer
            val fin = entry finished
            val term = TermSymbol(input at pointer, pointer)

            def pushFinished(f: List[entry.StackEntryItem]): List[StackEntry] =
                f flatMap ((x) =>
                    if (x.item finishable) {
                        if (x.generationPoint == null) {
                            // Parsing finished
                            if ((input at pointer) == EOFSymbol) {
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
            */
            true
        } else {
            false
        }
    def parseAll() = {
        while (parseStep()) ()
        result
    }

    case class Entry(
        // parent is not Entry's problem, but it's OctopusStack's thing
        // genpoint will be changed if the item is created by `subs` of ParsingItem
        // and preserved if the item is created by `proceed` of ParsingItem
        genpoint: Option[Entry],
        members: Seq[ParsingItem],
        symbol: ConcreteSymbol,
        pointer: Int)
}
