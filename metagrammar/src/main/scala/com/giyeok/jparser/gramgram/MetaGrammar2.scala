package com.giyeok.jparser.gramgram

import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Inputs, ParseForestFunc, ParseResultTree}

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

        case class Rule(lhs: LHS, rhs: List[RHS]) extends Def

        case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

        case class RHS(elems: List[Elem])

        sealed trait Elem

        sealed trait Processor extends Elem

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

        sealed trait Symbol extends Elem

        sealed trait BinSymbol extends Symbol

        case class JoinSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        case class ExceptSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

        sealed trait PreUnSymbol extends BinSymbol

        case class FollowedBy(expr: PreUnSymbol) extends PreUnSymbol

        case class NotFollowedBy(expr: PreUnSymbol) extends PreUnSymbol

        sealed trait PostUnSymbol extends PreUnSymbol

        case class Repeat(expr: PostUnSymbol, repeat: Node) extends PostUnSymbol

        sealed trait AtomSymbol extends PostUnSymbol

        case class Paren(choices: InPlaceChoices) extends AtomSymbol

        case class Longest(choices: InPlaceChoices) extends AtomSymbol

        object EmptySeq extends AtomSymbol

        case class InPlaceChoices(choices: List[InPlaceSequence]) extends AtomSymbol

        case class InPlaceSequence(seq: List[Symbol]) extends AtomSymbol

        case class Nonterminal(name: Node) extends AtomSymbol

        sealed trait Terminal extends AtomSymbol

        case class TerminalChar(char: Node) extends Terminal

        object AnyTerminal extends Terminal

        case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

        sealed trait TerminalChoiceElem

        case class TerminalChoiceChar(char: Node) extends TerminalChoiceElem

        case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

        case class StringLiteral(value: Node) extends AtomSymbol

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
          |  | Longest
          |  | EmptySequence {@EmptySeq()}
          |InPlaceChoices = InPlaceSequence (WS '|' WS InPlaceSequence)* {$InPlaceChoices(choices=[$0] + $1$3)}
          |InPlaceSequence = Symbol (WS Symbol)* {@InPlaceSequence(seq=[$0] + $1$1)}
          |Longest = '<' InPlaceChoices '>' {@Longest(choices=$1)}
          |EmptySequence = '#'
          |Nonterminal = Id {@Nonterminal(name=$0)}
          |Terminal: @Terminal = '\'' TerminalChar '\'' {@TerminalChar(char=$2)}
          |  | '.' {@AnyTerminal()}
          |@TerminalChoice(choices: [TerminalChoiceElem])
          |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {TerminalChoice([$1] + $2)}
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

        def unwindOptional(node: Node): Option[Node] = {
            val BindNode(_, body) = node
            body match {
                case BindNode(_, SequenceNode(_, List())) => None
                case BindNode(_, SequenceNode(_, List(optBody))) => Some(optBody)
            }
        }

        def unwindNonterm(name: String, node: Node): Node = {
            val BindNode(NNonterminal(_, Nonterminal(`name`), _), body) = node
            body
        }

        def matchNonterminal(node: Node): AST.Nonterminal = {
            val BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) = node
            AST.Nonterminal(transformNode(nonterminalBody))
        }

        def matchTypeDesc(node: Node): AST.TypeDesc = {
            ???
        }

        def matchLHS(node: Node): AST.LHS = {
            val BindNode(NNonterminal(_, Nonterminal("LHS"), _), BindNode(_, lhsBody: SequenceNode)) = node

            val name = matchNonterminal(lhsBody.children(0))
            val typeDesc = unwindOptional(lhsBody.children(1)) map { opt =>
                val BindNode(_, typeDescBody: SequenceNode) = opt
                matchTypeDesc(typeDescBody.children(3))
            }
            AST.LHS(name, typeDesc)
        }

        def matchTerminal(node: Node): AST.Terminal = node match {
            case BindNode(_, SequenceNode(_, List(_, BindNode(NNonterminal(_, Nonterminal("TerminalChar"), _), terminalCharBody), _))) =>
                AST.TerminalChar(transformNode(terminalCharBody))
            case _ => AST.AnyTerminal
        }

        def matchTerminalChoiceChar(node: Node): AST.TerminalChoiceChar =
            AST.TerminalChoiceChar(transformNode(node))

        def matchTerminalChoiceRange(node: Node): AST.TerminalChoiceRange = node match {
            case BindNode(_, seq: SequenceNode) =>
                AST.TerminalChoiceRange(matchTerminalChoiceChar(seq.children(0)), matchTerminalChoiceChar(seq.children(2)))
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
                AST.TerminalChoice(elem1 +: elem2)
            case BindNode(_, seq: SequenceNode) if seq.children.size == 3 =>
                val elem = matchTerminalChoiceElem(seq.children(1))
                AST.TerminalChoice(List(elem))
        }

        def matchLongest(node: Node): AST.Longest = {
            val BindNode(_, SequenceNode(_, List(_, body, _))) = node
            AST.Longest(matchInPlaceChoices(body))
        }

        def matchInPlaceSequence(node: Node): AST.InPlaceSequence = {
            val BindNode(_, BindNode(_, seq: SequenceNode)) = node

            val choices1 = matchSymbol(unwindNonterm("Symbol", seq.children(0)))
            val choices2_ = unwindRepeat(seq.children(1))
            val choices2 = choices2_ map { b =>
                val BindNode(_, BindNode(_, seq: SequenceNode)) = b
                matchSymbol(unwindNonterm("Symbol", seq.children(1)))
            }
            AST.InPlaceSequence(choices1 +: choices2)
        }

        def matchInPlaceChoices(node: Node): AST.InPlaceChoices = {
            val BindNode(_, BindNode(_, seq: SequenceNode)) = node

            val choices1 = matchInPlaceSequence(seq.children(0))
            val choices2_ = unwindRepeat(seq.children(1))
            val choices2 = choices2_ map { b =>
                val BindNode(_, BindNode(_, seq: SequenceNode)) = b
                matchInPlaceSequence(seq.children(3))
            }
            AST.InPlaceChoices(choices1 +: choices2)
        }

        def matchAtomSymbol(node: Node): AST.AtomSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("Terminal"), _), terminalBody) =>
                matchTerminal(terminalBody)
            case BindNode(NNonterminal(_, Nonterminal("TerminalChoice"), _), terminalChoiceBody) =>
                matchTerminalChoice(terminalChoiceBody)
            case BindNode(NNonterminal(_, Nonterminal("StringLiteral"), _), BindNode(_, stringBody: SequenceNode)) =>
                AST.StringLiteral(transformNode(stringBody.children(1)))
            case BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) =>
                AST.Nonterminal(transformNode(nonterminalBody))
            case BindNode(NNonterminal(_, Nonterminal("Longest"), _), longestBody) =>
                matchLongest(longestBody)
            case BindNode(NNonterminal(_, Nonterminal("EmptySequence"), _), emptySeqBody) =>
                AST.EmptySeq
            case BindNode(_, SequenceNode(_, List(_, parenElem, _))) =>
                matchInPlaceChoices(parenElem)
        }

        def matchPostUnSymbol(node: Node): AST.PostUnSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("AtomSymbol"), _), atomSymbolBody) =>
                matchAtomSymbol(atomSymbolBody)
            case BindNode(_, seq: SequenceNode) =>
                val sym = matchPostUnSymbol(unwindNonterm("PostUnSymbol", seq.children(0)))
                val op = seq.children(2)
                AST.Repeat(sym, transformNode(op))
        }

        def matchPreUnSymbol(node: Node): AST.PreUnSymbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("PostUnSymbol"), _), postUnSymbolBody) =>
                matchPostUnSymbol(postUnSymbolBody)
            case BindNode(_, seq: SequenceNode) =>
                val op = seq.children(0)
                val sym = matchPreUnSymbol(unwindNonterm("PreUnSymbol", seq.children(2)))
                op match {
                    case BindNode(_, TerminalNode(Inputs.Character('^'))) => AST.FollowedBy(sym)
                    case BindNode(_, TerminalNode(Inputs.Character('!'))) => AST.NotFollowedBy(sym)
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
                    case BindNode(_, TerminalNode(Inputs.Character('&'))) => AST.JoinSymbol(lhs, rhs)
                    case BindNode(_, TerminalNode(Inputs.Character('-'))) => AST.ExceptSymbol(lhs, rhs)
                }
        }

        def matchSymbol(node: Node): AST.Symbol = node match {
            case BindNode(NNonterminal(_, Nonterminal("BinSymbol"), _), binSymbolBody) =>
                matchBinSymbol(binSymbolBody)
        }

        def matchProcessor(node: Node): AST.Processor = {
            ???
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
            AST.RHS(elem1 +: elem2)
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
            AST.Rule(lhs, rhss)
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
            AST.Grammar(def1 +: def2)
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

    def main(args: Array[String]): Unit = {
        val ast = grammarSpecToAST(
            """Abc = Z-Y G&H !A ^X 'a'+ 'b'* ('c' 'd')? <'a-z'+>?
              |  | K&Z-"asdf" 'a' 'b-z' '0-9ax-z'
            """.stripMargin)

        println(ast)

        // 문법이 주어지면
        // 1a. processor가 없는 문법 텍스트
        // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
        // 1c. (나중엔) 제너레이트된 파서
        // 2. 정의된 타입들을 정의하는 자바 코드
        // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드
    }
}
