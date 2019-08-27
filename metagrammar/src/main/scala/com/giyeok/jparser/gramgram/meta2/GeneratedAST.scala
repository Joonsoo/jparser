package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}
import com.giyeok.jparser.nparser.NGrammar.{NNonterminal, NRepeat, NSequence, NStart, NTerminal}

object GeneratedAST {

    case class Grammar(defs: List[Def])

    sealed trait Def

    sealed trait TypeDef extends Def

    case class ClassDef(typeName: TypeName, params: Option[List[ClassParam]]) extends SubType with TypeDef

    case class SuperDef(typeName: TypeName, subs: Option[List[SubType]]) extends SubType with TypeDef

    case class TypeName(name: Node) extends SubType with ValueTypeDesc

    case class ClassParam(name: Node, typeDesc: Option[TypeDesc])

    case class TypeDesc(typ: ValueTypeDesc, optional: Node)

    sealed trait ValueTypeDesc

    case class ArrayTypeDesc(elemType: TypeDesc) extends ValueTypeDesc

    sealed trait SubType

    case class OnTheFlyTypeDef(name: TypeName, supers: Option[List[TypeName]]) extends ValueTypeDesc

    case class Rule(lhs: LHS, rhs: List[RHS]) extends Def

    case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

    case class RHS(elems: List[Elem])

    sealed trait Elem

    sealed trait Processor extends Elem

    sealed trait PExpr extends BoundedPExpr with Processor

    case class BinOpExpr(op: Node, lhs: PExpr, rhs: PTerm) extends PExpr

    sealed trait PTerm extends PExpr

    case class PTermParen(expr: PExpr) extends PTerm

    case class Ref(idx: Node) extends PTerm

    case class PTermSeq(elems: Option[List[PExpr]]) extends PTerm

    case class BoundPExpr(ctx: Ref, expr: BoundedPExpr) extends PTerm

    sealed trait BoundedPExpr

    sealed trait AbstractConstructExpr extends PTerm

    case class ConstructExpr(typeName: TypeName, params: Option[List[PExpr]]) extends AbstractConstructExpr

    case class OnTheFlyTypeDefConstructExpr(typeDef: OnTheFlyTypeDef, params: Option[List[NamedParam]]) extends AbstractConstructExpr

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

    case class EmptySeq() extends AtomSymbol

    case class InPlaceChoices(choices: List[InPlaceSequence])

    case class InPlaceSequence(seq: List[Symbol])

    case class Longest(choices: InPlaceChoices) extends AtomSymbol

    case class Nonterminal(name: Node) extends AtomSymbol

    sealed trait Terminal extends AtomSymbol

    case class AnyTerminal(c: Node) extends Terminal

    case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

    sealed trait TerminalChoiceElem

    case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

    case class StringLiteral(value: List[StringChar]) extends AtomSymbol

    case class CharUnicode(code: List[Node]) extends StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChar extends Terminal

    case class CharAsIs(c: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    case class CharEscaped(escapeCode: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChoiceChar extends TerminalChoiceElem

    sealed trait StringChar

    def matchStart(node: Node): Grammar = {
        // matchStart에만 node가 Start로 bind된 것이 오고, 다른 match** 함수들에는 그 심볼은 벗겨진 것이 감
        val BindNode(_: NStart, BindNode(NNonterminal(1, _, _), body)) = node
        matchGrammar(body)
    }

    def matchGrammar(node: Node): Grammar = {
        node match {
            case BindNode(symbol, body) =>
                symbol.id match {
                    case 1 => // WS Def (WS Def)* WS {@Grammar(defs=[$1] + $2$1)}
                        matchGrammarRHS0(body)
                }
        }
        ???
    }

    def unrollRepeat[T](node: Node, f: Node => T): List[T] = {
        val BindNode(repeat: NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
                unrollRepeat(repeating.children(0), f) :+ f(repeating.children(1))
            case BindNode(symbol, _) if symbol.id == repeat.baseSeq =>
                List(f(body))
            case seq@SequenceNode(symbol, _) if symbol.id == repeat.baseSeq =>
                seq.children.take(repeat.symbol.lower).toList map f
        }
    }

    def matchGrammarRHS0(node: Node): Grammar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 2 => // WS Def (WS Def)* WS {@Grammar(defs=[$1] + $2$1)}
                assert(symbol.isInstanceOf[NSequence] && body.isInstanceOf[SequenceNode])
                val bodySeq = body.asInstanceOf[SequenceNode]
                val defs = matchDef(bodySeq.children(1)) +: unrollRepeat(bodySeq.children(2), { x => matchDef(x) })
                Grammar(defs)
        }
    }

    def matchDef(node: Node): Def = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 3 => // Rule
                matchRule(node)
            case 4 => // TypeDef
                matchTypeDef(node)
        }
    }

    def matchRule(node: Node): Rule = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 5 => // LHS WS '=' WS RHSs {@Rule(lhs=$0, rhs=$4)}
                matchRuleRHS0(body)
        }
    }

    def matchRuleRHS0(node: Node): Rule = {
        ???
    }

    def matchTypeDef(node: Node): TypeDef = {
        ???
    }
}
