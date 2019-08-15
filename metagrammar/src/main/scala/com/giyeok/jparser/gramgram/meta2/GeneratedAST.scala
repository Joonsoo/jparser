package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.Node

object GeneratedAST {

    case class EmptySeq() extends AtomSymbol

    sealed trait Terminal extends AtomSymbol

    case class PTermSeq(elems: Option[List[PExpr]]) extends PTerm

    case class PTermParen(expr: PExpr) extends PTerm

    case class NotFollowedBy(expr: PreUnSymbol) extends PreUnSymbol

    case class Paren(choices: InPlaceChoices) extends AtomSymbol

    sealed trait TerminalChoiceElem

    case class Nonterminal(name: Node) extends AtomSymbol

    case class FollowedBy(expr: PreUnSymbol) extends PreUnSymbol

    case class BinOpExpr(op: Node, lhs: PExpr, rhs: PTerm) extends PExpr

    sealed trait AbstractConstructExpr extends PTerm

    sealed trait Processor extends Elem

    case class ClassDef(typeName: Node, params: Option[List[ClassParam]]) extends SubType with TypeDef

    case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

    sealed trait StringChar

    sealed trait PExpr extends BoundedPExpr with Processor

    case class ExceptSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

    case class StringLiteral(value: List[StringChar]) extends AtomSymbol

    case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

    case class InPlaceChoices(choices: List[InPlaceSequence])

    case class SuperDef(typeName: Node, subs: Option[List[SubType]]) extends SubType with TypeDef

    case class Rule(lhs: LHS, rhs: List[RHS]) extends Def

    case class Repeat(expr: PostUnSymbol, repeat: Node) extends PostUnSymbol

    case class OnTheFlyTypeDefConstructExpr(typeDef: OnTheFlyTypeDef, params: Option[List[NamedParam]]) extends AbstractConstructExpr

    sealed trait TypeDef extends Def

    sealed trait PostUnSymbol extends PreUnSymbol

    case class CharAsIs(c: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    case class AnyTerminal(c: Node) extends Terminal

    sealed trait TerminalChoiceChar extends TerminalChoiceElem

    case class JoinSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

    sealed trait Elem

    sealed trait AtomSymbol extends PostUnSymbol

    case class RHS(elems: List[Elem])

    case class ArrayTypeDesc(elemType: TypeDesc) extends ValueTypeDesc

    case class BoundPExpr(ctx: Ref, expr: BoundedPExpr) extends PTerm

    sealed trait SubType

    sealed trait BoundedPExpr

    case class ConstructExpr(typeName: Node, params: Option[List[PExpr]]) extends AbstractConstructExpr

    sealed trait ValueTypeDesc

    sealed trait Symbol extends Elem

    case class TypeDesc(typ: ValueTypeDesc, optional: Node)

    case class OnTheFlyTypeDef(name: Node, supers: Option[List[Node]]) extends ValueTypeDesc

    case class Longest(choices: InPlaceChoices) extends AtomSymbol

    case class NamedParam(name: Node, typeDesc: Option[TypeDesc], expr: PExpr)

    sealed trait Def

    sealed trait PTerm extends PExpr

    case class InPlaceSequence(seq: List[Symbol])

    case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

    case class CharUnicode(code: List[Node]) extends StringChar with TerminalChar with TerminalChoiceChar

    sealed trait PreUnSymbol extends BinSymbol

    sealed trait BinSymbol extends Symbol

    case class ClassParam(name: Node, typeDesc: Option[TypeDesc])

    sealed trait TerminalChar extends Terminal

    case class CharEscaped(escapeCode: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    case class Ref(idx: Node) extends PTerm

    case class Grammar(defs: List[Def])

}
