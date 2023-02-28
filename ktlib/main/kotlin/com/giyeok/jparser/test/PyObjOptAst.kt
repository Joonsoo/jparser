package com.giyeok.jparser.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.*

class PyObjKtOptAst(
        val inputs: List<Inputs.Input>,
        val history: List<KernelSet>,
        val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val symbolId: Int
    val start: Int
    val end: Int
  }

  sealed interface Value

  data class BoolValue(
          val value: BoolEnum,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "BoolValue(value=$value)"
  }

  data class IntLiteral(
    val value: String,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : AstNode {
    override fun toString(): String =
      "IntLiteral(value=$value)"
  }

  data class IntValue(
          val value: IntLiteral,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "IntValue(value=$value)"
  }

  data class ListValue(
          val elems: List<Value>?,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "ListValue(elems=$elems)"
  }

  data class ObjField(
          val name: StrLiteral,
          val value: Value,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "ObjField(name=$name, value=$value)"
  }

  data class PyObj(
          val fields: List<ObjField>?,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : AstNode {
    override fun toString(): String =
      "PyObj(fields=$fields)"
  }

  data class StrLiteral(
    val value: String,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : AstNode {
    override fun toString(): String =
      "StrLiteral(value=$value)"
  }

  data class StrValue(
          val value: StrLiteral,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "StrValue(value=$value)"
  }

  data class TupleValue(
          val elems: List<Value>,
          override val symbolId: Int,
          override val start: Int,
          override val end: Int
  ) : Value, AstNode {
    override fun toString(): String =
      "TupleValue(elems=$elems)"
  }

  enum class BoolEnum {
    False, True
  }

  fun matchStart(): PyObj {
    val lastGen = inputs.size
    val kernel = history[lastGen]
      .filter { it.symbolId() == 1 && it.endGen() == lastGen }
      .checkSingle()
    return matchPyObj(kernel.beginGen(), kernel.endGen())
  }

  fun matchPyObj(beginGen: Int, endGen: Int): PyObj {
    val var1 = getSequenceElems(history, 3, listOf(4, 5, 11, 88), beginGen, endGen)
    val var2 = history[var1[2].second].findByBeginGenOpt(12, 1, var1[2].first)
    val var3 = history[var1[2].second].findByBeginGenOpt(73, 1, var1[2].first)
    check(hasSingleTrue(var2 != null, var3 != null))
    val var4 = when {
      var2 != null -> {
        val var5 =
          getSequenceElems(history, 14, listOf(15, 83, 69, 5), var1[2].first, var1[2].second)
        val var6 = matchObjField(var5[0].first, var5[0].second)
        val var7 = unrollRepeat0(history, 83, 85, 8, 84, var5[1].first, var5[1].second).map { k ->
          val var8 = getSequenceElems(history, 87, listOf(5, 68, 5, 15), k.first, k.second)
          val var9 = matchObjField(var8[3].first, var8[3].second)
          var9
        }
        listOf(var6) + var7
      }

      else -> null
    }
    val var10 = PyObj(var4, nextId(), beginGen, endGen)
    return var10
  }

  fun matchObjField(beginGen: Int, endGen: Int): ObjField {
    val var11 = getSequenceElems(history, 16, listOf(17, 5, 27, 5, 28), beginGen, endGen)
    val var12 = matchStrLiteral(var11[0].first, var11[0].second)
    val var13 = matchValue(var11[4].first, var11[4].second)
    val var14 = ObjField(var12, var13, nextId(), beginGen, endGen)
    return var14
  }

  fun matchStrLiteral(beginGen: Int, endGen: Int): StrLiteral {
    val var15 = history[endGen].findByBeginGenOpt(18, 3, beginGen)
    val var16 = history[endGen].findByBeginGenOpt(25, 3, beginGen)
    check(hasSingleTrue(var15 != null, var16 != null))
    val var17 = when {
      var15 != null -> {
        val var18 = getSequenceElems(history, 18, listOf(19, 20, 19), beginGen, endGen)
        val var19 =
          unrollRepeat0(history, 20, 22, 8, 21, var18[1].first, var18[1].second).map { k ->
            val var20 = matchStrChar(k.first, k.second)
            var20
          }
        val var21 = StrLiteral(var19.joinToString("") { it.toString() }, nextId(), beginGen, endGen)
        var21
      }

      else -> {
        val var22 = getSequenceElems(history, 25, listOf(26, 20, 26), beginGen, endGen)
        val var23 =
          unrollRepeat0(history, 20, 22, 8, 21, var22[1].first, var22[1].second).map { k ->
            val var24 = matchStrChar(k.first, k.second)
            var24
          }
        val var25 = StrLiteral(var23.joinToString("") { it.toString() }, nextId(), beginGen, endGen)
        var25
      }
    }
    return var17
  }

  fun matchValue(beginGen: Int, endGen: Int): Value {
    val var26 = history[endGen].findByBeginGenOpt(29, 1, beginGen)
    val var27 = history[endGen].findByBeginGenOpt(45, 1, beginGen)
    val var28 = history[endGen].findByBeginGenOpt(54, 1, beginGen)
    val var29 = history[endGen].findByBeginGenOpt(55, 1, beginGen)
    val var30 = history[endGen].findByBeginGenOpt(75, 1, beginGen)
    check(hasSingleTrue(var26 != null, var27 != null, var28 != null, var29 != null, var30 != null))
    val var31 = when {
      var26 != null -> {
        val var32 = getSequenceElems(history, 29, listOf(30), beginGen, endGen)
        val var33 = matchBoolValue(var32[0].first, var32[0].second)
        val var34 = BoolValue(var33, nextId(), beginGen, endGen)
        var34
      }

      var27 != null -> {
        val var35 = getSequenceElems(history, 45, listOf(46), beginGen, endGen)
        val var36 = matchIntLiteral(var35[0].first, var35[0].second)
        val var37 = IntValue(var36, nextId(), beginGen, endGen)
        var37
      }

      var28 != null -> {
        val var38 = getSequenceElems(history, 54, listOf(17), beginGen, endGen)
        val var39 = matchStrLiteral(var38[0].first, var38[0].second)
        val var40 = StrValue(var39, nextId(), beginGen, endGen)
        var40
      }

      var29 != null -> {
        val var41 = getSequenceElems(history, 55, listOf(56), beginGen, endGen)
        val var42 = matchListValue(var41[0].first, var41[0].second)
        var42
      }

      else -> {
        val var43 = getSequenceElems(history, 75, listOf(76), beginGen, endGen)
        val var44 = matchTupleValue(var43[0].first, var43[0].second)
        var44
      }
    }
    return var31
  }

  fun matchIntLiteral(beginGen: Int, endGen: Int): IntLiteral {
    val var45 = history[endGen].findByBeginGenOpt(47, 1, beginGen)
    val var46 = history[endGen].findByBeginGenOpt(49, 2, beginGen)
    check(hasSingleTrue(var45 != null, var46 != null))
    val var47 = when {
      var45 != null -> {
        val var48 = IntLiteral("0", nextId(), beginGen, endGen)
        var48
      }

      else -> {
        val var49 = getSequenceElems(history, 49, listOf(50, 51), beginGen, endGen)
        val var50 =
          unrollRepeat0(history, 51, 53, 8, 52, var49[1].first, var49[1].second).map { k ->
            (inputs[k.first] as Inputs.Character).char()
          }
        val var51 =
          unrollRepeat0(history, 51, 53, 8, 52, var49[1].first, var49[1].second).map { k ->
            (inputs[k.first] as Inputs.Character).char()
          }
        val var52 = if (var51.isEmpty()) {
          "hello"
        } else {
          "world"
        }
        val var53 = IntLiteral(
          (inputs[var49[0].first] as Inputs.Character).char()
            .toString() + var50.joinToString("") { it.toString() } + var52,
          nextId(),
          beginGen,
          endGen)
        var53
      }
    }
    return var47
  }

  fun matchListValue(beginGen: Int, endGen: Int): ListValue {
    val var55 = getSequenceElems(history, 57, listOf(58, 5, 59, 74), beginGen, endGen)
    val var56 = history[var55[2].second].findByBeginGenOpt(60, 1, var55[2].first)
    val var57 = history[var55[2].second].findByBeginGenOpt(73, 1, var55[2].first)
    check(hasSingleTrue(var56 != null, var57 != null))
    val var58 = when {
      var56 != null -> {
        val var59 =
          getSequenceElems(history, 62, listOf(28, 63, 69, 5), var55[2].first, var55[2].second)
        val var60 = matchValue(var59[0].first, var59[0].second)
        val var61 =
          unrollRepeat0(history, 63, 65, 8, 64, var59[1].first, var59[1].second).map { k ->
            val var62 = getSequenceElems(history, 67, listOf(5, 68, 5, 28), k.first, k.second)
            val var63 = matchValue(var62[3].first, var62[3].second)
            var63
          }
        listOf(var60) + var61
      }

      else -> null
    }
    val var54 = var58
    val var64 = ListValue(var54 ?: listOf(), nextId(), beginGen, endGen)
    return var64
  }

  fun matchTupleValue(beginGen: Int, endGen: Int): TupleValue {
    val var65 = history[endGen].findByBeginGenOpt(77, 7, beginGen)
    val var66 = history[endGen].findByBeginGenOpt(80, 7, beginGen)
    check(hasSingleTrue(var65 != null, var66 != null))
    val var67 = when {
      var65 != null -> {
        val var68 = getSequenceElems(history, 77, listOf(78, 5, 28, 5, 68, 5, 79), beginGen, endGen)
        val var69 = matchValue(var68[2].first, var68[2].second)
        val var70 = TupleValue(listOf(var69), nextId(), beginGen, endGen)
        var70
      }

      else -> {
        val var71 =
          getSequenceElems(history, 80, listOf(78, 5, 28, 81, 69, 5, 79), beginGen, endGen)
        val var72 = matchValue(var71[2].first, var71[2].second)
        val var73 =
          unrollRepeat1(history, 81, 65, 65, 82, var71[3].first, var71[3].second).map { k ->
            val var74 = getSequenceElems(history, 67, listOf(5, 68, 5, 28), k.first, k.second)
            val var75 = matchValue(var74[3].first, var74[3].second)
            var75
          }
        val var76 = TupleValue(listOf(var72) + var73, nextId(), beginGen, endGen)
        var76
      }
    }
    return var67
  }

  fun matchBoolValue(beginGen: Int, endGen: Int): BoolEnum {
    val var77 = history[endGen].findByBeginGenOpt(31, 1, beginGen)
    val var78 = history[endGen].findByBeginGenOpt(38, 1, beginGen)
    check(hasSingleTrue(var77 != null, var78 != null))
    val var79 = when {
      var77 != null -> BoolEnum.True
      else -> BoolEnum.False
    }
    return var79
  }

  fun matchStrChar(beginGen: Int, endGen: Int): Char {
    val var80 = getSequenceElems(history, 23, listOf(24), beginGen, endGen)
    return (inputs[var80[0].first] as Inputs.Character).char()
  }

//
//  fun matchPyObj1(beginGen: Int, endGen: Int): PyObj {
//    // find body candidate (unroll choice)
//    val body = history[endGen].filter {
//      it.beginGen() == beginGen && (it.symbolId() == 3 && it.pointer() == 4)
//    }.checkSingle()
//
//    val sequence = getSequenceElems(3, beginGen, endGen)
//    println(sequence)
//    sequence[2]
//    // get sequence elem
//    val k3 = history[endGen].filter {
//      it.endGen() == endGen && (it.symbolId() == 88 && it.pointer() == 1)
//    }.checkSingle()
//    val k2 = history[k3.beginGen()].filter {
//      it.endGen() == k3.beginGen() && (it.symbolId() == 11 && it.pointer() == 1)
//    }.checkSingle()
//    TODO()
//  }
}
