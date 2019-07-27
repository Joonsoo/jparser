package com.giyeok.jparser.gramgram

import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.NGrammar.{NNonterminal, NProxy, NRepeat, NStart}
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Grammar, ParseForestFunc}

object MetaGrammar2 extends Grammar {
    private val grammar = MetaGrammar.translateForce("Meta Grammar 2",
        """Grammar = WS Def [WS Def]* WS
          |Def = Rule | TypeDef
          |
          |TypeDef = '@' ClassDef
          |  | '@' SuperDef
          |ClassDef = TypeName WS '(' WS [ClassParams WS]? ')'
          |SuperDef = TypeName WS '{' WS [SubTypes WS]? '}'
          |TypeName = Id
          |ClassParams = ClassParam [WS ',' WS ClassParam]*
          |ClassParam = ParamName [WS ':' WS TypeDesc]?
          |ParamName = Id
          |TypeDesc = ValueTypeDesc [WS '?']?
          |ValueTypeDesc = TypeName
          |  | OnTheFlyTypeDef
          |  | '[' WS TypeDesc WS ']'
          |SubTypes = SubType [WS ',' WS SubType]*
          |SubType = TypeName | ClassDef | SuperDef
          |
          |OnTheFlyTypeDef = '@' WS TypeName [WS SuperTypes]?
          |SuperTypes = '<' WS TypeName [WS ',' WS TypeName]* WS '>'
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
          |BinOp = "+"
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
          |  | OnTheFlyTypeDefConstructExpr
          |ConstructParams = '(' WS [PExpr [WS ',' WS PExpr]* WS]? ')'
          |OnTheFlyTypeDefConstructExpr = OnTheFlyTypeDef WS ConstructParamsWithType
          |ConstructParamsWithType = '(' WS [PExprWithType [WS ',' WS PExprWithType]* WS]? ')'
          |PExprWithType = ParamName [WS ':' WS TypeDesc]? WS '=' PExpr
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
          |  | '(' InPlaceChoices ')'
          |  | '<' InPlaceChoices '>'
          |  | EmptySequence
          |InPlaceChoices = InPlaceSequence [WS '|' WS InPlaceSequence]*
          |InPlaceSequence = Symbol [WS Symbol]*
          |EmptySequence = '#'
          |Nonterminal = Id
          |Terminal = '\'' TerminalChar '\''
          |  | '.'
          |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\''
          |  | '\'' TerminalChoiceRange '\''
          |TerminalChoiceElem = TerminalChoiceChar | TerminalChoiceRange
          |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar
          |StringLiteral = '"' StringChar* '"'
          |
          |UnicodeChar = '\\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}
          |TerminalChar = .-{\\}
          |  | '\\' {\'\\bnrt}
          |  | UnicodeChar
          |TerminalChoiceChar = .-{\'\-\\}
          |  | '\\' {\'\-\\bnrt}
          |  | UnicodeChar
          |StringChar = .-{"\\}
          |  | '\\' {"\\bnrt}
          |  | UnicodeChar
          |
          |StringLiteral = '"' StringChar* '"'
          |RefIdx = <('0' | [{1-9} {0-9}*])>
          |Id = <[{a-zA-Z} {a-zA-Z0-9}*]>
          |WS = ({ \n\r\t} | LineComment)*
          |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
          |EOF = !.
        """.stripMargin)

    sealed trait AST

    object AST {

        class Node()

        case class Grammar(defs: List[Def])

        sealed trait Def

        sealed trait TypeDef extends Def

        case class ClassDef(typeName: TypeName, params: List[ClassParam]) extends TypeDef

        case class SuperDef(typeName: TypeName, subs: List[SubType]) extends TypeDef

        case class TypeName(name: Node) extends ValueTypeDesc

        case class ClassParam(name: Node, typeDesc: Option[Node])

        case class TypeDesc(typ: ValueTypeDesc, optional: Boolean) extends ValueTypeDesc

        sealed trait ValueTypeDesc

        sealed trait SubType

        case class OnTheFlyTypeDef(name: TypeName, supers: List[TypeName]) extends ValueTypeDesc

        case class Rule(lhs: LHS, rhs: List[List[Elem]]) extends Def

        case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

        sealed trait Elem

        sealed trait Processor

        sealed trait PExpr extends BoundedPExpr

        case class BinOpExpr(op: Node, lhs: PExpr, rhs: PTerm) extends PExpr

        sealed trait PTerm extends PExpr

        case class Ref(idx: Node) extends PTerm with BoundedPExpr

        case class BoundPExpr(ctx: Ref, expr: BoundedPExpr) extends PTerm with BoundedPExpr

        sealed trait BoundedPExpr

        sealed trait AbstractConstructExpr

        case class ConstructExpr(typ: TypeName, params: List[PExpr]) extends PTerm

        case class PTermParen(expr: PExpr) extends PTerm

        case class PTermSeq(elems: List[PExpr]) extends PTerm

        case class OnTheFlyTypeDefConstructExpr(typeDef: OnTheFlyTypeDef, params: List[NamedParam]) extends BoundedPExpr

        case class NamedParam(name: Node, typeDesc: Option[TypeDesc], expr: PExpr)

        sealed trait Symbol

        sealed trait BinSymbol extends Symbol

        case class JoinSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        case class ExceptSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        sealed trait PreUnSymbol extends BinSymbol

        case class FollowedBy(expr: PreUnSymbol) extends PreUnSymbol

        case class NotFollowedBy(expr: PreUnSymbol) extends PreUnSymbol

        sealed trait PostUnSymbol extends PreUnSymbol

        case class Repeat(expr: PostUnSymbol, repeat: Node) extends PostUnSymbol

        sealed trait AtomSymbol extends PostUnSymbol

        case class Paren(choices: List[InPlaceChoices]) extends AtomSymbol

        case class Longest(choices: List[InPlaceChoices]) extends AtomSymbol

        object EmptySeq extends AtomSymbol

        case class InPlaceChoices(choices: List[InPlaceSequence]) extends AtomSymbol

        case class InPlaceSequence(seq: List[Symbol]) extends AtomSymbol

        case class Nonterminal(name: Node) extends AtomSymbol

        sealed trait Terminal extends AtomSymbol

        case class TerminalChar(char: Node) extends Terminal

        object AnyTerminal extends Terminal

        case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

        sealed trait TerminalChoiceElem

        case class TerminalChoiceRange(start: Node, end: Node) extends TerminalChoiceElem

        case class StringLiteral(value: Node) extends AtomSymbol

    }

    val name: String = grammar.name
    val rules: RuleMap = grammar.rules
    val startSymbol: Nonterminal = grammar.startSymbol

    val selfGrammar: String =
        """Grammar = WS Def (WS Def)* WS {@Grammar(defs=[$1 + $2$1])}
          |Def: @Def = Rule | TypeDef
          |
          |TypeDef: @TypeDef = '@' ClassDef
          |  | '@' SuperDef
          |ClassDef = TypeName WS '(' WS (ClassParams WS)? ')' {@ClassDef(typeName=$0, params=$4$0)}
          |SuperDef = TypeName WS '{' WS (SubTypes WS)? '}' {@SuperDef(typeName=$0, subs=$4$0)}
          |TypeName = Id
          |ClassParams = ClassParam (WS ',' WS ClassParam)* {[$0] + $1$3}
          |ClassParam = ParamName (WS ':' WS TypeDesc)? {@ClassParam(name=$0, typeDesc=$1$3)}
          |ParamName = Id
          |TypeDesc = ValueTypeDesc (WS '?')? {@TypeDesc(type=$0, optional:bool=$1)}
          |ValueTypeDesc: @ValueTypeDesc = TypeName
          |  | OnTheFlyTypeDef
          |  | '[' WS TypeDesc WS ']' $2
          |SubTypes = SubType (WS ',' WS SubType)*
          |SubType: @SubType = TypeName | ClassDef | SuperDef
          |
          |OnTheFlyTypeDef = '@' WS TypeName (WS OnTheFlySuperTypes)? {@OnTheFlyTypeDef(name=$2, supers=$4$1)}
          |OnTheFlySuperTypes = '<' WS TypeName (WS ',' WS TypeName)* WS '>' {[$2] + $3$3}
          |
          |Rule = LHS WS '=' WS RHSs {@Rule(lhs=$0, rhs=$4)}
          |LHS = Nonterminal (WS ':' WS TypeDesc)? {@LHS(name=$0, typeDesc=$1$3)}
          |RHSs = RHS (WS '|' WS RHS)* {[$0] + $1$3}
          |RHS: [Elem] = Elem (WS Elem)* {[$0] + $1$1}
          |Elem: @Elem = Processor | Symbol
          |
          |Processor: @Processor = Ref
          |  | '{' WS PExpr WS '}' $2
          |PExpr: @PExpr = PExpr WS <BinOp> WS PTerm {@BinOpExpr(op=$2, lhs=$0, rhs=$1)}
          |  | PTerm
          |BinOp = "+"
          |PTerm: @PTerm = Ref
          |  | BoundPExpr
          |  | ConstructExpr
          |  | '(' WS PExpr WS ')' {@PTermParen(expr=$2)}
          |  | '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {@PTermSeq(elems=$2{[$0] + $1$3})}
          |Ref = '$' RefIdx {@Ref(idx=$1)}
          |BoundPExpr = Ref BoundedPExpr {@BoundPExpr(ctx=$0, expr:@BoundedPExpr=$1)}
          |// type(BoundedPExpr)는 모두 BoundedPExpr의 subclass여야 함
          |BoundedPExpr = Ref
          |  | BoundPExpr
          |  | '{' WS PExpr WS '}' $2
          |// Ref, BoundPExpr, PExpr은 모두 BoundedPExpr의 subclass여야 함
          |ConstructExpr: @AbstractConstructExpr = TypeName WS ConstructParams {@ConstructExpr(type=$0, params=$2)}
          |  | OnTheFlyTypeDefConstructExpr
          |ConstructParams = '(' WS (PExpr (WS ',' WS PExpr)* WS)? ')' {$2{[$0] + $1$3}}
          |OnTheFlyTypeDefConstructExpr = OnTheFlyTypeDef WS NamedParams {@OnTheFlyTypeDefConstructExpr(typeDef=$0, params=$2)}
          |NamedParams = '(' WS (NamedParam (WS ',' WS NamedParam)* WS)? ')' {$2{[$0] + $1$3}}
          |NamedParam = ParamName (WS ':' WS TypeDesc)? WS '=' PExpr {@NamedParam(name=$0, typeDesc=$1$3, expr=$4)}
          |
          |Symbol: @Symbol = BinSymbol
          |BinSymbol: @BinSymbol = BinSymbol WS '&' WS PreUnSymbol {@JoinSymbol(symbol1=$0, symbol2=$4)}
          |  | BinSymbol WS '-' WS PreUnSymbol {@ExceptSymbol(symbol1=$0, symbol2=$4)}
          |  | PreUnSymbol
          |PreUnSymbol: @PreUnSymbol = '^' WS PreUnSymbol {@FollowedBy(expr=$2)}
          |  | '!' WS PreUnSymbol {@NotFollowedBy(expr=$2)}
          |  | PostUnSymbol
          |PostUnSymbol: @PostUnSymbol = PostUnSymbol WS ('?' | '*' | '+') {@Repeat(expr=$0, repeat=$2)}
          |  | AtomSymbol
          |AtomSymbol: @AtomSymbol = Terminal
          |  | TerminalChoice
          |  | StringLiteral
          |  | Nonterminal
          |  | '(' InPlaceChoices ')' {@Paren(choices=$1)}
          |  | '<' InPlaceChoices '>' {@Longest(choices=$1)}
          |  | EmptySequence {@EmptySeq()}
          |InPlaceChoices = InPlaceSequence (WS '|' WS InPlaceSequence)* {$InPlaceChoices(choices=[$0] + $1$3)}
          |InPlaceSequence = Symbol (WS Symbol)* {@InPlaceSequence(seq=[$0] + $1$1)}
          |EmptySequence = '#'
          |Nonterminal = Id {@Nonterminal(name=$0)}
          |Terminal: @Terminal = '\'' TerminalChar '\'' {@TerminalChar(char=$2)}
          |  | '.' {@AnyTerminal()}
          |@TerminalChoice(choices: [TerminalChoiceElem])
          |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {TerminalChoice([$1] + $2)}
          |  | '\'' TerminalChoiceRange '\'' {TerminalChoice([$1])}
          |TerminalChoiceElem: @TerminalChoiceElem = TerminalChoiceChar | TerminalChoiceRange
          |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar {@TerminalChoiceRange(start=$0, end=$2)}
          |StringLiteral = '"' StringChar* '"' {@StringLiteral(value=$1)}
          |
          |UnicodeChar = '\\' 'u' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f'
          |TerminalChar = .-'\\'
          |  | '\\' '\'\\bnrt'
          |  | UnicodeChar
          |TerminalChoiceChar = .-'\'\-\\'
          |  | '\\' '\'\-\\bnrt'
          |  | UnicodeChar
          |StringChar = .-'"\\'
          |  | '\\' '"\\bnrt'
          |  | UnicodeChar
          |
          |StringLiteral = '"' StringChar* '"'
          |RefIdx = <'0' | '1-9' '0-9'*>
          |Id = <'a-zA-Z' 'a-zA-Z0-9'*>
          |WS = (' \n\r\t' | LineComment)*
          |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
          |EOF = !.
        """.stripMargin

    object ASTifier {
        def unwindRepeat(node: Node): List[Node] = {
            val BindNode(repeat: NRepeat, body) = node
            body match {
                case BindNode(symbol, repeating) if symbol.id == repeat.baseSeq => ???
                case BindNode(symbol, repeating) if symbol.id == repeat.repeatSeq => ???
            }
        }

        def matchDef(node: Node): AST.Def = {
            ???
        }

        def matchGrammar(node: Node): AST.Grammar = {
            val BindNode(NStart(_, _), BindNode(NNonterminal(_, Nonterminal("Grammar"), _), SequenceNode(_, body))) = node
            val def1 = matchDef(body(1))
            val def2 = unwindRepeat(body(2)) map { repeat =>
                val BindNode(_: NProxy, SequenceNode(_, repeatBody)) = repeat
                matchDef(repeatBody(1))
            }
            ???
        }
    }

    def main(args: Array[String]): Unit = {
        println(grammar.rules)
        val ngrammar = NGrammar.fromGrammar(grammar)
        val parser = new NaiveParser(ngrammar)

        val result = parser.parse("A = b c d")

        val ast = result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
                reconstructor.reconstruct() match {
                    case Some(parseForest) =>
                        assert(parseForest.trees.size == 1)
                        val tree = parseForest.trees.head

                        Some(ASTifier.matchGrammar(tree))
                    case None =>
                        println("Incomplete input")
                        None
                }
            case Right(error) =>
                println(error)
                None
        }

        // 문법이 주어지면
        // 1a. processor가 없는 문법 텍스트
        // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
        // 1c. (나중엔) 제너레이트된 파서
        // 2. 정의된 타입들을 정의하는 자바 코드
        // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드
    }
}
