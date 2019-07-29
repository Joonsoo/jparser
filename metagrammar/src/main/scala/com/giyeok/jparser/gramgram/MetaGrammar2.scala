package com.giyeok.jparser.gramgram

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

import scala.collection.immutable.{ListMap, ListSet}

object MetaGrammar2 {
    private val oldGrammar = NGrammar.fromGrammar(MetaGrammar.translateForce("Meta Grammar 2",
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
          |OnTheFlyTypeDef = '@' WS TypeName [WS OnTheFlySuperTypes]?
          |OnTheFlySuperTypes = '<' WS TypeName [WS ',' WS TypeName]* WS '>'
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
          |  | ArrayTerm
          |Ref = '$' RefIdx
          |ArrayTerm = '[' WS [PExpr [WS ',' WS PExpr]* WS]? ']'
          |BoundPExpr = Ref BoundedPExpr
          |BoundedPExpr = Ref
          |  | BoundPExpr
          |  | '{' WS PExpr WS '}'
          |ConstructExpr = TypeName WS ConstructParams
          |  | OnTheFlyTypeDefConstructExpr
          |ConstructParams = '(' WS [PExpr [WS ',' WS PExpr]* WS]? ')'
          |OnTheFlyTypeDefConstructExpr = OnTheFlyTypeDef WS NamedParams
          |NamedParams = '(' WS [NamedParam [WS ',' WS NamedParam]* WS]? ')'
          |NamedParam = ParamName [WS ':' WS TypeDesc]? WS '=' WS PExpr
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
          |  | Longest
          |  | EmptySequence
          |InPlaceChoices = InPlaceSequence [WS '|' WS InPlaceSequence]*
          |InPlaceSequence = Symbol [WS Symbol]*
          |Longest = '<' InPlaceChoices '>'
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
        """.stripMargin))

    sealed trait AST

    object AST {

        class Node(val node: ParseResultTree.Node) {
            override def toString: String = {
                def rec(node: ParseResultTree.Node): String = node match {
                    case TerminalNode(input) => input.toRawString
                    case BindNode(_, body) => rec(body)
                    case JoinNode(body, _) => rec(body)
                    case seq: SequenceNode => seq.children map rec mkString ""
                    case _: CyclicBindNode | _: CyclicSequenceNode =>
                        throw new Exception("Cyclic bind")
                }

                rec(node)
            }

            def toRepr: String = super.toString
        }

        sealed trait AstNode {
            val nodeId: Int
        }

        case class Grammar(nodeId: Int, defs: List[Def]) extends AstNode

        sealed trait Def extends AstNode

        sealed trait TypeDef extends Def

        case class ClassDef(nodeId: Int, typeName: TypeName, params: List[ClassParam]) extends TypeDef

        case class SuperDef(nodeId: Int, typeName: TypeName, subs: List[SubType]) extends TypeDef

        case class TypeName(nodeId: Int, name: Node) extends ValueTypeDesc with AstNode

        case class ClassParam(nodeId: Int, name: ParamName, typeDesc: Option[Node]) extends AstNode

        case class ParamName(nodeId: Int, name: Node) extends AstNode

        case class TypeDesc(nodeId: Int, typ: ValueTypeDesc, optional: Boolean) extends AstNode

        case class ArrayTypeDesc(nodeId: Int, elemType: TypeDesc) extends ValueTypeDesc with AstNode

        sealed trait ValueTypeDesc extends AstNode

        sealed trait SubType extends AstNode

        case class OnTheFlyTypeDef(nodeId: Int, name: TypeName, supers: List[TypeName]) extends ValueTypeDesc with AstNode

        case class Rule(nodeId: Int, lhs: LHS, rhs: List[RHS]) extends Def with AstNode

        case class LHS(nodeId: Int, name: Nonterminal, typeDesc: Option[TypeDesc]) extends AstNode

        case class RHS(nodeId: Int, elems: List[Elem]) extends AstNode

        sealed trait Elem extends AstNode

        sealed trait Processor extends Elem with AstNode

        sealed trait PExpr extends Processor with BoundedPExpr with AstNode

        case class BinOpExpr(nodeId: Int, op: Node, lhs: PExpr, rhs: PTerm) extends PExpr with AstNode

        sealed trait PTerm extends PExpr with AstNode

        case class Ref(nodeId: Int, idx: Node) extends Processor with PTerm with BoundedPExpr with AstNode

        case class BoundPExpr(nodeId: Int, ctx: Ref, expr: BoundedPExpr) extends PTerm with BoundedPExpr with AstNode

        sealed trait BoundedPExpr extends AstNode

        sealed trait AbstractConstructExpr extends PTerm with AstNode

        case class ConstructExpr(nodeId: Int, typ: TypeName, params: List[PExpr]) extends PTerm with AbstractConstructExpr with AstNode

        case class PTermParen(nodeId: Int, expr: PExpr) extends PTerm with AstNode

        case class PTermSeq(nodeId: Int, elems: List[PExpr]) extends PTerm with AstNode

        case class OnTheFlyTypeDefConstructExpr(nodeId: Int, typeDef: OnTheFlyTypeDef, params: List[NamedParam]) extends BoundedPExpr with AbstractConstructExpr with AstNode

        case class NamedParam(nodeId: Int, name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr) extends AstNode

        sealed trait Symbol extends Elem with AstNode

        sealed trait BinSymbol extends Symbol with AstNode

        case class JoinSymbol(nodeId: Int, symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol with AstNode

        case class ExceptSymbol(nodeId: Int, symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol with AstNode

        sealed trait PreUnSymbol extends BinSymbol with AstNode

        case class FollowedBy(nodeId: Int, expr: PreUnSymbol) extends PreUnSymbol with AstNode

        case class NotFollowedBy(nodeId: Int, expr: PreUnSymbol) extends PreUnSymbol with AstNode

        sealed trait PostUnSymbol extends PreUnSymbol with AstNode

        case class Repeat(nodeId: Int, expr: PostUnSymbol, repeat: Node) extends PostUnSymbol with AstNode

        sealed trait AtomSymbol extends PostUnSymbol with AstNode

        case class Paren(nodeId: Int, choices: InPlaceChoices) extends AtomSymbol with AstNode

        case class Longest(nodeId: Int, choices: InPlaceChoices) extends AtomSymbol with AstNode

        case class EmptySeq(nodeId: Int) extends AtomSymbol with AstNode

        case class InPlaceChoices(nodeId: Int, choices: List[InPlaceSequence]) extends AtomSymbol with AstNode

        case class InPlaceSequence(nodeId: Int, seq: List[Symbol]) extends AtomSymbol with AstNode

        case class Nonterminal(nodeId: Int, name: Node) extends AtomSymbol with AstNode

        sealed trait Terminal extends AtomSymbol with AstNode

        case class TerminalChar(nodeId: Int, char: Node) extends Terminal with AstNode

        case class AnyTerminal(nodeId: Int) extends Terminal with AstNode

        case class TerminalChoice(nodeId: Int, choices: List[TerminalChoiceElem]) extends AtomSymbol with AstNode

        sealed trait TerminalChoiceElem extends AstNode

        case class TerminalChoiceChar(nodeId: Int, char: Node) extends TerminalChoiceElem with AstNode

        case class TerminalChoiceRange(nodeId: Int, start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem with AstNode

        case class StringLiteral(nodeId: Int, value: Node) extends AtomSymbol with AstNode

    }

    val newGrammar: String =
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
          |  | '[' WS TypeDesc WS ']' {@ArrayTypeDesc(elemType=$2)}
          |SubTypes = SubType (WS ',' WS SubType)*
          |SubType: @SubType = TypeName | ClassDef | SuperDef
          |
          |OnTheFlyTypeDef = '@' WS TypeName (WS OnTheFlySuperTypes)? {@OnTheFlyTypeDef(name=$2, supers=$4$1)}
          |OnTheFlySuperTypes = '<' WS TypeName (WS ',' WS TypeName)* WS '>' {[$2] + $3$3}
          |
          |Rule = LHS WS '=' WS RHSs {@Rule(lhs=$0, rhs=$4)}
          |LHS = Nonterminal (WS ':' WS TypeDesc)? {@LHS(name=$0, typeDesc=$1$3)}
          |RHSs = RHS (WS '|' WS RHS)* {[$0] + $1$3}
          |RHS = Elem (WS Elem)* {@RHS(elems=[$0] + $1$1)}
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
          |  | ArrayTerm
          |Ref = '$' RefIdx {@Ref(idx=$1)}
          |ArrayTerm = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {@PTermSeq(elems=$2{[$0] + $1$3})}
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
          |NamedParam = ParamName (WS ':' WS TypeDesc)? WS '=' WS PExpr {@NamedParam(name=$0, typeDesc=$1$3, expr=$5)}
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
          |  | Longest
          |  | EmptySequence {@EmptySeq()}
          |InPlaceChoices = InPlaceSequence (WS '|' WS InPlaceSequence)* {@InPlaceChoices(choices=[$0] + $1$3)}
          |// TODO InPlaceSequence에서 Symbol -> Elem 받도록 수정
          |InPlaceSequence = Symbol (WS Symbol)* {@InPlaceSequence(seq=[$0] + $1$1)}
          |Longest = '<' InPlaceChoices '>' {@Longest(choices=$1)}
          |EmptySequence = '#'
          |Nonterminal = Id {@Nonterminal(name=$0)}
          |Terminal: @Terminal = '\'' TerminalChar '\'' {@TerminalChar(char=$2)}
          |  | '.' {@AnyTerminal()}
          |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {@TerminalChoice(choices=[$1] + $2)}
          |  | '\'' TerminalChoiceRange '\'' {TerminalChoice([$1])}
          |TerminalChoiceElem: @TerminalChoiceElem = TerminalChoiceChar {@TerminalChoiceChar(char=$0)}
          |  | TerminalChoiceRange
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
        private var nextId = 0

        private def newId() = {
            nextId += 1
            nextId
        }

        def transformNode(node: Node): AST.Node = new AST.Node(node)

        def unwindRepeat(node: Node): List[Node] = {
            val BindNode(repeat: NRepeat, body) = node
            body match {
                case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
                    unwindRepeat(repeating.children(0)) :+ repeating.children(1)
                case BindNode(symbol, _) if symbol.id == repeat.baseSeq =>
                    List(body)
                case seq@SequenceNode(symbol, _) if symbol.id == repeat.baseSeq =>
                    seq.children.take(repeat.symbol.lower).toList
            }
        }

        def unwindOptional(node: Node): Option[SequenceNode] = {
            val BindNode(_, body) = node
            body match {
                case BindNode(_, BindNode(_, seq: SequenceNode)) =>
                    if (seq.children.isEmpty) None else Some(seq)
                case BindNode(_, seq: SequenceNode) =>
                    if (seq.children.isEmpty) None else Some(seq)
            }
        }

        def unwindNonterm(name: String, node: Node): Node = {
            val BindNode(NNonterminal(_, Nonterminal(`name`), _), body) = node
            body
        }

        def matchNonterminal(node: Node): AST.Nonterminal = {
            val BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) = node
            AST.Nonterminal(newId(), transformNode(nonterminalBody))
        }

        def matchTypeName(node: Node): AST.TypeName = {
            val BindNode(NNonterminal(_, Nonterminal("Id"), _), idBody) = node
            AST.TypeName(newId(), transformNode(idBody))
        }

        def matchParamName(node: Node): AST.ParamName = {
            val BindNode(NNonterminal(_, Nonterminal("Id"), _), idBody) = node
            AST.ParamName(newId(), transformNode(idBody))
        }

        def matchOnTheFlySuperTypes(node: Node): List[AST.TypeName] = {
            val BindNode(_, seq: SequenceNode) = node
            val super1 = matchTypeName(unwindNonterm("TypeName", seq.children(2)))
            val super2_ = unwindRepeat(seq.children(3))
            val super2 = super2_ map { b =>
                val BindNode(_, BindNode(_, seq1: SequenceNode)) = b
                matchTypeName(unwindNonterm("TypeName", seq1.children(3)))
            }
            super1 +: super2
        }

        def matchOnTheFlyTypeDef(node: Node): AST.OnTheFlyTypeDef = {
            val BindNode(_, seq: SequenceNode) = node
            val name = matchTypeName(unwindNonterm("TypeName", seq.children(2)))
            val supers: List[AST.TypeName] = (unwindOptional(seq.children(3)) map { seq =>
                matchOnTheFlySuperTypes(unwindNonterm("OnTheFlySuperTypes", seq.children(1)))
            }).getOrElse(List())
            AST.OnTheFlyTypeDef(newId(), name, supers)
        }

        def matchValueTypeDesc(node: Node): AST.ValueTypeDesc = node match {
            case BindNode(NNonterminal(_, Nonterminal("TypeName"), _), typeNameBody) =>
                matchTypeName(typeNameBody)
            case BindNode(NNonterminal(_, Nonterminal("OnTheFlyTypeDef"), _), ontheflyBody) =>
                matchOnTheFlyTypeDef(ontheflyBody)
            case seq: SequenceNode =>
                AST.ArrayTypeDesc(newId(), matchTypeDesc(unwindNonterm("TypeDesc", seq.children(2))))
        }

        def matchTypeDesc(node: Node): AST.TypeDesc = {
            val BindNode(_, seq: SequenceNode) = node
            val valueTypeDesc = matchValueTypeDesc(unwindNonterm("ValueTypeDesc", seq.children(0)))
            val optional = seq.children(1) match {
                case BindNode(_, BindNode(_, seq: SequenceNode)) =>
                    seq.children.nonEmpty
            }
            AST.TypeDesc(newId(), valueTypeDesc, optional)
        }

        def matchLHS(node: Node): AST.LHS = {
            val BindNode(NNonterminal(_, Nonterminal("LHS"), _), BindNode(_, lhsBody: SequenceNode)) = node

            val name = matchNonterminal(lhsBody.children(0))
            val typeDesc_ = unwindOptional(lhsBody.children(1))
            val typeDesc = typeDesc_ map { opt =>
                matchTypeDesc(unwindNonterm("TypeDesc", opt.children(3)))
            }
            AST.LHS(newId(), name, typeDesc)
        }

        def matchTerminal(node: Node): AST.Terminal = node match {
            case BindNode(_, SequenceNode(_, List(_, BindNode(NNonterminal(_, Nonterminal("TerminalChar"), _), terminalCharBody), _))) =>
                AST.TerminalChar(newId(), transformNode(terminalCharBody))
            case _ => AST.AnyTerminal(newId())
        }

        def matchTerminalChoiceChar(node: Node): AST.TerminalChoiceChar =
            AST.TerminalChoiceChar(newId(), transformNode(node))

        def matchTerminalChoiceRange(node: Node): AST.TerminalChoiceRange = node match {
            case BindNode(_, seq: SequenceNode) =>
                AST.TerminalChoiceRange(newId(), matchTerminalChoiceChar(seq.children(0)), matchTerminalChoiceChar(seq.children(2)))
        }

        def matchTerminalChoiceElem(node: Node): AST.TerminalChoiceElem = node match {
            case BindNode(NNonterminal(_, Nonterminal("TerminalChoiceChar"), _), charBody) =>
                matchTerminalChoiceChar(charBody)
            case BindNode(NNonterminal(_, Nonterminal("TerminalChoiceRange"), _), rangeBody) =>
                matchTerminalChoiceRange(rangeBody)
        }

        def matchTerminalChoice(node: Node): AST.TerminalChoice = node match {
            case BindNode(_, seq: SequenceNode) if seq.children.size == 4 =>
                val elem1 = matchTerminalChoiceElem(unwindNonterm("TerminalChoiceElem", seq.children(1)))
                val elem2_ = unwindRepeat(seq.children(2))
                val elem2 = elem2_ map { b => matchTerminalChoiceElem(unwindNonterm("TerminalChoiceElem", b)) }
                AST.TerminalChoice(newId(), elem1 +: elem2)
            case BindNode(_, seq: SequenceNode) if seq.children.size == 3 =>
                val elem = matchTerminalChoiceElem(seq.children(1))
                AST.TerminalChoice(newId(), List(elem))
        }

        def matchLongest(node: Node): AST.Longest = {
            val BindNode(_, SequenceNode(_, List(_, body, _))) = node
            AST.Longest(newId(), matchInPlaceChoices(body))
        }

        def matchInPlaceSequence(node: Node): AST.InPlaceSequence = {
            val BindNode(_, BindNode(_, seq: SequenceNode)) = node

            val choices1 = matchSymbol(unwindNonterm("Symbol", seq.children(0)))
            val choices2_ = unwindRepeat(seq.children(1))
            val choices2 = choices2_ map { b =>
                val BindNode(_, BindNode(_, seq: SequenceNode)) = b
                matchSymbol(unwindNonterm("Symbol", seq.children(1)))
            }
            AST.InPlaceSequence(newId(), choices1 +: choices2)
        }

        def matchInPlaceChoices(node: Node): AST.InPlaceChoices = {
            val BindNode(_, BindNode(_, seq: SequenceNode)) = node

            val choices1 = matchInPlaceSequence(seq.children(0))
            val choices2_ = unwindRepeat(seq.children(1))
            val choices2 = choices2_ map { b =>
                val BindNode(_, BindNode(_, seq: SequenceNode)) = b
                matchInPlaceSequence(seq.children(3))
            }
            AST.InPlaceChoices(newId(), choices1 +: choices2)
        }

        def matchAtomSymbol(node: Node): AST.AtomSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("Terminal"), _), terminalBody) =>
                matchTerminal(terminalBody)
            case BindNode(NNonterminal(_, Nonterminal("TerminalChoice"), _), terminalChoiceBody) =>
                matchTerminalChoice(terminalChoiceBody)
            case BindNode(NNonterminal(_, Nonterminal("StringLiteral"), _), BindNode(_, stringBody: SequenceNode)) =>
                AST.StringLiteral(newId(), transformNode(stringBody.children(1)))
            case BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) =>
                AST.Nonterminal(newId(), transformNode(nonterminalBody))
            case BindNode(NNonterminal(_, Nonterminal("Longest"), _), longestBody) =>
                matchLongest(longestBody)
            case BindNode(NNonterminal(_, Nonterminal("EmptySequence"), _), emptySeqBody) =>
                AST.EmptySeq(newId())
            case BindNode(_, SequenceNode(_, List(_, parenElem, _))) =>
                matchInPlaceChoices(parenElem)
        }

        def matchPostUnSymbol(node: Node): AST.PostUnSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("AtomSymbol"), _), atomSymbolBody) =>
                matchAtomSymbol(atomSymbolBody)
            case BindNode(_, seq: SequenceNode) =>
                val sym = matchPostUnSymbol(unwindNonterm("PostUnSymbol", seq.children(0)))
                val op = seq.children(2)
                AST.Repeat(newId(), sym, transformNode(op))
        }

        def matchPreUnSymbol(node: Node): AST.PreUnSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("PostUnSymbol"), _), postUnSymbolBody) =>
                matchPostUnSymbol(postUnSymbolBody)
            case BindNode(_, seq: SequenceNode) =>
                val op = seq.children(0)
                val sym = matchPreUnSymbol(unwindNonterm("PreUnSymbol", seq.children(2)))
                op match {
                    case BindNode(_, TerminalNode(Inputs.Character('^'))) => AST.FollowedBy(newId(), sym)
                    case BindNode(_, TerminalNode(Inputs.Character('!'))) => AST.NotFollowedBy(newId(), sym)
                }
        }

        def matchBinSymbol(node: Node): AST.BinSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("PreUnSymbol"), _), preUnSymbolBody) =>
                matchPreUnSymbol(preUnSymbolBody)
            case BindNode(_, seq: SequenceNode) =>
                val op = seq.children(2)
                val lhs = matchBinSymbol(unwindNonterm("BinSymbol", seq.children(0)))
                val rhs = matchPreUnSymbol(unwindNonterm("PreUnSymbol", seq.children(4)))
                op match {
                    case BindNode(_, TerminalNode(Inputs.Character('&'))) => AST.JoinSymbol(newId(), lhs, rhs)
                    case BindNode(_, TerminalNode(Inputs.Character('-'))) => AST.ExceptSymbol(newId(), lhs, rhs)
                }
        }

        def matchSymbol(node: Node): AST.Symbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("BinSymbol"), _), binSymbolBody) =>
                matchBinSymbol(binSymbolBody)
        }

        def matchRef(node: Node): AST.Ref = {
            val BindNode(_, seq: SequenceNode) = node
            AST.Ref(newId(), transformNode(seq.children(1)))
        }

        def matchProcessor(node: Node): AST.Processor = node match {
            case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
                matchRef(refBody)
            case BindNode(_, seq: SequenceNode) =>
                matchPExpr(unwindNonterm("PExpr", seq.children(2)))
        }

        def matchPExpr(node: Node): AST.PExpr = node match {
            case BindNode(NNonterminal(_, Nonterminal("PTerm"), _), ptermBody) =>
                matchPTerm(ptermBody)
            case BindNode(_, seq: SequenceNode) =>
                val op = seq.children(2)
                val lhs = matchPExpr(unwindNonterm("PExpr", seq.children(0)))
                val rhs = matchPTerm(unwindNonterm("PTerm", seq.children(4)))
                AST.BinOpExpr(newId(), transformNode(op), lhs, rhs)
        }

        def matchPTerm(node: Node): AST.PTerm = node match {
            case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
                matchRef(refBody)
            case BindNode(NNonterminal(_, Nonterminal("BoundPExpr"), _), boundPExprBody) =>
                matchBoundPExpr(boundPExprBody)
            case BindNode(NNonterminal(_, Nonterminal("ConstructExpr"), _), constructBody) =>
                matchConstructExpr(constructBody)
            case BindNode(NNonterminal(_, Nonterminal("ArrayTerm"), _), arrayBody) =>
                matchArrayTerm(arrayBody)
            case BindNode(_, seq: SequenceNode) =>
                AST.PTermParen(newId(), matchPExpr(unwindNonterm("PExpr", seq.children(2))))
        }

        def matchBoundPExpr(node: Node): AST.BoundPExpr = node match {
            case BindNode(_, seq: SequenceNode) =>
                val ctx = matchRef(unwindNonterm("Ref", seq.children(0)))
                val expr = matchBoundedPExpr(unwindNonterm("BoundedPExpr", seq.children(1)))
                AST.BoundPExpr(newId(), ctx, expr)
        }

        def matchBoundedPExpr(node: Node): AST.BoundedPExpr = node match {
            case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
                matchRef(refBody)
            case BindNode(NNonterminal(_, Nonterminal("BoundPExpr"), _), boundPExprBody) =>
                matchBoundPExpr(boundPExprBody)
            case BindNode(_, seq: SequenceNode) =>
                matchPExpr(unwindNonterm("PExpr", seq.children(2)))
        }

        def matchConstructExpr(node: Node): AST.AbstractConstructExpr = node match {
            case BindNode(NNonterminal(_, Nonterminal("OnTheFlyTypeDefConstructExpr"), _), ontheflyBody) =>
                matchOnTheFlyTypeDefConstructExpr(ontheflyBody)
            case BindNode(_, seq: SequenceNode) =>
                val typename = matchTypeName(unwindNonterm("TypeName", seq.children(0)))
                val params = matchConstructParams(unwindNonterm("ConstructParams", seq.children(2)))
                AST.ConstructExpr(newId(), typename, params)
        }

        def matchConstructParams(node: Node): List[AST.PExpr] = {
            val BindNode(_, seq: SequenceNode) = node
            val params_ = unwindOptional(seq.children(2))
            (params_ map { p =>
                val param1 = matchPExpr(unwindNonterm("PExpr", p.children(0)))
                val param2_ = unwindRepeat(p.children(1))
                val param2 = param2_ map { b =>
                    val BindNode(_, BindNode(_, s: SequenceNode)) = b
                    matchPExpr(unwindNonterm("PExpr", s.children(3)))
                }
                param1 +: param2
            }).getOrElse(List())
        }

        def matchOnTheFlyTypeDefConstructExpr(node: Node): AST.OnTheFlyTypeDefConstructExpr = {
            val BindNode(_, seq: SequenceNode) = node
            val onTheFlyTypeDef = matchOnTheFlyTypeDef(unwindNonterm("OnTheFlyTypeDef", seq.children(0)))
            val params = matchNamedParams(unwindNonterm("NamedParams", seq.children(2)))
            AST.OnTheFlyTypeDefConstructExpr(newId(), onTheFlyTypeDef, params)
        }

        def matchNamedParams(node: Node): List[AST.NamedParam] = {
            val BindNode(_, seq1: SequenceNode) = node

            (unwindOptional(seq1.children(2)) map { seq =>
                val param1 = matchNamedParam(unwindNonterm("NamedParam", seq.children(0)))
                val param2 = unwindRepeat(seq.children(1)) map { b =>
                    val BindNode(_, BindNode(_, s: SequenceNode)) = b
                    matchNamedParam(unwindNonterm("NamedParam", s.children(3)))
                }
                param1 +: param2
            }).getOrElse(List())
        }

        def matchNamedParam(node: Node): AST.NamedParam = {
            val BindNode(_, seq: SequenceNode) = node

            val name = matchParamName(unwindNonterm("ParamName", seq.children(0)))
            val typeDesc = unwindOptional(seq.children(1)) map { seq =>
                matchTypeDesc(unwindNonterm("TypeDesc", seq.children(3)))
            }
            val expr = matchPExpr(unwindNonterm("PExpr", seq.children(5)))
            AST.NamedParam(newId(), name, typeDesc, expr)
        }

        def matchArrayTerm(node: Node): AST.PTermSeq = {
            val BindNode(_, seq: SequenceNode) = node

            val elems_ = unwindOptional(seq.children(2))
            val elems = (elems_ map { elems =>
                val elem1 = matchPExpr(unwindNonterm("PExpr", elems.children(0)))
                val elem2_ = unwindRepeat(elems.children(1))
                val elem2 = elem2_ map { tails_ =>
                    val BindNode(_, tails: SequenceNode) = tails_
                    matchPExpr(unwindNonterm("PExpr", tails.children(3)))
                }
                elem1 +: elem2
            }).getOrElse(List())
            AST.PTermSeq(newId(), elems)
        }

        def matchElem(node: Node): AST.Elem = {
            val BindNode(_, body) = node
            body match {
                case BindNode(NNonterminal(_, Nonterminal("Symbol"), _), symbolBody) =>
                    matchSymbol(symbolBody)
                case BindNode(NNonterminal(_, Nonterminal("Processor"), _), processorBody) =>
                    matchProcessor(processorBody)
            }
        }

        def matchRHS(node: Node): AST.RHS = {
            val BindNode(_, BindNode(_, rhsBody: SequenceNode)) = node
            val elem1 = matchElem(rhsBody.children(0))
            val elem2_ = unwindRepeat(rhsBody.children(1))
            val elem2 = elem2_ map { repeat =>
                val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
                matchElem(repeatBody.children(1))
            }
            AST.RHS(newId(), elem1 +: elem2)
        }

        def matchRHSs(node: Node): List[AST.RHS] = {
            val BindNode(_, BindNode(_, rhsBody: SequenceNode)) = node
            val rhs1 = matchRHS(rhsBody.children(0))
            val rhs2_ = unwindRepeat(rhsBody.children(1))
            val rhs2 = rhs2_ map { repeat =>
                val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
                matchRHS(repeatBody.children(3))
            }
            rhs1 +: rhs2
        }

        def matchRule(node: Node): AST.Rule = {
            val BindNode(_, seqBody: SequenceNode) = node
            val lhs = matchLHS(seqBody.children(0))
            val rhss = matchRHSs(seqBody.children(4))
            AST.Rule(newId(), lhs, rhss)
        }

        def matchTypeDef(node: Node): AST.TypeDef = {
            ???
        }

        def matchDef(node: Node): AST.Def = {
            val BindNode(NNonterminal(_, Nonterminal("Def"), _), body) = node

            body match {
                case BindNode(NNonterminal(_, Nonterminal("Rule"), _), ruleBody) => matchRule(ruleBody)
                case BindNode(NNonterminal(_, Nonterminal("TypeDef"), _), typeDefBody) => matchTypeDef(typeDefBody)
            }
        }

        def matchGrammar(node: Node): AST.Grammar = {
            val BindNode(NStart(_, _), BindNode(NNonterminal(_, Nonterminal("Grammar"), _), BindNode(_: NSequence, grammarBody: SequenceNode))) = node
            val def1 = matchDef(grammarBody.children(1))
            val def2_ = unwindRepeat(grammarBody.children(2))
            val def2 = def2_ map { repeat =>
                val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
                matchDef(repeatBody.children(1))
            }
            AST.Grammar(newId(), def1 +: def2)
        }
    }

    val parser = new NaiveParser(oldGrammar)

    def grammarSpecToAST(grammar: String): Option[AST.Grammar] = {
        val result = parser.parse(grammar)

        result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(oldGrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
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
    }

    object Analyzer {

        sealed trait TypeSpec

        object NodeType extends TypeSpec

        sealed trait SingleTypeSpec extends TypeSpec

        case class ClassType(name: String, params: List[AST.NamedParam]) extends SingleTypeSpec

        case class AbstractType(name: String) extends SingleTypeSpec

        case class MultiType(types: Set[SingleTypeSpec]) extends TypeSpec

        class Analysis private[Analyzer](val grammarAst: AST.Grammar,
                                         val grammar: Grammar,
                                         val ngrammar: NGrammar,
                                         val subclasses: Map[AbstractType, Set[ClassType]],
                                         val inferredTypes: Map[String, TypeSpec])

        private class Analyzer(val grammarAst: AST.Grammar) {
            val ruleDefs: List[AST.Rule] = grammarAst.defs collect { case r: AST.Rule => r }
            val typeDefs: List[AST.TypeDef] = grammarAst.defs collect { case r: AST.TypeDef => r }

            private var allTypes = List[SingleTypeSpec]()
            private var superTypes = Map[AbstractType, Set[String]]()

            def collectTypeDefs(): Unit = {
                // TODO typeDefs
                def addTypeDefsLhs(typ: AST.ValueTypeDesc): Unit = typ match {
                    case AST.OnTheFlyTypeDef(_, name, supers) =>
                        val newType = AbstractType(name.toString)
                        val newSupers = superTypes.getOrElse(newType, List()) ++ (supers map {
                            _.name.toString
                        })
                        superTypes += newType -> newSupers.toSet
                        allTypes +:= newType
                    case AST.ArrayTypeDesc(_, elemType) =>
                        addTypeDefsLhs(elemType.typ)
                    case _ => // do nothing
                }

                def addTypeDefsOnTheFlyTypeDefConstructExpr(expr: AST.OnTheFlyTypeDefConstructExpr): Unit = expr match {
                    case AST.OnTheFlyTypeDefConstructExpr(_, tdef, params) =>
                        val newType = ClassType(tdef.name.name.toString, params)
                        allTypes +:= newType
                        params foreach { p => addTypeDefsExpr(p.expr) }
                }

                def addTypeDefsBoundedPExpr(expr: AST.BoundedPExpr): Unit = expr match {
                    case AST.BoundPExpr(_, _, e) => addTypeDefsBoundedPExpr(e)
                    case e: AST.PExpr => addTypeDefsExpr(e)
                    case e: AST.OnTheFlyTypeDefConstructExpr =>
                        addTypeDefsOnTheFlyTypeDefConstructExpr(e)
                    case _: AST.Ref => // do nothing
                }

                def addTypeDefsExpr(expr: AST.PExpr): Unit = expr match {
                    case AST.PTermParen(_, e) => addTypeDefsExpr(e)
                    case AST.BoundPExpr(_, _, e) => addTypeDefsBoundedPExpr(e)
                    case AST.ConstructExpr(_, _, params) => params foreach addTypeDefsExpr
                    case AST.PTermSeq(_, elems) => elems foreach addTypeDefsExpr
                    case AST.ConstructExpr(_, _, params) => params foreach addTypeDefsExpr
                    case e: AST.OnTheFlyTypeDefConstructExpr =>
                        addTypeDefsOnTheFlyTypeDefConstructExpr(e)
                    case AST.BinOpExpr(_, _, lhs, rhs) =>
                        addTypeDefsExpr(lhs)
                        addTypeDefsExpr(rhs)
                    case _: AST.Ref => // do nothing
                }

                ruleDefs foreach { rule =>
                    rule.lhs.typeDesc foreach { lhsType =>
                        addTypeDefsLhs(lhsType.typ)
                    }
                    rule.rhs foreach { rhs =>
                        rhs.elems foreach {
                            case processor: AST.PExpr =>
                                addTypeDefsExpr(processor)
                            case _ => // do nothing
                        }
                    }
                }
            }

            private val astToSymbols = scala.collection.mutable.Map[AST.Symbol, Symbols.Symbol]()

            private def addSymbol(ast: AST.Symbol, symbol: Symbols.Symbol): Symbols.Symbol = {
                astToSymbols get ast match {
                    case Some(existing) =>
                        assert(symbol == existing)
                        existing
                    case None =>
                        astToSymbols(ast) = symbol
                        symbol
                }
            }

            private def astToSymbol(ast: AST.Symbol): Symbols.Symbol = ast match {
                case AST.JoinSymbol(_, symbol1, symbol2) =>
                    val ns1 = astToSymbol(symbol1).asInstanceOf[Symbols.AtomicSymbol]
                    val ns2 = astToSymbol(symbol2).asInstanceOf[Symbols.AtomicSymbol]
                    addSymbol(ast, Symbols.Join(ns1, ns2))
                case AST.ExceptSymbol(_, symbol1, symbol2) =>
                    val ns1 = astToSymbol(symbol1).asInstanceOf[Symbols.AtomicSymbol]
                    val ns2 = astToSymbol(symbol2).asInstanceOf[Symbols.AtomicSymbol]
                    addSymbol(ast, Symbols.Except(ns1, ns2))
                case AST.FollowedBy(_, symbol) =>
                    val ns = astToSymbol(symbol).asInstanceOf[Symbols.AtomicSymbol]
                    addSymbol(ast, Symbols.LookaheadIs(ns))
                case AST.NotFollowedBy(_, symbol) =>
                    val ns = astToSymbol(symbol).asInstanceOf[Symbols.AtomicSymbol]
                    addSymbol(ast, Symbols.LookaheadExcept(ns))
                case AST.Repeat(_, symbol, repeatSpec) =>
                    val ns = astToSymbol(symbol).asInstanceOf[Symbols.AtomicSymbol]

                    repeatSpec.toString match {
                        case "?" => addSymbol(ast, ns.opt)
                        case "*" => addSymbol(ast, ns.star)
                        case "+" => addSymbol(ast, ns.plus)
                    }
                case AST.Longest(_, choices) =>
                    val ns = choices.choices map {
                        astToSymbol(_).asInstanceOf[Symbols.AtomicSymbol]
                    }
                    addSymbol(ast, Symbols.Longest(Symbols.OneOf(ListSet(ns: _*))))
                case AST.Nonterminal(_, name) =>
                    addSymbol(ast, Symbols.Nonterminal(name.toString))
                case AST.InPlaceChoices(_, choices) =>
                    val ns = choices map {
                        astToSymbol(_).asInstanceOf[Symbols.AtomicSymbol]
                    }
                    addSymbol(ast, Symbols.OneOf(ListSet(ns: _*)))
                case AST.Paren(_, choices) =>
                    val ns = choices.choices map {
                        astToSymbol(_).asInstanceOf[Symbols.AtomicSymbol]
                    }
                    addSymbol(ast, Symbols.OneOf(ListSet(ns: _*)))
                case AST.InPlaceSequence(_, seq) =>
                    val ns = seq map {
                        astToSymbol(_).asInstanceOf[Symbols.AtomicSymbol]
                    }
                    addSymbol(ast, Symbols.Proxy(Symbols.Sequence(ns)))
                case AST.StringLiteral(_, value) =>
                    // TODO
                    addSymbol(ast, i(value.toString))
                case AST.EmptySeq(_) =>
                    addSymbol(ast, Symbols.Sequence(Seq()))
                case AST.TerminalChoice(_, choices) =>
                    // TODO
                    addSymbol(ast, Symbols.Chars(choices.toString.toCharArray.toSet))
                case AST.TerminalChar(_, value) =>
                    // TODO
                    addSymbol(ast, Symbols.Chars(value.toString.toCharArray.toSet))
                case AST.AnyTerminal(_) =>
                    addSymbol(ast, Symbols.AnyChar)
            }

            // rhs에 포함된 ref가 실제로 가리키는 AST 노드를 반환
            private def findRef(rhs: AST.RHS, ref: AST.Ref): AST.Elem = {
                // TODO
                ???
            }

            object TypeAnalyzeGraph {

                sealed trait Node

                case class SymbolNode(symbol: Symbols.Symbol) extends Node

                case class ExprNode(expr: AST.PExpr) extends Node

                case class ConstructNode(expr: AST.ConstructExpr) extends Node

                case class ParamNode(className: String, paramIdx: Int, name: String) extends Node

                sealed trait TypeNode extends Node

                case class ClassTypeNode(className: String) extends TypeNode

                case class TypeGenArrayOf(typ: TypeNode) extends TypeNode

                case class TypeGenOptionalOf(typ: TypeNode) extends TypeNode

                case class TypeGenUnion(typs: Set[TypeNode]) extends TypeNode

                object EdgeTypes extends Enumeration {
                    val Is, Accepts, Returns, Extends, Has = Value
                }

                case class Edge(start: Node, end: Node, edgeType: EdgeTypes.Value) extends AbstractEdge[Node]

                class Builder() {
                    private var graph = new TypeAnalyzeGraph(Set(), Set(), Map(), Map())

                    private def addNode[T <: Node](node: T): T = {
                        graph = graph.addNode(node)
                        node
                    }

                    private def addEdge(edge: Edge): Edge = {
                        graph = graph.addEdge(edge)
                        edge
                    }

                    private def typeDescToTypeNode(typeDesc: AST.TypeDesc): TypeNode = {
                        val valueTypeNode = typeDesc.typ match {
                            case AST.ArrayTypeDesc(_, elemType) =>
                                val elemTypeNode = typeDescToTypeNode(elemType)
                                addNode(TypeGenArrayOf(elemTypeNode))
                            case AST.TypeName(_, typeName) =>
                                ClassTypeNode(typeName.toString)
                            case AST.OnTheFlyTypeDef(_, name, _) =>
                                ClassTypeNode(name.name.toString)
                        }
                        if (typeDesc.optional) addNode(TypeGenOptionalOf(valueTypeNode)) else valueTypeNode
                    }

                    def analyze() = {
                        allTypes foreach {
                            case ClassType(className, params) =>
                                val classNode = addNode(ClassTypeNode(className))
                                params.zipWithIndex foreach { case (paramAst, paramIdx) =>
                                    val paramNode = addNode(ParamNode(className, paramIdx, paramAst.name.name.toString))
                                    // ClassNode --has--> ParamNode
                                    addEdge(Edge(classNode, paramNode, EdgeTypes.Has))
                                    paramAst.typeDesc foreach { typeDesc =>
                                        val paramType = typeDescToTypeNode(typeDesc)
                                        // ParamNode --is--> TypeNode
                                        addEdge(Edge(paramNode, paramType, EdgeTypes.Is))
                                    }
                                }
                            case typ@AbstractType(name) =>
                                val abstractType = addNode(ClassTypeNode(name))
                                superTypes(typ) foreach { superTyp =>
                                    val subType = addNode(ClassTypeNode(superTyp))
                                    // ClassTypeNode --extends--> ClassTypeNode
                                    addEdge(Edge(subType, abstractType, EdgeTypes.Extends))
                                }
                        }
                        ruleDefs foreach { ruleDef =>
                            val lhsName = ruleDef.lhs.name.name.toString
                            val lhsNontermNode = addNode(SymbolNode(Symbols.Nonterminal(lhsName)))

                            ruleDef.lhs.typeDesc foreach { lhsType =>
                                val lhsTypeNode = typeDescToTypeNode(lhsType)
                                // SymbolNode --is--> TypeNode
                                addEdge(Edge(lhsNontermNode, lhsTypeNode, EdgeTypes.Is))
                            }

                            // ruleDef.rhs 를 하나씩 순회하면서 Param accept expr
                        }
                    }
                }

            }

            class TypeAnalyzeGraph private(val nodes: Set[TypeAnalyzeGraph.Node],
                                           val edges: Set[TypeAnalyzeGraph.Edge],
                                           val edgesByStart: Map[TypeAnalyzeGraph.Node, Set[TypeAnalyzeGraph.Edge]],
                                           val edgesByEnd: Map[TypeAnalyzeGraph.Node, Set[TypeAnalyzeGraph.Edge]])
                extends AbstractGraph[TypeAnalyzeGraph.Node, TypeAnalyzeGraph.Edge, TypeAnalyzeGraph] {
                override def createGraph(nodes: Set[TypeAnalyzeGraph.Node], edges: Set[TypeAnalyzeGraph.Edge],
                                         edgesByStart: Map[TypeAnalyzeGraph.Node, Set[TypeAnalyzeGraph.Edge]],
                                         edgesByEnd: Map[TypeAnalyzeGraph.Node, Set[TypeAnalyzeGraph.Edge]]): TypeAnalyzeGraph =
                    new TypeAnalyzeGraph(nodes, edges, edgesByStart, edgesByEnd)
            }

            def analyze(): Analysis = {
                collectTypeDefs()
                // TODO check name conflict in allTypes
                // TODO make sure no type has name the name "Node"

                allTypes foreach println

                ruleDefs foreach { rule =>
                    astToSymbol(rule.lhs.name)
                    rule.rhs foreach { rhs =>
                        rhs.elems foreach {
                            case elemAst: AST.Symbol =>
                                astToSymbol(elemAst)
                            case _ => // do nothing
                        }
                    }
                }

                val grammar = new Grammar {
                    override val name: String = "Intermediate"

                    private def rhsToSeq(rhs: AST.RHS): Symbols.Sequence = Symbols.Sequence(rhs.elems collect {
                        case sym: AST.Symbol =>
                            astToSymbol(sym).asInstanceOf[Symbols.AtomicSymbol]
                    })

                    override val rules: RuleMap = ListMap[String, ListSet[Symbols.Symbol]](ruleDefs map { ruleDef =>
                        val x = ListSet[Symbols.Symbol](ruleDef.rhs map rhsToSeq: _*)
                        ruleDef.lhs.name.name.toString -> x
                    }: _*)
                    override val startSymbol: Nonterminal = Symbols.Nonterminal(ruleDefs.head.lhs.name.name.toString)
                }
                val ngrammar = NGrammar.fromGrammar(grammar)

                val graph = new TypeAnalyzeGraph.Builder().analyze()

                new Analysis(grammarAst, grammar, ngrammar, ???, ???)
            }
        }

        def analyze(grammar: AST.Grammar): Analysis = {
            new Analyzer(grammar).analyze()
        }
    }

    // typeOf(nonterminal): Type

    def main(args: Array[String]): Unit = {
        val expressionGrammar =
            """expression: @Expression = term
              |    | expression '+' term {@BinOp(op=$1, lhs=$0, rhs=$2)}
              |term: @Term = factor
              |    | term '*' factor {BinOp($1, $0, $2)}
              |factor: @Factor = number {@Number(value=$0)}
              |    | variable
              |    | '(' expression ')' $1
              |number = '0'
              |    | '1-9' '0-9'*
              |variable = <'A-Za-z'+> {@Variable(name=$0)}
            """.stripMargin

        val ast = grammarSpecToAST(newGrammar)

        println(ast)

        val analysis = Analyzer.analyze(ast.get)

        // 문법이 주어지면
        // 1a. processor가 없는 문법 텍스트
        // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
        // 1c. (나중엔) 제너레이트된 파서
        // 2. 정의된 타입들을 정의하는 자바 코드
        // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드
    }
}
