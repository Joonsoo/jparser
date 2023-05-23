package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.ktlib.*

class MetaLang3Ast(
  val source: String,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val nodeId: Int
    val start: Int
    val end: Int
  }

sealed interface TerminalChoiceElem: AstNode

sealed interface PostUnSymbol: PreUnSymbol, AstNode

sealed interface BinderExpr: AstNode

data class EmptySeq(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class StringType(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ValueType, AstNode

sealed interface Terminal: AtomSymbol, AstNode

sealed interface Atom: PrefixNotExpr, AstNode

data class AbstractClassDef(
  val name: TypeName,
  val supers: List<TypeName>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassDef, AstNode

data class NotFollowedBy(
  val notFollowedBy: PreUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PreUnSymbol, AstNode

sealed interface AbstractEnumValue: Atom, AstNode

data class ClassParamDef(
  val name: ParamName,
  val typeDesc: TypeDesc?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class EnumTypeDef(
  val name: EnumTypeName,
  val values: List<String>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): TypeDef, AstNode

data class Nonterminal(
  val name: NonterminalName,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class FollowedBy(
  val followedBy: PreUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PreUnSymbol, AstNode

data class AnyType(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NonNullTypeDesc, AstNode

data class TerminalUnicodeCategory(
  val categoryName: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): TerminalChoiceElem, AstNode

sealed interface Processor: Elem, AstNode

data class TypeName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NonNullTypeDesc, SubType, AstNode

data class Optional(
  val body: PostUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PostUnSymbol, AstNode

sealed interface ValueType: NonNullTypeDesc, AstNode

data class NamedConstructExpr(
  val typeName: TypeName,
  val params: List<NamedParam>,
  val supers: List<TypeName>?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Atom, AstNode

sealed interface ClassDef: SubType, TypeDef, AstNode

sealed interface PExpr: AstNode

data class ValRef(
  val idx: String,
  val condSymPath: List<CondSymDir>?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Ref, AstNode

data class ArrayExpr(
  val elems: List<PExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Atom, AstNode

data class TerminalChoiceRange(
  val startChar: TerminalChoiceChar,
  val endChar: TerminalChoiceChar,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): TerminalChoiceElem, AstNode

sealed interface StringChar: AstNode

data class ExceptSymbol(
  val body: BinSymbol,
  val except: PreUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): BinSymbol, AstNode

data class RawRef(
  val idx: String,
  val condSymPath: List<CondSymDir>?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Ref, AstNode

data class BoolLiteral(
  val value: Boolean,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

sealed interface Literal: Atom, AstNode

data class LHS(
  val name: Nonterminal,
  val typeDesc: TypeDesc?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class TernaryOp(
  val cond: BoolOrExpr,
  val ifTrue: TernaryExpr,
  val ifFalse: TernaryExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): TernaryExpr, AstNode

sealed interface BoolAndExpr: BoolOrExpr, AstNode

sealed interface PrefixNotExpr: AdditiveExpr, AstNode

data class ConcreteClassDef(
  val name: TypeName,
  val supers: List<TypeName>?,
  val params: List<ClassParamDef>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassDef, AstNode

data class FuncCallOrConstructExpr(
  val funcName: TypeOrFuncName,
  val params: List<PExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Atom, AstNode

data class Sequence(
  val seq: List<Elem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Symbol, AstNode

data class InPlaceChoices(
  val choices: List<Sequence>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class SuperDef(
  val typeName: TypeName,
  val subs: List<SubType>?,
  val supers: List<TypeName>?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): SubType, TypeDef, AstNode

data class Rule(
  val lhs: LHS,
  val rhs: List<Sequence>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

sealed interface TypeDef: Def, NonNullTypeDesc, AstNode

data class StrLiteral(
  val value: List<StringChar>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

data class CharAsIs(
  val value: Char,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringChar, TerminalChar, TerminalChoiceChar, AstNode

data class ElvisOp(
  val value: AdditiveExpr,
  val ifNull: ElvisExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ElvisExpr, AstNode

data class AnyTerminal(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Terminal, AstNode

data class BindExpr(
  val ctx: ValRef,
  val binder: BinderExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Atom, BinderExpr, AstNode

sealed interface TerminalChoiceChar: TerminalChoiceElem, AstNode

data class RepeatFromZero(
  val body: PostUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PostUnSymbol, AstNode

data class ExprParen(
  val body: PExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Atom, AstNode

data class EnumTypeName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NonNullTypeDesc, AstNode

data class JoinSymbol(
  val body: BinSymbol,
  val join: PreUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): BinSymbol, AstNode

sealed interface Elem: AstNode

sealed interface AtomSymbol: PostUnSymbol, AstNode

data class ArrayTypeDesc(
  val elemType: TypeDesc,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NonNullTypeDesc, AstNode

data class NullLiteral(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

sealed interface SubType: AstNode

data class CharType(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ValueType, AstNode

data class StringSymbol(
  val value: List<StringChar>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class BinOp(
  val op: Op,
  val lhs: BoolAndExpr,
  val rhs: BoolOrExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AdditiveExpr, AstNode

sealed interface BoolOrExpr: TernaryExpr, AstNode

data class ProcessorBlock(
  val body: PExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): BinderExpr, Processor, AstNode

sealed interface Symbol: Elem, AstNode

data class TypeDesc(
  val typ: NonNullTypeDesc,
  val optional: Boolean,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

sealed interface TernaryExpr: PExpr, AstNode

data class NonterminalName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

sealed interface NonNullTypeDesc: AstNode

sealed interface AdditiveExpr: ElvisExpr, AstNode

data class RepeatFromOne(
  val body: PostUnSymbol,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PostUnSymbol, AstNode

data class Longest(
  val choices: InPlaceChoices,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class NamedParam(
  val name: ParamName,
  val typeDesc: TypeDesc?,
  val expr: PExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

sealed interface Def: AstNode

data class PrefixOp(
  val op: PreOp,
  val expr: PrefixNotExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PrefixNotExpr, AstNode

data class CharLiteral(
  val value: TerminalChar,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

data class TerminalChoice(
  val choices: List<TerminalChoiceElem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AtomSymbol, AstNode

data class ShortenedEnumValue(
  val valueName: EnumValueName,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AbstractEnumValue, AstNode

data class TypeOrFuncName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class ParamName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class CharUnicode(
  val code: List<Char>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringChar, TerminalChar, TerminalChoiceChar, AstNode

sealed interface PreUnSymbol: BinSymbol, AstNode

data class BooleanType(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ValueType, AstNode

sealed interface BinSymbol: Symbol, AstNode

data class TypedPExpr(
  val body: TernaryExpr,
  val typ: TypeDesc,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PExpr, AstNode

sealed interface TerminalChar: Terminal, AstNode

data class CanonicalEnumValue(
  val enumName: EnumTypeName,
  val valueName: EnumValueName,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AbstractEnumValue, AstNode

data class CharEscaped(
  val escapeCode: Char,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringChar, TerminalChar, TerminalChoiceChar, AstNode

sealed interface Ref: Atom, BinderExpr, Processor, AstNode

sealed interface BoolEqExpr: BoolAndExpr, AstNode

data class EnumValueName(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

sealed interface ElvisExpr: BoolEqExpr, AstNode

data class Grammar(
  val defs: List<Def>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode
enum class CondSymDir { BODY, COND }
enum class KeyWord { BOOLEAN, CHAR, FALSE, NULL, STRING, TRUE }
enum class Op { ADD, AND, EQ, NE, OR }
enum class PreOp { NOT }

fun matchStart(): Grammar {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchGrammar(kernel.beginGen, kernel.endGen)
}

fun matchGrammar(beginGen: Int, endGen: Int): Grammar {
val var1 = getSequenceElems(history, 3, listOf(4,57,476,4), beginGen, endGen)
val var2 = matchDef(var1[1].first, var1[1].second)
val var3 = unrollRepeat0(history, 476, 478, 7, 477, var1[2].first, var1[2].second).map { k ->
val var4 = getSequenceElems(history, 480, listOf(481,57), k.first, k.second)
val var5 = matchDef(var4[1].first, var4[1].second)
var5
}
val var6 = Grammar(listOf(var2) + var3, nextId(), beginGen, endGen)
return var6
}

fun matchDef(beginGen: Int, endGen: Int): Def {
val var7 = history[endGen].findByBeginGenOpt(58, 1, beginGen)
val var8 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var7 != null, var8 != null))
val var9 = when {
var7 != null -> {
val var10 = getSequenceElems(history, 58, listOf(59), beginGen, endGen)
val var11 = matchRule(var10[0].first, var10[0].second)
var11
}
else -> {
val var12 = getSequenceElems(history, 141, listOf(142), beginGen, endGen)
val var13 = matchTypeDef(var12[0].first, var12[0].second)
var13
}
}
return var9
}

fun matchRule(beginGen: Int, endGen: Int): Rule {
val var14 = getSequenceElems(history, 60, listOf(61,4,215,4,216), beginGen, endGen)
val var15 = matchLHS(var14[0].first, var14[0].second)
val var16 = getSequenceElems(history, 218, listOf(219,471), var14[4].first, var14[4].second)
val var17 = matchRHS(var16[0].first, var16[0].second)
val var18 = unrollRepeat0(history, 471, 473, 7, 472, var16[1].first, var16[1].second).map { k ->
val var19 = getSequenceElems(history, 475, listOf(4,304,4,219), k.first, k.second)
val var20 = matchRHS(var19[3].first, var19[3].second)
var20
}
val var21 = Rule(var15, listOf(var17) + var18, nextId(), beginGen, endGen)
return var21
}

fun matchLHS(beginGen: Int, endGen: Int): LHS {
val var22 = getSequenceElems(history, 62, listOf(63,116), beginGen, endGen)
val var23 = matchNonterminal(var22[0].first, var22[0].second)
val var24 = history[var22[1].second].findByBeginGenOpt(56, 1, var22[1].first)
val var25 = history[var22[1].second].findByBeginGenOpt(117, 1, var22[1].first)
check(hasSingleTrue(var24 != null, var25 != null))
val var26 = when {
var24 != null -> null
else -> {
val var27 = getSequenceElems(history, 119, listOf(4,120,4,121), var22[1].first, var22[1].second)
val var28 = matchTypeDesc(var27[3].first, var27[3].second)
var28
}
}
val var29 = LHS(var23, var26, nextId(), beginGen, endGen)
return var29
}

fun matchNonterminal(beginGen: Int, endGen: Int): Nonterminal {
val var30 = getSequenceElems(history, 64, listOf(65), beginGen, endGen)
val var31 = matchNonterminalName(var30[0].first, var30[0].second)
val var32 = Nonterminal(var31, nextId(), beginGen, endGen)
return var32
}

fun matchTypeDef(beginGen: Int, endGen: Int): TypeDef {
val var33 = history[endGen].findByBeginGenOpt(143, 1, beginGen)
val var34 = history[endGen].findByBeginGenOpt(178, 1, beginGen)
val var35 = history[endGen].findByBeginGenOpt(199, 1, beginGen)
check(hasSingleTrue(var33 != null, var34 != null, var35 != null))
val var36 = when {
var33 != null -> {
val var37 = getSequenceElems(history, 143, listOf(144), beginGen, endGen)
val var38 = matchClassDef(var37[0].first, var37[0].second)
var38
}
var34 != null -> {
val var39 = getSequenceElems(history, 178, listOf(179), beginGen, endGen)
val var40 = matchSuperDef(var39[0].first, var39[0].second)
var40
}
else -> {
val var41 = getSequenceElems(history, 199, listOf(200), beginGen, endGen)
val var42 = matchEnumTypeDef(var41[0].first, var41[0].second)
var42
}
}
return var36
}

fun matchEnumTypeDef(beginGen: Int, endGen: Int): EnumTypeDef {
val var43 = getSequenceElems(history, 201, listOf(138,4,185,4,202,4,198), beginGen, endGen)
val var44 = matchEnumTypeName(var43[0].first, var43[0].second)
val var45 = getSequenceElems(history, 204, listOf(70,205), var43[4].first, var43[4].second)
val var46 = matchId(var45[0].first, var45[0].second)
val var47 = unrollRepeat0(history, 205, 207, 7, 206, var45[1].first, var45[1].second).map { k ->
val var48 = getSequenceElems(history, 209, listOf(4,158,4,70), k.first, k.second)
val var49 = matchId(var48[3].first, var48[3].second)
var49
}
val var50 = EnumTypeDef(var44, listOf(var46) + var47, nextId(), beginGen, endGen)
return var50
}

fun matchId(beginGen: Int, endGen: Int): String {
val var51 = getSequenceElems(history, 71, listOf(72), beginGen, endGen)
val var52 = getSequenceElems(history, 75, listOf(76,77), var51[0].first, var51[0].second)
val var53 = unrollRepeat0(history, 77, 79, 7, 78, var52[1].first, var52[1].second).map { k ->
source[k.first]
}
return source[var52[0].first].toString() + var53.joinToString("") { it.toString() }
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var54 = history[endGen].findByBeginGenOpt(145, 3, beginGen)
val var55 = history[endGen].findByBeginGenOpt(160, 3, beginGen)
val var56 = history[endGen].findByBeginGenOpt(177, 5, beginGen)
check(hasSingleTrue(var54 != null, var55 != null, var56 != null))
val var57 = when {
var54 != null -> {
val var58 = getSequenceElems(history, 145, listOf(125,4,146), beginGen, endGen)
val var59 = matchTypeName(var58[0].first, var58[0].second)
val var60 = matchSuperTypes(var58[2].first, var58[2].second)
val var61 = AbstractClassDef(var59, var60, nextId(), beginGen, endGen)
var61
}
var55 != null -> {
val var62 = getSequenceElems(history, 160, listOf(125,4,161), beginGen, endGen)
val var63 = matchTypeName(var62[0].first, var62[0].second)
val var64 = matchClassParamsDef(var62[2].first, var62[2].second)
val var65 = ConcreteClassDef(var63, null, var64, nextId(), beginGen, endGen)
var65
}
else -> {
val var66 = getSequenceElems(history, 177, listOf(125,4,146,4,161), beginGen, endGen)
val var67 = matchTypeName(var66[0].first, var66[0].second)
val var68 = matchSuperTypes(var66[2].first, var66[2].second)
val var69 = matchClassParamsDef(var66[4].first, var66[4].second)
val var70 = ConcreteClassDef(var67, var68, var69, nextId(), beginGen, endGen)
var70
}
}
return var57
}

fun matchTypeName(beginGen: Int, endGen: Int): TypeName {
val var71 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
val var72 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
check(hasSingleTrue(var71 != null, var72 != null))
val var73 = when {
var71 != null -> {
val var74 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
val var75 = matchIdNoKeyword(var74[0].first, var74[0].second)
val var76 = TypeName(var75, nextId(), beginGen, endGen)
var76
}
else -> {
val var77 = getSequenceElems(history, 114, listOf(115,70,115), beginGen, endGen)
val var78 = matchId(var77[1].first, var77[1].second)
val var79 = TypeName(var78, nextId(), beginGen, endGen)
var79
}
}
return var73
}

fun matchIdNoKeyword(beginGen: Int, endGen: Int): String {
val var80 = getSequenceElems(history, 68, listOf(69), beginGen, endGen)
val var81 = matchId(var80[0].first, var80[0].second)
return var81
}

fun matchSuperDef(beginGen: Int, endGen: Int): SuperDef {
val var82 = getSequenceElems(history, 180, listOf(125,181,4,185,186,4,198), beginGen, endGen)
val var83 = matchTypeName(var82[0].first, var82[0].second)
val var84 = history[var82[4].second].findByBeginGenOpt(56, 1, var82[4].first)
val var85 = history[var82[4].second].findByBeginGenOpt(187, 1, var82[4].first)
check(hasSingleTrue(var84 != null, var85 != null))
val var86 = when {
var84 != null -> null
else -> {
val var87 = getSequenceElems(history, 189, listOf(4,190), var82[4].first, var82[4].second)
val var88 = matchSubTypes(var87[1].first, var87[1].second)
var88
}
}
val var89 = history[var82[1].second].findByBeginGenOpt(56, 1, var82[1].first)
val var90 = history[var82[1].second].findByBeginGenOpt(182, 1, var82[1].first)
check(hasSingleTrue(var89 != null, var90 != null))
val var91 = when {
var89 != null -> null
else -> {
val var92 = getSequenceElems(history, 184, listOf(4,146), var82[1].first, var82[1].second)
val var93 = matchSuperTypes(var92[1].first, var92[1].second)
var93
}
}
val var94 = SuperDef(var83, var86, var91, nextId(), beginGen, endGen)
return var94
}

fun matchSuperTypes(beginGen: Int, endGen: Int): List<TypeName> {
val var96 = getSequenceElems(history, 147, listOf(148,4,149,159), beginGen, endGen)
val var97 = history[var96[2].second].findByBeginGenOpt(56, 1, var96[2].first)
val var98 = history[var96[2].second].findByBeginGenOpt(150, 1, var96[2].first)
check(hasSingleTrue(var97 != null, var98 != null))
val var99 = when {
var97 != null -> null
else -> {
val var100 = getSequenceElems(history, 152, listOf(125,153,4), var96[2].first, var96[2].second)
val var101 = matchTypeName(var100[0].first, var100[0].second)
val var102 = unrollRepeat0(history, 153, 155, 7, 154, var100[1].first, var100[1].second).map { k ->
val var103 = getSequenceElems(history, 157, listOf(4,158,4,125), k.first, k.second)
val var104 = matchTypeName(var103[3].first, var103[3].second)
var104
}
listOf(var101) + var102
}
}
val var95 = var99
return (var95 ?: listOf())
}

fun matchSubTypes(beginGen: Int, endGen: Int): List<SubType> {
val var105 = getSequenceElems(history, 191, listOf(192,193), beginGen, endGen)
val var106 = matchSubType(var105[0].first, var105[0].second)
val var107 = unrollRepeat0(history, 193, 195, 7, 194, var105[1].first, var105[1].second).map { k ->
val var108 = getSequenceElems(history, 197, listOf(4,158,4,192), k.first, k.second)
val var109 = matchSubType(var108[3].first, var108[3].second)
var109
}
return listOf(var106) + var107
}

fun matchClassParamsDef(beginGen: Int, endGen: Int): List<ClassParamDef> {
val var111 = getSequenceElems(history, 162, listOf(163,4,164,4,176), beginGen, endGen)
val var112 = history[var111[2].second].findByBeginGenOpt(56, 1, var111[2].first)
val var113 = history[var111[2].second].findByBeginGenOpt(165, 1, var111[2].first)
check(hasSingleTrue(var112 != null, var113 != null))
val var114 = when {
var112 != null -> null
else -> {
val var115 = getSequenceElems(history, 167, listOf(168,171,4), var111[2].first, var111[2].second)
val var116 = matchClassParamDef(var115[0].first, var115[0].second)
val var117 = unrollRepeat0(history, 171, 173, 7, 172, var115[1].first, var115[1].second).map { k ->
val var118 = getSequenceElems(history, 175, listOf(4,158,4,168), k.first, k.second)
val var119 = matchClassParamDef(var118[3].first, var118[3].second)
var119
}
listOf(var116) + var117
}
}
val var110 = var114
return (var110 ?: listOf())
}

fun matchClassParamDef(beginGen: Int, endGen: Int): ClassParamDef {
val var120 = getSequenceElems(history, 169, listOf(170,116), beginGen, endGen)
val var121 = matchParamName(var120[0].first, var120[0].second)
val var122 = history[var120[1].second].findByBeginGenOpt(56, 1, var120[1].first)
val var123 = history[var120[1].second].findByBeginGenOpt(117, 1, var120[1].first)
check(hasSingleTrue(var122 != null, var123 != null))
val var124 = when {
var122 != null -> null
else -> {
val var125 = getSequenceElems(history, 119, listOf(4,120,4,121), var120[1].first, var120[1].second)
val var126 = matchTypeDesc(var125[3].first, var125[3].second)
var126
}
}
val var127 = ClassParamDef(var121, var124, nextId(), beginGen, endGen)
return var127
}

fun matchEnumTypeName(beginGen: Int, endGen: Int): EnumTypeName {
val var128 = getSequenceElems(history, 139, listOf(140,70), beginGen, endGen)
val var129 = matchId(var128[1].first, var128[1].second)
val var130 = EnumTypeName(var129, nextId(), beginGen, endGen)
return var130
}

fun matchRHS(beginGen: Int, endGen: Int): Sequence {
val var131 = getSequenceElems(history, 220, listOf(221), beginGen, endGen)
val var132 = matchSequence(var131[0].first, var131[0].second)
return var132
}

fun matchSequence(beginGen: Int, endGen: Int): Sequence {
val var133 = getSequenceElems(history, 222, listOf(223,466), beginGen, endGen)
val var134 = matchElem(var133[0].first, var133[0].second)
val var135 = unrollRepeat0(history, 466, 468, 7, 467, var133[1].first, var133[1].second).map { k ->
val var136 = getSequenceElems(history, 470, listOf(4,223), k.first, k.second)
val var137 = matchElem(var136[1].first, var136[1].second)
var137
}
val var138 = Sequence(listOf(var134) + var135, nextId(), beginGen, endGen)
return var138
}

fun matchElem(beginGen: Int, endGen: Int): Elem {
val var139 = history[endGen].findByBeginGenOpt(224, 1, beginGen)
val var140 = history[endGen].findByBeginGenOpt(314, 1, beginGen)
check(hasSingleTrue(var139 != null, var140 != null))
val var141 = when {
var139 != null -> {
val var142 = getSequenceElems(history, 224, listOf(225), beginGen, endGen)
val var143 = matchSymbol(var142[0].first, var142[0].second)
var143
}
else -> {
val var144 = getSequenceElems(history, 314, listOf(315), beginGen, endGen)
val var145 = matchProcessor(var144[0].first, var144[0].second)
var145
}
}
return var141
}

fun matchProcessor(beginGen: Int, endGen: Int): Processor {
val var146 = history[endGen].findByBeginGenOpt(316, 1, beginGen)
val var147 = history[endGen].findByBeginGenOpt(350, 1, beginGen)
check(hasSingleTrue(var146 != null, var147 != null))
val var148 = when {
var146 != null -> {
val var149 = getSequenceElems(history, 316, listOf(317), beginGen, endGen)
val var150 = matchRef(var149[0].first, var149[0].second)
var150
}
else -> {
val var151 = getSequenceElems(history, 350, listOf(351), beginGen, endGen)
val var152 = matchPExprBlock(var151[0].first, var151[0].second)
var152
}
}
return var148
}

fun matchSubType(beginGen: Int, endGen: Int): SubType {
val var153 = history[endGen].findByBeginGenOpt(124, 1, beginGen)
val var154 = history[endGen].findByBeginGenOpt(143, 1, beginGen)
val var155 = history[endGen].findByBeginGenOpt(178, 1, beginGen)
check(hasSingleTrue(var153 != null, var154 != null, var155 != null))
val var156 = when {
var153 != null -> {
val var157 = getSequenceElems(history, 124, listOf(125), beginGen, endGen)
val var158 = matchTypeName(var157[0].first, var157[0].second)
var158
}
var154 != null -> {
val var159 = getSequenceElems(history, 143, listOf(144), beginGen, endGen)
val var160 = matchClassDef(var159[0].first, var159[0].second)
var160
}
else -> {
val var161 = getSequenceElems(history, 178, listOf(179), beginGen, endGen)
val var162 = matchSuperDef(var161[0].first, var161[0].second)
var162
}
}
return var156
}

fun matchParamName(beginGen: Int, endGen: Int): ParamName {
val var163 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
val var164 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
check(hasSingleTrue(var163 != null, var164 != null))
val var165 = when {
var163 != null -> {
val var166 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
val var167 = matchIdNoKeyword(var166[0].first, var166[0].second)
val var168 = ParamName(var167, nextId(), beginGen, endGen)
var168
}
else -> {
val var169 = getSequenceElems(history, 114, listOf(115,70,115), beginGen, endGen)
val var170 = matchId(var169[1].first, var169[1].second)
val var171 = ParamName(var170, nextId(), beginGen, endGen)
var171
}
}
return var165
}

fun matchPExprBlock(beginGen: Int, endGen: Int): ProcessorBlock {
val var172 = getSequenceElems(history, 352, listOf(185,4,353,4,198), beginGen, endGen)
val var173 = matchPExpr(var172[2].first, var172[2].second)
val var174 = ProcessorBlock(var173, nextId(), beginGen, endGen)
return var174
}

fun matchPExpr(beginGen: Int, endGen: Int): PExpr {
val var175 = history[endGen].findByBeginGenOpt(354, 5, beginGen)
val var176 = history[endGen].findByBeginGenOpt(464, 1, beginGen)
check(hasSingleTrue(var175 != null, var176 != null))
val var177 = when {
var175 != null -> {
val var178 = getSequenceElems(history, 354, listOf(355,4,120,4,121), beginGen, endGen)
val var179 = matchTernaryExpr(var178[0].first, var178[0].second)
val var180 = matchTypeDesc(var178[4].first, var178[4].second)
val var181 = TypedPExpr(var179, var180, nextId(), beginGen, endGen)
var181
}
else -> {
val var182 = getSequenceElems(history, 464, listOf(355), beginGen, endGen)
val var183 = matchTernaryExpr(var182[0].first, var182[0].second)
var183
}
}
return var177
}

fun matchRef(beginGen: Int, endGen: Int): Ref {
val var184 = history[endGen].findByBeginGenOpt(318, 1, beginGen)
val var185 = history[endGen].findByBeginGenOpt(345, 1, beginGen)
check(hasSingleTrue(var184 != null, var185 != null))
val var186 = when {
var184 != null -> {
val var187 = getSequenceElems(history, 318, listOf(319), beginGen, endGen)
val var188 = matchValRef(var187[0].first, var187[0].second)
var188
}
else -> {
val var189 = getSequenceElems(history, 345, listOf(346), beginGen, endGen)
val var190 = matchRawRef(var189[0].first, var189[0].second)
var190
}
}
return var186
}

fun matchValRef(beginGen: Int, endGen: Int): ValRef {
val var191 = getSequenceElems(history, 320, listOf(321,322,332), beginGen, endGen)
val var192 = matchRefIdx(var191[2].first, var191[2].second)
val var193 = history[var191[1].second].findByBeginGenOpt(56, 1, var191[1].first)
val var194 = history[var191[1].second].findByBeginGenOpt(323, 1, var191[1].first)
check(hasSingleTrue(var193 != null, var194 != null))
val var195 = when {
var193 != null -> null
else -> {
val var196 = matchCondSymPath(var191[1].first, var191[1].second)
var196
}
}
val var197 = ValRef(var192, var195, nextId(), beginGen, endGen)
return var197
}

fun matchCondSymPath(beginGen: Int, endGen: Int): List<CondSymDir> {
val var198 = getSequenceElems(history, 324, listOf(325), beginGen, endGen)
val var199 = unrollRepeat1(history, 325, 326, 326, 331, var198[0].first, var198[0].second).map { k ->
val var200 = history[k.second].findByBeginGenOpt(327, 1, k.first)
val var201 = history[k.second].findByBeginGenOpt(329, 1, k.first)
check(hasSingleTrue(var200 != null, var201 != null))
val var202 = when {
var200 != null -> CondSymDir.BODY
else -> CondSymDir.COND
}
var202
}
return var199
}

fun matchRawRef(beginGen: Int, endGen: Int): RawRef {
val var203 = getSequenceElems(history, 347, listOf(348,322,332), beginGen, endGen)
val var204 = matchRefIdx(var203[2].first, var203[2].second)
val var205 = history[var203[1].second].findByBeginGenOpt(56, 1, var203[1].first)
val var206 = history[var203[1].second].findByBeginGenOpt(323, 1, var203[1].first)
check(hasSingleTrue(var205 != null, var206 != null))
val var207 = when {
var205 != null -> null
else -> {
val var208 = matchCondSymPath(var203[1].first, var203[1].second)
var208
}
}
val var209 = RawRef(var204, var207, nextId(), beginGen, endGen)
return var209
}

fun matchRefIdx(beginGen: Int, endGen: Int): String {
val var210 = getSequenceElems(history, 333, listOf(334), beginGen, endGen)
val var211 = history[var210[0].second].findByBeginGenOpt(336, 1, var210[0].first)
val var212 = history[var210[0].second].findByBeginGenOpt(339, 1, var210[0].first)
check(hasSingleTrue(var211 != null, var212 != null))
val var213 = when {
var211 != null -> {
val var214 = getSequenceElems(history, 337, listOf(338), var210[0].first, var210[0].second)
source[var214[0].first].toString()
}
else -> {
val var215 = getSequenceElems(history, 340, listOf(341,342), var210[0].first, var210[0].second)
val var216 = unrollRepeat0(history, 342, 344, 7, 343, var215[1].first, var215[1].second).map { k ->
source[k.first]
}
source[var215[0].first].toString() + var216.joinToString("") { it.toString() }
}
}
return var213
}

fun matchSymbol(beginGen: Int, endGen: Int): Symbol {
val var217 = getSequenceElems(history, 226, listOf(227), beginGen, endGen)
val var218 = matchBinSymbol(var217[0].first, var217[0].second)
return var218
}

fun matchBinSymbol(beginGen: Int, endGen: Int): BinSymbol {
val var219 = history[endGen].findByBeginGenOpt(228, 5, beginGen)
val var220 = history[endGen].findByBeginGenOpt(312, 5, beginGen)
val var221 = history[endGen].findByBeginGenOpt(313, 1, beginGen)
check(hasSingleTrue(var219 != null, var220 != null, var221 != null))
val var222 = when {
var219 != null -> {
val var223 = getSequenceElems(history, 228, listOf(227,4,229,4,230), beginGen, endGen)
val var224 = matchBinSymbol(var223[0].first, var223[0].second)
val var225 = matchPreUnSymbol(var223[4].first, var223[4].second)
val var226 = JoinSymbol(var224, var225, nextId(), beginGen, endGen)
var226
}
var220 != null -> {
val var227 = getSequenceElems(history, 312, listOf(227,4,273,4,230), beginGen, endGen)
val var228 = matchBinSymbol(var227[0].first, var227[0].second)
val var229 = matchPreUnSymbol(var227[4].first, var227[4].second)
val var230 = ExceptSymbol(var228, var229, nextId(), beginGen, endGen)
var230
}
else -> {
val var231 = getSequenceElems(history, 313, listOf(230), beginGen, endGen)
val var232 = matchPreUnSymbol(var231[0].first, var231[0].second)
var232
}
}
return var222
}

fun matchPreUnSymbol(beginGen: Int, endGen: Int): PreUnSymbol {
val var233 = history[endGen].findByBeginGenOpt(231, 3, beginGen)
val var234 = history[endGen].findByBeginGenOpt(233, 3, beginGen)
val var235 = history[endGen].findByBeginGenOpt(235, 1, beginGen)
check(hasSingleTrue(var233 != null, var234 != null, var235 != null))
val var236 = when {
var233 != null -> {
val var237 = getSequenceElems(history, 231, listOf(232,4,230), beginGen, endGen)
val var238 = matchPreUnSymbol(var237[2].first, var237[2].second)
val var239 = FollowedBy(var238, nextId(), beginGen, endGen)
var239
}
var234 != null -> {
val var240 = getSequenceElems(history, 233, listOf(234,4,230), beginGen, endGen)
val var241 = matchPreUnSymbol(var240[2].first, var240[2].second)
val var242 = NotFollowedBy(var241, nextId(), beginGen, endGen)
var242
}
else -> {
val var243 = getSequenceElems(history, 235, listOf(236), beginGen, endGen)
val var244 = matchPostUnSymbol(var243[0].first, var243[0].second)
var244
}
}
return var236
}

fun matchPostUnSymbol(beginGen: Int, endGen: Int): PostUnSymbol {
val var245 = history[endGen].findByBeginGenOpt(237, 3, beginGen)
val var246 = history[endGen].findByBeginGenOpt(238, 3, beginGen)
val var247 = history[endGen].findByBeginGenOpt(239, 3, beginGen)
val var248 = history[endGen].findByBeginGenOpt(241, 1, beginGen)
check(hasSingleTrue(var245 != null, var246 != null, var247 != null, var248 != null))
val var249 = when {
var245 != null -> {
val var250 = getSequenceElems(history, 237, listOf(236,4,214), beginGen, endGen)
val var251 = matchPostUnSymbol(var250[0].first, var250[0].second)
val var252 = Optional(var251, nextId(), beginGen, endGen)
var252
}
var246 != null -> {
val var253 = getSequenceElems(history, 238, listOf(236,4,43), beginGen, endGen)
val var254 = matchPostUnSymbol(var253[0].first, var253[0].second)
val var255 = RepeatFromZero(var254, nextId(), beginGen, endGen)
var255
}
var247 != null -> {
val var256 = getSequenceElems(history, 239, listOf(236,4,240), beginGen, endGen)
val var257 = matchPostUnSymbol(var256[0].first, var256[0].second)
val var258 = RepeatFromOne(var257, nextId(), beginGen, endGen)
var258
}
else -> {
val var259 = getSequenceElems(history, 241, listOf(242), beginGen, endGen)
val var260 = matchAtomSymbol(var259[0].first, var259[0].second)
var260
}
}
return var249
}

fun matchAtomSymbol(beginGen: Int, endGen: Int): AtomSymbol {
val var261 = history[endGen].findByBeginGenOpt(243, 1, beginGen)
val var262 = history[endGen].findByBeginGenOpt(259, 1, beginGen)
val var263 = history[endGen].findByBeginGenOpt(283, 1, beginGen)
val var264 = history[endGen].findByBeginGenOpt(295, 1, beginGen)
val var265 = history[endGen].findByBeginGenOpt(296, 5, beginGen)
val var266 = history[endGen].findByBeginGenOpt(305, 1, beginGen)
val var267 = history[endGen].findByBeginGenOpt(308, 1, beginGen)
check(hasSingleTrue(var261 != null, var262 != null, var263 != null, var264 != null, var265 != null, var266 != null, var267 != null))
val var268 = when {
var261 != null -> {
val var269 = getSequenceElems(history, 243, listOf(244), beginGen, endGen)
val var270 = matchTerminal(var269[0].first, var269[0].second)
var270
}
var262 != null -> {
val var271 = getSequenceElems(history, 259, listOf(260), beginGen, endGen)
val var272 = matchTerminalChoice(var271[0].first, var271[0].second)
var272
}
var263 != null -> {
val var273 = getSequenceElems(history, 283, listOf(284), beginGen, endGen)
val var274 = matchStringSymbol(var273[0].first, var273[0].second)
var274
}
var264 != null -> {
val var275 = getSequenceElems(history, 295, listOf(63), beginGen, endGen)
val var276 = matchNonterminal(var275[0].first, var275[0].second)
var276
}
var265 != null -> {
val var277 = getSequenceElems(history, 296, listOf(163,4,297,4,176), beginGen, endGen)
val var278 = matchInPlaceChoices(var277[2].first, var277[2].second)
var278
}
var266 != null -> {
val var279 = getSequenceElems(history, 305, listOf(306), beginGen, endGen)
val var280 = matchLongest(var279[0].first, var279[0].second)
var280
}
else -> {
val var281 = getSequenceElems(history, 308, listOf(309), beginGen, endGen)
val var282 = matchEmptySequence(var281[0].first, var281[0].second)
var282
}
}
return var268
}

fun matchTerminal(beginGen: Int, endGen: Int): Terminal {
val var283 = history[endGen].findByBeginGenOpt(245, 3, beginGen)
val var284 = history[endGen].findByBeginGenOpt(257, 1, beginGen)
check(hasSingleTrue(var283 != null, var284 != null))
val var285 = when {
var283 != null -> {
val var286 = getSequenceElems(history, 245, listOf(246,247,246), beginGen, endGen)
val var287 = matchTerminalChar(var286[1].first, var286[1].second)
var287
}
else -> {
val var288 = AnyTerminal(nextId(), beginGen, endGen)
var288
}
}
return var285
}

fun matchInPlaceChoices(beginGen: Int, endGen: Int): InPlaceChoices {
val var289 = getSequenceElems(history, 298, listOf(221,299), beginGen, endGen)
val var290 = matchSequence(var289[0].first, var289[0].second)
val var291 = unrollRepeat0(history, 299, 301, 7, 300, var289[1].first, var289[1].second).map { k ->
val var292 = getSequenceElems(history, 303, listOf(4,304,4,221), k.first, k.second)
val var293 = matchSequence(var292[3].first, var292[3].second)
var293
}
val var294 = InPlaceChoices(listOf(var290) + var291, nextId(), beginGen, endGen)
return var294
}

fun matchStringSymbol(beginGen: Int, endGen: Int): StringSymbol {
val var295 = getSequenceElems(history, 285, listOf(286,287,286), beginGen, endGen)
val var296 = unrollRepeat0(history, 287, 289, 7, 288, var295[1].first, var295[1].second).map { k ->
val var297 = matchStringChar(k.first, k.second)
var297
}
val var298 = StringSymbol(var296, nextId(), beginGen, endGen)
return var298
}

fun matchStringChar(beginGen: Int, endGen: Int): StringChar {
val var299 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
val var300 = history[endGen].findByBeginGenOpt(290, 1, beginGen)
val var301 = history[endGen].findByBeginGenOpt(293, 2, beginGen)
check(hasSingleTrue(var299 != null, var300 != null, var301 != null))
val var302 = when {
var299 != null -> {
val var303 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
val var304 = matchUnicodeChar(var303[0].first, var303[0].second)
var304
}
var300 != null -> {
val var305 = getSequenceElems(history, 290, listOf(291), beginGen, endGen)
val var306 = CharAsIs(source[var305[0].first], nextId(), beginGen, endGen)
var306
}
else -> {
val var307 = getSequenceElems(history, 293, listOf(250,294), beginGen, endGen)
val var308 = CharEscaped(source[var307[1].first], nextId(), beginGen, endGen)
var308
}
}
return var302
}

fun matchUnicodeChar(beginGen: Int, endGen: Int): CharUnicode {
val var309 = getSequenceElems(history, 255, listOf(250,106,256,256,256,256), beginGen, endGen)
val var310 = CharUnicode(listOf(source[var309[2].first], source[var309[3].first], source[var309[4].first], source[var309[5].first]), nextId(), beginGen, endGen)
return var310
}

fun matchLongest(beginGen: Int, endGen: Int): Longest {
val var311 = getSequenceElems(history, 307, listOf(148,4,297,4,159), beginGen, endGen)
val var312 = matchInPlaceChoices(var311[2].first, var311[2].second)
val var313 = Longest(var312, nextId(), beginGen, endGen)
return var313
}

fun matchTerminalChoice(beginGen: Int, endGen: Int): TerminalChoice {
val var314 = history[endGen].findByBeginGenOpt(261, 4, beginGen)
val var315 = history[endGen].findByBeginGenOpt(282, 3, beginGen)
check(hasSingleTrue(var314 != null, var315 != null))
val var316 = when {
var314 != null -> {
val var317 = getSequenceElems(history, 261, listOf(246,262,280,246), beginGen, endGen)
val var318 = matchTerminalChoiceElem(var317[1].first, var317[1].second)
val var319 = unrollRepeat1(history, 280, 262, 262, 281, var317[2].first, var317[2].second).map { k ->
val var320 = matchTerminalChoiceElem(k.first, k.second)
var320
}
val var321 = TerminalChoice(listOf(var318) + var319, nextId(), beginGen, endGen)
var321
}
else -> {
val var322 = getSequenceElems(history, 282, listOf(246,271,246), beginGen, endGen)
val var323 = matchTerminalChoiceRange(var322[1].first, var322[1].second)
val var324 = TerminalChoice(listOf(var323), nextId(), beginGen, endGen)
var324
}
}
return var316
}

fun matchTerminalChoiceElem(beginGen: Int, endGen: Int): TerminalChoiceElem {
val var325 = history[endGen].findByBeginGenOpt(263, 1, beginGen)
val var326 = history[endGen].findByBeginGenOpt(270, 1, beginGen)
val var327 = history[endGen].findByBeginGenOpt(274, 1, beginGen)
check(hasSingleTrue(var325 != null, var326 != null, var327 != null))
val var328 = when {
var325 != null -> {
val var329 = getSequenceElems(history, 263, listOf(264), beginGen, endGen)
val var330 = matchTerminalChoiceChar(var329[0].first, var329[0].second)
var330
}
var326 != null -> {
val var331 = getSequenceElems(history, 270, listOf(271), beginGen, endGen)
val var332 = matchTerminalChoiceRange(var331[0].first, var331[0].second)
var332
}
else -> {
val var333 = getSequenceElems(history, 274, listOf(275), beginGen, endGen)
val var334 = matchTerminalUnicodeCategory(var333[0].first, var333[0].second)
var334
}
}
return var328
}

fun matchTerminalUnicodeCategory(beginGen: Int, endGen: Int): TerminalUnicodeCategory {
val var335 = getSequenceElems(history, 276, listOf(250,277,278,279), beginGen, endGen)
val var336 = TerminalUnicodeCategory(source[var335[1].first].toString() + source[var335[2].first].toString(), nextId(), beginGen, endGen)
return var336
}

fun matchTerminalChoiceRange(beginGen: Int, endGen: Int): TerminalChoiceRange {
val var337 = getSequenceElems(history, 272, listOf(264,273,264), beginGen, endGen)
val var338 = matchTerminalChoiceChar(var337[0].first, var337[0].second)
val var339 = matchTerminalChoiceChar(var337[2].first, var337[2].second)
val var340 = TerminalChoiceRange(var338, var339, nextId(), beginGen, endGen)
return var340
}

fun matchTerminalChoiceChar(beginGen: Int, endGen: Int): TerminalChoiceChar {
val var341 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
val var342 = history[endGen].findByBeginGenOpt(265, 1, beginGen)
val var343 = history[endGen].findByBeginGenOpt(268, 2, beginGen)
check(hasSingleTrue(var341 != null, var342 != null, var343 != null))
val var344 = when {
var341 != null -> {
val var345 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
val var346 = matchUnicodeChar(var345[0].first, var345[0].second)
var346
}
var342 != null -> {
val var347 = getSequenceElems(history, 265, listOf(266), beginGen, endGen)
val var348 = CharAsIs(source[var347[0].first], nextId(), beginGen, endGen)
var348
}
else -> {
val var349 = getSequenceElems(history, 268, listOf(250,269), beginGen, endGen)
val var350 = CharEscaped(source[var349[1].first], nextId(), beginGen, endGen)
var350
}
}
return var344
}

fun matchEmptySequence(beginGen: Int, endGen: Int): EmptySeq {
val var351 = EmptySeq(nextId(), beginGen, endGen)
return var351
}

fun matchTerminalChar(beginGen: Int, endGen: Int): TerminalChar {
val var352 = history[endGen].findByBeginGenOpt(248, 1, beginGen)
val var353 = history[endGen].findByBeginGenOpt(251, 2, beginGen)
val var354 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
check(hasSingleTrue(var352 != null, var353 != null, var354 != null))
val var355 = when {
var352 != null -> {
val var356 = getSequenceElems(history, 248, listOf(249), beginGen, endGen)
val var357 = CharAsIs(source[var356[0].first], nextId(), beginGen, endGen)
var357
}
var353 != null -> {
val var358 = getSequenceElems(history, 251, listOf(250,252), beginGen, endGen)
val var359 = CharEscaped(source[var358[1].first], nextId(), beginGen, endGen)
var359
}
else -> {
val var360 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
val var361 = matchUnicodeChar(var360[0].first, var360[0].second)
var361
}
}
return var355
}

fun matchTypeDesc(beginGen: Int, endGen: Int): TypeDesc {
val var362 = getSequenceElems(history, 122, listOf(123,210), beginGen, endGen)
val var363 = matchNonNullTypeDesc(var362[0].first, var362[0].second)
val var364 = history[var362[1].second].findByBeginGenOpt(56, 1, var362[1].first)
val var365 = history[var362[1].second].findByBeginGenOpt(211, 1, var362[1].first)
check(hasSingleTrue(var364 != null, var365 != null))
val var366 = when {
var364 != null -> null
else -> {
val var367 = getSequenceElems(history, 213, listOf(4,214), var362[1].first, var362[1].second)
source[var367[1].first]
}
}
val var368 = TypeDesc(var363, var366 != null, nextId(), beginGen, endGen)
return var368
}

fun matchTernaryExpr(beginGen: Int, endGen: Int): TernaryExpr {
val var369 = history[endGen].findByBeginGenOpt(356, 9, beginGen)
val var370 = history[endGen].findByBeginGenOpt(465, 1, beginGen)
check(hasSingleTrue(var369 != null, var370 != null))
val var371 = when {
var369 != null -> {
val var372 = getSequenceElems(history, 356, listOf(357,4,214,4,461,4,120,4,461), beginGen, endGen)
val var373 = matchBoolOrExpr(var372[0].first, var372[0].second)
val var374 = getSequenceElems(history, 464, listOf(355), var372[4].first, var372[4].second)
val var375 = matchTernaryExpr(var374[0].first, var374[0].second)
val var376 = getSequenceElems(history, 464, listOf(355), var372[8].first, var372[8].second)
val var377 = matchTernaryExpr(var376[0].first, var376[0].second)
val var378 = TernaryOp(var373, var375, var377, nextId(), beginGen, endGen)
var378
}
else -> {
val var379 = getSequenceElems(history, 465, listOf(357), beginGen, endGen)
val var380 = matchBoolOrExpr(var379[0].first, var379[0].second)
var380
}
}
return var371
}

fun matchBoolOrExpr(beginGen: Int, endGen: Int): BoolOrExpr {
val var381 = history[endGen].findByBeginGenOpt(358, 5, beginGen)
val var382 = history[endGen].findByBeginGenOpt(460, 1, beginGen)
check(hasSingleTrue(var381 != null, var382 != null))
val var383 = when {
var381 != null -> {
val var384 = getSequenceElems(history, 358, listOf(359,4,458,4,357), beginGen, endGen)
val var385 = matchBoolAndExpr(var384[0].first, var384[0].second)
val var386 = matchBoolOrExpr(var384[4].first, var384[4].second)
val var387 = BinOp(Op.AND, var385, var386, nextId(), beginGen, endGen)
var387
}
else -> {
val var388 = getSequenceElems(history, 460, listOf(359), beginGen, endGen)
val var389 = matchBoolAndExpr(var388[0].first, var388[0].second)
var389
}
}
return var383
}

fun matchBoolAndExpr(beginGen: Int, endGen: Int): BoolAndExpr {
val var390 = history[endGen].findByBeginGenOpt(360, 5, beginGen)
val var391 = history[endGen].findByBeginGenOpt(457, 1, beginGen)
check(hasSingleTrue(var390 != null, var391 != null))
val var392 = when {
var390 != null -> {
val var393 = getSequenceElems(history, 360, listOf(361,4,455,4,359), beginGen, endGen)
val var394 = matchBoolEqExpr(var393[0].first, var393[0].second)
val var395 = matchBoolAndExpr(var393[4].first, var393[4].second)
val var396 = BinOp(Op.OR, var394, var395, nextId(), beginGen, endGen)
var396
}
else -> {
val var397 = getSequenceElems(history, 457, listOf(361), beginGen, endGen)
val var398 = matchBoolEqExpr(var397[0].first, var397[0].second)
var398
}
}
return var392
}

fun matchBoolEqExpr(beginGen: Int, endGen: Int): BoolEqExpr {
val var399 = history[endGen].findByBeginGenOpt(362, 5, beginGen)
val var400 = history[endGen].findByBeginGenOpt(454, 1, beginGen)
check(hasSingleTrue(var399 != null, var400 != null))
val var401 = when {
var399 != null -> {
val var402 = getSequenceElems(history, 362, listOf(363,4,445,4,361), beginGen, endGen)
val var403 = history[var402[2].second].findByBeginGenOpt(446, 1, var402[2].first)
val var404 = history[var402[2].second].findByBeginGenOpt(450, 1, var402[2].first)
check(hasSingleTrue(var403 != null, var404 != null))
val var405 = when {
var403 != null -> Op.EQ
else -> Op.NE
}
val var406 = matchElvisExpr(var402[0].first, var402[0].second)
val var407 = matchBoolEqExpr(var402[4].first, var402[4].second)
val var408 = BinOp(var405, var406, var407, nextId(), beginGen, endGen)
var408
}
else -> {
val var409 = getSequenceElems(history, 454, listOf(363), beginGen, endGen)
val var410 = matchElvisExpr(var409[0].first, var409[0].second)
var410
}
}
return var401
}

fun matchElvisExpr(beginGen: Int, endGen: Int): ElvisExpr {
val var411 = history[endGen].findByBeginGenOpt(364, 5, beginGen)
val var412 = history[endGen].findByBeginGenOpt(444, 1, beginGen)
check(hasSingleTrue(var411 != null, var412 != null))
val var413 = when {
var411 != null -> {
val var414 = getSequenceElems(history, 364, listOf(365,4,442,4,363), beginGen, endGen)
val var415 = matchAdditiveExpr(var414[0].first, var414[0].second)
val var416 = matchElvisExpr(var414[4].first, var414[4].second)
val var417 = ElvisOp(var415, var416, nextId(), beginGen, endGen)
var417
}
else -> {
val var418 = getSequenceElems(history, 444, listOf(365), beginGen, endGen)
val var419 = matchAdditiveExpr(var418[0].first, var418[0].second)
var419
}
}
return var413
}

fun matchAdditiveExpr(beginGen: Int, endGen: Int): AdditiveExpr {
val var420 = history[endGen].findByBeginGenOpt(366, 5, beginGen)
val var421 = history[endGen].findByBeginGenOpt(441, 1, beginGen)
check(hasSingleTrue(var420 != null, var421 != null))
val var422 = when {
var420 != null -> {
val var423 = getSequenceElems(history, 366, listOf(367,4,438,4,365), beginGen, endGen)
val var424 = matchPrefixNotExpr(var423[0].first, var423[0].second)
val var425 = matchAdditiveExpr(var423[4].first, var423[4].second)
val var426 = BinOp(Op.ADD, var424, var425, nextId(), beginGen, endGen)
var426
}
else -> {
val var427 = getSequenceElems(history, 441, listOf(367), beginGen, endGen)
val var428 = matchPrefixNotExpr(var427[0].first, var427[0].second)
var428
}
}
return var422
}

fun matchPrefixNotExpr(beginGen: Int, endGen: Int): PrefixNotExpr {
val var429 = history[endGen].findByBeginGenOpt(368, 3, beginGen)
val var430 = history[endGen].findByBeginGenOpt(369, 1, beginGen)
check(hasSingleTrue(var429 != null, var430 != null))
val var431 = when {
var429 != null -> {
val var432 = getSequenceElems(history, 368, listOf(234,4,367), beginGen, endGen)
val var433 = matchPrefixNotExpr(var432[2].first, var432[2].second)
val var434 = PrefixOp(PreOp.NOT, var433, nextId(), beginGen, endGen)
var434
}
else -> {
val var435 = getSequenceElems(history, 369, listOf(370), beginGen, endGen)
val var436 = matchAtom(var435[0].first, var435[0].second)
var436
}
}
return var431
}

fun matchAtom(beginGen: Int, endGen: Int): Atom {
val var437 = history[endGen].findByBeginGenOpt(316, 1, beginGen)
val var438 = history[endGen].findByBeginGenOpt(371, 1, beginGen)
val var439 = history[endGen].findByBeginGenOpt(375, 1, beginGen)
val var440 = history[endGen].findByBeginGenOpt(390, 1, beginGen)
val var441 = history[endGen].findByBeginGenOpt(405, 1, beginGen)
val var442 = history[endGen].findByBeginGenOpt(408, 1, beginGen)
val var443 = history[endGen].findByBeginGenOpt(422, 1, beginGen)
val var444 = history[endGen].findByBeginGenOpt(437, 5, beginGen)
check(hasSingleTrue(var437 != null, var438 != null, var439 != null, var440 != null, var441 != null, var442 != null, var443 != null, var444 != null))
val var445 = when {
var437 != null -> {
val var446 = getSequenceElems(history, 316, listOf(317), beginGen, endGen)
val var447 = matchRef(var446[0].first, var446[0].second)
var447
}
var438 != null -> {
val var448 = getSequenceElems(history, 371, listOf(372), beginGen, endGen)
val var449 = matchBindExpr(var448[0].first, var448[0].second)
var449
}
var439 != null -> {
val var450 = getSequenceElems(history, 375, listOf(376), beginGen, endGen)
val var451 = matchNamedConstructExpr(var450[0].first, var450[0].second)
var451
}
var440 != null -> {
val var452 = getSequenceElems(history, 390, listOf(391), beginGen, endGen)
val var453 = matchFuncCallOrConstructExpr(var452[0].first, var452[0].second)
var453
}
var441 != null -> {
val var454 = getSequenceElems(history, 405, listOf(406), beginGen, endGen)
val var455 = matchArrayExpr(var454[0].first, var454[0].second)
var455
}
var442 != null -> {
val var456 = getSequenceElems(history, 408, listOf(409), beginGen, endGen)
val var457 = matchLiteral(var456[0].first, var456[0].second)
var457
}
var443 != null -> {
val var458 = getSequenceElems(history, 422, listOf(423), beginGen, endGen)
val var459 = matchEnumValue(var458[0].first, var458[0].second)
var459
}
else -> {
val var460 = getSequenceElems(history, 437, listOf(163,4,353,4,176), beginGen, endGen)
val var461 = matchPExpr(var460[2].first, var460[2].second)
val var462 = ExprParen(var461, nextId(), beginGen, endGen)
var462
}
}
return var445
}

fun matchNamedConstructExpr(beginGen: Int, endGen: Int): NamedConstructExpr {
val var463 = getSequenceElems(history, 377, listOf(125,181,4,378), beginGen, endGen)
val var464 = matchTypeName(var463[0].first, var463[0].second)
val var465 = matchNamedConstructParams(var463[3].first, var463[3].second)
val var466 = history[var463[1].second].findByBeginGenOpt(56, 1, var463[1].first)
val var467 = history[var463[1].second].findByBeginGenOpt(182, 1, var463[1].first)
check(hasSingleTrue(var466 != null, var467 != null))
val var468 = when {
var466 != null -> null
else -> {
val var469 = getSequenceElems(history, 184, listOf(4,146), var463[1].first, var463[1].second)
val var470 = matchSuperTypes(var469[1].first, var469[1].second)
var470
}
}
val var471 = NamedConstructExpr(var464, var465, var468, nextId(), beginGen, endGen)
return var471
}

fun matchNamedConstructParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var472 = getSequenceElems(history, 379, listOf(163,4,380,176), beginGen, endGen)
val var473 = getSequenceElems(history, 382, listOf(383,385,4), var472[2].first, var472[2].second)
val var474 = matchNamedParam(var473[0].first, var473[0].second)
val var475 = unrollRepeat0(history, 385, 387, 7, 386, var473[1].first, var473[1].second).map { k ->
val var476 = getSequenceElems(history, 389, listOf(4,158,4,383), k.first, k.second)
val var477 = matchNamedParam(var476[3].first, var476[3].second)
var477
}
return listOf(var474) + var475
}

fun matchArrayExpr(beginGen: Int, endGen: Int): ArrayExpr {
val var479 = getSequenceElems(history, 407, listOf(127,4,396,128), beginGen, endGen)
val var480 = history[var479[2].second].findByBeginGenOpt(56, 1, var479[2].first)
val var481 = history[var479[2].second].findByBeginGenOpt(397, 1, var479[2].first)
check(hasSingleTrue(var480 != null, var481 != null))
val var482 = when {
var480 != null -> null
else -> {
val var483 = getSequenceElems(history, 399, listOf(353,400,4), var479[2].first, var479[2].second)
val var484 = matchPExpr(var483[0].first, var483[0].second)
val var485 = unrollRepeat0(history, 400, 402, 7, 401, var483[1].first, var483[1].second).map { k ->
val var486 = getSequenceElems(history, 404, listOf(4,158,4,353), k.first, k.second)
val var487 = matchPExpr(var486[3].first, var486[3].second)
var487
}
listOf(var484) + var485
}
}
val var478 = var482
val var488 = ArrayExpr((var478 ?: listOf()), nextId(), beginGen, endGen)
return var488
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var489 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var490 = history[endGen].findByBeginGenOpt(410, 1, beginGen)
val var491 = history[endGen].findByBeginGenOpt(414, 3, beginGen)
val var492 = history[endGen].findByBeginGenOpt(417, 3, beginGen)
check(hasSingleTrue(var489 != null, var490 != null, var491 != null, var492 != null))
val var493 = when {
var489 != null -> {
val var494 = NullLiteral(nextId(), beginGen, endGen)
var494
}
var490 != null -> {
val var495 = getSequenceElems(history, 410, listOf(411), beginGen, endGen)
val var496 = history[var495[0].second].findByBeginGenOpt(412, 1, var495[0].first)
val var497 = history[var495[0].second].findByBeginGenOpt(413, 1, var495[0].first)
check(hasSingleTrue(var496 != null, var497 != null))
val var498 = when {
var496 != null -> true
else -> false
}
val var499 = BoolLiteral(var498, nextId(), beginGen, endGen)
var499
}
var491 != null -> {
val var500 = getSequenceElems(history, 414, listOf(246,415,246), beginGen, endGen)
val var501 = matchCharChar(var500[1].first, var500[1].second)
val var502 = CharLiteral(var501, nextId(), beginGen, endGen)
var502
}
else -> {
val var503 = getSequenceElems(history, 417, listOf(286,418,286), beginGen, endGen)
val var504 = unrollRepeat0(history, 418, 420, 7, 419, var503[1].first, var503[1].second).map { k ->
val var505 = matchStrChar(k.first, k.second)
var505
}
val var506 = StrLiteral(var504, nextId(), beginGen, endGen)
var506
}
}
return var493
}

fun matchCharChar(beginGen: Int, endGen: Int): TerminalChar {
val var507 = getSequenceElems(history, 416, listOf(247), beginGen, endGen)
val var508 = matchTerminalChar(var507[0].first, var507[0].second)
return var508
}

fun matchFuncCallOrConstructExpr(beginGen: Int, endGen: Int): FuncCallOrConstructExpr {
val var509 = getSequenceElems(history, 392, listOf(393,4,394), beginGen, endGen)
val var510 = matchTypeOrFuncName(var509[0].first, var509[0].second)
val var511 = matchCallParams(var509[2].first, var509[2].second)
val var512 = FuncCallOrConstructExpr(var510, var511, nextId(), beginGen, endGen)
return var512
}

fun matchBindExpr(beginGen: Int, endGen: Int): BindExpr {
val var513 = getSequenceElems(history, 373, listOf(319,374), beginGen, endGen)
val var514 = matchValRef(var513[0].first, var513[0].second)
val var515 = matchBinderExpr(var513[1].first, var513[1].second)
val var516 = BindExpr(var514, var515, nextId(), beginGen, endGen)
return var516
}

fun matchEnumValue(beginGen: Int, endGen: Int): AbstractEnumValue {
val var517 = getSequenceElems(history, 424, listOf(425), beginGen, endGen)
val var518 = history[var517[0].second].findByBeginGenOpt(427, 1, var517[0].first)
val var519 = history[var517[0].second].findByBeginGenOpt(433, 1, var517[0].first)
check(hasSingleTrue(var518 != null, var519 != null))
val var520 = when {
var518 != null -> {
val var521 = getSequenceElems(history, 428, listOf(429), var517[0].first, var517[0].second)
val var522 = matchCanonicalEnumValue(var521[0].first, var521[0].second)
var522
}
else -> {
val var523 = getSequenceElems(history, 434, listOf(435), var517[0].first, var517[0].second)
val var524 = matchShortenedEnumValue(var523[0].first, var523[0].second)
var524
}
}
return var520
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var525 = getSequenceElems(history, 384, listOf(170,116,4,215,4,353), beginGen, endGen)
val var526 = matchParamName(var525[0].first, var525[0].second)
val var527 = history[var525[1].second].findByBeginGenOpt(56, 1, var525[1].first)
val var528 = history[var525[1].second].findByBeginGenOpt(117, 1, var525[1].first)
check(hasSingleTrue(var527 != null, var528 != null))
val var529 = when {
var527 != null -> null
else -> {
val var530 = getSequenceElems(history, 119, listOf(4,120,4,121), var525[1].first, var525[1].second)
val var531 = matchTypeDesc(var530[3].first, var530[3].second)
var531
}
}
val var532 = matchPExpr(var525[5].first, var525[5].second)
val var533 = NamedParam(var526, var529, var532, nextId(), beginGen, endGen)
return var533
}

fun matchBinderExpr(beginGen: Int, endGen: Int): BinderExpr {
val var534 = history[endGen].findByBeginGenOpt(316, 1, beginGen)
val var535 = history[endGen].findByBeginGenOpt(350, 1, beginGen)
val var536 = history[endGen].findByBeginGenOpt(371, 1, beginGen)
check(hasSingleTrue(var534 != null, var535 != null, var536 != null))
val var537 = when {
var534 != null -> {
val var538 = getSequenceElems(history, 316, listOf(317), beginGen, endGen)
val var539 = matchRef(var538[0].first, var538[0].second)
var539
}
var535 != null -> {
val var540 = getSequenceElems(history, 350, listOf(351), beginGen, endGen)
val var541 = matchPExprBlock(var540[0].first, var540[0].second)
var541
}
else -> {
val var542 = getSequenceElems(history, 371, listOf(372), beginGen, endGen)
val var543 = matchBindExpr(var542[0].first, var542[0].second)
var543
}
}
return var537
}

fun matchShortenedEnumValue(beginGen: Int, endGen: Int): ShortenedEnumValue {
val var544 = getSequenceElems(history, 436, listOf(140,431), beginGen, endGen)
val var545 = matchEnumValueName(var544[1].first, var544[1].second)
val var546 = ShortenedEnumValue(var545, nextId(), beginGen, endGen)
return var546
}

fun matchTypeOrFuncName(beginGen: Int, endGen: Int): TypeOrFuncName {
val var547 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
val var548 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
check(hasSingleTrue(var547 != null, var548 != null))
val var549 = when {
var547 != null -> {
val var550 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
val var551 = matchIdNoKeyword(var550[0].first, var550[0].second)
val var552 = TypeOrFuncName(var551, nextId(), beginGen, endGen)
var552
}
else -> {
val var553 = getSequenceElems(history, 114, listOf(115,70,115), beginGen, endGen)
val var554 = matchId(var553[1].first, var553[1].second)
val var555 = TypeOrFuncName(var554, nextId(), beginGen, endGen)
var555
}
}
return var549
}

fun matchCallParams(beginGen: Int, endGen: Int): List<PExpr> {
val var557 = getSequenceElems(history, 395, listOf(163,4,396,176), beginGen, endGen)
val var558 = history[var557[2].second].findByBeginGenOpt(56, 1, var557[2].first)
val var559 = history[var557[2].second].findByBeginGenOpt(397, 1, var557[2].first)
check(hasSingleTrue(var558 != null, var559 != null))
val var560 = when {
var558 != null -> null
else -> {
val var561 = getSequenceElems(history, 399, listOf(353,400,4), var557[2].first, var557[2].second)
val var562 = matchPExpr(var561[0].first, var561[0].second)
val var563 = unrollRepeat0(history, 400, 402, 7, 401, var561[1].first, var561[1].second).map { k ->
val var564 = getSequenceElems(history, 404, listOf(4,158,4,353), k.first, k.second)
val var565 = matchPExpr(var564[3].first, var564[3].second)
var565
}
listOf(var562) + var563
}
}
val var556 = var560
return (var556 ?: listOf())
}

fun matchCanonicalEnumValue(beginGen: Int, endGen: Int): CanonicalEnumValue {
val var566 = getSequenceElems(history, 430, listOf(138,258,431), beginGen, endGen)
val var567 = matchEnumTypeName(var566[0].first, var566[0].second)
val var568 = matchEnumValueName(var566[2].first, var566[2].second)
val var569 = CanonicalEnumValue(var567, var568, nextId(), beginGen, endGen)
return var569
}

fun matchEnumValueName(beginGen: Int, endGen: Int): EnumValueName {
val var570 = getSequenceElems(history, 432, listOf(70), beginGen, endGen)
val var571 = matchId(var570[0].first, var570[0].second)
val var572 = EnumValueName(var571, nextId(), beginGen, endGen)
return var572
}

fun matchStrChar(beginGen: Int, endGen: Int): StringChar {
val var573 = getSequenceElems(history, 421, listOf(289), beginGen, endGen)
val var574 = matchStringChar(var573[0].first, var573[0].second)
return var574
}

fun matchNonterminalName(beginGen: Int, endGen: Int): NonterminalName {
val var575 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
val var576 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
check(hasSingleTrue(var575 != null, var576 != null))
val var577 = when {
var575 != null -> {
val var578 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
val var579 = matchIdNoKeyword(var578[0].first, var578[0].second)
val var580 = NonterminalName(var579, nextId(), beginGen, endGen)
var580
}
else -> {
val var581 = getSequenceElems(history, 114, listOf(115,70,115), beginGen, endGen)
val var582 = matchId(var581[1].first, var581[1].second)
val var583 = NonterminalName(var582, nextId(), beginGen, endGen)
var583
}
}
return var577
}

fun matchNonNullTypeDesc(beginGen: Int, endGen: Int): NonNullTypeDesc {
val var584 = history[endGen].findByBeginGenOpt(124, 1, beginGen)
val var585 = history[endGen].findByBeginGenOpt(126, 5, beginGen)
val var586 = history[endGen].findByBeginGenOpt(129, 1, beginGen)
val var587 = history[endGen].findByBeginGenOpt(131, 1, beginGen)
val var588 = history[endGen].findByBeginGenOpt(137, 1, beginGen)
val var589 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var584 != null, var585 != null, var586 != null, var587 != null, var588 != null, var589 != null))
val var590 = when {
var584 != null -> {
val var591 = getSequenceElems(history, 124, listOf(125), beginGen, endGen)
val var592 = matchTypeName(var591[0].first, var591[0].second)
var592
}
var585 != null -> {
val var593 = getSequenceElems(history, 126, listOf(127,4,121,4,128), beginGen, endGen)
val var594 = matchTypeDesc(var593[2].first, var593[2].second)
val var595 = ArrayTypeDesc(var594, nextId(), beginGen, endGen)
var595
}
var586 != null -> {
val var596 = getSequenceElems(history, 129, listOf(130), beginGen, endGen)
val var597 = matchValueType(var596[0].first, var596[0].second)
var597
}
var587 != null -> {
val var598 = getSequenceElems(history, 131, listOf(132), beginGen, endGen)
val var599 = matchAnyType(var598[0].first, var598[0].second)
var599
}
var588 != null -> {
val var600 = getSequenceElems(history, 137, listOf(138), beginGen, endGen)
val var601 = matchEnumTypeName(var600[0].first, var600[0].second)
var601
}
else -> {
val var602 = getSequenceElems(history, 141, listOf(142), beginGen, endGen)
val var603 = matchTypeDef(var602[0].first, var602[0].second)
var603
}
}
return var590
}

fun matchAnyType(beginGen: Int, endGen: Int): AnyType {
val var604 = AnyType(nextId(), beginGen, endGen)
return var604
}

fun matchValueType(beginGen: Int, endGen: Int): ValueType {
val var605 = history[endGen].findByBeginGenOpt(81, 1, beginGen)
val var606 = history[endGen].findByBeginGenOpt(90, 1, beginGen)
val var607 = history[endGen].findByBeginGenOpt(96, 1, beginGen)
check(hasSingleTrue(var605 != null, var606 != null, var607 != null))
val var608 = when {
var605 != null -> {
val var609 = BooleanType(nextId(), beginGen, endGen)
var609
}
var606 != null -> {
val var610 = CharType(nextId(), beginGen, endGen)
var610
}
else -> {
val var611 = StringType(nextId(), beginGen, endGen)
var611
}
}
return var608
}

}
