package com.giyeok.jparser.mgroup3.generated

import com.giyeok.jparser.ktlib.*

class Ast(
  val source: String,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  // start/end are `var` so an incremental delta consumer
  // (AstProtoBinding.DeltaSession) can shift a retained node's span in place
  // when an edit shifts text after the dirty window. CONSUMPTION CONTRACT:
  // a shared node's span is mutated only while a delta is applied (during a
  // recompile), never concurrently with a read — the LSP session manager's
  // per-document lock guarantees edits and reads never overlap. nodeId is an
  // immutable `val` identity that survives across edits (reused nodes keep it).
  sealed interface AstNode {
    val nodeId: Int
    var start: Int
    var end: Int
    fun toShortString(): String
  }

data class ModuleDef(
  val name: String,
  val defs: List<SuperClassDef>,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): AstNode {
  override fun toShortString(): String = "ModuleDef(name=${name}, defs=${"[${defs.joinToString { it.toShortString() }}]"})"
}

data class Attributes(
  val attrs: List<Param>,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): AstNode {
  override fun toShortString(): String = "Attributes(attrs=${"[${attrs.joinToString { it.toShortString() }}]"})"
}

data class Param(
  val typeName: String,
  val typeAttr: TypeAttr,
  val name: String,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): AstNode {
  override fun toShortString(): String = "Param(typeName=${typeName}, typeAttr=${typeAttr}, name=${name})"
}

data class SubClassDef(
  val name: String,
  val params: List<Param>?,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): AstNode {
  override fun toShortString(): String = "SubClassDef(name=${name}, params=${params})"
}

data class SealedClassDefs(
  val subs: List<SubClassDef>,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): SuperClassDefBody, AstNode {
  override fun toShortString(): String = "SealedClassDefs(subs=${"[${subs.joinToString { it.toShortString() }}]"})"
}

sealed interface SuperClassDefBody: AstNode

data class SuperClassDef(
  val name: String,
  val body: SuperClassDefBody,
  val attrs: Attributes?,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): AstNode {
  override fun toShortString(): String = "SuperClassDef(name=${name}, body=${body.toShortString()}, attrs=${attrs?.toShortString()})"
}

data class TupleDef(
  val body: List<Param>,
  override val nodeId: Int,
  override var start: Int,
  override var end: Int,
): SuperClassDefBody, AstNode {
  override fun toShortString(): String = "TupleDef(body=${"[${body.joinToString { it.toShortString() }}]"})"
}
enum class TypeAttr { OPTIONAL, PLAIN, REPEATED }

fun matchStart(): ModuleDef {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchDefs(kernel.beginGen, kernel.endGen)
}

fun matchDefs(beginGen: Int, endGen: Int): ModuleDef {
val var1 = getSequenceElems(history, 3, listOf(4,33,4), beginGen, endGen)
val var2 = matchModuleDef(var1[1].first, var1[1].second)
return var2
}

fun matchModuleDef(beginGen: Int, endGen: Int): ModuleDef {
val var1 = getSequenceElems(history, 34, listOf(35,4,50,4,51,4,52,100,4,104), beginGen, endGen)
val var2 = matchName(var1[2].first, var1[2].second)
val var3 = matchSuperClassDef(var1[6].first, var1[6].second)
val var4 = unrollRepeat0(history, 100, 102, 6, 101, var1[7].first, var1[7].second).map { k ->
val var5 = getSequenceElems(history, 103, listOf(4,52), k.first, k.second)
val var6 = matchSuperClassDef(var5[1].first, var5[1].second)
var6
}
val var7 = ModuleDef(var2, listOf(var3) + var4, nextId(), beginGen, endGen)
return var7
}

fun matchName(beginGen: Int, endGen: Int): String {
val var1 = unrollRepeat1(history, 47, 48, 48, 49, beginGen, endGen).map { k ->
source[k.first]
}
return var1.joinToString("") { it.toString() }
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var1 = getSequenceElems(history, 53, listOf(50,4,54,4,55,85), beginGen, endGen)
val var2 = matchName(var1[0].first, var1[0].second)
val var3 = matchSuperClassDefBody(var1[4].first, var1[4].second)
val var4 = history[var1[5].second].findByBeginGenOpt(73, 1, var1[5].first)
val var5 = history[var1[5].second].findByBeginGenOpt(86, 1, var1[5].first)
check(hasSingleTrue(var4 != null, var5 != null)) {
  val candidates = listOfNotNull(if (var4 != null) "73 (seq [])" else null, if (var5 != null) "86 (seq [WS AttributesDef])" else null)
  "Ambiguity found $beginGen..$endGen (seq [WS AttributesDef])? to ${candidates.joinToString()}"
}
val var6 = when {
var4 != null -> null
else -> {
val var7 = getSequenceElems(history, 87, listOf(4,88), var1[5].first, var1[5].second)
val var8 = matchAttributesDef(var7[1].first, var7[1].second)
var8
}
}
val var9 = SuperClassDef(var2, var3, var6, nextId(), beginGen, endGen)
return var9
}

fun matchSuperClassDefBody(beginGen: Int, endGen: Int): SuperClassDefBody {
val var1 = history[endGen].findByBeginGenOpt(56, 2, beginGen)
val var2 = history[endGen].findByBeginGenOpt(62, 1, beginGen)
check(hasSingleTrue(var1 != null, var2 != null)) {
  val candidates = listOfNotNull(if (var1 != null) "56 seq [SubClassDef (seq [WS '|' WS SubClassDef])*]" else null, if (var2 != null) "62 Params" else null)
  "Ambiguity found $beginGen..$endGen SuperClassDefBody to ${candidates.joinToString()}"
}
val var3 = when {
var1 != null -> {
val var4 = getSequenceElems(history, 56, listOf(57,80), beginGen, endGen)
val var5 = matchSubClassDef(var4[0].first, var4[0].second)
val var6 = unrollRepeat0(history, 80, 82, 6, 81, var4[1].first, var4[1].second).map { k ->
val var7 = getSequenceElems(history, 83, listOf(4,84,4,57), k.first, k.second)
val var8 = matchSubClassDef(var7[3].first, var7[3].second)
var8
}
val var9 = SealedClassDefs(listOf(var5) + var6, nextId(), beginGen, endGen)
var9
}
else -> {
val var10 = matchParams(beginGen, endGen)
val var11 = TupleDef(var10, nextId(), beginGen, endGen)
var11
}
}
return var3
}

fun matchSubClassDef(beginGen: Int, endGen: Int): SubClassDef {
val var1 = getSequenceElems(history, 58, listOf(50,59), beginGen, endGen)
val var2 = matchName(var1[0].first, var1[0].second)
val var3 = history[var1[1].second].findByBeginGenOpt(60, 1, var1[1].first)
val var4 = history[var1[1].second].findByBeginGenOpt(73, 1, var1[1].first)
check(hasSingleTrue(var3 != null, var4 != null)) {
  val candidates = listOfNotNull(if (var3 != null) "60 (seq [WS Params])" else null, if (var4 != null) "73 (seq [])" else null)
  "Ambiguity found $beginGen..$endGen (seq [WS Params])? to ${candidates.joinToString()}"
}
val var5 = when {
var3 != null -> {
val var6 = getSequenceElems(history, 61, listOf(4,62), var1[1].first, var1[1].second)
val var7 = matchParams(var6[1].first, var6[1].second)
var7
}
else -> null
}
val var8 = SubClassDef(var2, var5, nextId(), beginGen, endGen)
return var8
}

fun matchParams(beginGen: Int, endGen: Int): List<Param> {
val var1 = getSequenceElems(history, 63, listOf(64,4,65,74,4,79), beginGen, endGen)
val var2 = matchParam(var1[2].first, var1[2].second)
val var3 = unrollRepeat0(history, 74, 76, 6, 75, var1[3].first, var1[3].second).map { k ->
val var4 = getSequenceElems(history, 77, listOf(4,78,4,65), k.first, k.second)
val var5 = matchParam(var4[3].first, var4[3].second)
var5
}
return listOf(var2) + var3
}

fun matchParam(beginGen: Int, endGen: Int): Param {
val var1 = getSequenceElems(history, 66, listOf(50,67,4,50), beginGen, endGen)
val var2 = matchName(var1[0].first, var1[0].second)
val var4 = history[var1[1].second].findByBeginGenOpt(68, 1, var1[1].first)
val var5 = history[var1[1].second].findByBeginGenOpt(73, 1, var1[1].first)
check(hasSingleTrue(var4 != null, var5 != null)) {
  val candidates = listOfNotNull(if (var4 != null) "68 (seq [WS {'*'|'?'}])" else null, if (var5 != null) "73 (seq [])" else null)
  "Ambiguity found $beginGen..$endGen (seq [WS {'*'|'?'}])? to ${candidates.joinToString()}"
}
val var6 = when {
var4 != null -> {
val var7 = getSequenceElems(history, 69, listOf(4,70), var1[1].first, var1[1].second)
val var8 = history[var7[1].second].findByBeginGenOpt(71, 1, var7[1].first)
val var9 = history[var7[1].second].findByBeginGenOpt(72, 1, var7[1].first)
check(hasSingleTrue(var8 != null, var9 != null)) {
  val candidates = listOfNotNull(if (var8 != null) "71 '*'" else null, if (var9 != null) "72 '?'" else null)
  "Ambiguity found $beginGen..$endGen {'*'|'?'} to ${candidates.joinToString()}"
}
val var10 = when {
var8 != null -> TypeAttr.REPEATED
else -> TypeAttr.OPTIONAL
}
var10
}
else -> null
}
val var3 = var6
val var11 = matchName(var1[3].first, var1[3].second)
val var12 = Param(var2, (var3 ?: TypeAttr.PLAIN), var11, nextId(), beginGen, endGen)
return var12
}

fun matchAttributesDef(beginGen: Int, endGen: Int): Attributes {
val var1 = getSequenceElems(history, 89, listOf(90,4,62), beginGen, endGen)
val var2 = matchParams(var1[2].first, var1[2].second)
val var3 = Attributes(var2, nextId(), beginGen, endGen)
return var3
}

}
