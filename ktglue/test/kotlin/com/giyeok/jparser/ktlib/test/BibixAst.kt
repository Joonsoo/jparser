package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.*

class BibixAst(
  val inputs: List<Inputs.Input>,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val nodeId: Int
    val start: Int
    val end: Int
  }

  data class VarRedef(
    val nameTokens: List<String>,
    val redefValue: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class This(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  sealed interface ClassBodyElem : AstNode
  data class ImportFrom(
    val source: Expr,
    val importing: Name,
    val rename: String?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ImportDef, AstNode

  data class ClassCastDef(
    val castTo: TypeExpr,
    val expr: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassBodyElem, AstNode

  data class TupleType(
    val elems: List<TypeExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NoUnionType, AstNode

  data class NoneType(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NoUnionType, AstNode

  data class NamedTupleExpr(
    val elems: List<NamedExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class MemberAccess(
    val target: Primary,
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class EscapeChar(
    val code: Char,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringElem, AstNode

  sealed interface NoUnionType : TypeExpr, AstNode
  data class TupleExpr(
    val elems: List<Expr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  sealed interface MergeOpOrPrimary : Expr, AstNode
  data class ActionRuleDef(
    val name: String,
    val params: List<ParamDef>,
    val impl: MethodRef,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassBodyElem, Def, AstNode

  data class ComplexExpr(
    val expr: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringExpr, AstNode

  data class Paren(
    val expr: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class BuildRuleDef(
    val name: String,
    val params: List<ParamDef>,
    val returnType: TypeExpr,
    val impl: MethodRef,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  data class Name(
    val tokens: List<String>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NoUnionType, AstNode

  data class DataClassDef(
    val name: String,
    val fields: List<ParamDef>,
    val body: List<ClassBodyElem>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassDef, AstNode

  data class CollectionType(
    val name: String,
    val typeParams: TypeParams,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NoUnionType, AstNode

  sealed interface ClassDef : Def, AstNode
  data class MethodRef(
    val targetName: Name,
    val className: Name,
    val methodName: String?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class SingleCallAction(
    val expr: CallExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ActionBody, AstNode

  sealed interface Expr : ListElem, AstNode
  data class CastExpr(
    val expr: Expr,
    val castTo: NoUnionType,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Expr, AstNode

  sealed interface ImportDef : Def, AstNode
  data class JustChar(
    val chr: Char,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringElem, AstNode

  sealed interface Literal : Primary, AstNode
  data class NameRef(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class StringLiteral(
    val elems: List<StringElem>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  sealed interface StringExpr : StringElem, AstNode
  data class CallExpr(
    val name: Name,
    val params: CallParams,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class NamedType(
    val name: String,
    val typ: TypeExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  sealed interface StringElem : AstNode
  data class NamedExpr(
    val name: String,
    val expr: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class MergeOp(
    val lhs: Expr,
    val rhs: Primary,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : MergeOpOrPrimary, AstNode

  data class VarDef(
    val name: String,
    val typ: TypeExpr?,
    val defaultValue: Expr?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  data class NamespaceDef(
    val name: String,
    val body: BuildScript,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  data class ListExpr(
    val elems: List<ListElem>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Primary, AstNode

  data class TypeParams(
    val params: List<TypeExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class EnumDef(
    val name: String,
    val values: List<String>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  sealed interface Primary : MergeOpOrPrimary, AstNode
  data class VarRedefs(
    val redefs: List<VarRedef>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  sealed interface TypeExpr : AstNode
  sealed interface ListElem : AstNode
  data class ParamDef(
    val name: String,
    val optional: Boolean,
    val typ: TypeExpr?,
    val defaultValue: Expr?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class UnionType(
    val elems: List<NoUnionType>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : TypeExpr, AstNode

  data class TargetDef(
    val name: String,
    val value: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  data class SuperClassDef(
    val name: String,
    val subs: List<String>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassDef, AstNode

  data class ActionDef(
    val name: String,
    val argsName: String?,
    val body: ActionBody,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  data class MultiCallActions(
    val exprs: List<CallExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ActionBody, AstNode

  sealed interface ActionBody : AstNode
  sealed interface Def : AstNode
  data class NamedParam(
    val name: String,
    val value: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class ImportAll(
    val source: Primary,
    val rename: String?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ImportDef, AstNode

  data class BuildScript(
    val packageName: Name?,
    val defs: List<Def>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class EllipsisElem(
    val value: Expr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ListElem, AstNode

  data class SimpleExpr(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringExpr, AstNode

  data class NoneLiteral(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  data class CallParams(
    val posParams: List<Expr>,
    val namedParams: List<NamedParam>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class NamedTupleType(
    val elems: List<NamedType>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NoUnionType, AstNode

  data class BooleanLiteral(
    val value: Boolean,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  fun matchStart(): BuildScript {
    val lastGen = inputs.size
    val kernel = history[lastGen]
      .filter { it.symbolId() == 2 && it.pointer() == 1 && it.endGen() == lastGen }
      .checkSingle()
    return matchBuildScript(kernel.beginGen(), kernel.endGen())
  }

  fun matchBuildScript(beginGen: Int, endGen: Int): BuildScript {
    val var1 = getSequenceElems(history, 3, listOf(4, 8, 132, 8), beginGen, endGen)
    val var2 = history[var1[0].second].findByBeginGenOpt(5, 1, var1[0].first)
    val var3 = history[var1[0].second].findByBeginGenOpt(131, 1, var1[0].first)
    check(hasSingleTrue(var2 != null, var3 != null))
    val var4 = when {
      var2 != null -> {
        val var5 = getSequenceElems(history, 7, listOf(8, 58), var1[0].first, var1[0].second)
        val var6 = matchPackageName(var5[1].first, var5[1].second)
        var6
      }

      else -> null
    }
    val var7 = matchDefs(var1[2].first, var1[2].second)
    val var8 = BuildScript(var4, var7, nextId(), beginGen, endGen)
    return var8
  }

  fun matchPackageName(beginGen: Int, endGen: Int): Name {
    val var9 = getSequenceElems(history, 59, listOf(60, 8, 86), beginGen, endGen)
    val var10 = matchName(var9[2].first, var9[2].second)
    return var10
  }

  fun matchDefs(beginGen: Int, endGen: Int): List<Def> {
    val var11 = getSequenceElems(history, 133, listOf(134, 450), beginGen, endGen)
    val var12 = matchDef(var11[0].first, var11[0].second)
    val var13 =
      unrollRepeat0(history, 450, 452, 11, 451, var11[1].first, var11[1].second).map { k ->
        val var14 = getSequenceElems(history, 454, listOf(8, 134), k.first, k.second)
        val var15 = matchDef(var14[1].first, var14[1].second)
        var15
      }
    return listOf(var12) + var13
  }

  fun matchName(beginGen: Int, endGen: Int): Name {
    val var16 = getSequenceElems(history, 87, listOf(88, 125), beginGen, endGen)
    val var17 = matchSimpleName(var16[0].first, var16[0].second)
    val var18 =
      unrollRepeat0(history, 125, 127, 11, 126, var16[1].first, var16[1].second).map { k ->
        val var19 = getSequenceElems(history, 129, listOf(8, 130, 8, 88), k.first, k.second)
        val var20 = matchSimpleName(var19[3].first, var19[3].second)
        var20
      }
    val var21 = Name(listOf(var17) + var18, nextId(), beginGen, endGen)
    return var21
  }

  fun matchSimpleName(beginGen: Int, endGen: Int): String {
    val var22 = getSequenceElems(history, 89, listOf(90), beginGen, endGen)
    val var23 = getSequenceElems(history, 94, listOf(95), var22[0].first, var22[0].second)
    val var24 = getSequenceElems(history, 98, listOf(99, 100), var23[0].first, var23[0].second)
    val var25 = unrollRepeat0(history, 100, 76, 11, 101, var24[1].first, var24[1].second).map { k ->
      (inputs[k.first] as Inputs.Character).char()
    }
    return (inputs[var24[0].first] as Inputs.Character).char()
      .toString() + var25.joinToString("") { it.toString() }
  }

  fun matchDef(beginGen: Int, endGen: Int): Def {
    val var26 = history[endGen].findByBeginGenOpt(135, 1, beginGen)
    val var27 = history[endGen].findByBeginGenOpt(323, 1, beginGen)
    val var28 = history[endGen].findByBeginGenOpt(326, 1, beginGen)
    val var29 = history[endGen].findByBeginGenOpt(328, 1, beginGen)
    val var30 = history[endGen].findByBeginGenOpt(349, 1, beginGen)
    val var31 = history[endGen].findByBeginGenOpt(391, 1, beginGen)
    val var32 = history[endGen].findByBeginGenOpt(418, 1, beginGen)
    val var33 = history[endGen].findByBeginGenOpt(424, 1, beginGen)
    val var34 = history[endGen].findByBeginGenOpt(435, 1, beginGen)
    val var35 = history[endGen].findByBeginGenOpt(447, 1, beginGen)
    check(
      hasSingleTrue(
        var26 != null,
        var27 != null,
        var28 != null,
        var29 != null,
        var30 != null,
        var31 != null,
        var32 != null,
        var33 != null,
        var34 != null,
        var35 != null
      )
    )
    val var36 = when {
      var26 != null -> {
        val var37 = getSequenceElems(history, 135, listOf(136), beginGen, endGen)
        val var38 = matchImportDef(var37[0].first, var37[0].second)
        var38
      }

      var27 != null -> {
        val var39 = getSequenceElems(history, 323, listOf(324), beginGen, endGen)
        val var40 = matchNamespaceDef(var39[0].first, var39[0].second)
        var40
      }

      var28 != null -> {
        val var41 = getSequenceElems(history, 326, listOf(327), beginGen, endGen)
        val var42 = matchTargetDef(var41[0].first, var41[0].second)
        var42
      }

      var29 != null -> {
        val var43 = getSequenceElems(history, 328, listOf(329), beginGen, endGen)
        val var44 = matchActionDef(var43[0].first, var43[0].second)
        var44
      }

      var30 != null -> {
        val var45 = getSequenceElems(history, 349, listOf(350), beginGen, endGen)
        val var46 = matchClassDef(var45[0].first, var45[0].second)
        var46
      }

      var31 != null -> {
        val var47 = getSequenceElems(history, 391, listOf(392), beginGen, endGen)
        val var48 = matchActionRuleDef(var47[0].first, var47[0].second)
        var48
      }

      var32 != null -> {
        val var49 = getSequenceElems(history, 418, listOf(419), beginGen, endGen)
        val var50 = matchEnumDef(var49[0].first, var49[0].second)
        var50
      }

      var33 != null -> {
        val var51 = getSequenceElems(history, 424, listOf(425), beginGen, endGen)
        val var52 = matchVarDef(var51[0].first, var51[0].second)
        var52
      }

      var34 != null -> {
        val var53 = getSequenceElems(history, 435, listOf(436), beginGen, endGen)
        val var54 = matchVarRedefs(var53[0].first, var53[0].second)
        var54
      }

      else -> {
        val var55 = getSequenceElems(history, 447, listOf(448), beginGen, endGen)
        val var56 = matchBuildRuleDef(var55[0].first, var55[0].second)
        var56
      }
    }
    return var36
  }

  fun matchActionRuleDef(beginGen: Int, endGen: Int): ActionRuleDef {
    val var57 = getSequenceElems(
      history,
      393,
      listOf(331, 8, 394, 8, 88, 8, 357, 8, 230, 8, 398),
      beginGen,
      endGen
    )
    val var58 = matchSimpleName(var57[4].first, var57[4].second)
    val var59 = matchParamsDef(var57[6].first, var57[6].second)
    val var60 = matchMethodRef(var57[10].first, var57[10].second)
    val var61 = ActionRuleDef(var58, var59, var60, nextId(), beginGen, endGen)
    return var61
  }

  fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
    val var62 = getSequenceElems(
      history,
      449,
      listOf(394, 8, 88, 8, 357, 8, 205, 8, 178, 8, 230, 8, 398),
      beginGen,
      endGen
    )
    val var63 = matchSimpleName(var62[2].first, var62[2].second)
    val var64 = matchParamsDef(var62[4].first, var62[4].second)
    val var65 = matchTypeExpr(var62[8].first, var62[8].second)
    val var66 = matchMethodRef(var62[12].first, var62[12].second)
    val var67 = BuildRuleDef(var63, var64, var65, var66, nextId(), beginGen, endGen)
    return var67
  }

  fun matchImportDef(beginGen: Int, endGen: Int): ImportDef {
    val var68 = history[endGen].findByBeginGenOpt(137, 4, beginGen)
    val var69 = history[endGen].findByBeginGenOpt(319, 8, beginGen)
    check(hasSingleTrue(var68 != null, var69 != null))
    val var70 = when {
      var68 != null -> {
        val var71 = getSequenceElems(history, 137, listOf(138, 8, 142, 315), beginGen, endGen)
        val var72 = matchPrimary(var71[2].first, var71[2].second)
        val var73 = history[var71[3].second].findByBeginGenOpt(131, 1, var71[3].first)
        val var74 = history[var71[3].second].findByBeginGenOpt(316, 1, var71[3].first)
        check(hasSingleTrue(var73 != null, var74 != null))
        val var75 = when {
          var73 != null -> null
          else -> {
            val var76 =
              getSequenceElems(history, 318, listOf(8, 155, 8, 88), var71[3].first, var71[3].second)
            val var77 = matchSimpleName(var76[3].first, var76[3].second)
            var77
          }
        }
        val var78 = ImportAll(var72, var75, nextId(), beginGen, endGen)
        var78
      }

      else -> {
        val var79 =
          getSequenceElems(history, 319, listOf(320, 8, 153, 8, 138, 8, 86, 315), beginGen, endGen)
        val var80 = matchExpr(var79[2].first, var79[2].second)
        val var81 = matchName(var79[6].first, var79[6].second)
        val var82 = history[var79[7].second].findByBeginGenOpt(131, 1, var79[7].first)
        val var83 = history[var79[7].second].findByBeginGenOpt(316, 1, var79[7].first)
        check(hasSingleTrue(var82 != null, var83 != null))
        val var84 = when {
          var82 != null -> null
          else -> {
            val var85 =
              getSequenceElems(history, 318, listOf(8, 155, 8, 88), var79[7].first, var79[7].second)
            val var86 = matchSimpleName(var85[3].first, var85[3].second)
            var86
          }
        }
        val var87 = ImportFrom(var80, var81, var84, nextId(), beginGen, endGen)
        var87
      }
    }
    return var70
  }

  fun matchExpr(beginGen: Int, endGen: Int): Expr {
    val var88 = history[endGen].findByBeginGenOpt(154, 5, beginGen)
    val var89 = history[endGen].findByBeginGenOpt(211, 1, beginGen)
    check(hasSingleTrue(var88 != null, var89 != null))
    val var90 = when {
      var88 != null -> {
        val var91 = getSequenceElems(history, 154, listOf(153, 8, 155, 8, 158), beginGen, endGen)
        val var92 = matchExpr(var91[0].first, var91[0].second)
        val var93 = matchNoUnionType(var91[4].first, var91[4].second)
        val var94 = CastExpr(var92, var93, nextId(), beginGen, endGen)
        var94
      }

      else -> {
        val var95 = getSequenceElems(history, 211, listOf(212), beginGen, endGen)
        val var96 = matchMergeOpOrPrimary(var95[0].first, var95[0].second)
        var96
      }
    }
    return var90
  }

  fun matchNoUnionType(beginGen: Int, endGen: Int): NoUnionType {
    val var97 = history[endGen].findByBeginGenOpt(159, 1, beginGen)
    val var98 = history[endGen].findByBeginGenOpt(160, 1, beginGen)
    val var99 = history[endGen].findByBeginGenOpt(162, 1, beginGen)
    val var100 = history[endGen].findByBeginGenOpt(197, 1, beginGen)
    val var101 = history[endGen].findByBeginGenOpt(200, 1, beginGen)
    check(
      hasSingleTrue(
        var97 != null,
        var98 != null,
        var99 != null,
        var100 != null,
        var101 != null
      )
    )
    val var102 = when {
      var97 != null -> {
        val var103 = getSequenceElems(history, 159, listOf(86), beginGen, endGen)
        val var104 = matchName(var103[0].first, var103[0].second)
        var104
      }

      var98 != null -> {
        val var105 = NoneType(nextId(), beginGen, endGen)
        var105
      }

      var99 != null -> {
        val var106 = getSequenceElems(history, 162, listOf(163), beginGen, endGen)
        val var107 = matchCollectionType(var106[0].first, var106[0].second)
        var107
      }

      var100 != null -> {
        val var108 = getSequenceElems(history, 197, listOf(198), beginGen, endGen)
        val var109 = matchTupleType(var108[0].first, var108[0].second)
        var109
      }

      else -> {
        val var110 = getSequenceElems(history, 200, listOf(201), beginGen, endGen)
        val var111 = matchNamedTupleType(var110[0].first, var110[0].second)
        var111
      }
    }
    return var102
  }

  fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
    val var112 = history[endGen].findByBeginGenOpt(213, 5, beginGen)
    val var113 = history[endGen].findByBeginGenOpt(215, 1, beginGen)
    check(hasSingleTrue(var112 != null, var113 != null))
    val var114 = when {
      var112 != null -> {
        val var115 = getSequenceElems(history, 213, listOf(153, 8, 214, 8, 142), beginGen, endGen)
        val var116 = matchExpr(var115[0].first, var115[0].second)
        val var117 = matchPrimary(var115[4].first, var115[4].second)
        val var118 = MergeOp(var116, var117, nextId(), beginGen, endGen)
        var118
      }

      else -> {
        val var119 = getSequenceElems(history, 215, listOf(142), beginGen, endGen)
        val var120 = matchPrimary(var119[0].first, var119[0].second)
        var120
      }
    }
    return var114
  }

  fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
    val var121 = getSequenceElems(history, 164, listOf(165, 8, 175), beginGen, endGen)
    val var122 = history[var121[0].second].findByBeginGenOpt(167, 1, var121[0].first)
    val var123 = history[var121[0].second].findByBeginGenOpt(171, 1, var121[0].first)
    check(hasSingleTrue(var122 != null, var123 != null))
    val var124 = when {
      var122 != null -> {
        val var125 = getSequenceElems(history, 168, listOf(169), var121[0].first, var121[0].second)
        "set"
      }

      else -> {
        val var126 = getSequenceElems(history, 172, listOf(173), var121[0].first, var121[0].second)
        "list"
      }
    }
    val var127 = matchTypeParams(var121[2].first, var121[2].second)
    val var128 = CollectionType(var124, var127, nextId(), beginGen, endGen)
    return var128
  }

  fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
    val var130 = getSequenceElems(history, 358, listOf(148, 359, 221, 8, 149), beginGen, endGen)
    val var131 = history[var130[1].second].findByBeginGenOpt(131, 1, var130[1].first)
    val var132 = history[var130[1].second].findByBeginGenOpt(360, 1, var130[1].first)
    check(hasSingleTrue(var131 != null, var132 != null))
    val var133 = when {
      var131 != null -> null
      else -> {
        val var134 =
          getSequenceElems(history, 362, listOf(8, 363, 374), var130[1].first, var130[1].second)
        val var135 = matchParamDef(var134[1].first, var134[1].second)
        val var136 =
          unrollRepeat0(history, 374, 376, 11, 375, var134[2].first, var134[2].second).map { k ->
            val var137 = getSequenceElems(history, 378, listOf(8, 189, 8, 363), k.first, k.second)
            val var138 = matchParamDef(var137[3].first, var137[3].second)
            var138
          }
        listOf(var135) + var136
      }
    }
    val var129 = var133
    return (var129 ?: listOf())
  }

  fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
    val var139 = getSequenceElems(history, 176, listOf(177, 8, 178, 191, 8, 196), beginGen, endGen)
    val var140 = matchTypeExpr(var139[2].first, var139[2].second)
    val var141 =
      unrollRepeat0(history, 191, 193, 11, 192, var139[3].first, var139[3].second).map { k ->
        val var142 = getSequenceElems(history, 195, listOf(8, 189, 8, 178), k.first, k.second)
        val var143 = matchTypeExpr(var142[3].first, var142[3].second)
        var143
      }
    val var144 = TypeParams(listOf(var140) + var141, nextId(), beginGen, endGen)
    return var144
  }

  fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
    val var145 = getSequenceElems(history, 330, listOf(331, 8, 88, 334, 8, 340), beginGen, endGen)
    val var146 = matchSimpleName(var145[2].first, var145[2].second)
    val var147 = history[var145[3].second].findByBeginGenOpt(131, 1, var145[3].first)
    val var148 = history[var145[3].second].findByBeginGenOpt(335, 1, var145[3].first)
    check(hasSingleTrue(var147 != null, var148 != null))
    val var149 = when {
      var147 != null -> null
      else -> {
        val var150 =
          getSequenceElems(history, 337, listOf(8, 338), var145[3].first, var145[3].second)
        val var151 = matchActionParams(var150[1].first, var150[1].second)
        var151
      }
    }
    val var152 = matchActionBody(var145[5].first, var145[5].second)
    val var153 = ActionDef(var146, var149, var152, nextId(), beginGen, endGen)
    return var153
  }

  fun matchActionParams(beginGen: Int, endGen: Int): String {
    val var154 = getSequenceElems(history, 339, listOf(148, 8, 88, 8, 149), beginGen, endGen)
    val var155 = matchSimpleName(var154[2].first, var154[2].second)
    return var155
  }

  fun matchActionBody(beginGen: Int, endGen: Int): ActionBody {
    val var156 = history[endGen].findByBeginGenOpt(341, 3, beginGen)
    val var157 = history[endGen].findByBeginGenOpt(343, 4, beginGen)
    check(hasSingleTrue(var156 != null, var157 != null))
    val var158 = when {
      var156 != null -> {
        val var159 = getSequenceElems(history, 341, listOf(230, 8, 342), beginGen, endGen)
        val var160 = matchActionExpr(var159[2].first, var159[2].second)
        val var161 = SingleCallAction(var160, nextId(), beginGen, endGen)
        var161
      }

      else -> {
        val var162 = getSequenceElems(history, 343, listOf(183, 344, 8, 190), beginGen, endGen)
        val var163 =
          unrollRepeat1(history, 344, 345, 345, 348, var162[1].first, var162[1].second).map { k ->
            val var164 = getSequenceElems(history, 347, listOf(8, 342), k.first, k.second)
            val var165 = matchActionExpr(var164[1].first, var164[1].second)
            var165
          }
        val var166 = MultiCallActions(var163, nextId(), beginGen, endGen)
        var166
      }
    }
    return var158
  }

  fun matchActionExpr(beginGen: Int, endGen: Int): CallExpr {
    val var167 = getSequenceElems(history, 143, listOf(144), beginGen, endGen)
    val var168 = matchCallExpr(var167[0].first, var167[0].second)
    return var168
  }

  fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
    val var169 = getSequenceElems(history, 145, listOf(86, 8, 146), beginGen, endGen)
    val var170 = matchName(var169[0].first, var169[0].second)
    val var171 = matchCallParams(var169[2].first, var169[2].second)
    val var172 = CallExpr(var170, var171, nextId(), beginGen, endGen)
    return var172
  }

  fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
    val var173 = getSequenceElems(history, 199, listOf(148, 8, 178, 191, 8, 149), beginGen, endGen)
    val var174 = matchTypeExpr(var173[2].first, var173[2].second)
    val var175 =
      unrollRepeat0(history, 191, 193, 11, 192, var173[3].first, var173[3].second).map { k ->
        val var176 = getSequenceElems(history, 195, listOf(8, 189, 8, 178), k.first, k.second)
        val var177 = matchTypeExpr(var176[3].first, var176[3].second)
        var177
      }
    val var178 = TupleType(listOf(var174) + var175, nextId(), beginGen, endGen)
    return var178
  }

  fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
    val var179 = history[endGen].findByBeginGenOpt(351, 1, beginGen)
    val var180 = history[endGen].findByBeginGenOpt(407, 1, beginGen)
    check(hasSingleTrue(var179 != null, var180 != null))
    val var181 = when {
      var179 != null -> {
        val var182 = getSequenceElems(history, 351, listOf(352), beginGen, endGen)
        val var183 = matchDataClassDef(var182[0].first, var182[0].second)
        var183
      }

      else -> {
        val var184 = getSequenceElems(history, 407, listOf(408), beginGen, endGen)
        val var185 = matchSuperClassDef(var184[0].first, var184[0].second)
        var185
      }
    }
    return var181
  }

  fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
    val var186 = getSequenceElems(history, 353, listOf(354, 8, 88, 8, 357, 379), beginGen, endGen)
    val var187 = matchSimpleName(var186[2].first, var186[2].second)
    val var188 = matchParamsDef(var186[4].first, var186[4].second)
    val var190 = history[var186[5].second].findByBeginGenOpt(131, 1, var186[5].first)
    val var191 = history[var186[5].second].findByBeginGenOpt(380, 1, var186[5].first)
    check(hasSingleTrue(var190 != null, var191 != null))
    val var192 = when {
      var190 != null -> null
      else -> {
        val var193 =
          getSequenceElems(history, 382, listOf(8, 383), var186[5].first, var186[5].second)
        val var194 = matchClassBody(var193[1].first, var193[1].second)
        var194
      }
    }
    val var189 = var192
    val var195 = DataClassDef(var187, var188, (var189 ?: listOf()), nextId(), beginGen, endGen)
    return var195
  }

  fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
    val var196 = getSequenceElems(history, 399, listOf(86, 8, 205, 8, 86, 400), beginGen, endGen)
    val var197 = matchName(var196[0].first, var196[0].second)
    val var198 = matchName(var196[4].first, var196[4].second)
    val var199 = history[var196[5].second].findByBeginGenOpt(131, 1, var196[5].first)
    val var200 = history[var196[5].second].findByBeginGenOpt(401, 1, var196[5].first)
    check(hasSingleTrue(var199 != null, var200 != null))
    val var201 = when {
      var199 != null -> null
      else -> {
        val var202 =
          getSequenceElems(history, 403, listOf(8, 205, 8, 88), var196[5].first, var196[5].second)
        val var203 = matchSimpleName(var202[3].first, var202[3].second)
        var203
      }
    }
    val var204 = MethodRef(var197, var198, var201, nextId(), beginGen, endGen)
    return var204
  }

  fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
    val var205 = getSequenceElems(history, 384, listOf(183, 385, 8, 190), beginGen, endGen)
    val var206 =
      unrollRepeat0(history, 385, 387, 11, 386, var205[1].first, var205[1].second).map { k ->
        val var207 = getSequenceElems(history, 389, listOf(8, 390), k.first, k.second)
        val var208 = matchClassBodyElem(var207[1].first, var207[1].second)
        var208
      }
    return var206
  }

  fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
    val var209 = history[endGen].findByBeginGenOpt(391, 1, beginGen)
    val var210 = history[endGen].findByBeginGenOpt(404, 1, beginGen)
    check(hasSingleTrue(var209 != null, var210 != null))
    val var211 = when {
      var209 != null -> {
        val var212 = getSequenceElems(history, 391, listOf(392), beginGen, endGen)
        val var213 = matchActionRuleDef(var212[0].first, var212[0].second)
        var213
      }

      else -> {
        val var214 = getSequenceElems(history, 404, listOf(405), beginGen, endGen)
        val var215 = matchClassCastDef(var214[0].first, var214[0].second)
        var215
      }
    }
    return var211
  }

  fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
    val var216 =
      getSequenceElems(history, 406, listOf(155, 8, 178, 8, 230, 8, 153), beginGen, endGen)
    val var217 = matchTypeExpr(var216[2].first, var216[2].second)
    val var218 = matchExpr(var216[6].first, var216[6].second)
    val var219 = ClassCastDef(var217, var218, nextId(), beginGen, endGen)
    return var219
  }

  fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
    val var220 = getSequenceElems(history, 426, listOf(427, 8, 88, 431, 370), beginGen, endGen)
    val var221 = matchSimpleName(var220[2].first, var220[2].second)
    val var222 = history[var220[3].second].findByBeginGenOpt(131, 1, var220[3].first)
    val var223 = history[var220[3].second].findByBeginGenOpt(432, 1, var220[3].first)
    check(hasSingleTrue(var222 != null, var223 != null))
    val var224 = when {
      var222 != null -> null
      else -> {
        val var225 =
          getSequenceElems(history, 434, listOf(8, 205, 8, 178), var220[3].first, var220[3].second)
        val var226 = matchTypeExpr(var225[3].first, var225[3].second)
        var226
      }
    }
    val var227 = history[var220[4].second].findByBeginGenOpt(131, 1, var220[4].first)
    val var228 = history[var220[4].second].findByBeginGenOpt(371, 1, var220[4].first)
    check(hasSingleTrue(var227 != null, var228 != null))
    val var229 = when {
      var227 != null -> null
      else -> {
        val var230 =
          getSequenceElems(history, 373, listOf(8, 230, 8, 153), var220[4].first, var220[4].second)
        val var231 = matchExpr(var230[3].first, var230[3].second)
        var231
      }
    }
    val var232 = VarDef(var221, var224, var229, nextId(), beginGen, endGen)
    return var232
  }

  fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
    val var233 = getSequenceElems(history, 325, listOf(88, 8, 183, 2, 190), beginGen, endGen)
    val var234 = matchSimpleName(var233[0].first, var233[0].second)
    val var235 = matchBuildScript(var233[3].first, var233[3].second)
    val var236 = NamespaceDef(var234, var235, nextId(), beginGen, endGen)
    return var236
  }

  fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
    val var237 = getSequenceElems(
      history,
      420,
      listOf(421, 8, 88, 8, 183, 8, 88, 413, 221, 8, 190),
      beginGen,
      endGen
    )
    val var238 = matchSimpleName(var237[2].first, var237[2].second)
    val var239 = matchSimpleName(var237[6].first, var237[6].second)
    val var240 =
      unrollRepeat0(history, 413, 415, 11, 414, var237[7].first, var237[7].second).map { k ->
        val var241 = getSequenceElems(history, 417, listOf(8, 189, 8, 88), k.first, k.second)
        val var242 = matchSimpleName(var241[3].first, var241[3].second)
        var242
      }
    val var243 = EnumDef(var238, listOf(var239) + var240, nextId(), beginGen, endGen)
    return var243
  }

  fun matchPrimary(beginGen: Int, endGen: Int): Primary {
    val var244 = history[endGen].findByBeginGenOpt(143, 1, beginGen)
    val var245 = history[endGen].findByBeginGenOpt(237, 5, beginGen)
    val var246 = history[endGen].findByBeginGenOpt(238, 1, beginGen)
    val var247 = history[endGen].findByBeginGenOpt(239, 4, beginGen)
    val var248 = history[endGen].findByBeginGenOpt(256, 8, beginGen)
    val var249 = history[endGen].findByBeginGenOpt(261, 4, beginGen)
    val var250 = history[endGen].findByBeginGenOpt(273, 1, beginGen)
    val var251 = history[endGen].findByBeginGenOpt(312, 1, beginGen)
    val var252 = history[endGen].findByBeginGenOpt(314, 5, beginGen)
    check(
      hasSingleTrue(
        var244 != null,
        var245 != null,
        var246 != null,
        var247 != null,
        var248 != null,
        var249 != null,
        var250 != null,
        var251 != null,
        var252 != null
      )
    )
    val var253 = when {
      var244 != null -> {
        val var254 = getSequenceElems(history, 143, listOf(144), beginGen, endGen)
        val var255 = matchCallExpr(var254[0].first, var254[0].second)
        var255
      }

      var245 != null -> {
        val var256 = getSequenceElems(history, 237, listOf(142, 8, 130, 8, 88), beginGen, endGen)
        val var257 = matchPrimary(var256[0].first, var256[0].second)
        val var258 = matchSimpleName(var256[4].first, var256[4].second)
        val var259 = MemberAccess(var257, var258, nextId(), beginGen, endGen)
        var259
      }

      var246 != null -> {
        val var260 = getSequenceElems(history, 238, listOf(88), beginGen, endGen)
        val var261 = matchSimpleName(var260[0].first, var260[0].second)
        val var262 = NameRef(var261, nextId(), beginGen, endGen)
        var262
      }

      var247 != null -> {
        val var264 = getSequenceElems(history, 239, listOf(240, 241, 8, 255), beginGen, endGen)
        val var265 = history[var264[1].second].findByBeginGenOpt(131, 1, var264[1].first)
        val var266 = history[var264[1].second].findByBeginGenOpt(242, 1, var264[1].first)
        check(hasSingleTrue(var265 != null, var266 != null))
        val var267 = when {
          var265 != null -> null
          else -> {
            val var268 = getSequenceElems(
              history,
              244,
              listOf(8, 245, 250, 221),
              var264[1].first,
              var264[1].second
            )
            val var269 = matchListElem(var268[1].first, var268[1].second)
            val var270 =
              unrollRepeat0(
                history,
                250,
                252,
                11,
                251,
                var268[2].first,
                var268[2].second
              ).map { k ->
                val var271 =
                  getSequenceElems(history, 254, listOf(8, 189, 8, 245), k.first, k.second)
                val var272 = matchListElem(var271[3].first, var271[3].second)
                var272
              }
            listOf(var269) + var270
          }
        }
        val var263 = var267
        val var273 = ListExpr((var263 ?: listOf()), nextId(), beginGen, endGen)
        var273
      }

      var248 != null -> {
        val var274 =
          getSequenceElems(history, 256, listOf(148, 8, 153, 8, 189, 257, 8, 149), beginGen, endGen)
        val var275 = matchExpr(var274[2].first, var274[2].second)
        val var277 = history[var274[5].second].findByBeginGenOpt(131, 1, var274[5].first)
        val var278 = history[var274[5].second].findByBeginGenOpt(258, 1, var274[5].first)
        check(hasSingleTrue(var277 != null, var278 != null))
        val var279 = when {
          var277 != null -> null
          else -> {
            val var280 = getSequenceElems(
              history,
              260,
              listOf(8, 153, 216, 221),
              var274[5].first,
              var274[5].second
            )
            val var281 = matchExpr(var280[1].first, var280[1].second)
            val var282 =
              unrollRepeat0(
                history,
                216,
                218,
                11,
                217,
                var280[2].first,
                var280[2].second
              ).map { k ->
                val var283 =
                  getSequenceElems(history, 220, listOf(8, 189, 8, 153), k.first, k.second)
                val var284 = matchExpr(var283[3].first, var283[3].second)
                var284
              }
            listOf(var281) + var282
          }
        }
        val var276 = var279
        val var285 = TupleExpr(listOf(var275) + (var276 ?: listOf()), nextId(), beginGen, endGen)
        var285
      }

      var249 != null -> {
        val var287 = getSequenceElems(history, 261, listOf(148, 262, 8, 149), beginGen, endGen)
        val var288 = history[var287[1].second].findByBeginGenOpt(131, 1, var287[1].first)
        val var289 = history[var287[1].second].findByBeginGenOpt(263, 1, var287[1].first)
        check(hasSingleTrue(var288 != null, var289 != null))
        val var290 = when {
          var288 != null -> null
          else -> {
            val var291 = getSequenceElems(
              history,
              265,
              listOf(8, 266, 268, 221),
              var287[1].first,
              var287[1].second
            )
            val var292 = matchNamedExpr(var291[1].first, var291[1].second)
            val var293 =
              unrollRepeat0(
                history,
                268,
                270,
                11,
                269,
                var291[2].first,
                var291[2].second
              ).map { k ->
                val var294 =
                  getSequenceElems(history, 272, listOf(8, 189, 8, 266), k.first, k.second)
                val var295 = matchNamedExpr(var294[3].first, var294[3].second)
                var295
              }
            listOf(var292) + var293
          }
        }
        val var286 = var290
        val var296 = NamedTupleExpr((var286 ?: listOf()), nextId(), beginGen, endGen)
        var296
      }

      var250 != null -> {
        val var297 = getSequenceElems(history, 273, listOf(274), beginGen, endGen)
        val var298 = matchLiteral(var297[0].first, var297[0].second)
        var298
      }

      var251 != null -> {
        val var299 = This(nextId(), beginGen, endGen)
        var299
      }

      else -> {
        val var300 = getSequenceElems(history, 314, listOf(148, 8, 153, 8, 149), beginGen, endGen)
        val var301 = matchExpr(var300[2].first, var300[2].second)
        val var302 = Paren(var301, nextId(), beginGen, endGen)
        var302
      }
    }
    return var253
  }

  fun matchLiteral(beginGen: Int, endGen: Int): Literal {
    val var303 = history[endGen].findByBeginGenOpt(275, 1, beginGen)
    val var304 = history[endGen].findByBeginGenOpt(303, 1, beginGen)
    val var305 = history[endGen].findByBeginGenOpt(310, 1, beginGen)
    check(hasSingleTrue(var303 != null, var304 != null, var305 != null))
    val var306 = when {
      var303 != null -> {
        val var307 = getSequenceElems(history, 275, listOf(276), beginGen, endGen)
        val var308 = matchStringLiteral(var307[0].first, var307[0].second)
        var308
      }

      var304 != null -> {
        val var309 = getSequenceElems(history, 303, listOf(304), beginGen, endGen)
        val var310 = matchBooleanLiteral(var309[0].first, var309[0].second)
        var310
      }

      else -> {
        val var311 = getSequenceElems(history, 310, listOf(311), beginGen, endGen)
        val var312 = matchNoneLiteral(var311[0].first, var311[0].second)
        var312
      }
    }
    return var306
  }

  fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
    val var313 = getSequenceElems(history, 277, listOf(278, 279, 278), beginGen, endGen)
    val var314 =
      unrollRepeat0(history, 279, 281, 11, 280, var313[1].first, var313[1].second).map { k ->
        val var315 = getSequenceElems(history, 284, listOf(285), k.first, k.second)
        val var316 = matchStringElem(var315[0].first, var315[0].second)
        var316
      }
    val var317 = StringLiteral(var314, nextId(), beginGen, endGen)
    return var317
  }

  fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
    val var318 = NoneLiteral(nextId(), beginGen, endGen)
    return var318
  }

  fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
    val var319 = history[endGen].findByBeginGenOpt(286, 1, beginGen)
    val var320 = history[endGen].findByBeginGenOpt(289, 1, beginGen)
    val var321 = history[endGen].findByBeginGenOpt(294, 1, beginGen)
    check(hasSingleTrue(var319 != null, var320 != null, var321 != null))
    val var322 = when {
      var319 != null -> {
        val var323 = getSequenceElems(history, 286, listOf(287), beginGen, endGen)
        val var324 =
          JustChar((inputs[var323[0].first] as Inputs.Character).char(), nextId(), beginGen, endGen)
        var324
      }

      var320 != null -> {
        val var325 = getSequenceElems(history, 289, listOf(290), beginGen, endGen)
        val var326 = matchEscapeChar(var325[0].first, var325[0].second)
        var326
      }

      else -> {
        val var327 = getSequenceElems(history, 294, listOf(295), beginGen, endGen)
        val var328 = matchStringExpr(var327[0].first, var327[0].second)
        var328
      }
    }
    return var322
  }

  fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
    val var329 = getSequenceElems(history, 291, listOf(292, 293), beginGen, endGen)
    val var330 =
      EscapeChar((inputs[var329[1].first] as Inputs.Character).char(), nextId(), beginGen, endGen)
    return var330
  }

  fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
    val var331 = history[endGen].findByBeginGenOpt(296, 1, beginGen)
    val var332 = history[endGen].findByBeginGenOpt(302, 6, beginGen)
    check(hasSingleTrue(var331 != null, var332 != null))
    val var333 = when {
      var331 != null -> {
        val var334 = getSequenceElems(history, 296, listOf(297), beginGen, endGen)
        val var335 =
          getSequenceElems(history, 300, listOf(301, 88), var334[0].first, var334[0].second)
        val var336 = matchSimpleName(var335[1].first, var335[1].second)
        val var337 = SimpleExpr(var336, nextId(), beginGen, endGen)
        var337
      }

      else -> {
        val var338 =
          getSequenceElems(history, 302, listOf(301, 183, 8, 153, 8, 190), beginGen, endGen)
        val var339 = matchExpr(var338[3].first, var338[3].second)
        val var340 = ComplexExpr(var339, nextId(), beginGen, endGen)
        var340
      }
    }
    return var333
  }

  fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
    val var341 = getSequenceElems(history, 267, listOf(88, 8, 205, 8, 153), beginGen, endGen)
    val var342 = matchSimpleName(var341[0].first, var341[0].second)
    val var343 = matchExpr(var341[4].first, var341[4].second)
    val var344 = NamedExpr(var342, var343, nextId(), beginGen, endGen)
    return var344
  }

  fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
    val var345 = getSequenceElems(history, 437, listOf(427, 8, 438, 442), beginGen, endGen)
    val var346 = matchVarRedef(var345[2].first, var345[2].second)
    val var347 =
      unrollRepeat0(history, 442, 444, 11, 443, var345[3].first, var345[3].second).map { k ->
        val var348 = getSequenceElems(history, 446, listOf(8, 189, 8, 438), k.first, k.second)
        val var349 = matchVarRedef(var348[3].first, var348[3].second)
        var349
      }
    val var350 = VarRedefs(listOf(var346) + var347, nextId(), beginGen, endGen)
    return var350
  }

  fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
    val var351 = getSequenceElems(history, 439, listOf(88, 440, 8, 230, 8, 153), beginGen, endGen)
    val var352 = matchSimpleName(var351[0].first, var351[0].second)
    val var353 =
      unrollRepeat1(history, 440, 127, 127, 441, var351[1].first, var351[1].second).map { k ->
        val var354 = getSequenceElems(history, 129, listOf(8, 130, 8, 88), k.first, k.second)
        val var355 = matchSimpleName(var354[3].first, var354[3].second)
        var355
      }
    val var356 = matchExpr(var351[5].first, var351[5].second)
    val var357 = VarRedef(listOf(var352) + var353, var356, nextId(), beginGen, endGen)
    return var357
  }

  fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
    val var358 = history[endGen].findByBeginGenOpt(179, 1, beginGen)
    val var359 = history[endGen].findByBeginGenOpt(180, 1, beginGen)
    check(hasSingleTrue(var358 != null, var359 != null))
    val var360 = when {
      var358 != null -> {
        val var361 = getSequenceElems(history, 179, listOf(158), beginGen, endGen)
        val var362 = matchNoUnionType(var361[0].first, var361[0].second)
        var362
      }

      else -> {
        val var363 = getSequenceElems(history, 180, listOf(181), beginGen, endGen)
        val var364 = matchUnionType(var363[0].first, var363[0].second)
        var364
      }
    }
    return var360
  }

  fun matchListElem(beginGen: Int, endGen: Int): ListElem {
    val var365 = history[endGen].findByBeginGenOpt(246, 1, beginGen)
    val var366 = history[endGen].findByBeginGenOpt(247, 3, beginGen)
    check(hasSingleTrue(var365 != null, var366 != null))
    val var367 = when {
      var365 != null -> {
        val var368 = getSequenceElems(history, 246, listOf(153), beginGen, endGen)
        val var369 = matchExpr(var368[0].first, var368[0].second)
        var369
      }

      else -> {
        val var370 = getSequenceElems(history, 247, listOf(248, 8, 153), beginGen, endGen)
        val var371 = matchExpr(var370[2].first, var370[2].second)
        val var372 = EllipsisElem(var371, nextId(), beginGen, endGen)
        var372
      }
    }
    return var367
  }

  fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
    val var373 = history[endGen].findByBeginGenOpt(229, 5, beginGen)
    val var374 = history[endGen].findByBeginGenOpt(364, 7, beginGen)
    check(hasSingleTrue(var373 != null, var374 != null))
    val var375 = when {
      var373 != null -> {
        val var376 = getSequenceElems(history, 229, listOf(88, 8, 230, 8, 153), beginGen, endGen)
        val var377 = matchSimpleName(var376[0].first, var376[0].second)
        val var378 = matchExpr(var376[4].first, var376[4].second)
        val var379 = ParamDef(var377, false, null, var378, nextId(), beginGen, endGen)
        var379
      }

      else -> {
        val var380 =
          getSequenceElems(history, 364, listOf(88, 365, 8, 205, 8, 178, 370), beginGen, endGen)
        val var381 = matchSimpleName(var380[0].first, var380[0].second)
        val var382 = history[var380[1].second].findByBeginGenOpt(131, 1, var380[1].first)
        val var383 = history[var380[1].second].findByBeginGenOpt(366, 1, var380[1].first)
        check(hasSingleTrue(var382 != null, var383 != null))
        val var384 = when {
          var382 != null -> null
          else -> {
            val var385 =
              getSequenceElems(history, 368, listOf(8, 369), var380[1].first, var380[1].second)
            (inputs[var385[1].first] as Inputs.Character).char()
          }
        }
        val var386 = matchTypeExpr(var380[5].first, var380[5].second)
        val var387 = history[var380[6].second].findByBeginGenOpt(131, 1, var380[6].first)
        val var388 = history[var380[6].second].findByBeginGenOpt(371, 1, var380[6].first)
        check(hasSingleTrue(var387 != null, var388 != null))
        val var389 = when {
          var387 != null -> null
          else -> {
            val var390 = getSequenceElems(
              history,
              373,
              listOf(8, 230, 8, 153),
              var380[6].first,
              var380[6].second
            )
            val var391 = matchExpr(var390[3].first, var390[3].second)
            var391
          }
        }
        val var392 = ParamDef(var381, var384 != null, var386, var389, nextId(), beginGen, endGen)
        var392
      }
    }
    return var375
  }

  fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
    val var393 = getSequenceElems(history, 182, listOf(183, 8, 158, 184, 8, 190), beginGen, endGen)
    val var394 = matchNoUnionType(var393[2].first, var393[2].second)
    val var395 =
      unrollRepeat0(history, 184, 186, 11, 185, var393[3].first, var393[3].second).map { k ->
        val var396 = getSequenceElems(history, 188, listOf(8, 189, 8, 158), k.first, k.second)
        val var397 = matchNoUnionType(var396[3].first, var396[3].second)
        var397
      }
    val var398 = UnionType(listOf(var394) + var395, nextId(), beginGen, endGen)
    return var398
  }

  fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
    val var399 = getSequenceElems(history, 229, listOf(88, 8, 230, 8, 153), beginGen, endGen)
    val var400 = matchSimpleName(var399[0].first, var399[0].second)
    val var401 = matchExpr(var399[4].first, var399[4].second)
    val var402 = TargetDef(var400, var401, nextId(), beginGen, endGen)
    return var402
  }

  fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
    val var403 = getSequenceElems(
      history,
      409,
      listOf(410, 8, 354, 8, 88, 8, 183, 8, 88, 413, 8, 190),
      beginGen,
      endGen
    )
    val var404 = matchSimpleName(var403[4].first, var403[4].second)
    val var405 = matchSimpleName(var403[8].first, var403[8].second)
    val var406 =
      unrollRepeat0(history, 413, 415, 11, 414, var403[9].first, var403[9].second).map { k ->
        val var407 = getSequenceElems(history, 417, listOf(8, 189, 8, 88), k.first, k.second)
        val var408 = matchSimpleName(var407[3].first, var407[3].second)
        var408
      }
    val var409 = SuperClassDef(var404, listOf(var405) + var406, nextId(), beginGen, endGen)
    return var409
  }

  fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
    val var410 = history[endGen].findByBeginGenOpt(147, 3, beginGen)
    val var411 = history[endGen].findByBeginGenOpt(150, 6, beginGen)
    val var412 = history[endGen].findByBeginGenOpt(225, 6, beginGen)
    val var413 = history[endGen].findByBeginGenOpt(236, 10, beginGen)
    check(hasSingleTrue(var410 != null, var411 != null, var412 != null, var413 != null))
    val var414 = when {
      var410 != null -> {
        val var415 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
        var415
      }

      var411 != null -> {
        val var416 =
          getSequenceElems(history, 150, listOf(148, 8, 151, 221, 8, 149), beginGen, endGen)
        val var417 = matchPositionalParams(var416[2].first, var416[2].second)
        val var418 = CallParams(var417, listOf(), nextId(), beginGen, endGen)
        var418
      }

      var412 != null -> {
        val var419 =
          getSequenceElems(history, 225, listOf(148, 8, 226, 221, 8, 149), beginGen, endGen)
        val var420 = matchNamedParams(var419[2].first, var419[2].second)
        val var421 = CallParams(listOf(), var420, nextId(), beginGen, endGen)
        var421
      }

      else -> {
        val var422 = getSequenceElems(
          history,
          236,
          listOf(148, 8, 151, 8, 189, 8, 226, 221, 8, 149),
          beginGen,
          endGen
        )
        val var423 = matchPositionalParams(var422[2].first, var422[2].second)
        val var424 = matchNamedParams(var422[6].first, var422[6].second)
        val var425 = CallParams(var423, var424, nextId(), beginGen, endGen)
        var425
      }
    }
    return var414
  }

  fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
    val var426 = getSequenceElems(history, 152, listOf(153, 216), beginGen, endGen)
    val var427 = matchExpr(var426[0].first, var426[0].second)
    val var428 =
      unrollRepeat0(history, 216, 218, 11, 217, var426[1].first, var426[1].second).map { k ->
        val var429 = getSequenceElems(history, 220, listOf(8, 189, 8, 153), k.first, k.second)
        val var430 = matchExpr(var429[3].first, var429[3].second)
        var430
      }
    return listOf(var427) + var428
  }

  fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
    val var431 = getSequenceElems(history, 227, listOf(228, 231), beginGen, endGen)
    val var432 = matchNamedParam(var431[0].first, var431[0].second)
    val var433 =
      unrollRepeat0(history, 231, 233, 11, 232, var431[1].first, var431[1].second).map { k ->
        val var434 = getSequenceElems(history, 235, listOf(8, 189, 8, 228), k.first, k.second)
        val var435 = matchNamedParam(var434[3].first, var434[3].second)
        var435
      }
    return listOf(var432) + var433
  }

  fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
    val var436 = getSequenceElems(history, 229, listOf(88, 8, 230, 8, 153), beginGen, endGen)
    val var437 = matchSimpleName(var436[0].first, var436[0].second)
    val var438 = matchExpr(var436[4].first, var436[4].second)
    val var439 = NamedParam(var437, var438, nextId(), beginGen, endGen)
    return var439
  }

  fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
    val var440 = getSequenceElems(history, 202, listOf(148, 8, 203, 206, 8, 149), beginGen, endGen)
    val var441 = matchNamedType(var440[2].first, var440[2].second)
    val var442 =
      unrollRepeat0(history, 206, 208, 11, 207, var440[3].first, var440[3].second).map { k ->
        val var443 = getSequenceElems(history, 210, listOf(8, 189, 8, 203), k.first, k.second)
        val var444 = matchNamedType(var443[3].first, var443[3].second)
        var444
      }
    val var445 = NamedTupleType(listOf(var441) + var442, nextId(), beginGen, endGen)
    return var445
  }

  fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
    val var446 = getSequenceElems(history, 204, listOf(88, 8, 205, 8, 178), beginGen, endGen)
    val var447 = matchSimpleName(var446[0].first, var446[0].second)
    val var448 = matchTypeExpr(var446[4].first, var446[4].second)
    val var449 = NamedType(var447, var448, nextId(), beginGen, endGen)
    return var449
  }

  fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
    val var450 = getSequenceElems(history, 305, listOf(306), beginGen, endGen)
    val var451 = history[var450[0].second].findByBeginGenOpt(308, 1, var450[0].first)
    val var452 = history[var450[0].second].findByBeginGenOpt(309, 1, var450[0].first)
    check(hasSingleTrue(var451 != null, var452 != null))
    val var453 = when {
      var451 != null -> {
        val var454 = BooleanLiteral(true, nextId(), var450[0].first, var450[0].second)
        var454
      }

      else -> {
        val var455 = BooleanLiteral(false, nextId(), var450[0].first, var450[0].second)
        var455
      }
    }
    return var453
  }
}
