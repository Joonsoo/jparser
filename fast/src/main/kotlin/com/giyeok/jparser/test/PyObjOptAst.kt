package com.giyeok.jparser.test

import com.giyeok.jparser.Inputs

interface IdIssuer {
  fun nextId(): Int
}

class IdIssuerImpl(startId: Int = 0) : IdIssuer {
  private var idCounter = startId

  override fun nextId(): Int {
    idCounter += 1
    return idCounter
  }
}

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
  ) : Value, AstNode

  data class IntLiteral(
    val value: String,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : AstNode

  data class IntValue(
    val value: IntLiteral,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : Value, AstNode

  data class ListValue(
    val elems: List<Value>?,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : Value, AstNode

  data class ObjField(
    val name: StrLiteral,
    val value: Value,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : Value, AstNode

  data class PyObj(
    val fields: List<ObjField>?,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : AstNode

  data class StrLiteral(
    val value: String,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : AstNode

  data class StrValue(
    val value: StrLiteral,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : Value, AstNode

  data class TupleValue(
    val elems: List<Value>,
    override val symbolId: Int,
    override val start: Int,
    override val end: Int
  ) : Value, AstNode

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
        val var7 =
          getSequenceElems(history, 14, listOf(15, 83, 69, 5), var1[2].first, var1[2].second)
        val var8 = unrollRepeat0(history, 83, 85, 8, 84, var7[1].first, var7[1].second).map { k ->
          val var9 = getSequenceElems(history, 87, listOf(5, 68, 5, 15), k.first, k.second)
          val var10 = matchObjField(var9[3].first, var9[3].second)
          var10
        }
        listOf(var6) + var8
      }

      else -> null
    }
    val var11 = PyObj(var4, nextId(), beginGen, endGen)
    return var11
  }

  fun matchObjField(beginGen: Int, endGen: Int): ObjField {
    val var12 = getSequenceElems(history, 16, listOf(17, 5, 27, 5, 28), beginGen, endGen)
    val var13 = matchStrLiteral(var12[0].first, var12[0].second)
    val var14 = getSequenceElems(history, 16, listOf(17, 5, 27, 5, 28), beginGen, endGen)
    val var15 = matchValue(var14[4].first, var14[4].second)
    val var16 = ObjField(var13, var15, nextId(), beginGen, endGen)
    return var16
  }

  fun matchStrLiteral(beginGen: Int, endGen: Int): StrLiteral {
    val var17 = history[endGen].findByBeginGenOpt(18, 3, beginGen)
    val var18 = history[endGen].findByBeginGenOpt(25, 3, beginGen)
    check(hasSingleTrue(var17 != null, var18 != null))
    val var19 = when {
      var17 != null -> {
        val var20 = StrLiteral("TODO_funccall(Str)", nextId(), beginGen, endGen)
        var20
      }

      else -> {
        val var21 = StrLiteral("TODO_funccall(Str)", nextId(), beginGen, endGen)
        var21
      }
    }
    return var19
  }

  fun matchValue(beginGen: Int, endGen: Int): Value {
    val var22 = history[endGen].findByBeginGenOpt(29, 1, beginGen)
    val var23 = history[endGen].findByBeginGenOpt(45, 1, beginGen)
    val var24 = history[endGen].findByBeginGenOpt(54, 1, beginGen)
    val var25 = history[endGen].findByBeginGenOpt(55, 1, beginGen)
    val var26 = history[endGen].findByBeginGenOpt(75, 1, beginGen)
    check(hasSingleTrue(var22 != null, var23 != null, var24 != null, var25 != null, var26 != null))
    val var27 = when {
      var22 != null -> {
        val var28 = getSequenceElems(history, 29, listOf(30), beginGen, endGen)
        val var29 = matchBoolValue(var28[0].first, var28[0].second)
        val var30 = BoolValue(var29, nextId(), beginGen, endGen)
        var30
      }

      var23 != null -> {
        val var31 = getSequenceElems(history, 45, listOf(46), beginGen, endGen)
        val var32 = matchIntLiteral(var31[0].first, var31[0].second)
        val var33 = IntValue(var32, nextId(), beginGen, endGen)
        var33
      }

      var24 != null -> {
        val var34 = getSequenceElems(history, 54, listOf(17), beginGen, endGen)
        val var35 = matchStrLiteral(var34[0].first, var34[0].second)
        val var36 = StrValue(var35, nextId(), beginGen, endGen)
        var36
      }

      var25 != null -> {
        val var37 = getSequenceElems(history, 55, listOf(56), beginGen, endGen)
        val var38 = matchListValue(var37[0].first, var37[0].second)
        var38
      }

      else -> {
        val var39 = getSequenceElems(history, 75, listOf(76), beginGen, endGen)
        val var40 = matchTupleValue(var39[0].first, var39[0].second)
        var40
      }
    }
    return var27
  }

  fun matchIntLiteral(beginGen: Int, endGen: Int): IntLiteral {
    val var41 = history[endGen].findByBeginGenOpt(47, 1, beginGen)
    val var42 = history[endGen].findByBeginGenOpt(49, 2, beginGen)
    check(hasSingleTrue(var41 != null, var42 != null))
    val var43 = when {
      var41 != null -> {
        val var44 = IntLiteral("0", nextId(), beginGen, endGen)
        var44
      }

      else -> {
        val var45 = IntLiteral("TODO_funccall(Str)", nextId(), beginGen, endGen)
        var45
      }
    }
    return var43
  }

  fun matchListValue(beginGen: Int, endGen: Int): ListValue {
    val var46 = getSequenceElems(history, 57, listOf(58, 5, 59, 74), beginGen, endGen)
    val var47 = history[var46[2].second].findByBeginGenOpt(60, 1, var46[2].first)
    val var48 = history[var46[2].second].findByBeginGenOpt(73, 1, var46[2].first)
    check(hasSingleTrue(var47 != null, var48 != null))
    val var49 = when {
      var47 != null -> {
        val var50 =
          getSequenceElems(history, 62, listOf(28, 63, 69, 5), var46[2].first, var46[2].second)
        val var51 = matchValue(var50[0].first, var50[0].second)
        val var52 =
          getSequenceElems(history, 62, listOf(28, 63, 69, 5), var46[2].first, var46[2].second)
        val var53 =
          unrollRepeat0(history, 63, 65, 8, 64, var52[1].first, var52[1].second).map { k ->
            val var54 = getSequenceElems(history, 67, listOf(5, 68, 5, 28), k.first, k.second)
            val var55 = matchValue(var54[3].first, var54[3].second)
            var55
          }
        listOf(var51) + var53
      }

      else -> null
    }
    val var56 = ListValue(var49, nextId(), beginGen, endGen)
    return var56
  }

  fun matchTupleValue(beginGen: Int, endGen: Int): TupleValue {
    val var57 = history[endGen].findByBeginGenOpt(77, 7, beginGen)
    val var58 = history[endGen].findByBeginGenOpt(80, 7, beginGen)
    check(hasSingleTrue(var57 != null, var58 != null))
    val var59 = when {
      var57 != null -> {
        val var60 = getSequenceElems(history, 77, listOf(78, 5, 28, 5, 68, 5, 79), beginGen, endGen)
        val var61 = matchValue(var60[2].first, var60[2].second)
        val var62 = TupleValue(listOf(var61), nextId(), beginGen, endGen)
        var62
      }

      else -> {
        val var63 =
          getSequenceElems(history, 80, listOf(78, 5, 28, 81, 69, 5, 79), beginGen, endGen)
        val var64 = matchValue(var63[2].first, var63[2].second)
        val var65 =
          getSequenceElems(history, 80, listOf(78, 5, 28, 81, 69, 5, 79), beginGen, endGen)
        val var66 = unrollRepeat1(history, 81, 65, 82, var65[3].first, var65[3].second).map { k ->
          val var67 = getSequenceElems(history, 67, listOf(5, 68, 5, 28), k.first, k.second)
          val var68 = matchValue(var67[3].first, var67[3].second)
          var68
        }
        val var69 = TupleValue(listOf(var64) + var66, nextId(), beginGen, endGen)
        var69
      }
    }
    return var59
  }

  fun matchBoolValue(beginGen: Int, endGen: Int): BoolEnum {
    val var70 = history[endGen].findByBeginGenOpt(31, 1, beginGen)
    val var71 = history[endGen].findByBeginGenOpt(38, 1, beginGen)
    check(hasSingleTrue(var70 != null, var71 != null))
    val var72 = when {
      var70 != null -> BoolEnum.True
      else -> BoolEnum.False
    }
    return var72
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
