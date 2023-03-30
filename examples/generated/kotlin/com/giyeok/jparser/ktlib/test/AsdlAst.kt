package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.ktlib.*

class AsdlAst(
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

data class ModuleDef(
  val name: String,
  val defs: List<SuperClassDef>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class Attributes(
  val attrs: List<Param>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class Param(
  val typeName: String,
  val typeAttr: TypeAttr,
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class SubClassDef(
  val name: String,
  val params: List<Param>?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class SealedClassDefs(
  val subs: List<SubClassDef>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): SuperClassDefBody, AstNode

sealed interface SuperClassDefBody: AstNode

data class SuperClassDef(
  val name: String,
  val body: SuperClassDefBody,
  val attrs: Attributes?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class TupleDef(
  val body: List<Param>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): SuperClassDefBody, AstNode
enum class TypeAttr { OPTIONAL, PLAIN, REPEATED }

fun matchStart(): ModuleDef {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchDefs(kernel.beginGen, kernel.endGen)
}

fun matchDefs(beginGen: Int, endGen: Int): ModuleDef {
val var1 = getSequenceElems(history, 3, listOf(4,47,4), beginGen, endGen)
val var2 = matchModuleDef(var1[1].first, var1[1].second)
return var2
}

fun matchModuleDef(beginGen: Int, endGen: Int): ModuleDef {
val var3 = getSequenceElems(history, 48, listOf(49,4,67,4,68,4,69,126,4,131), beginGen, endGen)
val var4 = matchName(var3[2].first, var3[2].second)
val var5 = matchSuperClassDef(var3[6].first, var3[6].second)
val var6 = unrollRepeat0(history, 126, 128, 7, 127, var3[7].first, var3[7].second).map { k ->
val var7 = getSequenceElems(history, 130, listOf(4,69), k.first, k.second)
val var8 = matchSuperClassDef(var7[1].first, var7[1].second)
var8
}
val var9 = ModuleDef(var4, listOf(var5) + var6, nextId(), beginGen, endGen)
return var9
}

fun matchName(beginGen: Int, endGen: Int): String {
val var10 = getSequenceElems(history, 59, listOf(60), beginGen, endGen)
val var11 = getSequenceElems(history, 63, listOf(64), var10[0].first, var10[0].second)
val var12 = unrollRepeat1(history, 64, 65, 65, 66, var11[0].first, var11[0].second).map { k ->
source[k.first]
}
return var12.joinToString("") { it.toString() }
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var13 = getSequenceElems(history, 70, listOf(67,4,71,4,72,111), beginGen, endGen)
val var14 = matchName(var13[0].first, var13[0].second)
val var15 = matchSuperClassDefBody(var13[4].first, var13[4].second)
val var16 = history[var13[5].second].findByBeginGenOpt(96, 1, var13[5].first)
val var17 = history[var13[5].second].findByBeginGenOpt(112, 1, var13[5].first)
check(hasSingleTrue(var16 != null, var17 != null))
val var18 = when {
var16 != null -> null
else -> {
val var19 = getSequenceElems(history, 114, listOf(4,115), var13[5].first, var13[5].second)
val var20 = matchAttributesDef(var19[1].first, var19[1].second)
var20
}
}
val var21 = SuperClassDef(var14, var15, var18, nextId(), beginGen, endGen)
return var21
}

fun matchSuperClassDefBody(beginGen: Int, endGen: Int): SuperClassDefBody {
val var22 = history[endGen].findByBeginGenOpt(73, 2, beginGen)
val var23 = history[endGen].findByBeginGenOpt(110, 1, beginGen)
check(hasSingleTrue(var22 != null, var23 != null))
val var24 = when {
var22 != null -> {
val var25 = getSequenceElems(history, 73, listOf(74,104), beginGen, endGen)
val var26 = matchSubClassDef(var25[0].first, var25[0].second)
val var27 = unrollRepeat0(history, 104, 106, 7, 105, var25[1].first, var25[1].second).map { k ->
val var28 = getSequenceElems(history, 108, listOf(4,109,4,74), k.first, k.second)
val var29 = matchSubClassDef(var28[3].first, var28[3].second)
var29
}
val var30 = SealedClassDefs(listOf(var26) + var27, nextId(), beginGen, endGen)
var30
}
else -> {
val var31 = getSequenceElems(history, 110, listOf(80), beginGen, endGen)
val var32 = matchParams(var31[0].first, var31[0].second)
val var33 = TupleDef(var32, nextId(), beginGen, endGen)
var33
}
}
return var24
}

fun matchSubClassDef(beginGen: Int, endGen: Int): SubClassDef {
val var34 = getSequenceElems(history, 75, listOf(67,76), beginGen, endGen)
val var35 = matchName(var34[0].first, var34[0].second)
val var36 = history[var34[1].second].findByBeginGenOpt(77, 1, var34[1].first)
val var37 = history[var34[1].second].findByBeginGenOpt(96, 1, var34[1].first)
check(hasSingleTrue(var36 != null, var37 != null))
val var38 = when {
var36 != null -> {
val var39 = getSequenceElems(history, 79, listOf(4,80), var34[1].first, var34[1].second)
val var40 = matchParams(var39[1].first, var39[1].second)
var40
}
else -> null
}
val var41 = SubClassDef(var35, var38, nextId(), beginGen, endGen)
return var41
}

fun matchParams(beginGen: Int, endGen: Int): List<Param> {
val var42 = getSequenceElems(history, 81, listOf(82,4,83,97,4,103), beginGen, endGen)
val var43 = matchParam(var42[2].first, var42[2].second)
val var44 = unrollRepeat0(history, 97, 99, 7, 98, var42[3].first, var42[3].second).map { k ->
val var45 = getSequenceElems(history, 101, listOf(4,102,4,83), k.first, k.second)
val var46 = matchParam(var45[3].first, var45[3].second)
var46
}
return listOf(var43) + var44
}

fun matchParam(beginGen: Int, endGen: Int): Param {
val var47 = getSequenceElems(history, 84, listOf(67,85,4,67), beginGen, endGen)
val var48 = matchName(var47[0].first, var47[0].second)
val var50 = history[var47[1].second].findByBeginGenOpt(86, 1, var47[1].first)
val var51 = history[var47[1].second].findByBeginGenOpt(96, 1, var47[1].first)
check(hasSingleTrue(var50 != null, var51 != null))
val var52 = when {
var50 != null -> {
val var53 = getSequenceElems(history, 88, listOf(4,89), var47[1].first, var47[1].second)
val var54 = history[var53[1].second].findByBeginGenOpt(90, 1, var53[1].first)
val var55 = history[var53[1].second].findByBeginGenOpt(93, 1, var53[1].first)
check(hasSingleTrue(var54 != null, var55 != null))
val var56 = when {
var54 != null -> TypeAttr.REPEATED
else -> TypeAttr.OPTIONAL
}
var56
}
else -> null
}
val var49 = var52
val var57 = matchName(var47[3].first, var47[3].second)
val var58 = Param(var48, (var49 ?: TypeAttr.PLAIN), var57, nextId(), beginGen, endGen)
return var58
}

fun matchAttributesDef(beginGen: Int, endGen: Int): Attributes {
val var59 = getSequenceElems(history, 116, listOf(117,4,80), beginGen, endGen)
val var60 = matchParams(var59[2].first, var59[2].second)
val var61 = Attributes(var60, nextId(), beginGen, endGen)
return var61
}

}
