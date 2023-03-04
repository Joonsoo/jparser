package com.giyeok.jparser.metalang3.ast

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.*

class MetaLang3Ast(
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

  sealed interface TerminalChoiceElem : AstNode

  sealed interface PostUnSymbol : PreUnSymbol, AstNode

  sealed interface BinderExpr : AstNode

  data class EmptySeq(
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class StringType(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ValueType, AstNode

  sealed interface Terminal : AtomSymbol, AstNode

  sealed interface Atom : PrefixNotExpr, AstNode

  data class AbstractClassDef(
    val name: TypeName,
    val supers: List<TypeName>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassDef, AstNode

  data class NotFollowedBy(
    val notFollowedBy: PreUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PreUnSymbol, AstNode

  sealed interface AbstractEnumValue : Atom, AstNode

  data class ClassParamDef(
    val name: ParamName,
    val typeDesc: TypeDesc?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class EnumTypeDef(
    val name: EnumTypeName,
    val values: List<String>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : TypeDef, AstNode

  data class Nonterminal(
    val name: NonterminalName,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class FollowedBy(
    val followedBy: PreUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PreUnSymbol, AstNode

  data class AnyType(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NonNullTypeDesc, AstNode

  sealed interface Processor : Elem, AstNode

  data class TypeName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NonNullTypeDesc, SubType, AstNode

  data class Optional(
    val body: PostUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PostUnSymbol, AstNode

  sealed interface ValueType : NonNullTypeDesc, AstNode

  data class NamedConstructExpr(
    val typeName: TypeName,
    val params: List<NamedParam>,
    val supers: List<TypeName>?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Atom, AstNode

  sealed interface ClassDef : SubType, TypeDef, AstNode

  sealed interface PExpr : AstNode

  data class ValRef(
    val idx: String,
    val condSymPath: List<CondSymDir>?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Ref, AstNode

  data class ArrayExpr(
    val elems: List<PExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Atom, AstNode

  data class TerminalChoiceRange(
    val charStart: TerminalChoiceChar,
    val charEnd: TerminalChoiceChar,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : TerminalChoiceElem, AstNode

  sealed interface StringChar : AstNode

  data class ExceptSymbol(
    val body: BinSymbol,
    val except: PreUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : BinSymbol, AstNode

  data class RawRef(
    val idx: String,
    val condSymPath: List<CondSymDir>?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Ref, AstNode

  data class BoolLiteral(
    val value: Boolean,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  sealed interface Literal : Atom, AstNode

  data class LHS(
    val name: Nonterminal,
    val typeDesc: TypeDesc?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class TernaryOp(
    val cond: BoolOrExpr,
    val ifTrue: TernaryExpr,
    val ifFalse: TernaryExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : TernaryExpr, AstNode

  sealed interface BoolAndExpr : BoolOrExpr, AstNode

  sealed interface PrefixNotExpr : AdditiveExpr, AstNode

  data class ConcreteClassDef(
    val name: TypeName,
    val supers: List<TypeName>?,
    val params: List<ClassParamDef>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ClassDef, AstNode

  data class FuncCallOrConstructExpr(
    val funcName: TypeOrFuncName,
    val params: List<PExpr>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Atom, AstNode

  data class Sequence(
    val seq: List<Elem>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Symbol, AstNode

  data class InPlaceChoices(
    val choices: List<Sequence>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class SuperDef(
    val typeName: TypeName,
    val subs: List<SubType>?,
    val supers: List<TypeName>?,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : SubType, TypeDef, AstNode

  data class Rule(
    val lhs: LHS,
    val rhs: List<Sequence>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Def, AstNode

  sealed interface TypeDef : Def, NonNullTypeDesc, AstNode

  data class StrLiteral(
    val value: List<StringChar>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  data class CharAsIs(
    val value: Char,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringChar, TerminalChar, TerminalChoiceChar, AstNode

  data class ElvisOp(
    val value: AdditiveExpr,
    val ifNull: ElvisExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ElvisExpr, AstNode

  data class AnyTerminal(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Terminal, AstNode

  data class BindExpr(
    val ctx: ValRef,
    val binder: BinderExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Atom, BinderExpr, AstNode

  sealed interface TerminalChoiceChar : TerminalChoiceElem, AstNode

  data class RepeatFromZero(
    val body: PostUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PostUnSymbol, AstNode

  data class ExprParen(
    val body: PExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Atom, AstNode

  data class EnumTypeName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NonNullTypeDesc, AstNode

  data class JoinSymbol(
    val body: BinSymbol,
    val join: PreUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : BinSymbol, AstNode

  sealed interface Elem : AstNode

  sealed interface AtomSymbol : PostUnSymbol, AstNode

  data class ArrayTypeDesc(
    val elemType: TypeDesc,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : NonNullTypeDesc, AstNode

  data class NullLiteral(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  sealed interface SubType : AstNode

  data class CharType(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ValueType, AstNode

  data class StringSymbol(
    val value: List<StringChar>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class BinOp(
    val op: Op,
    val lhs: BoolAndExpr,
    val rhs: BoolOrExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AdditiveExpr, AstNode

  sealed interface BoolOrExpr : TernaryExpr, AstNode

  data class ProcessorBlock(
    val body: PExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : BinderExpr, Processor, AstNode

  sealed interface Symbol : Elem, AstNode

  data class TypeDesc(
    val typ: NonNullTypeDesc,
    val optional: Boolean,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  sealed interface TernaryExpr : PExpr, AstNode

  data class NonterminalName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  sealed interface NonNullTypeDesc : AstNode

  sealed interface AdditiveExpr : ElvisExpr, AstNode

  data class RepeatFromOne(
    val body: PostUnSymbol,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PostUnSymbol, AstNode

  data class Longest(
    val choices: InPlaceChoices,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class NamedParam(
    val name: ParamName,
    val typeDesc: TypeDesc?,
    val expr: PExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  sealed interface Def : AstNode

  data class PrefixOp(
    val op: PreOp,
    val expr: PrefixNotExpr,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PrefixNotExpr, AstNode

  data class CharLiteral(
    val value: TerminalChar,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : Literal, AstNode

  data class TerminalChoice(
    val choices: List<TerminalChoiceElem>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AtomSymbol, AstNode

  data class ShortenedEnumValue(
    val valueName: EnumValueName,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AbstractEnumValue, AstNode

  data class TypeOrFuncName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class ParamName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  data class CharUnicode(
    val code: List<Char>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringChar, TerminalChar, TerminalChoiceChar, AstNode

  sealed interface PreUnSymbol : BinSymbol, AstNode

  data class BooleanType(

    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : ValueType, AstNode

  sealed interface BinSymbol : Symbol, AstNode

  data class TypedPExpr(
    val body: TernaryExpr,
    val typ: TypeDesc,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : PExpr, AstNode

  sealed interface TerminalChar : Terminal, AstNode

  data class CanonicalEnumValue(
    val enumName: EnumTypeName,
    val valueName: EnumValueName,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AbstractEnumValue, AstNode

  data class CharEscaped(
    val escapeCode: Char,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : StringChar, TerminalChar, TerminalChoiceChar, AstNode

  sealed interface Ref : Atom, BinderExpr, Processor, AstNode

  sealed interface BoolEqExpr : BoolAndExpr, AstNode

  data class EnumValueName(
    val name: String,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  sealed interface ElvisExpr : BoolEqExpr, AstNode

  data class Grammar(
    val defs: List<Def>,
    override val nodeId: Int,
    override val start: Int,
    override val end: Int,
  ) : AstNode

  enum class CondSymDir { BODY, COND }
  enum class KeyWord { BOOLEAN, CHAR, FALSE, NULL, STRING, TRUE }
  enum class Op { ADD, AND, EQ, NE, OR }
  enum class PreOp { NOT }

  fun matchStart(): Grammar {
    val lastGen = inputs.size
    val kernel = history[lastGen]
      .filter { it.symbolId() == 2 && it.pointer() == 1 && it.endGen() == lastGen }
      .checkSingle()
    return matchGrammar(kernel.beginGen(), kernel.endGen())
  }

  fun matchGrammar(beginGen: Int, endGen: Int): Grammar {
    val var1 = getSequenceElems(history, 3, listOf(4, 57, 470, 4), beginGen, endGen)
    val var2 = matchDef(var1[1].first, var1[1].second)
    val var3 = unrollRepeat0(history, 470, 472, 7, 471, var1[2].first, var1[2].second).map { k ->
      val var4 = getSequenceElems(history, 474, listOf(475, 57), k.first, k.second)
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
    val var14 = getSequenceElems(history, 60, listOf(61, 4, 215, 4, 216), beginGen, endGen)
    val var15 = matchLHS(var14[0].first, var14[0].second)
    val var16 = getSequenceElems(history, 218, listOf(219, 465), var14[4].first, var14[4].second)
    val var17 = matchRHS(var16[0].first, var16[0].second)
    val var18 = unrollRepeat0(history, 465, 467, 7, 466, var16[1].first, var16[1].second).map { k ->
      val var19 = getSequenceElems(history, 469, listOf(4, 298, 4, 219), k.first, k.second)
      val var20 = matchRHS(var19[3].first, var19[3].second)
      var20
    }
    val var21 = Rule(var15, listOf(var17) + var18, nextId(), beginGen, endGen)
    return var21
  }

  fun matchLHS(beginGen: Int, endGen: Int): LHS {
    val var22 = getSequenceElems(history, 62, listOf(63, 116), beginGen, endGen)
    val var23 = matchNonterminal(var22[0].first, var22[0].second)
    val var24 = history[var22[1].second].findByBeginGenOpt(56, 1, var22[1].first)
    val var25 = history[var22[1].second].findByBeginGenOpt(117, 1, var22[1].first)
    check(hasSingleTrue(var24 != null, var25 != null))
    val var26 = when {
      var24 != null -> null
      else -> {
        val var27 =
          getSequenceElems(history, 119, listOf(4, 120, 4, 121), var22[1].first, var22[1].second)
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
    val var43 =
      getSequenceElems(history, 201, listOf(138, 4, 185, 4, 202, 4, 198), beginGen, endGen)
    val var44 = matchEnumTypeName(var43[0].first, var43[0].second)
    val var45 = getSequenceElems(history, 204, listOf(70, 205), var43[4].first, var43[4].second)
    val var46 = matchId(var45[0].first, var45[0].second)
    val var47 = unrollRepeat0(history, 205, 207, 7, 206, var45[1].first, var45[1].second).map { k ->
      val var48 = getSequenceElems(history, 209, listOf(4, 158, 4, 70), k.first, k.second)
      val var49 = matchId(var48[3].first, var48[3].second)
      var49
    }
    val var50 = EnumTypeDef(var44, listOf(var46) + var47, nextId(), beginGen, endGen)
    return var50
  }

  fun matchId(beginGen: Int, endGen: Int): String {
    val var51 = getSequenceElems(history, 71, listOf(72), beginGen, endGen)
    val var52 = getSequenceElems(history, 75, listOf(76, 77), var51[0].first, var51[0].second)
    val var53 = unrollRepeat0(history, 77, 79, 7, 78, var52[1].first, var52[1].second).map { k ->
      (inputs[k.first] as Inputs.Character).char()
    }
    return (inputs[var52[0].first] as Inputs.Character).char()
      .toString() + var53.joinToString("") { it.toString() }
  }

  fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
    val var54 = history[endGen].findByBeginGenOpt(145, 3, beginGen)
    val var55 = history[endGen].findByBeginGenOpt(160, 3, beginGen)
    val var56 = history[endGen].findByBeginGenOpt(177, 5, beginGen)
    check(hasSingleTrue(var54 != null, var55 != null, var56 != null))
    val var57 = when {
      var54 != null -> {
        val var58 = getSequenceElems(history, 145, listOf(125, 4, 146), beginGen, endGen)
        val var59 = matchTypeName(var58[0].first, var58[0].second)
        val var60 = matchSuperTypes(var58[2].first, var58[2].second)
        val var61 = AbstractClassDef(var59, var60, nextId(), beginGen, endGen)
        var61
      }

      var55 != null -> {
        val var62 = getSequenceElems(history, 160, listOf(125, 4, 161), beginGen, endGen)
        val var63 = matchTypeName(var62[0].first, var62[0].second)
        val var64 = matchClassParamsDef(var62[2].first, var62[2].second)
        val var65 = ConcreteClassDef(var63, null, var64, nextId(), beginGen, endGen)
        var65
      }

      else -> {
        val var66 = getSequenceElems(history, 177, listOf(125, 4, 146, 4, 161), beginGen, endGen)
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
        val var77 = getSequenceElems(history, 114, listOf(115, 70, 115), beginGen, endGen)
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
    val var82 =
      getSequenceElems(history, 180, listOf(125, 181, 4, 185, 186, 4, 198), beginGen, endGen)
    val var83 = matchTypeName(var82[0].first, var82[0].second)
    val var84 = history[var82[4].second].findByBeginGenOpt(56, 1, var82[4].first)
    val var85 = history[var82[4].second].findByBeginGenOpt(187, 1, var82[4].first)
    check(hasSingleTrue(var84 != null, var85 != null))
    val var86 = when {
      var84 != null -> null
      else -> {
        val var87 = getSequenceElems(history, 189, listOf(4, 190), var82[4].first, var82[4].second)
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
        val var92 = getSequenceElems(history, 184, listOf(4, 146), var82[1].first, var82[1].second)
        val var93 = matchSuperTypes(var92[1].first, var92[1].second)
        var93
      }
    }
    val var94 = SuperDef(var83, var86, var91, nextId(), beginGen, endGen)
    return var94
  }

  fun matchSuperTypes(beginGen: Int, endGen: Int): List<TypeName> {
    val var96 = getSequenceElems(history, 147, listOf(148, 4, 149, 159), beginGen, endGen)
    val var97 = history[var96[2].second].findByBeginGenOpt(56, 1, var96[2].first)
    val var98 = history[var96[2].second].findByBeginGenOpt(150, 1, var96[2].first)
    check(hasSingleTrue(var97 != null, var98 != null))
    val var99 = when {
      var97 != null -> null
      else -> {
        val var100 =
          getSequenceElems(history, 152, listOf(125, 153, 4), var96[2].first, var96[2].second)
        val var101 = matchTypeName(var100[0].first, var100[0].second)
        val var102 =
          unrollRepeat0(history, 153, 155, 7, 154, var100[1].first, var100[1].second).map { k ->
            val var103 = getSequenceElems(history, 157, listOf(4, 158, 4, 125), k.first, k.second)
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
    val var105 = getSequenceElems(history, 191, listOf(192, 193), beginGen, endGen)
    val var106 = matchSubType(var105[0].first, var105[0].second)
    val var107 =
      unrollRepeat0(history, 193, 195, 7, 194, var105[1].first, var105[1].second).map { k ->
        val var108 = getSequenceElems(history, 197, listOf(4, 158, 4, 192), k.first, k.second)
        val var109 = matchSubType(var108[3].first, var108[3].second)
        var109
      }
    return listOf(var106) + var107
  }

  fun matchClassParamsDef(beginGen: Int, endGen: Int): List<ClassParamDef> {
    val var111 = getSequenceElems(history, 162, listOf(163, 4, 164, 4, 176), beginGen, endGen)
    val var112 = history[var111[2].second].findByBeginGenOpt(56, 1, var111[2].first)
    val var113 = history[var111[2].second].findByBeginGenOpt(165, 1, var111[2].first)
    check(hasSingleTrue(var112 != null, var113 != null))
    val var114 = when {
      var112 != null -> null
      else -> {
        val var115 =
          getSequenceElems(history, 167, listOf(168, 171, 4), var111[2].first, var111[2].second)
        val var116 = matchClassParamDef(var115[0].first, var115[0].second)
        val var117 =
          unrollRepeat0(history, 171, 173, 7, 172, var115[1].first, var115[1].second).map { k ->
            val var118 = getSequenceElems(history, 175, listOf(4, 158, 4, 168), k.first, k.second)
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
    val var120 = getSequenceElems(history, 169, listOf(170, 116), beginGen, endGen)
    val var121 = matchParamName(var120[0].first, var120[0].second)
    val var122 = history[var120[1].second].findByBeginGenOpt(56, 1, var120[1].first)
    val var123 = history[var120[1].second].findByBeginGenOpt(117, 1, var120[1].first)
    check(hasSingleTrue(var122 != null, var123 != null))
    val var124 = when {
      var122 != null -> null
      else -> {
        val var125 =
          getSequenceElems(history, 119, listOf(4, 120, 4, 121), var120[1].first, var120[1].second)
        val var126 = matchTypeDesc(var125[3].first, var125[3].second)
        var126
      }
    }
    val var127 = ClassParamDef(var121, var124, nextId(), beginGen, endGen)
    return var127
  }

  fun matchEnumTypeName(beginGen: Int, endGen: Int): EnumTypeName {
    val var128 = getSequenceElems(history, 139, listOf(140, 70), beginGen, endGen)
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
    val var133 = getSequenceElems(history, 222, listOf(223, 460), beginGen, endGen)
    val var134 = matchElem(var133[0].first, var133[0].second)
    val var135 =
      unrollRepeat0(history, 460, 462, 7, 461, var133[1].first, var133[1].second).map { k ->
        val var136 = getSequenceElems(history, 464, listOf(4, 223), k.first, k.second)
        val var137 = matchElem(var136[1].first, var136[1].second)
        var137
      }
    val var138 = Sequence(listOf(var134) + var135, nextId(), beginGen, endGen)
    return var138
  }

  fun matchElem(beginGen: Int, endGen: Int): Elem {
    val var139 = history[endGen].findByBeginGenOpt(224, 1, beginGen)
    val var140 = history[endGen].findByBeginGenOpt(308, 1, beginGen)
    check(hasSingleTrue(var139 != null, var140 != null))
    val var141 = when {
      var139 != null -> {
        val var142 = getSequenceElems(history, 224, listOf(225), beginGen, endGen)
        val var143 = matchSymbol(var142[0].first, var142[0].second)
        var143
      }

      else -> {
        val var144 = getSequenceElems(history, 308, listOf(309), beginGen, endGen)
        val var145 = matchProcessor(var144[0].first, var144[0].second)
        var145
      }
    }
    return var141
  }

  fun matchProcessor(beginGen: Int, endGen: Int): Processor {
    val var146 = history[endGen].findByBeginGenOpt(310, 1, beginGen)
    val var147 = history[endGen].findByBeginGenOpt(344, 1, beginGen)
    check(hasSingleTrue(var146 != null, var147 != null))
    val var148 = when {
      var146 != null -> {
        val var149 = getSequenceElems(history, 310, listOf(311), beginGen, endGen)
        val var150 = matchRef(var149[0].first, var149[0].second)
        var150
      }

      else -> {
        val var151 = getSequenceElems(history, 344, listOf(345), beginGen, endGen)
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
        val var169 = getSequenceElems(history, 114, listOf(115, 70, 115), beginGen, endGen)
        val var170 = matchId(var169[1].first, var169[1].second)
        val var171 = ParamName(var170, nextId(), beginGen, endGen)
        var171
      }
    }
    return var165
  }

  fun matchPExprBlock(beginGen: Int, endGen: Int): ProcessorBlock {
    val var172 = getSequenceElems(history, 346, listOf(185, 4, 347, 4, 198), beginGen, endGen)
    val var173 = matchPExpr(var172[2].first, var172[2].second)
    val var174 = ProcessorBlock(var173, nextId(), beginGen, endGen)
    return var174
  }

  fun matchPExpr(beginGen: Int, endGen: Int): PExpr {
    val var175 = history[endGen].findByBeginGenOpt(348, 5, beginGen)
    val var176 = history[endGen].findByBeginGenOpt(458, 1, beginGen)
    check(hasSingleTrue(var175 != null, var176 != null))
    val var177 = when {
      var175 != null -> {
        val var178 = getSequenceElems(history, 348, listOf(349, 4, 120, 4, 121), beginGen, endGen)
        val var179 = matchTernaryExpr(var178[0].first, var178[0].second)
        val var180 = matchTypeDesc(var178[4].first, var178[4].second)
        val var181 = TypedPExpr(var179, var180, nextId(), beginGen, endGen)
        var181
      }

      else -> {
        val var182 = getSequenceElems(history, 458, listOf(349), beginGen, endGen)
        val var183 = matchTernaryExpr(var182[0].first, var182[0].second)
        var183
      }
    }
    return var177
  }

  fun matchRef(beginGen: Int, endGen: Int): Ref {
    val var184 = history[endGen].findByBeginGenOpt(312, 1, beginGen)
    val var185 = history[endGen].findByBeginGenOpt(339, 1, beginGen)
    check(hasSingleTrue(var184 != null, var185 != null))
    val var186 = when {
      var184 != null -> {
        val var187 = getSequenceElems(history, 312, listOf(313), beginGen, endGen)
        val var188 = matchValRef(var187[0].first, var187[0].second)
        var188
      }

      else -> {
        val var189 = getSequenceElems(history, 339, listOf(340), beginGen, endGen)
        val var190 = matchRawRef(var189[0].first, var189[0].second)
        var190
      }
    }
    return var186
  }

  fun matchValRef(beginGen: Int, endGen: Int): ValRef {
    val var191 = getSequenceElems(history, 314, listOf(315, 316, 326), beginGen, endGen)
    val var192 = matchRefIdx(var191[2].first, var191[2].second)
    val var193 = history[var191[1].second].findByBeginGenOpt(56, 1, var191[1].first)
    val var194 = history[var191[1].second].findByBeginGenOpt(317, 1, var191[1].first)
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
    val var198 = getSequenceElems(history, 318, listOf(319), beginGen, endGen)
    val var199 =
      unrollRepeat1(history, 319, 320, 320, 325, var198[0].first, var198[0].second).map { k ->
        val var200 = history[k.second].findByBeginGenOpt(321, 1, k.first)
        val var201 = history[k.second].findByBeginGenOpt(323, 1, k.first)
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
    val var203 = getSequenceElems(history, 341, listOf(342, 316, 326), beginGen, endGen)
    val var204 = matchRefIdx(var203[2].first, var203[2].second)
    val var205 = history[var203[1].second].findByBeginGenOpt(56, 1, var203[1].first)
    val var206 = history[var203[1].second].findByBeginGenOpt(317, 1, var203[1].first)
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
    val var210 = getSequenceElems(history, 327, listOf(328), beginGen, endGen)
    val var211 = history[var210[0].second].findByBeginGenOpt(330, 1, var210[0].first)
    val var212 = history[var210[0].second].findByBeginGenOpt(333, 1, var210[0].first)
    check(hasSingleTrue(var211 != null, var212 != null))
    val var213 = when {
      var211 != null -> {
        val var214 = getSequenceElems(history, 331, listOf(332), var210[0].first, var210[0].second)
        (inputs[var214[0].first] as Inputs.Character).char().toString()
      }

      else -> {
        val var215 =
          getSequenceElems(history, 334, listOf(335, 336), var210[0].first, var210[0].second)
        val var216 =
          unrollRepeat0(history, 336, 338, 7, 337, var215[1].first, var215[1].second).map { k ->
            (inputs[k.first] as Inputs.Character).char()
          }
        (inputs[var215[0].first] as Inputs.Character).char()
          .toString() + var216.joinToString("") { it.toString() }
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
    val var220 = history[endGen].findByBeginGenOpt(306, 5, beginGen)
    val var221 = history[endGen].findByBeginGenOpt(307, 1, beginGen)
    check(hasSingleTrue(var219 != null, var220 != null, var221 != null))
    val var222 = when {
      var219 != null -> {
        val var223 = getSequenceElems(history, 228, listOf(227, 4, 229, 4, 230), beginGen, endGen)
        val var224 = matchBinSymbol(var223[0].first, var223[0].second)
        val var225 = matchPreUnSymbol(var223[4].first, var223[4].second)
        val var226 = JoinSymbol(var224, var225, nextId(), beginGen, endGen)
        var226
      }

      var220 != null -> {
        val var227 = getSequenceElems(history, 306, listOf(227, 4, 273, 4, 230), beginGen, endGen)
        val var228 = matchBinSymbol(var227[0].first, var227[0].second)
        val var229 = matchPreUnSymbol(var227[4].first, var227[4].second)
        val var230 = ExceptSymbol(var228, var229, nextId(), beginGen, endGen)
        var230
      }

      else -> {
        val var231 = getSequenceElems(history, 307, listOf(230), beginGen, endGen)
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
        val var237 = getSequenceElems(history, 231, listOf(232, 4, 230), beginGen, endGen)
        val var238 = matchPreUnSymbol(var237[2].first, var237[2].second)
        val var239 = FollowedBy(var238, nextId(), beginGen, endGen)
        var239
      }

      var234 != null -> {
        val var240 = getSequenceElems(history, 233, listOf(234, 4, 230), beginGen, endGen)
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
        val var250 = getSequenceElems(history, 237, listOf(236, 4, 214), beginGen, endGen)
        val var251 = matchPostUnSymbol(var250[0].first, var250[0].second)
        val var252 = Optional(var251, nextId(), beginGen, endGen)
        var252
      }

      var246 != null -> {
        val var253 = getSequenceElems(history, 238, listOf(236, 4, 43), beginGen, endGen)
        val var254 = matchPostUnSymbol(var253[0].first, var253[0].second)
        val var255 = RepeatFromZero(var254, nextId(), beginGen, endGen)
        var255
      }

      var247 != null -> {
        val var256 = getSequenceElems(history, 239, listOf(236, 4, 240), beginGen, endGen)
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
    val var263 = history[endGen].findByBeginGenOpt(277, 1, beginGen)
    val var264 = history[endGen].findByBeginGenOpt(289, 1, beginGen)
    val var265 = history[endGen].findByBeginGenOpt(290, 5, beginGen)
    val var266 = history[endGen].findByBeginGenOpt(299, 1, beginGen)
    val var267 = history[endGen].findByBeginGenOpt(302, 1, beginGen)
    check(
      hasSingleTrue(
        var261 != null,
        var262 != null,
        var263 != null,
        var264 != null,
        var265 != null,
        var266 != null,
        var267 != null
      )
    )
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
        val var273 = getSequenceElems(history, 277, listOf(278), beginGen, endGen)
        val var274 = matchStringSymbol(var273[0].first, var273[0].second)
        var274
      }

      var264 != null -> {
        val var275 = getSequenceElems(history, 289, listOf(63), beginGen, endGen)
        val var276 = matchNonterminal(var275[0].first, var275[0].second)
        var276
      }

      var265 != null -> {
        val var277 = getSequenceElems(history, 290, listOf(163, 4, 291, 4, 176), beginGen, endGen)
        val var278 = matchInPlaceChoices(var277[2].first, var277[2].second)
        var278
      }

      var266 != null -> {
        val var279 = getSequenceElems(history, 299, listOf(300), beginGen, endGen)
        val var280 = matchLongest(var279[0].first, var279[0].second)
        var280
      }

      else -> {
        val var281 = getSequenceElems(history, 302, listOf(303), beginGen, endGen)
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
        val var286 = getSequenceElems(history, 245, listOf(246, 247, 246), beginGen, endGen)
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
    val var289 = getSequenceElems(history, 292, listOf(221, 293), beginGen, endGen)
    val var290 = matchSequence(var289[0].first, var289[0].second)
    val var291 =
      unrollRepeat0(history, 293, 295, 7, 294, var289[1].first, var289[1].second).map { k ->
        val var292 = getSequenceElems(history, 297, listOf(4, 298, 4, 221), k.first, k.second)
        val var293 = matchSequence(var292[3].first, var292[3].second)
        var293
      }
    val var294 = InPlaceChoices(listOf(var290) + var291, nextId(), beginGen, endGen)
    return var294
  }

  fun matchStringSymbol(beginGen: Int, endGen: Int): StringSymbol {
    val var295 = getSequenceElems(history, 279, listOf(280, 281, 280), beginGen, endGen)
    val var296 =
      unrollRepeat0(history, 281, 283, 7, 282, var295[1].first, var295[1].second).map { k ->
        val var297 = matchStringChar(k.first, k.second)
        var297
      }
    val var298 = StringSymbol(var296, nextId(), beginGen, endGen)
    return var298
  }

  fun matchStringChar(beginGen: Int, endGen: Int): StringChar {
    val var299 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
    val var300 = history[endGen].findByBeginGenOpt(284, 1, beginGen)
    val var301 = history[endGen].findByBeginGenOpt(287, 2, beginGen)
    check(hasSingleTrue(var299 != null, var300 != null, var301 != null))
    val var302 = when {
      var299 != null -> {
        val var303 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
        val var304 = matchUnicodeChar(var303[0].first, var303[0].second)
        var304
      }

      var300 != null -> {
        val var305 = getSequenceElems(history, 284, listOf(285), beginGen, endGen)
        val var306 =
          CharAsIs((inputs[var305[0].first] as Inputs.Character).char(), nextId(), beginGen, endGen)
        var306
      }

      else -> {
        val var307 = getSequenceElems(history, 287, listOf(250, 288), beginGen, endGen)
        val var308 = CharEscaped(
          (inputs[var307[1].first] as Inputs.Character).char(),
          nextId(),
          beginGen,
          endGen
        )
        var308
      }
    }
    return var302
  }

  fun matchUnicodeChar(beginGen: Int, endGen: Int): CharUnicode {
    val var309 =
      getSequenceElems(history, 255, listOf(250, 106, 256, 256, 256, 256), beginGen, endGen)
    val var310 = CharUnicode(
      listOf(
        (inputs[var309[2].first] as Inputs.Character).char(),
        (inputs[var309[3].first] as Inputs.Character).char(),
        (inputs[var309[4].first] as Inputs.Character).char(),
        (inputs[var309[5].first] as Inputs.Character).char()
      ), nextId(), beginGen, endGen
    )
    return var310
  }

  fun matchLongest(beginGen: Int, endGen: Int): Longest {
    val var311 = getSequenceElems(history, 301, listOf(148, 4, 291, 4, 159), beginGen, endGen)
    val var312 = matchInPlaceChoices(var311[2].first, var311[2].second)
    val var313 = Longest(var312, nextId(), beginGen, endGen)
    return var313
  }

  fun matchTerminalChoice(beginGen: Int, endGen: Int): TerminalChoice {
    val var314 = history[endGen].findByBeginGenOpt(261, 4, beginGen)
    val var315 = history[endGen].findByBeginGenOpt(276, 3, beginGen)
    check(hasSingleTrue(var314 != null, var315 != null))
    val var316 = when {
      var314 != null -> {
        val var317 = getSequenceElems(history, 261, listOf(246, 262, 274, 246), beginGen, endGen)
        val var318 = matchTerminalChoiceElem(var317[1].first, var317[1].second)
        val var319 =
          unrollRepeat1(history, 274, 262, 262, 275, var317[2].first, var317[2].second).map { k ->
            val var320 = matchTerminalChoiceElem(k.first, k.second)
            var320
          }
        val var321 = TerminalChoice(listOf(var318) + var319, nextId(), beginGen, endGen)
        var321
      }

      else -> {
        val var322 = getSequenceElems(history, 276, listOf(246, 271, 246), beginGen, endGen)
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
    check(hasSingleTrue(var325 != null, var326 != null))
    val var327 = when {
      var325 != null -> {
        val var328 = getSequenceElems(history, 263, listOf(264), beginGen, endGen)
        val var329 = matchTerminalChoiceChar(var328[0].first, var328[0].second)
        var329
      }

      else -> {
        val var330 = getSequenceElems(history, 270, listOf(271), beginGen, endGen)
        val var331 = matchTerminalChoiceRange(var330[0].first, var330[0].second)
        var331
      }
    }
    return var327
  }

  fun matchTerminalChoiceRange(beginGen: Int, endGen: Int): TerminalChoiceRange {
    val var332 = getSequenceElems(history, 272, listOf(264, 273, 264), beginGen, endGen)
    val var333 = matchTerminalChoiceChar(var332[0].first, var332[0].second)
    val var334 = matchTerminalChoiceChar(var332[2].first, var332[2].second)
    val var335 = TerminalChoiceRange(var333, var334, nextId(), beginGen, endGen)
    return var335
  }

  fun matchTerminalChoiceChar(beginGen: Int, endGen: Int): TerminalChoiceChar {
    val var336 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
    val var337 = history[endGen].findByBeginGenOpt(265, 1, beginGen)
    val var338 = history[endGen].findByBeginGenOpt(268, 2, beginGen)
    check(hasSingleTrue(var336 != null, var337 != null, var338 != null))
    val var339 = when {
      var336 != null -> {
        val var340 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
        val var341 = matchUnicodeChar(var340[0].first, var340[0].second)
        var341
      }

      var337 != null -> {
        val var342 = getSequenceElems(history, 265, listOf(266), beginGen, endGen)
        val var343 =
          CharAsIs((inputs[var342[0].first] as Inputs.Character).char(), nextId(), beginGen, endGen)
        var343
      }

      else -> {
        val var344 = getSequenceElems(history, 268, listOf(250, 269), beginGen, endGen)
        val var345 = CharEscaped(
          (inputs[var344[1].first] as Inputs.Character).char(),
          nextId(),
          beginGen,
          endGen
        )
        var345
      }
    }
    return var339
  }

  fun matchEmptySequence(beginGen: Int, endGen: Int): EmptySeq {
    val var346 = EmptySeq(nextId(), beginGen, endGen)
    return var346
  }

  fun matchTerminalChar(beginGen: Int, endGen: Int): TerminalChar {
    val var347 = history[endGen].findByBeginGenOpt(248, 1, beginGen)
    val var348 = history[endGen].findByBeginGenOpt(251, 2, beginGen)
    val var349 = history[endGen].findByBeginGenOpt(253, 1, beginGen)
    check(hasSingleTrue(var347 != null, var348 != null, var349 != null))
    val var350 = when {
      var347 != null -> {
        val var351 = getSequenceElems(history, 248, listOf(249), beginGen, endGen)
        val var352 =
          CharAsIs((inputs[var351[0].first] as Inputs.Character).char(), nextId(), beginGen, endGen)
        var352
      }

      var348 != null -> {
        val var353 = getSequenceElems(history, 251, listOf(250, 252), beginGen, endGen)
        val var354 = CharEscaped(
          (inputs[var353[1].first] as Inputs.Character).char(),
          nextId(),
          beginGen,
          endGen
        )
        var354
      }

      else -> {
        val var355 = getSequenceElems(history, 253, listOf(254), beginGen, endGen)
        val var356 = matchUnicodeChar(var355[0].first, var355[0].second)
        var356
      }
    }
    return var350
  }

  fun matchTypeDesc(beginGen: Int, endGen: Int): TypeDesc {
    val var357 = getSequenceElems(history, 122, listOf(123, 210), beginGen, endGen)
    val var358 = matchNonNullTypeDesc(var357[0].first, var357[0].second)
    val var359 = history[var357[1].second].findByBeginGenOpt(56, 1, var357[1].first)
    val var360 = history[var357[1].second].findByBeginGenOpt(211, 1, var357[1].first)
    check(hasSingleTrue(var359 != null, var360 != null))
    val var361 = when {
      var359 != null -> null
      else -> {
        val var362 =
          getSequenceElems(history, 213, listOf(4, 214), var357[1].first, var357[1].second)
        (inputs[var362[1].first] as Inputs.Character).char()
      }
    }
    val var363 = TypeDesc(var358, var361 != null, nextId(), beginGen, endGen)
    return var363
  }

  fun matchTernaryExpr(beginGen: Int, endGen: Int): TernaryExpr {
    val var364 = history[endGen].findByBeginGenOpt(350, 9, beginGen)
    val var365 = history[endGen].findByBeginGenOpt(459, 1, beginGen)
    check(hasSingleTrue(var364 != null, var365 != null))
    val var366 = when {
      var364 != null -> {
        val var367 = getSequenceElems(
          history,
          350,
          listOf(351, 4, 214, 4, 455, 4, 120, 4, 455),
          beginGen,
          endGen
        )
        val var368 = matchBoolOrExpr(var367[0].first, var367[0].second)
        val var369 = getSequenceElems(history, 458, listOf(349), var367[4].first, var367[4].second)
        val var370 = matchTernaryExpr(var369[0].first, var369[0].second)
        val var371 = getSequenceElems(history, 458, listOf(349), var367[8].first, var367[8].second)
        val var372 = matchTernaryExpr(var371[0].first, var371[0].second)
        val var373 = TernaryOp(var368, var370, var372, nextId(), beginGen, endGen)
        var373
      }

      else -> {
        val var374 = getSequenceElems(history, 459, listOf(351), beginGen, endGen)
        val var375 = matchBoolOrExpr(var374[0].first, var374[0].second)
        var375
      }
    }
    return var366
  }

  fun matchBoolOrExpr(beginGen: Int, endGen: Int): BoolOrExpr {
    val var376 = history[endGen].findByBeginGenOpt(352, 5, beginGen)
    val var377 = history[endGen].findByBeginGenOpt(454, 1, beginGen)
    check(hasSingleTrue(var376 != null, var377 != null))
    val var378 = when {
      var376 != null -> {
        val var379 = getSequenceElems(history, 352, listOf(353, 4, 452, 4, 351), beginGen, endGen)
        val var380 = matchBoolAndExpr(var379[0].first, var379[0].second)
        val var381 = matchBoolOrExpr(var379[4].first, var379[4].second)
        val var382 = BinOp(Op.AND, var380, var381, nextId(), beginGen, endGen)
        var382
      }

      else -> {
        val var383 = getSequenceElems(history, 454, listOf(353), beginGen, endGen)
        val var384 = matchBoolAndExpr(var383[0].first, var383[0].second)
        var384
      }
    }
    return var378
  }

  fun matchBoolAndExpr(beginGen: Int, endGen: Int): BoolAndExpr {
    val var385 = history[endGen].findByBeginGenOpt(354, 5, beginGen)
    val var386 = history[endGen].findByBeginGenOpt(451, 1, beginGen)
    check(hasSingleTrue(var385 != null, var386 != null))
    val var387 = when {
      var385 != null -> {
        val var388 = getSequenceElems(history, 354, listOf(355, 4, 449, 4, 353), beginGen, endGen)
        val var389 = matchBoolEqExpr(var388[0].first, var388[0].second)
        val var390 = matchBoolAndExpr(var388[4].first, var388[4].second)
        val var391 = BinOp(Op.OR, var389, var390, nextId(), beginGen, endGen)
        var391
      }

      else -> {
        val var392 = getSequenceElems(history, 451, listOf(355), beginGen, endGen)
        val var393 = matchBoolEqExpr(var392[0].first, var392[0].second)
        var393
      }
    }
    return var387
  }

  fun matchBoolEqExpr(beginGen: Int, endGen: Int): BoolEqExpr {
    val var394 = history[endGen].findByBeginGenOpt(356, 5, beginGen)
    val var395 = history[endGen].findByBeginGenOpt(448, 1, beginGen)
    check(hasSingleTrue(var394 != null, var395 != null))
    val var396 = when {
      var394 != null -> {
        val var397 = getSequenceElems(history, 356, listOf(357, 4, 439, 4, 355), beginGen, endGen)
        val var398 = history[var397[2].second].findByBeginGenOpt(440, 1, var397[2].first)
        val var399 = history[var397[2].second].findByBeginGenOpt(444, 1, var397[2].first)
        check(hasSingleTrue(var398 != null, var399 != null))
        val var400 = when {
          var398 != null -> Op.EQ
          else -> Op.NE
        }
        val var401 = matchElvisExpr(var397[0].first, var397[0].second)
        val var402 = matchBoolEqExpr(var397[4].first, var397[4].second)
        val var403 = BinOp(var400, var401, var402, nextId(), beginGen, endGen)
        var403
      }

      else -> {
        val var404 = getSequenceElems(history, 448, listOf(357), beginGen, endGen)
        val var405 = matchElvisExpr(var404[0].first, var404[0].second)
        var405
      }
    }
    return var396
  }

  fun matchElvisExpr(beginGen: Int, endGen: Int): ElvisExpr {
    val var406 = history[endGen].findByBeginGenOpt(358, 5, beginGen)
    val var407 = history[endGen].findByBeginGenOpt(438, 1, beginGen)
    check(hasSingleTrue(var406 != null, var407 != null))
    val var408 = when {
      var406 != null -> {
        val var409 = getSequenceElems(history, 358, listOf(359, 4, 436, 4, 357), beginGen, endGen)
        val var410 = matchAdditiveExpr(var409[0].first, var409[0].second)
        val var411 = matchElvisExpr(var409[4].first, var409[4].second)
        val var412 = ElvisOp(var410, var411, nextId(), beginGen, endGen)
        var412
      }

      else -> {
        val var413 = getSequenceElems(history, 438, listOf(359), beginGen, endGen)
        val var414 = matchAdditiveExpr(var413[0].first, var413[0].second)
        var414
      }
    }
    return var408
  }

  fun matchAdditiveExpr(beginGen: Int, endGen: Int): AdditiveExpr {
    val var415 = history[endGen].findByBeginGenOpt(360, 5, beginGen)
    val var416 = history[endGen].findByBeginGenOpt(435, 1, beginGen)
    check(hasSingleTrue(var415 != null, var416 != null))
    val var417 = when {
      var415 != null -> {
        val var418 = getSequenceElems(history, 360, listOf(361, 4, 432, 4, 359), beginGen, endGen)
        val var419 = matchPrefixNotExpr(var418[0].first, var418[0].second)
        val var420 = matchAdditiveExpr(var418[4].first, var418[4].second)
        val var421 = BinOp(Op.ADD, var419, var420, nextId(), beginGen, endGen)
        var421
      }

      else -> {
        val var422 = getSequenceElems(history, 435, listOf(361), beginGen, endGen)
        val var423 = matchPrefixNotExpr(var422[0].first, var422[0].second)
        var423
      }
    }
    return var417
  }

  fun matchPrefixNotExpr(beginGen: Int, endGen: Int): PrefixNotExpr {
    val var424 = history[endGen].findByBeginGenOpt(362, 3, beginGen)
    val var425 = history[endGen].findByBeginGenOpt(363, 1, beginGen)
    check(hasSingleTrue(var424 != null, var425 != null))
    val var426 = when {
      var424 != null -> {
        val var427 = getSequenceElems(history, 362, listOf(234, 4, 361), beginGen, endGen)
        val var428 = matchPrefixNotExpr(var427[2].first, var427[2].second)
        val var429 = PrefixOp(PreOp.NOT, var428, nextId(), beginGen, endGen)
        var429
      }

      else -> {
        val var430 = getSequenceElems(history, 363, listOf(364), beginGen, endGen)
        val var431 = matchAtom(var430[0].first, var430[0].second)
        var431
      }
    }
    return var426
  }

  fun matchAtom(beginGen: Int, endGen: Int): Atom {
    val var432 = history[endGen].findByBeginGenOpt(310, 1, beginGen)
    val var433 = history[endGen].findByBeginGenOpt(365, 1, beginGen)
    val var434 = history[endGen].findByBeginGenOpt(369, 1, beginGen)
    val var435 = history[endGen].findByBeginGenOpt(384, 1, beginGen)
    val var436 = history[endGen].findByBeginGenOpt(399, 1, beginGen)
    val var437 = history[endGen].findByBeginGenOpt(402, 1, beginGen)
    val var438 = history[endGen].findByBeginGenOpt(416, 1, beginGen)
    val var439 = history[endGen].findByBeginGenOpt(431, 5, beginGen)
    check(
      hasSingleTrue(
        var432 != null,
        var433 != null,
        var434 != null,
        var435 != null,
        var436 != null,
        var437 != null,
        var438 != null,
        var439 != null
      )
    )
    val var440 = when {
      var432 != null -> {
        val var441 = getSequenceElems(history, 310, listOf(311), beginGen, endGen)
        val var442 = matchRef(var441[0].first, var441[0].second)
        var442
      }

      var433 != null -> {
        val var443 = getSequenceElems(history, 365, listOf(366), beginGen, endGen)
        val var444 = matchBindExpr(var443[0].first, var443[0].second)
        var444
      }

      var434 != null -> {
        val var445 = getSequenceElems(history, 369, listOf(370), beginGen, endGen)
        val var446 = matchNamedConstructExpr(var445[0].first, var445[0].second)
        var446
      }

      var435 != null -> {
        val var447 = getSequenceElems(history, 384, listOf(385), beginGen, endGen)
        val var448 = matchFuncCallOrConstructExpr(var447[0].first, var447[0].second)
        var448
      }

      var436 != null -> {
        val var449 = getSequenceElems(history, 399, listOf(400), beginGen, endGen)
        val var450 = matchArrayExpr(var449[0].first, var449[0].second)
        var450
      }

      var437 != null -> {
        val var451 = getSequenceElems(history, 402, listOf(403), beginGen, endGen)
        val var452 = matchLiteral(var451[0].first, var451[0].second)
        var452
      }

      var438 != null -> {
        val var453 = getSequenceElems(history, 416, listOf(417), beginGen, endGen)
        val var454 = matchEnumValue(var453[0].first, var453[0].second)
        var454
      }

      else -> {
        val var455 = getSequenceElems(history, 431, listOf(163, 4, 347, 4, 176), beginGen, endGen)
        val var456 = matchPExpr(var455[2].first, var455[2].second)
        val var457 = ExprParen(var456, nextId(), beginGen, endGen)
        var457
      }
    }
    return var440
  }

  fun matchNamedConstructExpr(beginGen: Int, endGen: Int): NamedConstructExpr {
    val var458 = getSequenceElems(history, 371, listOf(125, 181, 4, 372), beginGen, endGen)
    val var459 = matchTypeName(var458[0].first, var458[0].second)
    val var460 = matchNamedConstructParams(var458[3].first, var458[3].second)
    val var461 = history[var458[1].second].findByBeginGenOpt(56, 1, var458[1].first)
    val var462 = history[var458[1].second].findByBeginGenOpt(182, 1, var458[1].first)
    check(hasSingleTrue(var461 != null, var462 != null))
    val var463 = when {
      var461 != null -> null
      else -> {
        val var464 =
          getSequenceElems(history, 184, listOf(4, 146), var458[1].first, var458[1].second)
        val var465 = matchSuperTypes(var464[1].first, var464[1].second)
        var465
      }
    }
    val var466 = NamedConstructExpr(var459, var460, var463, nextId(), beginGen, endGen)
    return var466
  }

  fun matchNamedConstructParams(beginGen: Int, endGen: Int): List<NamedParam> {
    val var467 = getSequenceElems(history, 373, listOf(163, 4, 374, 176), beginGen, endGen)
    val var468 =
      getSequenceElems(history, 376, listOf(377, 379, 4), var467[2].first, var467[2].second)
    val var469 = matchNamedParam(var468[0].first, var468[0].second)
    val var470 =
      unrollRepeat0(history, 379, 381, 7, 380, var468[1].first, var468[1].second).map { k ->
        val var471 = getSequenceElems(history, 383, listOf(4, 158, 4, 377), k.first, k.second)
        val var472 = matchNamedParam(var471[3].first, var471[3].second)
        var472
      }
    return listOf(var469) + var470
  }

  fun matchArrayExpr(beginGen: Int, endGen: Int): ArrayExpr {
    val var474 = getSequenceElems(history, 401, listOf(127, 4, 390, 128), beginGen, endGen)
    val var475 = history[var474[2].second].findByBeginGenOpt(56, 1, var474[2].first)
    val var476 = history[var474[2].second].findByBeginGenOpt(391, 1, var474[2].first)
    check(hasSingleTrue(var475 != null, var476 != null))
    val var477 = when {
      var475 != null -> null
      else -> {
        val var478 =
          getSequenceElems(history, 393, listOf(347, 394, 4), var474[2].first, var474[2].second)
        val var479 = matchPExpr(var478[0].first, var478[0].second)
        val var480 =
          unrollRepeat0(history, 394, 396, 7, 395, var478[1].first, var478[1].second).map { k ->
            val var481 = getSequenceElems(history, 398, listOf(4, 158, 4, 347), k.first, k.second)
            val var482 = matchPExpr(var481[3].first, var481[3].second)
            var482
          }
        listOf(var479) + var480
      }
    }
    val var473 = var477
    val var483 = ArrayExpr((var473 ?: listOf()), nextId(), beginGen, endGen)
    return var483
  }

  fun matchLiteral(beginGen: Int, endGen: Int): Literal {
    val var484 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
    val var485 = history[endGen].findByBeginGenOpt(404, 1, beginGen)
    val var486 = history[endGen].findByBeginGenOpt(408, 3, beginGen)
    val var487 = history[endGen].findByBeginGenOpt(411, 3, beginGen)
    check(hasSingleTrue(var484 != null, var485 != null, var486 != null, var487 != null))
    val var488 = when {
      var484 != null -> {
        val var489 = NullLiteral(nextId(), beginGen, endGen)
        var489
      }

      var485 != null -> {
        val var490 = getSequenceElems(history, 404, listOf(405), beginGen, endGen)
        val var491 = history[var490[0].second].findByBeginGenOpt(406, 1, var490[0].first)
        val var492 = history[var490[0].second].findByBeginGenOpt(407, 1, var490[0].first)
        check(hasSingleTrue(var491 != null, var492 != null))
        val var493 = when {
          var491 != null -> true
          else -> false
        }
        val var494 = BoolLiteral(var493, nextId(), beginGen, endGen)
        var494
      }

      var486 != null -> {
        val var495 = getSequenceElems(history, 408, listOf(246, 409, 246), beginGen, endGen)
        val var496 = matchCharChar(var495[1].first, var495[1].second)
        val var497 = CharLiteral(var496, nextId(), beginGen, endGen)
        var497
      }

      else -> {
        val var498 = getSequenceElems(history, 411, listOf(280, 412, 280), beginGen, endGen)
        val var499 =
          unrollRepeat0(history, 412, 414, 7, 413, var498[1].first, var498[1].second).map { k ->
            val var500 = matchStrChar(k.first, k.second)
            var500
          }
        val var501 = StrLiteral(var499, nextId(), beginGen, endGen)
        var501
      }
    }
    return var488
  }

  fun matchCharChar(beginGen: Int, endGen: Int): TerminalChar {
    val var502 = getSequenceElems(history, 410, listOf(247), beginGen, endGen)
    val var503 = matchTerminalChar(var502[0].first, var502[0].second)
    return var503
  }

  fun matchFuncCallOrConstructExpr(beginGen: Int, endGen: Int): FuncCallOrConstructExpr {
    val var504 = getSequenceElems(history, 386, listOf(387, 4, 388), beginGen, endGen)
    val var505 = matchTypeOrFuncName(var504[0].first, var504[0].second)
    val var506 = matchCallParams(var504[2].first, var504[2].second)
    val var507 = FuncCallOrConstructExpr(var505, var506, nextId(), beginGen, endGen)
    return var507
  }

  fun matchBindExpr(beginGen: Int, endGen: Int): BindExpr {
    val var508 = getSequenceElems(history, 367, listOf(313, 368), beginGen, endGen)
    val var509 = matchValRef(var508[0].first, var508[0].second)
    val var510 = matchBinderExpr(var508[1].first, var508[1].second)
    val var511 = BindExpr(var509, var510, nextId(), beginGen, endGen)
    return var511
  }

  fun matchEnumValue(beginGen: Int, endGen: Int): AbstractEnumValue {
    val var512 = getSequenceElems(history, 418, listOf(419), beginGen, endGen)
    val var513 = history[var512[0].second].findByBeginGenOpt(421, 1, var512[0].first)
    val var514 = history[var512[0].second].findByBeginGenOpt(427, 1, var512[0].first)
    check(hasSingleTrue(var513 != null, var514 != null))
    val var515 = when {
      var513 != null -> {
        val var516 = getSequenceElems(history, 422, listOf(423), var512[0].first, var512[0].second)
        val var517 = matchCanonicalEnumValue(var516[0].first, var516[0].second)
        var517
      }

      else -> {
        val var518 = getSequenceElems(history, 428, listOf(429), var512[0].first, var512[0].second)
        val var519 = matchShortenedEnumValue(var518[0].first, var518[0].second)
        var519
      }
    }
    return var515
  }

  fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
    val var520 = getSequenceElems(history, 378, listOf(170, 116, 4, 215, 4, 347), beginGen, endGen)
    val var521 = matchParamName(var520[0].first, var520[0].second)
    val var522 = history[var520[1].second].findByBeginGenOpt(56, 1, var520[1].first)
    val var523 = history[var520[1].second].findByBeginGenOpt(117, 1, var520[1].first)
    check(hasSingleTrue(var522 != null, var523 != null))
    val var524 = when {
      var522 != null -> null
      else -> {
        val var525 =
          getSequenceElems(history, 119, listOf(4, 120, 4, 121), var520[1].first, var520[1].second)
        val var526 = matchTypeDesc(var525[3].first, var525[3].second)
        var526
      }
    }
    val var527 = matchPExpr(var520[5].first, var520[5].second)
    val var528 = NamedParam(var521, var524, var527, nextId(), beginGen, endGen)
    return var528
  }

  fun matchBinderExpr(beginGen: Int, endGen: Int): BinderExpr {
    val var529 = history[endGen].findByBeginGenOpt(310, 1, beginGen)
    val var530 = history[endGen].findByBeginGenOpt(344, 1, beginGen)
    val var531 = history[endGen].findByBeginGenOpt(365, 1, beginGen)
    check(hasSingleTrue(var529 != null, var530 != null, var531 != null))
    val var532 = when {
      var529 != null -> {
        val var533 = getSequenceElems(history, 310, listOf(311), beginGen, endGen)
        val var534 = matchRef(var533[0].first, var533[0].second)
        var534
      }

      var530 != null -> {
        val var535 = getSequenceElems(history, 344, listOf(345), beginGen, endGen)
        val var536 = matchPExprBlock(var535[0].first, var535[0].second)
        var536
      }

      else -> {
        val var537 = getSequenceElems(history, 365, listOf(366), beginGen, endGen)
        val var538 = matchBindExpr(var537[0].first, var537[0].second)
        var538
      }
    }
    return var532
  }

  fun matchShortenedEnumValue(beginGen: Int, endGen: Int): ShortenedEnumValue {
    val var539 = getSequenceElems(history, 430, listOf(140, 425), beginGen, endGen)
    val var540 = matchEnumValueName(var539[1].first, var539[1].second)
    val var541 = ShortenedEnumValue(var540, nextId(), beginGen, endGen)
    return var541
  }

  fun matchTypeOrFuncName(beginGen: Int, endGen: Int): TypeOrFuncName {
    val var542 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
    val var543 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
    check(hasSingleTrue(var542 != null, var543 != null))
    val var544 = when {
      var542 != null -> {
        val var545 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
        val var546 = matchIdNoKeyword(var545[0].first, var545[0].second)
        val var547 = TypeOrFuncName(var546, nextId(), beginGen, endGen)
        var547
      }

      else -> {
        val var548 = getSequenceElems(history, 114, listOf(115, 70, 115), beginGen, endGen)
        val var549 = matchId(var548[1].first, var548[1].second)
        val var550 = TypeOrFuncName(var549, nextId(), beginGen, endGen)
        var550
      }
    }
    return var544
  }

  fun matchCallParams(beginGen: Int, endGen: Int): List<PExpr> {
    val var552 = getSequenceElems(history, 389, listOf(163, 4, 390, 176), beginGen, endGen)
    val var553 = history[var552[2].second].findByBeginGenOpt(56, 1, var552[2].first)
    val var554 = history[var552[2].second].findByBeginGenOpt(391, 1, var552[2].first)
    check(hasSingleTrue(var553 != null, var554 != null))
    val var555 = when {
      var553 != null -> null
      else -> {
        val var556 =
          getSequenceElems(history, 393, listOf(347, 394, 4), var552[2].first, var552[2].second)
        val var557 = matchPExpr(var556[0].first, var556[0].second)
        val var558 =
          unrollRepeat0(history, 394, 396, 7, 395, var556[1].first, var556[1].second).map { k ->
            val var559 = getSequenceElems(history, 398, listOf(4, 158, 4, 347), k.first, k.second)
            val var560 = matchPExpr(var559[3].first, var559[3].second)
            var560
          }
        listOf(var557) + var558
      }
    }
    val var551 = var555
    return (var551 ?: listOf())
  }

  fun matchCanonicalEnumValue(beginGen: Int, endGen: Int): CanonicalEnumValue {
    val var561 = getSequenceElems(history, 424, listOf(138, 258, 425), beginGen, endGen)
    val var562 = matchEnumTypeName(var561[0].first, var561[0].second)
    val var563 = matchEnumValueName(var561[2].first, var561[2].second)
    val var564 = CanonicalEnumValue(var562, var563, nextId(), beginGen, endGen)
    return var564
  }

  fun matchEnumValueName(beginGen: Int, endGen: Int): EnumValueName {
    val var565 = getSequenceElems(history, 426, listOf(70), beginGen, endGen)
    val var566 = matchId(var565[0].first, var565[0].second)
    val var567 = EnumValueName(var566, nextId(), beginGen, endGen)
    return var567
  }

  fun matchStrChar(beginGen: Int, endGen: Int): StringChar {
    val var568 = getSequenceElems(history, 415, listOf(283), beginGen, endGen)
    val var569 = matchStringChar(var568[0].first, var568[0].second)
    return var569
  }

  fun matchNonterminalName(beginGen: Int, endGen: Int): NonterminalName {
    val var570 = history[endGen].findByBeginGenOpt(66, 1, beginGen)
    val var571 = history[endGen].findByBeginGenOpt(114, 3, beginGen)
    check(hasSingleTrue(var570 != null, var571 != null))
    val var572 = when {
      var570 != null -> {
        val var573 = getSequenceElems(history, 66, listOf(67), beginGen, endGen)
        val var574 = matchIdNoKeyword(var573[0].first, var573[0].second)
        val var575 = NonterminalName(var574, nextId(), beginGen, endGen)
        var575
      }

      else -> {
        val var576 = getSequenceElems(history, 114, listOf(115, 70, 115), beginGen, endGen)
        val var577 = matchId(var576[1].first, var576[1].second)
        val var578 = NonterminalName(var577, nextId(), beginGen, endGen)
        var578
      }
    }
    return var572
  }

  fun matchNonNullTypeDesc(beginGen: Int, endGen: Int): NonNullTypeDesc {
    val var579 = history[endGen].findByBeginGenOpt(124, 1, beginGen)
    val var580 = history[endGen].findByBeginGenOpt(126, 5, beginGen)
    val var581 = history[endGen].findByBeginGenOpt(129, 1, beginGen)
    val var582 = history[endGen].findByBeginGenOpt(131, 1, beginGen)
    val var583 = history[endGen].findByBeginGenOpt(137, 1, beginGen)
    val var584 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
    check(
      hasSingleTrue(
        var579 != null,
        var580 != null,
        var581 != null,
        var582 != null,
        var583 != null,
        var584 != null
      )
    )
    val var585 = when {
      var579 != null -> {
        val var586 = getSequenceElems(history, 124, listOf(125), beginGen, endGen)
        val var587 = matchTypeName(var586[0].first, var586[0].second)
        var587
      }

      var580 != null -> {
        val var588 = getSequenceElems(history, 126, listOf(127, 4, 121, 4, 128), beginGen, endGen)
        val var589 = matchTypeDesc(var588[2].first, var588[2].second)
        val var590 = ArrayTypeDesc(var589, nextId(), beginGen, endGen)
        var590
      }

      var581 != null -> {
        val var591 = getSequenceElems(history, 129, listOf(130), beginGen, endGen)
        val var592 = matchValueType(var591[0].first, var591[0].second)
        var592
      }

      var582 != null -> {
        val var593 = getSequenceElems(history, 131, listOf(132), beginGen, endGen)
        val var594 = matchAnyType(var593[0].first, var593[0].second)
        var594
      }

      var583 != null -> {
        val var595 = getSequenceElems(history, 137, listOf(138), beginGen, endGen)
        val var596 = matchEnumTypeName(var595[0].first, var595[0].second)
        var596
      }

      else -> {
        val var597 = getSequenceElems(history, 141, listOf(142), beginGen, endGen)
        val var598 = matchTypeDef(var597[0].first, var597[0].second)
        var598
      }
    }
    return var585
  }

  fun matchAnyType(beginGen: Int, endGen: Int): AnyType {
    val var599 = AnyType(nextId(), beginGen, endGen)
    return var599
  }

  fun matchValueType(beginGen: Int, endGen: Int): ValueType {
    val var600 = history[endGen].findByBeginGenOpt(81, 1, beginGen)
    val var601 = history[endGen].findByBeginGenOpt(90, 1, beginGen)
    val var602 = history[endGen].findByBeginGenOpt(96, 1, beginGen)
    check(hasSingleTrue(var600 != null, var601 != null, var602 != null))
    val var603 = when {
      var600 != null -> {
        val var604 = BooleanType(nextId(), beginGen, endGen)
        var604
      }

      var601 != null -> {
        val var605 = CharType(nextId(), beginGen, endGen)
        var605
      }

      else -> {
        val var606 = StringType(nextId(), beginGen, endGen)
        var606
      }
    }
    return var603
  }

}
