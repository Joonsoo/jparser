package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.ktlib.*

class CmakeDebugAst(
  val source: String,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val nodeId: Int
    val start: Int
    val end: Int
    fun toShortString(): String
  }

sealed interface Argument: AstNode

data class UnquotedElems(
  val elems: List<UnquotedElem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): UnquotedArgument, AstNode {
  override fun toShortString(): String = "UnquotedElems(elems=${"[${elems.joinToString { it.toShortString() }}]"})"
}

data class BracketArgument(
  val contents: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Argument, AstNode {
  override fun toShortString(): String = "BracketArgument(contents=${contents})"
}

sealed interface UnquotedArgument: Argument, AstNode

sealed interface UnquotedElem: AstNode

data class UnquotedChars(
  val c: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): UnquotedElem, AstNode {
  override fun toShortString(): String = "UnquotedChars(c=${c})"
}


fun matchStart(): Argument {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchArgument(kernel.beginGen, kernel.endGen)
}

fun matchArgument(beginGen: Int, endGen: Int): Argument {
val var1 = history[endGen].findByBeginGenOpt(3, 1, beginGen)
val var2 = history[endGen].findByBeginGenOpt(42, 1, beginGen)
check(hasSingleTrue(var1 != null, var2 != null)) {
  val candidates = listOfNotNull(if (var1 != null) "3 bracket_argument" else null, if (var2 != null) "42 unquoted_argument" else null)
  "Ambiguity found $beginGen..$endGen argument to ${candidates.joinToString()}"
}
val var3 = when {
var1 != null -> {
val var4 = matchBracket_argument(beginGen, endGen)
var4
}
else -> {
val var5 = matchUnquoted_argument(beginGen, endGen)
var5
}
}
return var3
}

fun matchBracket_argument(beginGen: Int, endGen: Int): BracketArgument {
val var1 = history[endGen].findByBeginGenOpt(4, 3, beginGen)
val var2 = history[endGen].findByBeginGenOpt(25, 3, beginGen)
check(hasSingleTrue(var1 != null, var2 != null)) {
  val candidates = listOfNotNull(if (var1 != null) "4 seq [bracket_open_0 (seq [(seq [<any> (la_except bracket_close_0)])* <any>])? bracket_close_0]" else null, if (var2 != null) "25 seq [bracket_open_1 (seq [(seq [<any> (la_except bracket_close_1)])* <any>])? bracket_close_1]" else null)
  "Ambiguity found $beginGen..$endGen bracket_argument to ${candidates.joinToString()}"
}
val var3 = when {
var1 != null -> {
val var5 = getSequenceElems(history, 4, listOf(5,9,20), beginGen, endGen)
val var6 = history[var5[1].second].findByBeginGenOpt(10, 1, var5[1].first)
val var7 = history[var5[1].second].findByBeginGenOpt(24, 1, var5[1].first)
check(hasSingleTrue(var6 != null, var7 != null)) {
  val candidates = listOfNotNull(if (var6 != null) "10 (seq [(seq [<any> (la_except bracket_close_0)])* <any>])" else null, if (var7 != null) "24 (seq [])" else null)
  "Ambiguity found $beginGen..$endGen (seq [(seq [<any> (la_except bracket_close_0)])* <any>])? to ${candidates.joinToString()}"
}
val var8 = when {
var6 != null -> {
val var9 = getSequenceElems(history, 11, listOf(12,17), var5[1].first, var5[1].second)
val var10 = unrollRepeat0(history, 12, 15, 13, 14, var9[0].first, var9[0].second).map { k ->
val var11 = getSequenceElems(history, 16, listOf(17,18), k.first, k.second)
source[var11[0].first]
}
var10.joinToString("") { it.toString() } + source[var9[1].first].toString()
}
else -> null
}
val var4 = var8
val var12 = BracketArgument((var4 ?: ""), nextId(), beginGen, endGen)
var12
}
else -> {
val var14 = getSequenceElems(history, 25, listOf(26,30,39), beginGen, endGen)
val var15 = history[var14[1].second].findByBeginGenOpt(24, 1, var14[1].first)
val var16 = history[var14[1].second].findByBeginGenOpt(31, 1, var14[1].first)
check(hasSingleTrue(var15 != null, var16 != null)) {
  val candidates = listOfNotNull(if (var15 != null) "24 (seq [])" else null, if (var16 != null) "31 (seq [(seq [<any> (la_except bracket_close_1)])* <any>])" else null)
  "Ambiguity found $beginGen..$endGen (seq [(seq [<any> (la_except bracket_close_1)])* <any>])? to ${candidates.joinToString()}"
}
val var17 = when {
var15 != null -> null
else -> {
val var18 = getSequenceElems(history, 32, listOf(33,17), var14[1].first, var14[1].second)
val var19 = unrollRepeat0(history, 33, 35, 13, 34, var18[0].first, var18[0].second).map { k ->
val var20 = getSequenceElems(history, 36, listOf(17,37), k.first, k.second)
source[var20[0].first]
}
var19.joinToString("") { it.toString() } + source[var18[1].first].toString()
}
}
val var13 = var17
val var21 = BracketArgument((var13 ?: ""), nextId(), beginGen, endGen)
var21
}
}
return var3
}

fun matchUnquoted_argument(beginGen: Int, endGen: Int): UnquotedArgument {
val var1 = getSequenceElems(history, 43, listOf(44,47), beginGen, endGen)
val var2 = unrollRepeat1(history, 49, 50, 50, 57, var1[1].first, var1[1].second).map { k ->
val var3 = matchUnquoted_element(k.first, k.second)
var3
}
val var4 = UnquotedElems(var2, nextId(), beginGen, endGen)
return var4
}

fun matchUnquoted_element(beginGen: Int, endGen: Int): UnquotedElem {
val var1 = unrollRepeat1(history, 52, 53, 53, 56, beginGen, endGen).map { k ->
source[k.first]
}
val var2 = UnquotedChars(var1.joinToString("") { it.toString() }, nextId(), beginGen, endGen)
return var2
}

}
