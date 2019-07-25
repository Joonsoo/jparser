package com.giyeok.jparser.gramgram

import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Grammar, ParseForestFunc}

object MetaGrammar2 extends Grammar {
    private val grammar = MetaGrammar.translateForce("Meta Grammar 2",
        """Grammar = WS [TypeDefs WS]? Rules WS
          |TypeDefs = TypeDef [WS TypeDef]*
          |Rules = Rule [WS Rule]*
          |
          |TypeDef = '@' ClassDef
          |  | '@' SuperDef
          |ClassDef = TypeName WS '(' WS [ClassParams WS]? ')'
          |SuperDef = TypeName WS '{' WS [SubTypes WS]? '}'
          |ClassParams = ClassParam [WS ',' WS ClassParam]*
          |ClassParam = ParamName [WS ':' WS TypeDesc]?
          |ParamName = Id
          |TypeDesc = ValueTypeDesc [WS '?']?
          |ValueTypeDesc = TypeName
          |  | '[' WS TypeDesc WS ']'
          |  | '{' WS EnumList WS '}'
          |EnumList = StringLiteral [WS ',' WS StringLiteral]*
          |SubTypes = SubType [WS ',' WS SubType]*
          |SubType = StringLiteral | TypeName | ClassDef | SuperDef
          |
          |Rule = LHS WS '=' WS RHSs
          |LHS = Nonterminal [WS ':' WS TypeDesc]?
          |RHSs = RHS [WS '|' WS RHS]*
          |RHS = Elem [WS Elem]*
          |Elem = Processor | Symbol
          |
          |Processor = Ref
          |  | '{' WS PExpr WS '}'
          |PExpr = PExpr WS BinOp WS PTerm
          |  | PTerm
          |BinOp = <("+" | "++")>
          |PTerm = Ref
          |  | BoundPExpr
          |  | ConstructExpr
          |  | '(' WS PExpr WS ')'
          |  | '[' WS [PExpr [WS ',' WS PExpr]* WS]? ']'
          |Ref = '$' RefIdx
          |BoundPExpr = '$' RefIdx BoundedPExpr
          |BoundedPExpr = Ref
          |  | BoundPExpr
          |  | '{' WS PExpr WS '}'
          |ConstructExpr = TypeName WS ConstructParams
          |  | ConstructWithTypeDefExpr
          |ConstructParams = '(' WS [PExpr [WS ',' WS PExpr]* WS]? ')'
          |ConstructWithTypeDefExpr = '@' TypeName [WS SuperTypes]? WS ConstructParamsWithType
          |SuperTypes = '<' WS TypeName [WS ',' WS TypeName]* WS '>'
          |ConstructParamsWithType = '(' WS [PExprWithType [WS ',' WS PExprWithType]* WS]? ')'
          |PExprWithType = PExpr [WS ':' WS TypeDesc]?
          |
          |Symbol = BinSymbol
          |BinSymbol = BinSymbol WS '&' WS PreUnSymbol
          |  | BinSymbol WS '-' WS PreUnSymbol
          |  | PreUnSymbol
          |PreUnSymbol = '^' WS PreUnSymbol
          |  | '!' WS PreUnSymbol
          |  | PostUnSymbol
          |PostUnSymbol = PostUnSymbol WS '?'
          |  | PostUnSymbol WS '*'
          |  | PostUnSymbol WS '+'
          |  | AtomSymbol
          |AtomSymbol = Terminal
          |  | TerminalChoice
          |  | StringLiteral
          |  | Nonterminal
          |  | '(' InPlaceChoice ')'
          |  | '<' InPlaceChoice '>'
          |  | EmptySequence
          |InPlaceChoice = InPlaceSequence [WS '|' WS InPlaceSequence]*
          |InPlaceSequence = Symbol [WS Symbol]*
          |EmptySequence = '#'
          |Nonterminal = Id
          |Terminal = '\'' TerminalChar '\''
          |  | '.'
          |TerminalChoice = '\'' TerminalChoiceChar (TerminalChoiceChar | TerminalChoiceRange)+ '\''
          |  | '\'' TerminalChoiceRange '\''
          |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar
          |StringLiteral = '"' StringChar* '"'
          |
          |UnicodeChar = '\\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}
          |TerminalChar = .-'\\'
          |    | '\\' {"'\\bnrt}
          |    | UnicodeChar
          |TerminalChoiceChar = .-{'\-\\}
          |    | '\\' {"'\-\\bnrt}
          |    | UnicodeChar
          |StringChar = .-{"\\}
          |    | '\\' {"'\\bnrt}
          |    | UnicodeChar
          |
          |StringLiteral = '"' {a-zA-Z}* '"'
          |TypeName = Id
          |RefIdx = <{0-9}+>
          |Id = <{a-zA-Z}+>
          |WS = { \n\r\t}*
        """.stripMargin)

    sealed trait AST

    object AST {

        case class Grammar(typeDefs: Seq[TypeDef], rules: Seq[Rule])

        sealed trait TypeDef

        case class ClassDef(name: String, classParams: Seq[ClassParam]) extends TypeDef with SubType

        case class SuperDef(name: String, subTypes: Seq[SubType]) extends TypeDef with SubType

        sealed trait SubType

        case class SubEnum(value: String) extends SubType

        case class ClassParam(paramName: String, typeDesc: TypeDesc)

        case class TypeDesc(valueTypeDesc: ValueTypeDesc, optional: Boolean)

        sealed trait ValueTypeDesc

        case class TypeName(name: String) extends ValueTypeDesc with SubType

        case class ArrayType(elemType: TypeDesc)

        case class EnumList(values: Seq[String])

        case class Rule(lhs: LHS, rhs: Seq[RHS])

        case class LHS(name: String, typeDesc: Option[TypeDesc])

        case class RHS(elems: List[Elem])

        sealed trait Elem

        sealed trait Processor extends Elem

        case class Ref(id: String) extends Processor with PTerm

        sealed trait PExpr extends Processor

        case class PBinExpr(lhs: PExpr, binOp: String, rhs: PTerm) extends PExpr

        sealed trait PTerm extends PExpr

        case class BoundPExpr(ref: Ref, expr: PExpr) extends PTerm

        case class ConstructExpr(typeName: String, params: List[PExpr]) extends PTerm

        case class ConstructWithTypeDefExpr() extends PTerm // TODO

        case class ParenTerm(expr: PExpr) extends PTerm

        case class ListExpr(elems: Seq[PExpr]) extends PTerm

        sealed trait Symbol extends Elem

        sealed trait BinSymbol extends Symbol

        case class JoinSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        case class ExceptSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        sealed trait PreUnSymbol extends BinSymbol

        case class FollowedBySymbol(symbol: PreUnSymbol) extends PreUnSymbol

        case class NotFollowedBySymbol(symbol: PreUnSymbol) extends PreUnSymbol

        sealed trait PostUnSymbol extends PreUnSymbol

        case class Repeat(symbol: PostUnSymbol, repeatSpec: String) extends PostUnSymbol

        sealed trait AtomSymbol extends PostUnSymbol

        case class Terminal(c: Char) extends AtomSymbol

        case class TerminalChoice(c: Seq[Range]) extends AtomSymbol

        case class StringLiteral(s: String) extends AtomSymbol

        case class Nonterminal(name: String) extends AtomSymbol

        case class Choice(choices: Seq[Symbol]) extends AtomSymbol

        case class Longest(choices: Seq[Symbol]) extends AtomSymbol

        object EmptySequence extends AtomSymbol

    }

    val name: String = grammar.name
    val rules: RuleMap = grammar.rules
    val startSymbol: Nonterminal = grammar.startSymbol

    def main(args: Array[String]): Unit = {
        println(grammar.rules)
        val ngrammar = NGrammar.fromGrammar(grammar)
        val parser = new NaiveParser(ngrammar)

        val result = parser.parse("A = b c d")

        result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
                reconstructor.reconstruct() match {
                    case Some(parseTree) => println(parseTree.trees.head.toHorizontalHierarchyString)
                    case None => println("Incomplete input")
                }
            case Right(error) =>
                println(error)
        }

        // 문법이 주어지면
        // 1a. processor가 없는 문법 텍스트
        // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
        // 1c. (나중엔) 제너레이트된 파서
        // 2. 정의된 타입들을 정의하는 자바 코드
        // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드
    }
}
