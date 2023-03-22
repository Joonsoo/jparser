package com.giyeok.jparser.metalang3.ast

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
import MetaLang3Ast._

object MetaLang3Ast {

  sealed trait WithIdAndParseNode { val id: Int; val parseNode: Node }
  case class AbstractClassDef(name: TypeName, supers: List[TypeName])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode
  sealed trait AbstractEnumValue extends Atom with WithIdAndParseNode
  sealed trait AdditiveExpr extends ElvisExpr with WithIdAndParseNode
  case class AnyTerminal()(override val id: Int, override val parseNode: Node) extends Terminal with WithIdAndParseNode
  case class AnyType()(override val id: Int, override val parseNode: Node) extends NonNullTypeDesc with WithIdAndParseNode
  case class ArrayExpr(elems: List[PExpr])(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class ArrayTypeDesc(elemType: TypeDesc)(override val id: Int, override val parseNode: Node) extends NonNullTypeDesc with WithIdAndParseNode
  sealed trait Atom extends PrefixNotExpr with WithIdAndParseNode
  sealed trait AtomSymbol extends PostUnSymbol with WithIdAndParseNode
  case class BinOp(op: Op.Value, lhs: BoolAndExpr, rhs: BoolOrExpr)(override val id: Int, override val parseNode: Node) extends AdditiveExpr with WithIdAndParseNode
  sealed trait BinSymbol extends Symbol with WithIdAndParseNode
  case class BindExpr(ctx: ValRef, binder: BinderExpr)(override val id: Int, override val parseNode: Node) extends Atom with BinderExpr with WithIdAndParseNode
  sealed trait BinderExpr extends WithIdAndParseNode
  sealed trait BoolAndExpr extends BoolOrExpr with WithIdAndParseNode
  sealed trait BoolEqExpr extends BoolAndExpr with WithIdAndParseNode
  case class BoolLiteral(value: Boolean)(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode
  sealed trait BoolOrExpr extends TernaryExpr with WithIdAndParseNode
  case class BooleanType()(override val id: Int, override val parseNode: Node) extends ValueType with WithIdAndParseNode
  case class CanonicalEnumValue(enumName: EnumTypeName, valueName: EnumValueName)(override val id: Int, override val parseNode: Node) extends AbstractEnumValue with WithIdAndParseNode
  case class CharAsIs(value: Char)(override val id: Int, override val parseNode: Node) extends StringChar with TerminalChar with TerminalChoiceChar with WithIdAndParseNode
  case class CharEscaped(escapeCode: Char)(override val id: Int, override val parseNode: Node) extends StringChar with TerminalChar with TerminalChoiceChar with WithIdAndParseNode
  case class CharLiteral(value: TerminalChar)(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode
  case class CharType()(override val id: Int, override val parseNode: Node) extends ValueType with WithIdAndParseNode
  case class CharUnicode(code: List[Char])(override val id: Int, override val parseNode: Node) extends StringChar with TerminalChar with TerminalChoiceChar with WithIdAndParseNode
  sealed trait ClassDef extends SubType with TypeDef with WithIdAndParseNode
  case class ClassParamDef(name: ParamName, typeDesc: Option[TypeDesc])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class ConcreteClassDef(name: TypeName, supers: Option[List[TypeName]], params: List[ClassParamDef])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode
  sealed trait Def extends WithIdAndParseNode
  sealed trait Elem extends WithIdAndParseNode
  sealed trait ElvisExpr extends BoolEqExpr with WithIdAndParseNode
  case class ElvisOp(value: AdditiveExpr, ifNull: ElvisExpr)(override val id: Int, override val parseNode: Node) extends ElvisExpr with WithIdAndParseNode
  case class EmptySeq()(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class EnumTypeDef(name: EnumTypeName, values: List[String])(override val id: Int, override val parseNode: Node) extends TypeDef with WithIdAndParseNode
  case class EnumTypeName(name: String)(override val id: Int, override val parseNode: Node) extends NonNullTypeDesc with WithIdAndParseNode
  case class EnumValueName(name: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class ExceptSymbol(body: BinSymbol, except: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends BinSymbol with WithIdAndParseNode
  case class ExprParen(body: PExpr)(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class FollowedBy(followedBy: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends PreUnSymbol with WithIdAndParseNode
  case class FuncCallOrConstructExpr(funcName: TypeOrFuncName, params: List[PExpr])(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class Grammar(defs: List[Def])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class InPlaceChoices(choices: List[Sequence])(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class JoinSymbol(body: BinSymbol, join: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends BinSymbol with WithIdAndParseNode
  case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  sealed trait Literal extends Atom with WithIdAndParseNode
  case class Longest(choices: InPlaceChoices)(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class NamedConstructExpr(typeName: TypeName, params: List[NamedParam], supers: Option[List[TypeName]])(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class NamedParam(name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  sealed trait NonNullTypeDesc extends WithIdAndParseNode
  case class Nonterminal(name: NonterminalName)(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class NonterminalName(name: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class NotFollowedBy(notFollowedBy: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends PreUnSymbol with WithIdAndParseNode
  case class NullLiteral()(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode
  case class Optional(body: PostUnSymbol)(override val id: Int, override val parseNode: Node) extends PostUnSymbol with WithIdAndParseNode
  sealed trait PExpr extends WithIdAndParseNode
  case class ParamName(name: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  sealed trait PostUnSymbol extends PreUnSymbol with WithIdAndParseNode
  sealed trait PreUnSymbol extends BinSymbol with WithIdAndParseNode
  sealed trait PrefixNotExpr extends AdditiveExpr with WithIdAndParseNode
  case class PrefixOp(op: PreOp.Value, expr: PrefixNotExpr)(override val id: Int, override val parseNode: Node) extends PrefixNotExpr with WithIdAndParseNode
  sealed trait Processor extends Elem with WithIdAndParseNode
  case class ProcessorBlock(body: PExpr)(override val id: Int, override val parseNode: Node) extends BinderExpr with Processor with WithIdAndParseNode
  case class RawRef(idx: String, condSymPath: Option[List[CondSymDir.Value]])(override val id: Int, override val parseNode: Node) extends Ref with WithIdAndParseNode
  sealed trait Ref extends Atom with BinderExpr with Processor with WithIdAndParseNode
  case class RepeatFromOne(body: PostUnSymbol)(override val id: Int, override val parseNode: Node) extends PostUnSymbol with WithIdAndParseNode
  case class RepeatFromZero(body: PostUnSymbol)(override val id: Int, override val parseNode: Node) extends PostUnSymbol with WithIdAndParseNode
  case class Rule(lhs: LHS, rhs: List[Sequence])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode
  case class Sequence(seq: List[Elem])(override val id: Int, override val parseNode: Node) extends Symbol with WithIdAndParseNode
  case class ShortenedEnumValue(valueName: EnumValueName)(override val id: Int, override val parseNode: Node) extends AbstractEnumValue with WithIdAndParseNode
  case class StrLiteral(value: List[StringChar])(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode
  sealed trait StringChar extends WithIdAndParseNode
  case class StringSymbol(value: List[StringChar])(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class StringType()(override val id: Int, override val parseNode: Node) extends ValueType with WithIdAndParseNode
  sealed trait SubType extends WithIdAndParseNode
  case class SuperDef(typeName: TypeName, subs: Option[List[SubType]], supers: Option[List[TypeName]])(override val id: Int, override val parseNode: Node) extends SubType with TypeDef with WithIdAndParseNode
  sealed trait Symbol extends Elem with WithIdAndParseNode
  sealed trait Terminal extends AtomSymbol with WithIdAndParseNode
  sealed trait TerminalChar extends Terminal with WithIdAndParseNode
  case class TerminalChoice(choices: List[TerminalChoiceElem])(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  sealed trait TerminalChoiceChar extends TerminalChoiceElem with WithIdAndParseNode
  sealed trait TerminalChoiceElem extends WithIdAndParseNode
  case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar)(override val id: Int, override val parseNode: Node) extends TerminalChoiceElem with WithIdAndParseNode
  case class TerminalUnicodeCategory(categoryName: String)(override val id: Int, override val parseNode: Node) extends TerminalChoiceElem with WithIdAndParseNode
  sealed trait TernaryExpr extends PExpr with WithIdAndParseNode
  case class TernaryOp(cond: BoolOrExpr, ifTrue: TernaryExpr, ifFalse: TernaryExpr)(override val id: Int, override val parseNode: Node) extends TernaryExpr with WithIdAndParseNode
  sealed trait TypeDef extends Def with NonNullTypeDesc with WithIdAndParseNode
  case class TypeDesc(typ: NonNullTypeDesc, optional: Boolean)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class TypeName(name: String)(override val id: Int, override val parseNode: Node) extends NonNullTypeDesc with SubType with WithIdAndParseNode
  case class TypeOrFuncName(name: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class TypedPExpr(body: TernaryExpr, typ: TypeDesc)(override val id: Int, override val parseNode: Node) extends PExpr with WithIdAndParseNode
  case class ValRef(idx: String, condSymPath: Option[List[CondSymDir.Value]])(override val id: Int, override val parseNode: Node) extends Ref with WithIdAndParseNode
  sealed trait ValueType extends NonNullTypeDesc with WithIdAndParseNode
  object CondSymDir extends Enumeration { val BODY, COND = Value }
  object KeyWord extends Enumeration { val BOOLEAN, CHAR, FALSE, NULL, STRING, TRUE = Value }
  object Op extends Enumeration { val ADD, AND, EQ, NE, OR = Value }
  object PreOp extends Enumeration { val NOT = Value }



}

class MetaLang3Ast {
  private var idCounter = 0

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  def matchStart(node: Node): Grammar = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchGrammar(body)
  }
  def matchAdditiveExpr(node: Node): AdditiveExpr = {
    val BindNode(v1, v2) = node
    val v20 = v1.id match {
      case 366 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 438)
        val BindNode(v6, v7) = v5
        val v10 = v6.id match {
        case 439 =>
          val BindNode(v8, v9) = v7
          assert(v8.id == 440)
          Op.ADD
      }
        val v11 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v12, v13) = v11
        assert(v12.id == 367)
        val v14 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v15, v16) = v14
        assert(v15.id == 365)
        BinOp(v10, matchPrefixNotExpr(v13), matchAdditiveExpr(v16))(nextId(), v2)
      case 441 =>
        val v17 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v18, v19) = v17
        assert(v18.id == 367)
        matchPrefixNotExpr(v19)
    }
    v20
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v21, v22) = node
    val v23 = v21.id match {
      case 133 =>
      AnyType()(nextId(), v22)
    }
    v23
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v24, v25) = node
    val v52 = v24.id match {
      case 407 =>
        val v27 = v25.asInstanceOf[SequenceNode].children(2)
        val BindNode(v28, v29) = v27
        assert(v28.id == 396)
        val BindNode(v30, v31) = v29
        val v51 = v30.id match {
        case 56 =>
        None
        case 397 =>
          val BindNode(v32, v33) = v31
          assert(v32.id == 398)
          val BindNode(v34, v35) = v33
          assert(v34.id == 399)
          val v36 = v35.asInstanceOf[SequenceNode].children.head
          val BindNode(v37, v38) = v36
          assert(v37.id == 353)
          val v39 = v35.asInstanceOf[SequenceNode].children(1)
          val v40 = unrollRepeat0(v39).map { elem =>
          val BindNode(v41, v42) = elem
          assert(v41.id == 402)
          val BindNode(v43, v44) = v42
          val v50 = v43.id match {
          case 403 =>
            val BindNode(v45, v46) = v44
            assert(v45.id == 404)
            val v47 = v46.asInstanceOf[SequenceNode].children(3)
            val BindNode(v48, v49) = v47
            assert(v48.id == 353)
            matchPExpr(v49)
        }
          v50
          }
          Some(List(matchPExpr(v38)) ++ v40)
      }
        val v26 = v51
        ArrayExpr(if (v26.isDefined) v26.get else List())(nextId(), v25)
    }
    v52
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v53, v54) = node
    val v79 = v53.id match {
      case 316 =>
        val v55 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v56, v57) = v55
        assert(v56.id == 317)
        matchRef(v57)
      case 371 =>
        val v58 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v59, v60) = v58
        assert(v59.id == 372)
        matchBindExpr(v60)
      case 375 =>
        val v61 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v62, v63) = v61
        assert(v62.id == 376)
        matchNamedConstructExpr(v63)
      case 390 =>
        val v64 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v65, v66) = v64
        assert(v65.id == 391)
        matchFuncCallOrConstructExpr(v66)
      case 405 =>
        val v67 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v68, v69) = v67
        assert(v68.id == 406)
        matchArrayExpr(v69)
      case 408 =>
        val v70 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v71, v72) = v70
        assert(v71.id == 409)
        matchLiteral(v72)
      case 422 =>
        val v73 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v74, v75) = v73
        assert(v74.id == 423)
        matchEnumValue(v75)
      case 437 =>
        val v76 = v54.asInstanceOf[SequenceNode].children(2)
        val BindNode(v77, v78) = v76
        assert(v77.id == 353)
        ExprParen(matchPExpr(v78))(nextId(), v54)
    }
    v79
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v80, v81) = node
    val v103 = v80.id match {
      case 243 =>
        val v82 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v83, v84) = v82
        assert(v83.id == 244)
        matchTerminal(v84)
      case 259 =>
        val v85 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v86, v87) = v85
        assert(v86.id == 260)
        matchTerminalChoice(v87)
      case 283 =>
        val v88 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v89, v90) = v88
        assert(v89.id == 284)
        matchStringSymbol(v90)
      case 295 =>
        val v91 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v92, v93) = v91
        assert(v92.id == 63)
        matchNonterminal(v93)
      case 296 =>
        val v94 = v81.asInstanceOf[SequenceNode].children(2)
        val BindNode(v95, v96) = v94
        assert(v95.id == 297)
        matchInPlaceChoices(v96)
      case 305 =>
        val v97 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v98, v99) = v97
        assert(v98.id == 306)
        matchLongest(v99)
      case 308 =>
        val v100 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v101, v102) = v100
        assert(v101.id == 309)
        matchEmptySequence(v102)
    }
    v103
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v104, v105) = node
    val v121 = v104.id match {
      case 228 =>
        val v106 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v107, v108) = v106
        assert(v107.id == 227)
        val v109 = v105.asInstanceOf[SequenceNode].children(4)
        val BindNode(v110, v111) = v109
        assert(v110.id == 230)
        JoinSymbol(matchBinSymbol(v108), matchPreUnSymbol(v111))(nextId(), v105)
      case 312 =>
        val v112 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v113, v114) = v112
        assert(v113.id == 227)
        val v115 = v105.asInstanceOf[SequenceNode].children(4)
        val BindNode(v116, v117) = v115
        assert(v116.id == 230)
        ExceptSymbol(matchBinSymbol(v114), matchPreUnSymbol(v117))(nextId(), v105)
      case 313 =>
        val v118 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v119, v120) = v118
        assert(v119.id == 230)
        matchPreUnSymbol(v120)
    }
    v121
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v122, v123) = node
    val v130 = v122.id match {
      case 373 =>
        val v124 = v123.asInstanceOf[SequenceNode].children.head
        val BindNode(v125, v126) = v124
        assert(v125.id == 319)
        val v127 = v123.asInstanceOf[SequenceNode].children(1)
        val BindNode(v128, v129) = v127
        assert(v128.id == 374)
        BindExpr(matchValRef(v126), matchBinderExpr(v129))(nextId(), v123)
    }
    v130
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v131, v132) = node
    val v142 = v131.id match {
      case 316 =>
        val v133 = v132.asInstanceOf[SequenceNode].children.head
        val BindNode(v134, v135) = v133
        assert(v134.id == 317)
        matchRef(v135)
      case 350 =>
        val v136 = v132.asInstanceOf[SequenceNode].children.head
        val BindNode(v137, v138) = v136
        assert(v137.id == 351)
        matchPExprBlock(v138)
      case 371 =>
        val v139 = v132.asInstanceOf[SequenceNode].children.head
        val BindNode(v140, v141) = v139
        assert(v140.id == 372)
        matchBindExpr(v141)
    }
    v142
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v143, v144) = node
    val v154 = v143.id match {
      case 360 =>
        val v145 = v144.asInstanceOf[SequenceNode].children.head
        val BindNode(v146, v147) = v145
        assert(v146.id == 361)
        val v148 = v144.asInstanceOf[SequenceNode].children(4)
        val BindNode(v149, v150) = v148
        assert(v149.id == 359)
        BinOp(Op.OR, matchBoolEqExpr(v147), matchBoolAndExpr(v150))(nextId(), v144)
      case 457 =>
        val v151 = v144.asInstanceOf[SequenceNode].children.head
        val BindNode(v152, v153) = v151
        assert(v152.id == 361)
        matchBoolEqExpr(v153)
    }
    v154
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v155, v156) = node
    val v176 = v155.id match {
      case 362 =>
        val v157 = v156.asInstanceOf[SequenceNode].children(2)
        val BindNode(v158, v159) = v157
        assert(v158.id == 445)
        val BindNode(v160, v161) = v159
        val v166 = v160.id match {
        case 446 =>
          val BindNode(v162, v163) = v161
          assert(v162.id == 447)
          Op.EQ
        case 450 =>
          val BindNode(v164, v165) = v161
          assert(v164.id == 451)
          Op.NE
      }
        val v167 = v156.asInstanceOf[SequenceNode].children.head
        val BindNode(v168, v169) = v167
        assert(v168.id == 363)
        val v170 = v156.asInstanceOf[SequenceNode].children(4)
        val BindNode(v171, v172) = v170
        assert(v171.id == 361)
        BinOp(v166, matchElvisExpr(v169), matchBoolEqExpr(v172))(nextId(), v156)
      case 454 =>
        val v173 = v156.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 363)
        matchElvisExpr(v175)
    }
    v176
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v177, v178) = node
    val v188 = v177.id match {
      case 358 =>
        val v179 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v180, v181) = v179
        assert(v180.id == 359)
        val v182 = v178.asInstanceOf[SequenceNode].children(4)
        val BindNode(v183, v184) = v182
        assert(v183.id == 357)
        BinOp(Op.AND, matchBoolAndExpr(v181), matchBoolOrExpr(v184))(nextId(), v178)
      case 460 =>
        val v185 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v186, v187) = v185
        assert(v186.id == 359)
        matchBoolAndExpr(v187)
    }
    v188
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v189, v190) = node
    val v218 = v189.id match {
      case 395 =>
        val v192 = v190.asInstanceOf[SequenceNode].children(2)
        val BindNode(v193, v194) = v192
        assert(v193.id == 396)
        val BindNode(v195, v196) = v194
        val v217 = v195.id match {
        case 56 =>
        None
        case 397 =>
          val BindNode(v197, v198) = v196
          val v216 = v197.id match {
          case 398 =>
            val BindNode(v199, v200) = v198
            assert(v199.id == 399)
            val v201 = v200.asInstanceOf[SequenceNode].children.head
            val BindNode(v202, v203) = v201
            assert(v202.id == 353)
            val v204 = v200.asInstanceOf[SequenceNode].children(1)
            val v205 = unrollRepeat0(v204).map { elem =>
            val BindNode(v206, v207) = elem
            assert(v206.id == 402)
            val BindNode(v208, v209) = v207
            val v215 = v208.id match {
            case 403 =>
              val BindNode(v210, v211) = v209
              assert(v210.id == 404)
              val v212 = v211.asInstanceOf[SequenceNode].children(3)
              val BindNode(v213, v214) = v212
              assert(v213.id == 353)
              matchPExpr(v214)
          }
            v215
            }
            List(matchPExpr(v203)) ++ v205
        }
          Some(v216)
      }
        val v191 = v217
        if (v191.isDefined) v191.get else List()
    }
    v218
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v219, v220) = node
    val v227 = v219.id match {
      case 430 =>
        val v221 = v220.asInstanceOf[SequenceNode].children.head
        val BindNode(v222, v223) = v221
        assert(v222.id == 138)
        val v224 = v220.asInstanceOf[SequenceNode].children(2)
        val BindNode(v225, v226) = v224
        assert(v225.id == 431)
        CanonicalEnumValue(matchEnumTypeName(v223), matchEnumValueName(v226))(nextId(), v220)
    }
    v227
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v228, v229) = node
    val v233 = v228.id match {
      case 416 =>
        val v230 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 247)
        matchTerminalChar(v232)
    }
    v233
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v234, v235) = node
    val v257 = v234.id match {
      case 145 =>
        val v236 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v237, v238) = v236
        assert(v237.id == 125)
        val v239 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v240, v241) = v239
        assert(v240.id == 146)
        AbstractClassDef(matchTypeName(v238), matchSuperTypes(v241))(nextId(), v235)
      case 160 =>
        val v242 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v243, v244) = v242
        assert(v243.id == 125)
        val v245 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v246, v247) = v245
        assert(v246.id == 161)
        ConcreteClassDef(matchTypeName(v244), None, matchClassParamsDef(v247))(nextId(), v235)
      case 177 =>
        val v248 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v249, v250) = v248
        assert(v249.id == 125)
        val v251 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v252, v253) = v251
        assert(v252.id == 146)
        val v254 = v235.asInstanceOf[SequenceNode].children(4)
        val BindNode(v255, v256) = v254
        assert(v255.id == 161)
        ConcreteClassDef(matchTypeName(v250), Some(matchSuperTypes(v253)), matchClassParamsDef(v256))(nextId(), v235)
    }
    v257
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v258, v259) = node
    val v277 = v258.id match {
      case 169 =>
        val v260 = v259.asInstanceOf[SequenceNode].children.head
        val BindNode(v261, v262) = v260
        assert(v261.id == 170)
        val v263 = v259.asInstanceOf[SequenceNode].children(1)
        val BindNode(v264, v265) = v263
        assert(v264.id == 116)
        val BindNode(v266, v267) = v265
        val v276 = v266.id match {
        case 56 =>
        None
        case 117 =>
          val BindNode(v268, v269) = v267
          val v275 = v268.id match {
          case 118 =>
            val BindNode(v270, v271) = v269
            assert(v270.id == 119)
            val v272 = v271.asInstanceOf[SequenceNode].children(3)
            val BindNode(v273, v274) = v272
            assert(v273.id == 121)
            matchTypeDesc(v274)
        }
          Some(v275)
      }
        ClassParamDef(matchParamName(v262), v276)(nextId(), v259)
    }
    v277
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v278, v279) = node
    val v307 = v278.id match {
      case 162 =>
        val v281 = v279.asInstanceOf[SequenceNode].children(2)
        val BindNode(v282, v283) = v281
        assert(v282.id == 164)
        val BindNode(v284, v285) = v283
        val v306 = v284.id match {
        case 56 =>
        None
        case 165 =>
          val BindNode(v286, v287) = v285
          val v305 = v286.id match {
          case 166 =>
            val BindNode(v288, v289) = v287
            assert(v288.id == 167)
            val v290 = v289.asInstanceOf[SequenceNode].children.head
            val BindNode(v291, v292) = v290
            assert(v291.id == 168)
            val v293 = v289.asInstanceOf[SequenceNode].children(1)
            val v294 = unrollRepeat0(v293).map { elem =>
            val BindNode(v295, v296) = elem
            assert(v295.id == 173)
            val BindNode(v297, v298) = v296
            val v304 = v297.id match {
            case 174 =>
              val BindNode(v299, v300) = v298
              assert(v299.id == 175)
              val v301 = v300.asInstanceOf[SequenceNode].children(3)
              val BindNode(v302, v303) = v301
              assert(v302.id == 168)
              matchClassParamDef(v303)
          }
            v304
            }
            List(matchClassParamDef(v292)) ++ v294
        }
          Some(v305)
      }
        val v280 = v306
        if (v280.isDefined) v280.get else List()
    }
    v307
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v308, v309) = node
    val v321 = v308.id match {
      case 324 =>
        val v310 = v309.asInstanceOf[SequenceNode].children.head
        val v311 = unrollRepeat1(v310).map { elem =>
        val BindNode(v312, v313) = elem
        assert(v312.id == 326)
        val BindNode(v314, v315) = v313
        val v320 = v314.id match {
        case 327 =>
          val BindNode(v316, v317) = v315
          assert(v316.id == 328)
          CondSymDir.BODY
        case 329 =>
          val BindNode(v318, v319) = v315
          assert(v318.id == 330)
          CondSymDir.COND
      }
        v320
        }
        v311
    }
    v321
  }

  def matchDef(node: Node): Def = {
    val BindNode(v322, v323) = node
    val v330 = v322.id match {
      case 58 =>
        val v324 = v323.asInstanceOf[SequenceNode].children.head
        val BindNode(v325, v326) = v324
        assert(v325.id == 59)
        matchRule(v326)
      case 141 =>
        val v327 = v323.asInstanceOf[SequenceNode].children.head
        val BindNode(v328, v329) = v327
        assert(v328.id == 142)
        matchTypeDef(v329)
    }
    v330
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v331, v332) = node
    val v339 = v331.id match {
      case 224 =>
        val v333 = v332.asInstanceOf[SequenceNode].children.head
        val BindNode(v334, v335) = v333
        assert(v334.id == 225)
        matchSymbol(v335)
      case 314 =>
        val v336 = v332.asInstanceOf[SequenceNode].children.head
        val BindNode(v337, v338) = v336
        assert(v337.id == 315)
        matchProcessor(v338)
    }
    v339
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v340, v341) = node
    val v351 = v340.id match {
      case 364 =>
        val v342 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v343, v344) = v342
        assert(v343.id == 365)
        val v345 = v341.asInstanceOf[SequenceNode].children(4)
        val BindNode(v346, v347) = v345
        assert(v346.id == 363)
        ElvisOp(matchAdditiveExpr(v344), matchElvisExpr(v347))(nextId(), v341)
      case 444 =>
        val v348 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v349, v350) = v348
        assert(v349.id == 365)
        matchAdditiveExpr(v350)
    }
    v351
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v352, v353) = node
    val v354 = v352.id match {
      case 310 =>
      EmptySeq()(nextId(), v353)
    }
    v354
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v355, v356) = node
    val v383 = v355.id match {
      case 201 =>
        val v357 = v356.asInstanceOf[SequenceNode].children.head
        val BindNode(v358, v359) = v357
        assert(v358.id == 138)
        val v360 = v356.asInstanceOf[SequenceNode].children(4)
        val BindNode(v361, v362) = v360
        assert(v361.id == 202)
        val BindNode(v363, v364) = v362
        val v382 = v363.id match {
        case 203 =>
          val BindNode(v365, v366) = v364
          assert(v365.id == 204)
          val v367 = v366.asInstanceOf[SequenceNode].children.head
          val BindNode(v368, v369) = v367
          assert(v368.id == 70)
          val v370 = v366.asInstanceOf[SequenceNode].children(1)
          val v371 = unrollRepeat0(v370).map { elem =>
          val BindNode(v372, v373) = elem
          assert(v372.id == 207)
          val BindNode(v374, v375) = v373
          val v381 = v374.id match {
          case 208 =>
            val BindNode(v376, v377) = v375
            assert(v376.id == 209)
            val v378 = v377.asInstanceOf[SequenceNode].children(3)
            val BindNode(v379, v380) = v378
            assert(v379.id == 70)
            matchId(v380)
        }
          v381
          }
          List(matchId(v369)) ++ v371
      }
        EnumTypeDef(matchEnumTypeName(v359), v382)(nextId(), v356)
    }
    v383
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v384, v385) = node
    val v389 = v384.id match {
      case 139 =>
        val v386 = v385.asInstanceOf[SequenceNode].children(1)
        val BindNode(v387, v388) = v386
        assert(v387.id == 70)
        EnumTypeName(matchId(v388))(nextId(), v385)
    }
    v389
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v390, v391) = node
    val v410 = v390.id match {
      case 424 =>
        val v392 = v391.asInstanceOf[SequenceNode].children.head
        val BindNode(v393, v394) = v392
        assert(v393.id == 425)
        val BindNode(v395, v396) = v394
        assert(v395.id == 426)
        val BindNode(v397, v398) = v396
        val v409 = v397.id match {
        case 427 =>
          val BindNode(v399, v400) = v398
          assert(v399.id == 428)
          val v401 = v400.asInstanceOf[SequenceNode].children.head
          val BindNode(v402, v403) = v401
          assert(v402.id == 429)
          matchCanonicalEnumValue(v403)
        case 433 =>
          val BindNode(v404, v405) = v398
          assert(v404.id == 434)
          val v406 = v405.asInstanceOf[SequenceNode].children.head
          val BindNode(v407, v408) = v406
          assert(v407.id == 435)
          matchShortenedEnumValue(v408)
      }
        v409
    }
    v410
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v411, v412) = node
    val v416 = v411.id match {
      case 432 =>
        val v413 = v412.asInstanceOf[SequenceNode].children.head
        val BindNode(v414, v415) = v413
        assert(v414.id == 70)
        EnumValueName(matchId(v415))(nextId(), v412)
    }
    v416
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v417, v418) = node
    val v425 = v417.id match {
      case 392 =>
        val v419 = v418.asInstanceOf[SequenceNode].children.head
        val BindNode(v420, v421) = v419
        assert(v420.id == 393)
        val v422 = v418.asInstanceOf[SequenceNode].children(2)
        val BindNode(v423, v424) = v422
        assert(v423.id == 394)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v421), matchCallParams(v424))(nextId(), v418)
    }
    v425
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v426, v427) = node
    val v443 = v426.id match {
      case 3 =>
        val v428 = v427.asInstanceOf[SequenceNode].children(1)
        val BindNode(v429, v430) = v428
        assert(v429.id == 57)
        val v431 = v427.asInstanceOf[SequenceNode].children(2)
        val v432 = unrollRepeat0(v431).map { elem =>
        val BindNode(v433, v434) = elem
        assert(v433.id == 478)
        val BindNode(v435, v436) = v434
        val v442 = v435.id match {
        case 479 =>
          val BindNode(v437, v438) = v436
          assert(v437.id == 480)
          val v439 = v438.asInstanceOf[SequenceNode].children(1)
          val BindNode(v440, v441) = v439
          assert(v440.id == 57)
          matchDef(v441)
      }
        v442
        }
        Grammar(List(matchDef(v430)) ++ v432)(nextId(), v427)
    }
    v443
  }

  def matchId(node: Node): String = {
    val BindNode(v444, v445) = node
    val v463 = v444.id match {
      case 71 =>
        val v446 = v445.asInstanceOf[SequenceNode].children.head
        val BindNode(v447, v448) = v446
        assert(v447.id == 72)
        val BindNode(v449, v450) = v448
        assert(v449.id == 73)
        val BindNode(v451, v452) = v450
        val v462 = v451.id match {
        case 74 =>
          val BindNode(v453, v454) = v452
          assert(v453.id == 75)
          val v455 = v454.asInstanceOf[SequenceNode].children.head
          val BindNode(v456, v457) = v455
          assert(v456.id == 76)
          val v458 = v454.asInstanceOf[SequenceNode].children(1)
          val v459 = unrollRepeat0(v458).map { elem =>
          val BindNode(v460, v461) = elem
          assert(v460.id == 79)
          v461.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v457.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v459.map(x => x.toString).mkString("")
      }
        v462
    }
    v463
  }

  def matchIdNoKeyword(node: Node): String = {
    val BindNode(v464, v465) = node
    val v471 = v464.id match {
      case 68 =>
        val v466 = v465.asInstanceOf[SequenceNode].children.head
        val BindNode(v467, v468) = v466
        assert(v467.id == 69)
        val BindNode(v469, v470) = v468
        assert(v469.id == 70)
        matchId(v470)
    }
    v471
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v472, v473) = node
    val v489 = v472.id match {
      case 298 =>
        val v474 = v473.asInstanceOf[SequenceNode].children.head
        val BindNode(v475, v476) = v474
        assert(v475.id == 221)
        val v477 = v473.asInstanceOf[SequenceNode].children(1)
        val v478 = unrollRepeat0(v477).map { elem =>
        val BindNode(v479, v480) = elem
        assert(v479.id == 301)
        val BindNode(v481, v482) = v480
        val v488 = v481.id match {
        case 302 =>
          val BindNode(v483, v484) = v482
          assert(v483.id == 303)
          val v485 = v484.asInstanceOf[SequenceNode].children(3)
          val BindNode(v486, v487) = v485
          assert(v486.id == 221)
          matchSequence(v487)
      }
        v488
        }
        InPlaceChoices(List(matchSequence(v476)) ++ v478)(nextId(), v473)
    }
    v489
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v490, v491) = node
    val v509 = v490.id match {
      case 62 =>
        val v492 = v491.asInstanceOf[SequenceNode].children.head
        val BindNode(v493, v494) = v492
        assert(v493.id == 63)
        val v495 = v491.asInstanceOf[SequenceNode].children(1)
        val BindNode(v496, v497) = v495
        assert(v496.id == 116)
        val BindNode(v498, v499) = v497
        val v508 = v498.id match {
        case 56 =>
        None
        case 117 =>
          val BindNode(v500, v501) = v499
          val v507 = v500.id match {
          case 118 =>
            val BindNode(v502, v503) = v501
            assert(v502.id == 119)
            val v504 = v503.asInstanceOf[SequenceNode].children(3)
            val BindNode(v505, v506) = v504
            assert(v505.id == 121)
            matchTypeDesc(v506)
        }
          Some(v507)
      }
        LHS(matchNonterminal(v494), v508)(nextId(), v491)
    }
    v509
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v510, v511) = node
    val v529 = v510.id match {
      case 111 =>
      NullLiteral()(nextId(), v511)
      case 410 =>
        val v512 = v511.asInstanceOf[SequenceNode].children.head
        val BindNode(v513, v514) = v512
        assert(v513.id == 411)
        val BindNode(v515, v516) = v514
        val v521 = v515.id match {
        case 412 =>
          val BindNode(v517, v518) = v516
          assert(v517.id == 103)
          true
        case 413 =>
          val BindNode(v519, v520) = v516
          assert(v519.id == 107)
          false
      }
        BoolLiteral(v521)(nextId(), v511)
      case 414 =>
        val v522 = v511.asInstanceOf[SequenceNode].children(1)
        val BindNode(v523, v524) = v522
        assert(v523.id == 415)
        CharLiteral(matchCharChar(v524))(nextId(), v511)
      case 417 =>
        val v525 = v511.asInstanceOf[SequenceNode].children(1)
        val v526 = unrollRepeat0(v525).map { elem =>
        val BindNode(v527, v528) = elem
        assert(v527.id == 420)
        matchStrChar(v528)
        }
        StrLiteral(v526)(nextId(), v511)
    }
    v529
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v530, v531) = node
    val v535 = v530.id match {
      case 307 =>
        val v532 = v531.asInstanceOf[SequenceNode].children(2)
        val BindNode(v533, v534) = v532
        assert(v533.id == 297)
        Longest(matchInPlaceChoices(v534))(nextId(), v531)
    }
    v535
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v536, v537) = node
    val v558 = v536.id match {
      case 377 =>
        val v538 = v537.asInstanceOf[SequenceNode].children.head
        val BindNode(v539, v540) = v538
        assert(v539.id == 125)
        val v541 = v537.asInstanceOf[SequenceNode].children(3)
        val BindNode(v542, v543) = v541
        assert(v542.id == 378)
        val v544 = v537.asInstanceOf[SequenceNode].children(1)
        val BindNode(v545, v546) = v544
        assert(v545.id == 181)
        val BindNode(v547, v548) = v546
        val v557 = v547.id match {
        case 56 =>
        None
        case 182 =>
          val BindNode(v549, v550) = v548
          val v556 = v549.id match {
          case 183 =>
            val BindNode(v551, v552) = v550
            assert(v551.id == 184)
            val v553 = v552.asInstanceOf[SequenceNode].children(1)
            val BindNode(v554, v555) = v553
            assert(v554.id == 146)
            matchSuperTypes(v555)
        }
          Some(v556)
      }
        NamedConstructExpr(matchTypeName(v540), matchNamedConstructParams(v543), v557)(nextId(), v537)
    }
    v558
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v559, v560) = node
    val v584 = v559.id match {
      case 379 =>
        val v561 = v560.asInstanceOf[SequenceNode].children(2)
        val BindNode(v562, v563) = v561
        assert(v562.id == 380)
        val BindNode(v564, v565) = v563
        val v583 = v564.id match {
        case 381 =>
          val BindNode(v566, v567) = v565
          assert(v566.id == 382)
          val v568 = v567.asInstanceOf[SequenceNode].children.head
          val BindNode(v569, v570) = v568
          assert(v569.id == 383)
          val v571 = v567.asInstanceOf[SequenceNode].children(1)
          val v572 = unrollRepeat0(v571).map { elem =>
          val BindNode(v573, v574) = elem
          assert(v573.id == 387)
          val BindNode(v575, v576) = v574
          val v582 = v575.id match {
          case 388 =>
            val BindNode(v577, v578) = v576
            assert(v577.id == 389)
            val v579 = v578.asInstanceOf[SequenceNode].children(3)
            val BindNode(v580, v581) = v579
            assert(v580.id == 383)
            matchNamedParam(v581)
        }
          v582
          }
          List(matchNamedParam(v570)) ++ v572
      }
        v583
    }
    v584
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v585, v586) = node
    val v607 = v585.id match {
      case 384 =>
        val v587 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v588, v589) = v587
        assert(v588.id == 170)
        val v590 = v586.asInstanceOf[SequenceNode].children(1)
        val BindNode(v591, v592) = v590
        assert(v591.id == 116)
        val BindNode(v593, v594) = v592
        val v603 = v593.id match {
        case 56 =>
        None
        case 117 =>
          val BindNode(v595, v596) = v594
          val v602 = v595.id match {
          case 118 =>
            val BindNode(v597, v598) = v596
            assert(v597.id == 119)
            val v599 = v598.asInstanceOf[SequenceNode].children(3)
            val BindNode(v600, v601) = v599
            assert(v600.id == 121)
            matchTypeDesc(v601)
        }
          Some(v602)
      }
        val v604 = v586.asInstanceOf[SequenceNode].children(5)
        val BindNode(v605, v606) = v604
        assert(v605.id == 353)
        NamedParam(matchParamName(v589), v603, matchPExpr(v606))(nextId(), v586)
    }
    v607
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v608, v609) = node
    val v628 = v608.id match {
      case 124 =>
        val v610 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v611, v612) = v610
        assert(v611.id == 125)
        matchTypeName(v612)
      case 126 =>
        val v613 = v609.asInstanceOf[SequenceNode].children(2)
        val BindNode(v614, v615) = v613
        assert(v614.id == 121)
        ArrayTypeDesc(matchTypeDesc(v615))(nextId(), v609)
      case 129 =>
        val v616 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v617, v618) = v616
        assert(v617.id == 130)
        matchValueType(v618)
      case 131 =>
        val v619 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v620, v621) = v619
        assert(v620.id == 132)
        matchAnyType(v621)
      case 137 =>
        val v622 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v623, v624) = v622
        assert(v623.id == 138)
        matchEnumTypeName(v624)
      case 141 =>
        val v625 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v626, v627) = v625
        assert(v626.id == 142)
        matchTypeDef(v627)
    }
    v628
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v629, v630) = node
    val v634 = v629.id match {
      case 64 =>
        val v631 = v630.asInstanceOf[SequenceNode].children.head
        val BindNode(v632, v633) = v631
        assert(v632.id == 65)
        Nonterminal(matchNonterminalName(v633))(nextId(), v630)
    }
    v634
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v635, v636) = node
    val v643 = v635.id match {
      case 66 =>
        val v637 = v636.asInstanceOf[SequenceNode].children.head
        val BindNode(v638, v639) = v637
        assert(v638.id == 67)
        NonterminalName(matchIdNoKeyword(v639))(nextId(), v636)
      case 114 =>
        val v640 = v636.asInstanceOf[SequenceNode].children(1)
        val BindNode(v641, v642) = v640
        assert(v641.id == 70)
        NonterminalName(matchId(v642))(nextId(), v636)
    }
    v643
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v644, v645) = node
    val v655 = v644.id match {
      case 354 =>
        val v646 = v645.asInstanceOf[SequenceNode].children.head
        val BindNode(v647, v648) = v646
        assert(v647.id == 355)
        val v649 = v645.asInstanceOf[SequenceNode].children(4)
        val BindNode(v650, v651) = v649
        assert(v650.id == 121)
        TypedPExpr(matchTernaryExpr(v648), matchTypeDesc(v651))(nextId(), v645)
      case 464 =>
        val v652 = v645.asInstanceOf[SequenceNode].children.head
        val BindNode(v653, v654) = v652
        assert(v653.id == 355)
        matchTernaryExpr(v654)
    }
    v655
  }

  def matchPExprBlock(node: Node): ProcessorBlock = {
    val BindNode(v656, v657) = node
    val v661 = v656.id match {
      case 352 =>
        val v658 = v657.asInstanceOf[SequenceNode].children(2)
        val BindNode(v659, v660) = v658
        assert(v659.id == 353)
        ProcessorBlock(matchPExpr(v660))(nextId(), v657)
    }
    v661
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v662, v663) = node
    val v670 = v662.id match {
      case 66 =>
        val v664 = v663.asInstanceOf[SequenceNode].children.head
        val BindNode(v665, v666) = v664
        assert(v665.id == 67)
        ParamName(matchIdNoKeyword(v666))(nextId(), v663)
      case 114 =>
        val v667 = v663.asInstanceOf[SequenceNode].children(1)
        val BindNode(v668, v669) = v667
        assert(v668.id == 70)
        ParamName(matchId(v669))(nextId(), v663)
    }
    v670
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v671, v672) = node
    val v685 = v671.id match {
      case 237 =>
        val v673 = v672.asInstanceOf[SequenceNode].children.head
        val BindNode(v674, v675) = v673
        assert(v674.id == 236)
        Optional(matchPostUnSymbol(v675))(nextId(), v672)
      case 238 =>
        val v676 = v672.asInstanceOf[SequenceNode].children.head
        val BindNode(v677, v678) = v676
        assert(v677.id == 236)
        RepeatFromZero(matchPostUnSymbol(v678))(nextId(), v672)
      case 239 =>
        val v679 = v672.asInstanceOf[SequenceNode].children.head
        val BindNode(v680, v681) = v679
        assert(v680.id == 236)
        RepeatFromOne(matchPostUnSymbol(v681))(nextId(), v672)
      case 241 =>
        val v682 = v672.asInstanceOf[SequenceNode].children.head
        val BindNode(v683, v684) = v682
        assert(v683.id == 242)
        matchAtomSymbol(v684)
    }
    v685
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v686, v687) = node
    val v697 = v686.id match {
      case 231 =>
        val v688 = v687.asInstanceOf[SequenceNode].children(2)
        val BindNode(v689, v690) = v688
        assert(v689.id == 230)
        FollowedBy(matchPreUnSymbol(v690))(nextId(), v687)
      case 233 =>
        val v691 = v687.asInstanceOf[SequenceNode].children(2)
        val BindNode(v692, v693) = v691
        assert(v692.id == 230)
        NotFollowedBy(matchPreUnSymbol(v693))(nextId(), v687)
      case 235 =>
        val v694 = v687.asInstanceOf[SequenceNode].children.head
        val BindNode(v695, v696) = v694
        assert(v695.id == 236)
        matchPostUnSymbol(v696)
    }
    v697
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v698, v699) = node
    val v706 = v698.id match {
      case 368 =>
        val v700 = v699.asInstanceOf[SequenceNode].children(2)
        val BindNode(v701, v702) = v700
        assert(v701.id == 367)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v702))(nextId(), v699)
      case 369 =>
        val v703 = v699.asInstanceOf[SequenceNode].children.head
        val BindNode(v704, v705) = v703
        assert(v704.id == 370)
        matchAtom(v705)
    }
    v706
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v707, v708) = node
    val v715 = v707.id match {
      case 316 =>
        val v709 = v708.asInstanceOf[SequenceNode].children.head
        val BindNode(v710, v711) = v709
        assert(v710.id == 317)
        matchRef(v711)
      case 350 =>
        val v712 = v708.asInstanceOf[SequenceNode].children.head
        val BindNode(v713, v714) = v712
        assert(v713.id == 351)
        matchPExprBlock(v714)
    }
    v715
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v716, v717) = node
    val v721 = v716.id match {
      case 220 =>
        val v718 = v717.asInstanceOf[SequenceNode].children.head
        val BindNode(v719, v720) = v718
        assert(v719.id == 221)
        matchSequence(v720)
    }
    v721
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v722, v723) = node
    val v733 = v722.id match {
      case 347 =>
        val v724 = v723.asInstanceOf[SequenceNode].children(2)
        val BindNode(v725, v726) = v724
        assert(v725.id == 332)
        val v727 = v723.asInstanceOf[SequenceNode].children(1)
        val BindNode(v728, v729) = v727
        assert(v728.id == 322)
        val BindNode(v730, v731) = v729
        val v732 = v730.id match {
        case 56 =>
        None
        case 323 =>
        Some(matchCondSymPath(v731))
      }
        RawRef(matchRefIdx(v726), v732)(nextId(), v723)
    }
    v733
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v734, v735) = node
    val v742 = v734.id match {
      case 318 =>
        val v736 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v737, v738) = v736
        assert(v737.id == 319)
        matchValRef(v738)
      case 345 =>
        val v739 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v740, v741) = v739
        assert(v740.id == 346)
        matchRawRef(v741)
    }
    v742
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v743, v744) = node
    val v767 = v743.id match {
      case 333 =>
        val v745 = v744.asInstanceOf[SequenceNode].children.head
        val BindNode(v746, v747) = v745
        assert(v746.id == 334)
        val BindNode(v748, v749) = v747
        assert(v748.id == 335)
        val BindNode(v750, v751) = v749
        val v766 = v750.id match {
        case 336 =>
          val BindNode(v752, v753) = v751
          assert(v752.id == 337)
          val v754 = v753.asInstanceOf[SequenceNode].children.head
          val BindNode(v755, v756) = v754
          assert(v755.id == 338)
          v756.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString
        case 339 =>
          val BindNode(v757, v758) = v751
          assert(v757.id == 340)
          val v759 = v758.asInstanceOf[SequenceNode].children.head
          val BindNode(v760, v761) = v759
          assert(v760.id == 341)
          val v762 = v758.asInstanceOf[SequenceNode].children(1)
          val v763 = unrollRepeat0(v762).map { elem =>
          val BindNode(v764, v765) = elem
          assert(v764.id == 344)
          v765.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v761.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v763.map(x => x.toString).mkString("")
      }
        v766
    }
    v767
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v768, v769) = node
    val v796 = v768.id match {
      case 60 =>
        val v770 = v769.asInstanceOf[SequenceNode].children.head
        val BindNode(v771, v772) = v770
        assert(v771.id == 61)
        val v773 = v769.asInstanceOf[SequenceNode].children(4)
        val BindNode(v774, v775) = v773
        assert(v774.id == 216)
        val BindNode(v776, v777) = v775
        val v795 = v776.id match {
        case 217 =>
          val BindNode(v778, v779) = v777
          assert(v778.id == 218)
          val v780 = v779.asInstanceOf[SequenceNode].children.head
          val BindNode(v781, v782) = v780
          assert(v781.id == 219)
          val v783 = v779.asInstanceOf[SequenceNode].children(1)
          val v784 = unrollRepeat0(v783).map { elem =>
          val BindNode(v785, v786) = elem
          assert(v785.id == 473)
          val BindNode(v787, v788) = v786
          val v794 = v787.id match {
          case 474 =>
            val BindNode(v789, v790) = v788
            assert(v789.id == 475)
            val v791 = v790.asInstanceOf[SequenceNode].children(3)
            val BindNode(v792, v793) = v791
            assert(v792.id == 219)
            matchRHS(v793)
        }
          v794
          }
          List(matchRHS(v782)) ++ v784
      }
        Rule(matchLHS(v772), v795)(nextId(), v769)
    }
    v796
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v797, v798) = node
    val v814 = v797.id match {
      case 222 =>
        val v799 = v798.asInstanceOf[SequenceNode].children.head
        val BindNode(v800, v801) = v799
        assert(v800.id == 223)
        val v802 = v798.asInstanceOf[SequenceNode].children(1)
        val v803 = unrollRepeat0(v802).map { elem =>
        val BindNode(v804, v805) = elem
        assert(v804.id == 468)
        val BindNode(v806, v807) = v805
        val v813 = v806.id match {
        case 469 =>
          val BindNode(v808, v809) = v807
          assert(v808.id == 470)
          val v810 = v809.asInstanceOf[SequenceNode].children(1)
          val BindNode(v811, v812) = v810
          assert(v811.id == 223)
          matchElem(v812)
      }
        v813
        }
        Sequence(List(matchElem(v801)) ++ v803)(nextId(), v798)
    }
    v814
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v815, v816) = node
    val v820 = v815.id match {
      case 436 =>
        val v817 = v816.asInstanceOf[SequenceNode].children(1)
        val BindNode(v818, v819) = v817
        assert(v818.id == 431)
        ShortenedEnumValue(matchEnumValueName(v819))(nextId(), v816)
    }
    v820
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v821, v822) = node
    val v826 = v821.id match {
      case 421 =>
        val v823 = v822.asInstanceOf[SequenceNode].children.head
        val BindNode(v824, v825) = v823
        assert(v824.id == 289)
        matchStringChar(v825)
    }
    v826
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v827, v828) = node
    val v840 = v827.id match {
      case 253 =>
        val v829 = v828.asInstanceOf[SequenceNode].children.head
        val BindNode(v830, v831) = v829
        assert(v830.id == 254)
        matchUnicodeChar(v831)
      case 290 =>
        val v832 = v828.asInstanceOf[SequenceNode].children.head
        val BindNode(v833, v834) = v832
        assert(v833.id == 291)
        val BindNode(v835, v836) = v834
        assert(v835.id == 28)
        CharAsIs(v836.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v828)
      case 293 =>
        val v837 = v828.asInstanceOf[SequenceNode].children(1)
        val BindNode(v838, v839) = v837
        assert(v838.id == 294)
        CharEscaped(v839.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v828)
    }
    v840
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v841, v842) = node
    val v847 = v841.id match {
      case 285 =>
        val v843 = v842.asInstanceOf[SequenceNode].children(1)
        val v844 = unrollRepeat0(v843).map { elem =>
        val BindNode(v845, v846) = elem
        assert(v845.id == 289)
        matchStringChar(v846)
        }
        StringSymbol(v844)(nextId(), v842)
    }
    v847
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v848, v849) = node
    val v859 = v848.id match {
      case 124 =>
        val v850 = v849.asInstanceOf[SequenceNode].children.head
        val BindNode(v851, v852) = v850
        assert(v851.id == 125)
        matchTypeName(v852)
      case 143 =>
        val v853 = v849.asInstanceOf[SequenceNode].children.head
        val BindNode(v854, v855) = v853
        assert(v854.id == 144)
        matchClassDef(v855)
      case 178 =>
        val v856 = v849.asInstanceOf[SequenceNode].children.head
        val BindNode(v857, v858) = v856
        assert(v857.id == 179)
        matchSuperDef(v858)
    }
    v859
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v860, v861) = node
    val v877 = v860.id match {
      case 191 =>
        val v862 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v863, v864) = v862
        assert(v863.id == 192)
        val v865 = v861.asInstanceOf[SequenceNode].children(1)
        val v866 = unrollRepeat0(v865).map { elem =>
        val BindNode(v867, v868) = elem
        assert(v867.id == 195)
        val BindNode(v869, v870) = v868
        val v876 = v869.id match {
        case 196 =>
          val BindNode(v871, v872) = v870
          assert(v871.id == 197)
          val v873 = v872.asInstanceOf[SequenceNode].children(3)
          val BindNode(v874, v875) = v873
          assert(v874.id == 192)
          matchSubType(v875)
      }
        v876
        }
        List(matchSubType(v864)) ++ v866
    }
    v877
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v878, v879) = node
    val v911 = v878.id match {
      case 180 =>
        val v880 = v879.asInstanceOf[SequenceNode].children.head
        val BindNode(v881, v882) = v880
        assert(v881.id == 125)
        val v883 = v879.asInstanceOf[SequenceNode].children(4)
        val BindNode(v884, v885) = v883
        assert(v884.id == 186)
        val BindNode(v886, v887) = v885
        val v896 = v886.id match {
        case 56 =>
        None
        case 187 =>
          val BindNode(v888, v889) = v887
          val v895 = v888.id match {
          case 188 =>
            val BindNode(v890, v891) = v889
            assert(v890.id == 189)
            val v892 = v891.asInstanceOf[SequenceNode].children(1)
            val BindNode(v893, v894) = v892
            assert(v893.id == 190)
            matchSubTypes(v894)
        }
          Some(v895)
      }
        val v897 = v879.asInstanceOf[SequenceNode].children(1)
        val BindNode(v898, v899) = v897
        assert(v898.id == 181)
        val BindNode(v900, v901) = v899
        val v910 = v900.id match {
        case 56 =>
        None
        case 182 =>
          val BindNode(v902, v903) = v901
          val v909 = v902.id match {
          case 183 =>
            val BindNode(v904, v905) = v903
            assert(v904.id == 184)
            val v906 = v905.asInstanceOf[SequenceNode].children(1)
            val BindNode(v907, v908) = v906
            assert(v907.id == 146)
            matchSuperTypes(v908)
        }
          Some(v909)
      }
        SuperDef(matchTypeName(v882), v896, v910)(nextId(), v879)
    }
    v911
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v912, v913) = node
    val v941 = v912.id match {
      case 147 =>
        val v915 = v913.asInstanceOf[SequenceNode].children(2)
        val BindNode(v916, v917) = v915
        assert(v916.id == 149)
        val BindNode(v918, v919) = v917
        val v940 = v918.id match {
        case 56 =>
        None
        case 150 =>
          val BindNode(v920, v921) = v919
          val v939 = v920.id match {
          case 151 =>
            val BindNode(v922, v923) = v921
            assert(v922.id == 152)
            val v924 = v923.asInstanceOf[SequenceNode].children.head
            val BindNode(v925, v926) = v924
            assert(v925.id == 125)
            val v927 = v923.asInstanceOf[SequenceNode].children(1)
            val v928 = unrollRepeat0(v927).map { elem =>
            val BindNode(v929, v930) = elem
            assert(v929.id == 155)
            val BindNode(v931, v932) = v930
            val v938 = v931.id match {
            case 156 =>
              val BindNode(v933, v934) = v932
              assert(v933.id == 157)
              val v935 = v934.asInstanceOf[SequenceNode].children(3)
              val BindNode(v936, v937) = v935
              assert(v936.id == 125)
              matchTypeName(v937)
          }
            v938
            }
            List(matchTypeName(v926)) ++ v928
        }
          Some(v939)
      }
        val v914 = v940
        if (v914.isDefined) v914.get else List()
    }
    v941
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v942, v943) = node
    val v947 = v942.id match {
      case 226 =>
        val v944 = v943.asInstanceOf[SequenceNode].children.head
        val BindNode(v945, v946) = v944
        assert(v945.id == 227)
        matchBinSymbol(v946)
    }
    v947
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v948, v949) = node
    val v953 = v948.id match {
      case 245 =>
        val v950 = v949.asInstanceOf[SequenceNode].children(1)
        val BindNode(v951, v952) = v950
        assert(v951.id == 247)
        matchTerminalChar(v952)
      case 257 =>
      AnyTerminal()(nextId(), v949)
    }
    v953
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v954, v955) = node
    val v967 = v954.id match {
      case 248 =>
        val v956 = v955.asInstanceOf[SequenceNode].children.head
        val BindNode(v957, v958) = v956
        assert(v957.id == 249)
        val BindNode(v959, v960) = v958
        assert(v959.id == 28)
        CharAsIs(v960.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v955)
      case 251 =>
        val v961 = v955.asInstanceOf[SequenceNode].children(1)
        val BindNode(v962, v963) = v961
        assert(v962.id == 252)
        CharEscaped(v963.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v955)
      case 253 =>
        val v964 = v955.asInstanceOf[SequenceNode].children.head
        val BindNode(v965, v966) = v964
        assert(v965.id == 254)
        matchUnicodeChar(v966)
    }
    v967
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v968, v969) = node
    val v980 = v968.id match {
      case 261 =>
        val v970 = v969.asInstanceOf[SequenceNode].children(1)
        val BindNode(v971, v972) = v970
        assert(v971.id == 262)
        val v973 = v969.asInstanceOf[SequenceNode].children(2)
        val v974 = unrollRepeat1(v973).map { elem =>
        val BindNode(v975, v976) = elem
        assert(v975.id == 262)
        matchTerminalChoiceElem(v976)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v972)) ++ v974)(nextId(), v969)
      case 282 =>
        val v977 = v969.asInstanceOf[SequenceNode].children(1)
        val BindNode(v978, v979) = v977
        assert(v978.id == 271)
        TerminalChoice(List(matchTerminalChoiceRange(v979)))(nextId(), v969)
    }
    v980
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v981, v982) = node
    val v994 = v981.id match {
      case 253 =>
        val v983 = v982.asInstanceOf[SequenceNode].children.head
        val BindNode(v984, v985) = v983
        assert(v984.id == 254)
        matchUnicodeChar(v985)
      case 265 =>
        val v986 = v982.asInstanceOf[SequenceNode].children.head
        val BindNode(v987, v988) = v986
        assert(v987.id == 266)
        val BindNode(v989, v990) = v988
        assert(v989.id == 28)
        CharAsIs(v990.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v982)
      case 268 =>
        val v991 = v982.asInstanceOf[SequenceNode].children(1)
        val BindNode(v992, v993) = v991
        assert(v992.id == 269)
        CharEscaped(v993.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v982)
    }
    v994
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v995, v996) = node
    val v1006 = v995.id match {
      case 263 =>
        val v997 = v996.asInstanceOf[SequenceNode].children.head
        val BindNode(v998, v999) = v997
        assert(v998.id == 264)
        matchTerminalChoiceChar(v999)
      case 270 =>
        val v1000 = v996.asInstanceOf[SequenceNode].children.head
        val BindNode(v1001, v1002) = v1000
        assert(v1001.id == 271)
        matchTerminalChoiceRange(v1002)
      case 274 =>
        val v1003 = v996.asInstanceOf[SequenceNode].children.head
        val BindNode(v1004, v1005) = v1003
        assert(v1004.id == 275)
        matchTerminalUnicodeCategory(v1005)
    }
    v1006
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v1007, v1008) = node
    val v1015 = v1007.id match {
      case 272 =>
        val v1009 = v1008.asInstanceOf[SequenceNode].children.head
        val BindNode(v1010, v1011) = v1009
        assert(v1010.id == 264)
        val v1012 = v1008.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1013, v1014) = v1012
        assert(v1013.id == 264)
        TerminalChoiceRange(matchTerminalChoiceChar(v1011), matchTerminalChoiceChar(v1014))(nextId(), v1008)
    }
    v1015
  }

  def matchTerminalUnicodeCategory(node: Node): TerminalUnicodeCategory = {
    val BindNode(v1016, v1017) = node
    val v1024 = v1016.id match {
      case 276 =>
        val v1018 = v1017.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1019, v1020) = v1018
        assert(v1019.id == 277)
        val v1021 = v1017.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1022, v1023) = v1021
        assert(v1022.id == 278)
        TerminalUnicodeCategory(v1020.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1023.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v1017)
    }
    v1024
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v1025, v1026) = node
    val v1059 = v1025.id match {
      case 356 =>
        val v1027 = v1026.asInstanceOf[SequenceNode].children.head
        val BindNode(v1028, v1029) = v1027
        assert(v1028.id == 357)
        val v1030 = v1026.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1031, v1032) = v1030
        assert(v1031.id == 461)
        val BindNode(v1033, v1034) = v1032
        assert(v1033.id == 462)
        val BindNode(v1035, v1036) = v1034
        val v1042 = v1035.id match {
        case 463 =>
          val BindNode(v1037, v1038) = v1036
          assert(v1037.id == 464)
          val v1039 = v1038.asInstanceOf[SequenceNode].children.head
          val BindNode(v1040, v1041) = v1039
          assert(v1040.id == 355)
          matchTernaryExpr(v1041)
      }
        val v1043 = v1026.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1044, v1045) = v1043
        assert(v1044.id == 461)
        val BindNode(v1046, v1047) = v1045
        assert(v1046.id == 462)
        val BindNode(v1048, v1049) = v1047
        val v1055 = v1048.id match {
        case 463 =>
          val BindNode(v1050, v1051) = v1049
          assert(v1050.id == 464)
          val v1052 = v1051.asInstanceOf[SequenceNode].children.head
          val BindNode(v1053, v1054) = v1052
          assert(v1053.id == 355)
          matchTernaryExpr(v1054)
      }
        TernaryOp(matchBoolOrExpr(v1029), v1042, v1055)(nextId(), v1026)
      case 465 =>
        val v1056 = v1026.asInstanceOf[SequenceNode].children.head
        val BindNode(v1057, v1058) = v1056
        assert(v1057.id == 357)
        matchBoolOrExpr(v1058)
    }
    v1059
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v1060, v1061) = node
    val v1071 = v1060.id match {
      case 143 =>
        val v1062 = v1061.asInstanceOf[SequenceNode].children.head
        val BindNode(v1063, v1064) = v1062
        assert(v1063.id == 144)
        matchClassDef(v1064)
      case 178 =>
        val v1065 = v1061.asInstanceOf[SequenceNode].children.head
        val BindNode(v1066, v1067) = v1065
        assert(v1066.id == 179)
        matchSuperDef(v1067)
      case 199 =>
        val v1068 = v1061.asInstanceOf[SequenceNode].children.head
        val BindNode(v1069, v1070) = v1068
        assert(v1069.id == 200)
        matchEnumTypeDef(v1070)
    }
    v1071
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v1072, v1073) = node
    val v1091 = v1072.id match {
      case 122 =>
        val v1074 = v1073.asInstanceOf[SequenceNode].children.head
        val BindNode(v1075, v1076) = v1074
        assert(v1075.id == 123)
        val v1077 = v1073.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1078, v1079) = v1077
        assert(v1078.id == 210)
        val BindNode(v1080, v1081) = v1079
        val v1090 = v1080.id match {
        case 56 =>
        None
        case 211 =>
          val BindNode(v1082, v1083) = v1081
          val v1089 = v1082.id match {
          case 212 =>
            val BindNode(v1084, v1085) = v1083
            assert(v1084.id == 213)
            val v1086 = v1085.asInstanceOf[SequenceNode].children(1)
            val BindNode(v1087, v1088) = v1086
            assert(v1087.id == 214)
            v1088.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
          Some(v1089)
      }
        TypeDesc(matchNonNullTypeDesc(v1076), v1090.isDefined)(nextId(), v1073)
    }
    v1091
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v1092, v1093) = node
    val v1100 = v1092.id match {
      case 66 =>
        val v1094 = v1093.asInstanceOf[SequenceNode].children.head
        val BindNode(v1095, v1096) = v1094
        assert(v1095.id == 67)
        TypeName(matchIdNoKeyword(v1096))(nextId(), v1093)
      case 114 =>
        val v1097 = v1093.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1098, v1099) = v1097
        assert(v1098.id == 70)
        TypeName(matchId(v1099))(nextId(), v1093)
    }
    v1100
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v1101, v1102) = node
    val v1109 = v1101.id match {
      case 66 =>
        val v1103 = v1102.asInstanceOf[SequenceNode].children.head
        val BindNode(v1104, v1105) = v1103
        assert(v1104.id == 67)
        TypeOrFuncName(matchIdNoKeyword(v1105))(nextId(), v1102)
      case 114 =>
        val v1106 = v1102.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1107, v1108) = v1106
        assert(v1107.id == 70)
        TypeOrFuncName(matchId(v1108))(nextId(), v1102)
    }
    v1109
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v1110, v1111) = node
    val v1124 = v1110.id match {
      case 255 =>
        val v1112 = v1111.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1113, v1114) = v1112
        assert(v1113.id == 256)
        val v1115 = v1111.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1116, v1117) = v1115
        assert(v1116.id == 256)
        val v1118 = v1111.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1119, v1120) = v1118
        assert(v1119.id == 256)
        val v1121 = v1111.asInstanceOf[SequenceNode].children(5)
        val BindNode(v1122, v1123) = v1121
        assert(v1122.id == 256)
        CharUnicode(List(v1114.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1117.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1120.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1123.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char))(nextId(), v1111)
    }
    v1124
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v1125, v1126) = node
    val v1136 = v1125.id match {
      case 320 =>
        val v1127 = v1126.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1128, v1129) = v1127
        assert(v1128.id == 332)
        val v1130 = v1126.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1131, v1132) = v1130
        assert(v1131.id == 322)
        val BindNode(v1133, v1134) = v1132
        val v1135 = v1133.id match {
        case 56 =>
        None
        case 323 =>
        Some(matchCondSymPath(v1134))
      }
        ValRef(matchRefIdx(v1129), v1135)(nextId(), v1126)
    }
    v1136
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v1137, v1138) = node
    val v1139 = v1137.id match {
      case 81 =>
      BooleanType()(nextId(), v1138)
      case 90 =>
      CharType()(nextId(), v1138)
      case 96 =>
      StringType()(nextId(), v1138)
    }
    v1139
  }
}
