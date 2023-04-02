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
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1NoUnbind
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
  sealed trait BinSymbol1 extends Symbol with WithIdAndParseNode
  sealed trait BinSymbol2 extends BinSymbol1 with WithIdAndParseNode
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
  case class ExceptSymbol(body: BinSymbol2, except: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends BinSymbol2 with WithIdAndParseNode
  case class ExprParen(body: PExpr)(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class FollowedBy(followedBy: PreUnSymbol)(override val id: Int, override val parseNode: Node) extends PreUnSymbol with WithIdAndParseNode
  case class FuncCallOrConstructExpr(funcName: TypeOrFuncName, params: List[PExpr])(override val id: Int, override val parseNode: Node) extends Atom with WithIdAndParseNode
  case class Grammar(defs: List[Def])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode
  case class InPlaceChoices(choices: List[Sequence])(override val id: Int, override val parseNode: Node) extends AtomSymbol with WithIdAndParseNode
  case class JoinSymbol(body: BinSymbol2, join: BinSymbol1)(override val id: Int, override val parseNode: Node) extends BinSymbol1 with WithIdAndParseNode
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
  sealed trait PreUnSymbol extends BinSymbol2 with WithIdAndParseNode
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
    val v12 = v1.id match {
      case 284 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 190)
        val v6 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v7, v8) = v6
        assert(v7.id == 285)
        val v9 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v10, v11) = v9
        assert(v10.id == 283)
        BinOp(Op.ADD, matchPrefixNotExpr(v8), matchAdditiveExpr(v11))(nextId(), v2)
      case 285 =>
      matchPrefixNotExpr(v2)
    }
    v12
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v13, v14) = node
    val v15 = v13.id match {
      case 102 =>
      AnyType()(nextId(), v14)
    }
    v15
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v16, v17) = node
    val v39 = v16.id match {
      case 316 =>
        val v19 = v17.asInstanceOf[SequenceNode].children(2)
        val BindNode(v20, v21) = v19
        assert(v20.id == 308)
        val BindNode(v22, v23) = v21
        val v38 = v22.id match {
        case 41 =>
        None
        case 309 =>
          val BindNode(v24, v25) = v23
          assert(v24.id == 310)
          val v26 = v25.asInstanceOf[SequenceNode].children.head
          val BindNode(v27, v28) = v26
          assert(v27.id == 271)
          val v29 = v25.asInstanceOf[SequenceNode].children(1)
          val v30 = unrollRepeat0(v29).map { elem =>
          val BindNode(v31, v32) = elem
          assert(v31.id == 313)
          val BindNode(v33, v34) = v32
          assert(v33.id == 314)
          val v35 = v34.asInstanceOf[SequenceNode].children(3)
          val BindNode(v36, v37) = v35
          assert(v36.id == 271)
          matchPExpr(v37)
          }
          Some(List(matchPExpr(v28)) ++ v30)
      }
        val v18 = v38
        ArrayExpr(if (v18.isDefined) v18.get else List())(nextId(), v17)
    }
    v39
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v40, v41) = node
    val v45 = v40.id match {
      case 246 =>
      matchRef(v41)
      case 288 =>
      matchBindExpr(v41)
      case 291 =>
      matchNamedConstructExpr(v41)
      case 303 =>
      matchFuncCallOrConstructExpr(v41)
      case 315 =>
      matchArrayExpr(v41)
      case 317 =>
      matchLiteral(v41)
      case 325 =>
      matchEnumValue(v41)
      case 333 =>
        val v42 = v41.asInstanceOf[SequenceNode].children(2)
        val BindNode(v43, v44) = v42
        assert(v43.id == 271)
        ExprParen(matchPExpr(v44))(nextId(), v41)
    }
    v45
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v46, v47) = node
    val v51 = v46.id match {
      case 47 =>
      matchNonterminal(v47)
      case 192 =>
      matchTerminal(v47)
      case 204 =>
      matchTerminalChoice(v47)
      case 222 =>
      matchStringSymbol(v47)
      case 232 =>
        val v48 = v47.asInstanceOf[SequenceNode].children(2)
        val BindNode(v49, v50) = v48
        assert(v49.id == 233)
        matchInPlaceChoices(v50)
      case 240 =>
      matchLongest(v47)
      case 242 =>
      matchEmptySequence(v47)
    }
    v51
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v52, v53) = node
    val v60 = v52.id match {
      case 289 =>
        val v54 = v53.asInstanceOf[SequenceNode].children.head
        val BindNode(v55, v56) = v54
        assert(v55.id == 247)
        val v57 = v53.asInstanceOf[SequenceNode].children(1)
        val BindNode(v58, v59) = v57
        assert(v58.id == 290)
        BindExpr(matchValRef(v56), matchBinderExpr(v59))(nextId(), v53)
    }
    v60
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v61, v62) = node
    val v63 = v61.id match {
      case 246 =>
      matchRef(v62)
      case 269 =>
      matchPExprBlock(v62)
      case 288 =>
      matchBindExpr(v62)
    }
    v63
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v64, v65) = node
    val v72 = v64.id match {
      case 278 =>
        val v66 = v65.asInstanceOf[SequenceNode].children.head
        val BindNode(v67, v68) = v66
        assert(v67.id == 279)
        val v69 = v65.asInstanceOf[SequenceNode].children(4)
        val BindNode(v70, v71) = v69
        assert(v70.id == 277)
        BinOp(Op.OR, matchBoolEqExpr(v68), matchBoolAndExpr(v71))(nextId(), v65)
      case 279 =>
      matchBoolEqExpr(v65)
    }
    v72
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v73, v74) = node
    val v87 = v73.id match {
      case 280 =>
        val v75 = v74.asInstanceOf[SequenceNode].children(2)
        val BindNode(v76, v77) = v75
        assert(v76.id == 336)
        val BindNode(v78, v79) = v77
        val v80 = v78.id match {
        case 337 =>
        Op.EQ
        case 339 =>
        Op.NE
      }
        val v81 = v74.asInstanceOf[SequenceNode].children.head
        val BindNode(v82, v83) = v81
        assert(v82.id == 281)
        val v84 = v74.asInstanceOf[SequenceNode].children(4)
        val BindNode(v85, v86) = v84
        assert(v85.id == 279)
        BinOp(v80, matchElvisExpr(v83), matchBoolEqExpr(v86))(nextId(), v74)
      case 281 =>
      matchElvisExpr(v74)
    }
    v87
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v88, v89) = node
    val v96 = v88.id match {
      case 276 =>
        val v90 = v89.asInstanceOf[SequenceNode].children.head
        val BindNode(v91, v92) = v90
        assert(v91.id == 277)
        val v93 = v89.asInstanceOf[SequenceNode].children(4)
        val BindNode(v94, v95) = v93
        assert(v94.id == 275)
        BinOp(Op.AND, matchBoolAndExpr(v92), matchBoolOrExpr(v95))(nextId(), v89)
      case 277 =>
      matchBoolAndExpr(v89)
    }
    v96
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v97, v98) = node
    val v120 = v97.id match {
      case 307 =>
        val v100 = v98.asInstanceOf[SequenceNode].children(2)
        val BindNode(v101, v102) = v100
        assert(v101.id == 308)
        val BindNode(v103, v104) = v102
        val v119 = v103.id match {
        case 41 =>
        None
        case 309 =>
          val BindNode(v105, v106) = v104
          assert(v105.id == 310)
          val v107 = v106.asInstanceOf[SequenceNode].children.head
          val BindNode(v108, v109) = v107
          assert(v108.id == 271)
          val v110 = v106.asInstanceOf[SequenceNode].children(1)
          val v111 = unrollRepeat0(v110).map { elem =>
          val BindNode(v112, v113) = elem
          assert(v112.id == 313)
          val BindNode(v114, v115) = v113
          assert(v114.id == 314)
          val v116 = v115.asInstanceOf[SequenceNode].children(3)
          val BindNode(v117, v118) = v116
          assert(v117.id == 271)
          matchPExpr(v118)
          }
          Some(List(matchPExpr(v109)) ++ v111)
      }
        val v99 = v119
        if (v99.isDefined) v99.get else List()
    }
    v120
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v121, v122) = node
    val v129 = v121.id match {
      case 329 =>
        val v123 = v122.asInstanceOf[SequenceNode].children.head
        val BindNode(v124, v125) = v123
        assert(v124.id == 105)
        val v126 = v122.asInstanceOf[SequenceNode].children(2)
        val BindNode(v127, v128) = v126
        assert(v127.id == 330)
        CanonicalEnumValue(matchEnumTypeName(v125), matchEnumValueName(v128))(nextId(), v122)
    }
    v129
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v130, v131) = node
    val v132 = v130.id match {
      case 195 =>
      matchTerminalChar(v131)
    }
    v132
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v133, v134) = node
    val v156 = v133.id match {
      case 110 =>
        val v135 = v134.asInstanceOf[SequenceNode].children.head
        val BindNode(v136, v137) = v135
        assert(v136.id == 96)
        val v138 = v134.asInstanceOf[SequenceNode].children(2)
        val BindNode(v139, v140) = v138
        assert(v139.id == 111)
        AbstractClassDef(matchTypeName(v137), matchSuperTypes(v140))(nextId(), v134)
      case 123 =>
        val v141 = v134.asInstanceOf[SequenceNode].children.head
        val BindNode(v142, v143) = v141
        assert(v142.id == 96)
        val v144 = v134.asInstanceOf[SequenceNode].children(2)
        val BindNode(v145, v146) = v144
        assert(v145.id == 124)
        ConcreteClassDef(matchTypeName(v143), None, matchClassParamsDef(v146))(nextId(), v134)
      case 138 =>
        val v147 = v134.asInstanceOf[SequenceNode].children.head
        val BindNode(v148, v149) = v147
        assert(v148.id == 96)
        val v150 = v134.asInstanceOf[SequenceNode].children(2)
        val BindNode(v151, v152) = v150
        assert(v151.id == 111)
        val v153 = v134.asInstanceOf[SequenceNode].children(4)
        val BindNode(v154, v155) = v153
        assert(v154.id == 124)
        ConcreteClassDef(matchTypeName(v149), Some(matchSuperTypes(v152)), matchClassParamsDef(v155))(nextId(), v134)
    }
    v156
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v157, v158) = node
    val v173 = v157.id match {
      case 131 =>
        val v159 = v158.asInstanceOf[SequenceNode].children.head
        val BindNode(v160, v161) = v159
        assert(v160.id == 132)
        val v162 = v158.asInstanceOf[SequenceNode].children(1)
        val BindNode(v163, v164) = v162
        assert(v163.id == 89)
        val BindNode(v165, v166) = v164
        val v172 = v165.id match {
        case 41 =>
        None
        case 90 =>
          val BindNode(v167, v168) = v166
          assert(v167.id == 91)
          val v169 = v168.asInstanceOf[SequenceNode].children(3)
          val BindNode(v170, v171) = v169
          assert(v170.id == 93)
          Some(matchTypeDesc(v171))
      }
        ClassParamDef(matchParamName(v161), v172)(nextId(), v158)
    }
    v173
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v174, v175) = node
    val v197 = v174.id match {
      case 125 =>
        val v177 = v175.asInstanceOf[SequenceNode].children(2)
        val BindNode(v178, v179) = v177
        assert(v178.id == 127)
        val BindNode(v180, v181) = v179
        val v196 = v180.id match {
        case 41 =>
        None
        case 128 =>
          val BindNode(v182, v183) = v181
          assert(v182.id == 129)
          val v184 = v183.asInstanceOf[SequenceNode].children.head
          val BindNode(v185, v186) = v184
          assert(v185.id == 130)
          val v187 = v183.asInstanceOf[SequenceNode].children(1)
          val v188 = unrollRepeat0(v187).map { elem =>
          val BindNode(v189, v190) = elem
          assert(v189.id == 135)
          val BindNode(v191, v192) = v190
          assert(v191.id == 136)
          val v193 = v192.asInstanceOf[SequenceNode].children(3)
          val BindNode(v194, v195) = v193
          assert(v194.id == 130)
          matchClassParamDef(v195)
          }
          Some(List(matchClassParamDef(v186)) ++ v188)
      }
        val v176 = v196
        if (v176.isDefined) v176.get else List()
    }
    v197
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v198, v199) = node
    val v206 = v198.id match {
      case 252 =>
        val v200 = unrollRepeat1NoUnbind(254, 253, v199).map { elem =>
        val BindNode(v201, v202) = elem
        assert(v201.id == 253)
        val BindNode(v203, v204) = v202
        val v205 = v203.id match {
        case 113 =>
        CondSymDir.BODY
        case 122 =>
        CondSymDir.COND
      }
        v205
        }
        v200
    }
    v206
  }

  def matchDef(node: Node): Def = {
    val BindNode(v207, v208) = node
    val v209 = v207.id match {
      case 43 =>
      matchRule(v208)
      case 108 =>
      matchTypeDef(v208)
    }
    v209
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v210, v211) = node
    val v212 = v210.id match {
      case 175 =>
      matchSymbol(v211)
      case 245 =>
      matchProcessor(v211)
    }
    v212
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v213, v214) = node
    val v221 = v213.id match {
      case 282 =>
        val v215 = v214.asInstanceOf[SequenceNode].children.head
        val BindNode(v216, v217) = v215
        assert(v216.id == 283)
        val v218 = v214.asInstanceOf[SequenceNode].children(4)
        val BindNode(v219, v220) = v218
        assert(v219.id == 281)
        ElvisOp(matchAdditiveExpr(v217), matchElvisExpr(v220))(nextId(), v214)
      case 283 =>
      matchAdditiveExpr(v214)
    }
    v221
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v222, v223) = node
    val v224 = v222.id match {
      case 243 =>
      EmptySeq()(nextId(), v223)
    }
    v224
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v225, v226) = node
    val v247 = v225.id match {
      case 157 =>
        val v227 = v226.asInstanceOf[SequenceNode].children.head
        val BindNode(v228, v229) = v227
        assert(v228.id == 105)
        val v230 = v226.asInstanceOf[SequenceNode].children(4)
        val BindNode(v231, v232) = v230
        assert(v231.id == 158)
        val BindNode(v233, v234) = v232
        assert(v233.id == 159)
        val v235 = v234.asInstanceOf[SequenceNode].children.head
        val BindNode(v236, v237) = v235
        assert(v236.id == 51)
        val v238 = v234.asInstanceOf[SequenceNode].children(1)
        val v239 = unrollRepeat0(v238).map { elem =>
        val BindNode(v240, v241) = elem
        assert(v240.id == 162)
        val BindNode(v242, v243) = v241
        assert(v242.id == 163)
        val v244 = v243.asInstanceOf[SequenceNode].children(3)
        val BindNode(v245, v246) = v244
        assert(v245.id == 51)
        matchId(v246)
        }
        EnumTypeDef(matchEnumTypeName(v229), List(matchId(v237)) ++ v239)(nextId(), v226)
    }
    v247
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v248, v249) = node
    val v253 = v248.id match {
      case 106 =>
        val v250 = v249.asInstanceOf[SequenceNode].children(1)
        val BindNode(v251, v252) = v250
        assert(v251.id == 51)
        EnumTypeName(matchId(v252))(nextId(), v249)
    }
    v253
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v254, v255) = node
    val v261 = v254.id match {
      case 326 =>
        val BindNode(v256, v257) = v255
        assert(v256.id == 327)
        val BindNode(v258, v259) = v257
        val v260 = v258.id match {
        case 328 =>
        matchCanonicalEnumValue(v259)
        case 331 =>
        matchShortenedEnumValue(v259)
      }
        v260
    }
    v261
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v262, v263) = node
    val v264 = v262.id match {
      case 51 =>
      EnumValueName(matchId(v263))(nextId(), v263)
    }
    v264
  }

  def matchExceptSymbol(node: Node): BinSymbol2 = {
    val BindNode(v265, v266) = node
    val v273 = v265.id match {
      case 179 =>
        val v267 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v268, v269) = v267
        assert(v268.id == 178)
        val v270 = v266.asInstanceOf[SequenceNode].children(4)
        val BindNode(v271, v272) = v270
        assert(v271.id == 181)
        ExceptSymbol(matchExceptSymbol(v269), matchPreUnSymbol(v272))(nextId(), v266)
      case 181 =>
      matchPreUnSymbol(v266)
    }
    v273
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v274, v275) = node
    val v282 = v274.id match {
      case 304 =>
        val v276 = v275.asInstanceOf[SequenceNode].children.head
        val BindNode(v277, v278) = v276
        assert(v277.id == 305)
        val v279 = v275.asInstanceOf[SequenceNode].children(2)
        val BindNode(v280, v281) = v279
        assert(v280.id == 306)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v278), matchCallParams(v281))(nextId(), v275)
    }
    v282
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v283, v284) = node
    val v297 = v283.id match {
      case 3 =>
        val v285 = v284.asInstanceOf[SequenceNode].children(1)
        val BindNode(v286, v287) = v285
        assert(v286.id == 42)
        val v288 = v284.asInstanceOf[SequenceNode].children(2)
        val v289 = unrollRepeat0(v288).map { elem =>
        val BindNode(v290, v291) = elem
        assert(v290.id == 357)
        val BindNode(v292, v293) = v291
        assert(v292.id == 358)
        val v294 = v293.asInstanceOf[SequenceNode].children(1)
        val BindNode(v295, v296) = v294
        assert(v295.id == 42)
        matchDef(v296)
        }
        Grammar(List(matchDef(v287)) ++ v289)(nextId(), v284)
    }
    v297
  }

  def matchId(node: Node): String = {
    val BindNode(v298, v299) = node
    val v311 = v298.id match {
      case 52 =>
        val BindNode(v300, v301) = v299
        assert(v300.id == 53)
        val BindNode(v302, v303) = v301
        assert(v302.id == 54)
        val v304 = v303.asInstanceOf[SequenceNode].children.head
        val BindNode(v305, v306) = v304
        assert(v305.id == 55)
        val v307 = v303.asInstanceOf[SequenceNode].children(1)
        val v308 = unrollRepeat0(v307).map { elem =>
        val BindNode(v309, v310) = elem
        assert(v309.id == 58)
        v310.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v306.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v308.map(x => x.toString).mkString("")
    }
    v311
  }

  def matchIdNoKeyword(node: Node): String = {
    val BindNode(v312, v313) = node
    val v316 = v312.id match {
      case 50 =>
        val BindNode(v314, v315) = v313
        assert(v314.id == 51)
        matchId(v315)
    }
    v316
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v317, v318) = node
    val v331 = v317.id match {
      case 234 =>
        val v319 = v318.asInstanceOf[SequenceNode].children.head
        val BindNode(v320, v321) = v319
        assert(v320.id == 172)
        val v322 = v318.asInstanceOf[SequenceNode].children(1)
        val v323 = unrollRepeat0(v322).map { elem =>
        val BindNode(v324, v325) = elem
        assert(v324.id == 237)
        val BindNode(v326, v327) = v325
        assert(v326.id == 238)
        val v328 = v327.asInstanceOf[SequenceNode].children(3)
        val BindNode(v329, v330) = v328
        assert(v329.id == 172)
        matchSequence(v330)
        }
        InPlaceChoices(List(matchSequence(v321)) ++ v323)(nextId(), v318)
    }
    v331
  }

  def matchJoinSymbol(node: Node): BinSymbol1 = {
    val BindNode(v332, v333) = node
    val v340 = v332.id match {
      case 177 =>
        val v334 = v333.asInstanceOf[SequenceNode].children.head
        val BindNode(v335, v336) = v334
        assert(v335.id == 178)
        val v337 = v333.asInstanceOf[SequenceNode].children(4)
        val BindNode(v338, v339) = v337
        assert(v338.id == 176)
        JoinSymbol(matchExceptSymbol(v336), matchJoinSymbol(v339))(nextId(), v333)
      case 178 =>
      matchExceptSymbol(v333)
    }
    v340
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v341, v342) = node
    val v357 = v341.id match {
      case 46 =>
        val v343 = v342.asInstanceOf[SequenceNode].children.head
        val BindNode(v344, v345) = v343
        assert(v344.id == 47)
        val v346 = v342.asInstanceOf[SequenceNode].children(1)
        val BindNode(v347, v348) = v346
        assert(v347.id == 89)
        val BindNode(v349, v350) = v348
        val v356 = v349.id match {
        case 41 =>
        None
        case 90 =>
          val BindNode(v351, v352) = v350
          assert(v351.id == 91)
          val v353 = v352.asInstanceOf[SequenceNode].children(3)
          val BindNode(v354, v355) = v353
          assert(v354.id == 93)
          Some(matchTypeDesc(v355))
      }
        LHS(matchNonterminal(v345), v356)(nextId(), v342)
    }
    v357
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v358, v359) = node
    val v370 = v358.id match {
      case 85 =>
      NullLiteral()(nextId(), v359)
      case 318 =>
        val BindNode(v360, v361) = v359
        val v362 = v360.id match {
        case 79 =>
        true
        case 82 =>
        false
      }
        BoolLiteral(v362)(nextId(), v359)
      case 319 =>
        val v363 = v359.asInstanceOf[SequenceNode].children(1)
        val BindNode(v364, v365) = v363
        assert(v364.id == 320)
        CharLiteral(matchCharChar(v365))(nextId(), v359)
      case 321 =>
        val v366 = v359.asInstanceOf[SequenceNode].children(1)
        val v367 = unrollRepeat0(v366).map { elem =>
        val BindNode(v368, v369) = elem
        assert(v368.id == 324)
        matchStrChar(v369)
        }
        StrLiteral(v367)(nextId(), v359)
    }
    v370
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v371, v372) = node
    val v376 = v371.id match {
      case 241 =>
        val v373 = v372.asInstanceOf[SequenceNode].children(2)
        val BindNode(v374, v375) = v373
        assert(v374.id == 233)
        Longest(matchInPlaceChoices(v375))(nextId(), v372)
    }
    v376
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v377, v378) = node
    val v396 = v377.id match {
      case 292 =>
        val v379 = v378.asInstanceOf[SequenceNode].children.head
        val BindNode(v380, v381) = v379
        assert(v380.id == 96)
        val v382 = v378.asInstanceOf[SequenceNode].children(3)
        val BindNode(v383, v384) = v382
        assert(v383.id == 293)
        val v385 = v378.asInstanceOf[SequenceNode].children(1)
        val BindNode(v386, v387) = v385
        assert(v386.id == 141)
        val BindNode(v388, v389) = v387
        val v395 = v388.id match {
        case 41 =>
        None
        case 142 =>
          val BindNode(v390, v391) = v389
          assert(v390.id == 143)
          val v392 = v391.asInstanceOf[SequenceNode].children(1)
          val BindNode(v393, v394) = v392
          assert(v393.id == 111)
          Some(matchSuperTypes(v394))
      }
        NamedConstructExpr(matchTypeName(v381), matchNamedConstructParams(v384), v395)(nextId(), v378)
    }
    v396
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v397, v398) = node
    val v416 = v397.id match {
      case 294 =>
        val v399 = v398.asInstanceOf[SequenceNode].children(2)
        val BindNode(v400, v401) = v399
        assert(v400.id == 295)
        val BindNode(v402, v403) = v401
        assert(v402.id == 296)
        val v404 = v403.asInstanceOf[SequenceNode].children.head
        val BindNode(v405, v406) = v404
        assert(v405.id == 297)
        val v407 = v403.asInstanceOf[SequenceNode].children(1)
        val v408 = unrollRepeat0(v407).map { elem =>
        val BindNode(v409, v410) = elem
        assert(v409.id == 301)
        val BindNode(v411, v412) = v410
        assert(v411.id == 302)
        val v413 = v412.asInstanceOf[SequenceNode].children(3)
        val BindNode(v414, v415) = v413
        assert(v414.id == 297)
        matchNamedParam(v415)
        }
        List(matchNamedParam(v406)) ++ v408
    }
    v416
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v417, v418) = node
    val v436 = v417.id match {
      case 298 =>
        val v419 = v418.asInstanceOf[SequenceNode].children.head
        val BindNode(v420, v421) = v419
        assert(v420.id == 132)
        val v422 = v418.asInstanceOf[SequenceNode].children(1)
        val BindNode(v423, v424) = v422
        assert(v423.id == 89)
        val BindNode(v425, v426) = v424
        val v432 = v425.id match {
        case 41 =>
        None
        case 90 =>
          val BindNode(v427, v428) = v426
          assert(v427.id == 91)
          val v429 = v428.asInstanceOf[SequenceNode].children(3)
          val BindNode(v430, v431) = v429
          assert(v430.id == 93)
          Some(matchTypeDesc(v431))
      }
        val v433 = v418.asInstanceOf[SequenceNode].children(5)
        val BindNode(v434, v435) = v433
        assert(v434.id == 271)
        NamedParam(matchParamName(v421), v432, matchPExpr(v435))(nextId(), v418)
    }
    v436
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v437, v438) = node
    val v442 = v437.id match {
      case 96 =>
      matchTypeName(v438)
      case 97 =>
        val v439 = v438.asInstanceOf[SequenceNode].children(2)
        val BindNode(v440, v441) = v439
        assert(v440.id == 93)
        ArrayTypeDesc(matchTypeDesc(v441))(nextId(), v438)
      case 100 =>
      matchValueType(v438)
      case 101 =>
      matchAnyType(v438)
      case 105 =>
      matchEnumTypeName(v438)
      case 108 =>
      matchTypeDef(v438)
    }
    v442
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v443, v444) = node
    val v445 = v443.id match {
      case 48 =>
      Nonterminal(matchNonterminalName(v444))(nextId(), v444)
    }
    v445
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v446, v447) = node
    val v451 = v446.id match {
      case 49 =>
      NonterminalName(matchIdNoKeyword(v447))(nextId(), v447)
      case 87 =>
        val v448 = v447.asInstanceOf[SequenceNode].children(1)
        val BindNode(v449, v450) = v448
        assert(v449.id == 51)
        NonterminalName(matchId(v450))(nextId(), v447)
    }
    v451
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v452, v453) = node
    val v460 = v452.id match {
      case 272 =>
        val v454 = v453.asInstanceOf[SequenceNode].children.head
        val BindNode(v455, v456) = v454
        assert(v455.id == 273)
        val v457 = v453.asInstanceOf[SequenceNode].children(4)
        val BindNode(v458, v459) = v457
        assert(v458.id == 93)
        TypedPExpr(matchTernaryExpr(v456), matchTypeDesc(v459))(nextId(), v453)
      case 273 =>
      matchTernaryExpr(v453)
    }
    v460
  }

  def matchPExprBlock(node: Node): ProcessorBlock = {
    val BindNode(v461, v462) = node
    val v466 = v461.id match {
      case 270 =>
        val v463 = v462.asInstanceOf[SequenceNode].children(2)
        val BindNode(v464, v465) = v463
        assert(v464.id == 271)
        ProcessorBlock(matchPExpr(v465))(nextId(), v462)
    }
    v466
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v467, v468) = node
    val v472 = v467.id match {
      case 49 =>
      ParamName(matchIdNoKeyword(v468))(nextId(), v468)
      case 87 =>
        val v469 = v468.asInstanceOf[SequenceNode].children(1)
        val BindNode(v470, v471) = v469
        assert(v470.id == 51)
        ParamName(matchId(v471))(nextId(), v468)
    }
    v472
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v473, v474) = node
    val v484 = v473.id match {
      case 187 =>
        val v475 = v474.asInstanceOf[SequenceNode].children.head
        val BindNode(v476, v477) = v475
        assert(v476.id == 186)
        Optional(matchPostUnSymbol(v477))(nextId(), v474)
      case 188 =>
        val v478 = v474.asInstanceOf[SequenceNode].children.head
        val BindNode(v479, v480) = v478
        assert(v479.id == 186)
        RepeatFromZero(matchPostUnSymbol(v480))(nextId(), v474)
      case 189 =>
        val v481 = v474.asInstanceOf[SequenceNode].children.head
        val BindNode(v482, v483) = v481
        assert(v482.id == 186)
        RepeatFromOne(matchPostUnSymbol(v483))(nextId(), v474)
      case 191 =>
      matchAtomSymbol(v474)
    }
    v484
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v485, v486) = node
    val v493 = v485.id match {
      case 182 =>
        val v487 = v486.asInstanceOf[SequenceNode].children(2)
        val BindNode(v488, v489) = v487
        assert(v488.id == 181)
        FollowedBy(matchPreUnSymbol(v489))(nextId(), v486)
      case 184 =>
        val v490 = v486.asInstanceOf[SequenceNode].children(2)
        val BindNode(v491, v492) = v490
        assert(v491.id == 181)
        NotFollowedBy(matchPreUnSymbol(v492))(nextId(), v486)
      case 186 =>
      matchPostUnSymbol(v486)
    }
    v493
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v494, v495) = node
    val v499 = v494.id match {
      case 286 =>
        val v496 = v495.asInstanceOf[SequenceNode].children(2)
        val BindNode(v497, v498) = v496
        assert(v497.id == 285)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v498))(nextId(), v495)
      case 287 =>
      matchAtom(v495)
    }
    v499
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v500, v501) = node
    val v502 = v500.id match {
      case 246 =>
      matchRef(v501)
      case 269 =>
      matchPExprBlock(v501)
    }
    v502
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v503, v504) = node
    val v505 = v503.id match {
      case 172 =>
      matchSequence(v504)
    }
    v505
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v506, v507) = node
    val v517 = v506.id match {
      case 266 =>
        val v508 = v507.asInstanceOf[SequenceNode].children(2)
        val BindNode(v509, v510) = v508
        assert(v509.id == 255)
        val v511 = v507.asInstanceOf[SequenceNode].children(1)
        val BindNode(v512, v513) = v511
        assert(v512.id == 250)
        val BindNode(v514, v515) = v513
        val v516 = v514.id match {
        case 41 =>
        None
        case 251 =>
        Some(matchCondSymPath(v515))
      }
        RawRef(matchRefIdx(v510), v516)(nextId(), v507)
    }
    v517
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v518, v519) = node
    val v520 = v518.id match {
      case 247 =>
      matchValRef(v519)
      case 265 =>
      matchRawRef(v519)
    }
    v520
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v521, v522) = node
    val v537 = v521.id match {
      case 256 =>
        val BindNode(v523, v524) = v522
        assert(v523.id == 257)
        val BindNode(v525, v526) = v524
        val v536 = v525.id match {
        case 258 =>
        v526.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString
        case 259 =>
          val BindNode(v527, v528) = v526
          assert(v527.id == 260)
          val v529 = v528.asInstanceOf[SequenceNode].children.head
          val BindNode(v530, v531) = v529
          assert(v530.id == 261)
          val v532 = v528.asInstanceOf[SequenceNode].children(1)
          val v533 = unrollRepeat0(v532).map { elem =>
          val BindNode(v534, v535) = elem
          assert(v534.id == 264)
          v535.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v531.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v533.map(x => x.toString).mkString("")
      }
        v536
    }
    v537
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v538, v539) = node
    val v560 = v538.id match {
      case 44 =>
        val v540 = v539.asInstanceOf[SequenceNode].children.head
        val BindNode(v541, v542) = v540
        assert(v541.id == 45)
        val v543 = v539.asInstanceOf[SequenceNode].children(4)
        val BindNode(v544, v545) = v543
        assert(v544.id == 169)
        val BindNode(v546, v547) = v545
        assert(v546.id == 170)
        val v548 = v547.asInstanceOf[SequenceNode].children.head
        val BindNode(v549, v550) = v548
        assert(v549.id == 171)
        val v551 = v547.asInstanceOf[SequenceNode].children(1)
        val v552 = unrollRepeat0(v551).map { elem =>
        val BindNode(v553, v554) = elem
        assert(v553.id == 353)
        val BindNode(v555, v556) = v554
        assert(v555.id == 354)
        val v557 = v556.asInstanceOf[SequenceNode].children(3)
        val BindNode(v558, v559) = v557
        assert(v558.id == 171)
        matchRHS(v559)
        }
        Rule(matchLHS(v542), List(matchRHS(v550)) ++ v552)(nextId(), v539)
    }
    v560
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v561, v562) = node
    val v575 = v561.id match {
      case 173 =>
        val v563 = v562.asInstanceOf[SequenceNode].children.head
        val BindNode(v564, v565) = v563
        assert(v564.id == 174)
        val v566 = v562.asInstanceOf[SequenceNode].children(1)
        val v567 = unrollRepeat0(v566).map { elem =>
        val BindNode(v568, v569) = elem
        assert(v568.id == 349)
        val BindNode(v570, v571) = v569
        assert(v570.id == 350)
        val v572 = v571.asInstanceOf[SequenceNode].children(1)
        val BindNode(v573, v574) = v572
        assert(v573.id == 174)
        matchElem(v574)
        }
        Sequence(List(matchElem(v565)) ++ v567)(nextId(), v562)
    }
    v575
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v576, v577) = node
    val v581 = v576.id match {
      case 332 =>
        val v578 = v577.asInstanceOf[SequenceNode].children(1)
        val BindNode(v579, v580) = v578
        assert(v579.id == 330)
        ShortenedEnumValue(matchEnumValueName(v580))(nextId(), v577)
    }
    v581
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v582, v583) = node
    val v584 = v582.id match {
      case 227 =>
      matchStringChar(v583)
    }
    v584
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v585, v586) = node
    val v592 = v585.id match {
      case 200 =>
      matchUnicodeChar(v586)
      case 228 =>
        val BindNode(v587, v588) = v586
        assert(v587.id == 20)
        CharAsIs(v588.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v586)
      case 230 =>
        val v589 = v586.asInstanceOf[SequenceNode].children(1)
        val BindNode(v590, v591) = v589
        assert(v590.id == 231)
        CharEscaped(v591.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v586)
    }
    v592
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v593, v594) = node
    val v599 = v593.id match {
      case 223 =>
        val v595 = v594.asInstanceOf[SequenceNode].children(1)
        val v596 = unrollRepeat0(v595).map { elem =>
        val BindNode(v597, v598) = elem
        assert(v597.id == 227)
        matchStringChar(v598)
        }
        StringSymbol(v596)(nextId(), v594)
    }
    v599
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v600, v601) = node
    val v602 = v600.id match {
      case 96 =>
      matchTypeName(v601)
      case 109 =>
      matchClassDef(v601)
      case 139 =>
      matchSuperDef(v601)
    }
    v602
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v603, v604) = node
    val v617 = v603.id match {
      case 149 =>
        val v605 = v604.asInstanceOf[SequenceNode].children.head
        val BindNode(v606, v607) = v605
        assert(v606.id == 150)
        val v608 = v604.asInstanceOf[SequenceNode].children(1)
        val v609 = unrollRepeat0(v608).map { elem =>
        val BindNode(v610, v611) = elem
        assert(v610.id == 153)
        val BindNode(v612, v613) = v611
        assert(v612.id == 154)
        val v614 = v613.asInstanceOf[SequenceNode].children(3)
        val BindNode(v615, v616) = v614
        assert(v615.id == 150)
        matchSubType(v616)
        }
        List(matchSubType(v607)) ++ v609
    }
    v617
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v618, v619) = node
    val v645 = v618.id match {
      case 140 =>
        val v620 = v619.asInstanceOf[SequenceNode].children.head
        val BindNode(v621, v622) = v620
        assert(v621.id == 96)
        val v623 = v619.asInstanceOf[SequenceNode].children(4)
        val BindNode(v624, v625) = v623
        assert(v624.id == 145)
        val BindNode(v626, v627) = v625
        val v633 = v626.id match {
        case 41 =>
        None
        case 146 =>
          val BindNode(v628, v629) = v627
          assert(v628.id == 147)
          val v630 = v629.asInstanceOf[SequenceNode].children(1)
          val BindNode(v631, v632) = v630
          assert(v631.id == 148)
          Some(matchSubTypes(v632))
      }
        val v634 = v619.asInstanceOf[SequenceNode].children(1)
        val BindNode(v635, v636) = v634
        assert(v635.id == 141)
        val BindNode(v637, v638) = v636
        val v644 = v637.id match {
        case 41 =>
        None
        case 142 =>
          val BindNode(v639, v640) = v638
          assert(v639.id == 143)
          val v641 = v640.asInstanceOf[SequenceNode].children(1)
          val BindNode(v642, v643) = v641
          assert(v642.id == 111)
          Some(matchSuperTypes(v643))
      }
        SuperDef(matchTypeName(v622), v633, v644)(nextId(), v619)
    }
    v645
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v646, v647) = node
    val v669 = v646.id match {
      case 112 =>
        val v649 = v647.asInstanceOf[SequenceNode].children(2)
        val BindNode(v650, v651) = v649
        assert(v650.id == 114)
        val BindNode(v652, v653) = v651
        val v668 = v652.id match {
        case 41 =>
        None
        case 115 =>
          val BindNode(v654, v655) = v653
          assert(v654.id == 116)
          val v656 = v655.asInstanceOf[SequenceNode].children.head
          val BindNode(v657, v658) = v656
          assert(v657.id == 96)
          val v659 = v655.asInstanceOf[SequenceNode].children(1)
          val v660 = unrollRepeat0(v659).map { elem =>
          val BindNode(v661, v662) = elem
          assert(v661.id == 119)
          val BindNode(v663, v664) = v662
          assert(v663.id == 120)
          val v665 = v664.asInstanceOf[SequenceNode].children(3)
          val BindNode(v666, v667) = v665
          assert(v666.id == 96)
          matchTypeName(v667)
          }
          Some(List(matchTypeName(v658)) ++ v660)
      }
        val v648 = v668
        if (v648.isDefined) v648.get else List()
    }
    v669
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v670, v671) = node
    val v672 = v670.id match {
      case 176 =>
      matchJoinSymbol(v671)
    }
    v672
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v673, v674) = node
    val v678 = v673.id match {
      case 193 =>
        val v675 = v674.asInstanceOf[SequenceNode].children(1)
        val BindNode(v676, v677) = v675
        assert(v676.id == 195)
        matchTerminalChar(v677)
      case 203 =>
      AnyTerminal()(nextId(), v674)
    }
    v678
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v679, v680) = node
    val v686 = v679.id match {
      case 196 =>
        val BindNode(v681, v682) = v680
        assert(v681.id == 20)
        CharAsIs(v682.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v680)
      case 198 =>
        val v683 = v680.asInstanceOf[SequenceNode].children(1)
        val BindNode(v684, v685) = v683
        assert(v684.id == 199)
        CharEscaped(v685.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v680)
      case 200 =>
      matchUnicodeChar(v680)
    }
    v686
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v687, v688) = node
    val v699 = v687.id match {
      case 205 =>
        val v689 = v688.asInstanceOf[SequenceNode].children(1)
        val BindNode(v690, v691) = v689
        assert(v690.id == 206)
        val v692 = v688.asInstanceOf[SequenceNode].children(2)
        val v693 = unrollRepeat1(v692).map { elem =>
        val BindNode(v694, v695) = elem
        assert(v694.id == 206)
        matchTerminalChoiceElem(v695)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v691)) ++ v693)(nextId(), v688)
      case 221 =>
        val v696 = v688.asInstanceOf[SequenceNode].children(1)
        val BindNode(v697, v698) = v696
        assert(v697.id == 212)
        TerminalChoice(List(matchTerminalChoiceRange(v698)))(nextId(), v688)
    }
    v699
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v700, v701) = node
    val v707 = v700.id match {
      case 200 =>
      matchUnicodeChar(v701)
      case 208 =>
        val BindNode(v702, v703) = v701
        assert(v702.id == 20)
        CharAsIs(v703.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v701)
      case 210 =>
        val v704 = v701.asInstanceOf[SequenceNode].children(1)
        val BindNode(v705, v706) = v704
        assert(v705.id == 211)
        CharEscaped(v706.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v701)
    }
    v707
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v708, v709) = node
    val v710 = v708.id match {
      case 207 =>
      matchTerminalChoiceChar(v709)
      case 212 =>
      matchTerminalChoiceRange(v709)
      case 214 =>
      matchTerminalUnicodeCategory(v709)
    }
    v710
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v711, v712) = node
    val v719 = v711.id match {
      case 213 =>
        val v713 = v712.asInstanceOf[SequenceNode].children.head
        val BindNode(v714, v715) = v713
        assert(v714.id == 207)
        val v716 = v712.asInstanceOf[SequenceNode].children(2)
        val BindNode(v717, v718) = v716
        assert(v717.id == 207)
        TerminalChoiceRange(matchTerminalChoiceChar(v715), matchTerminalChoiceChar(v718))(nextId(), v712)
    }
    v719
  }

  def matchTerminalUnicodeCategory(node: Node): TerminalUnicodeCategory = {
    val BindNode(v720, v721) = node
    val v728 = v720.id match {
      case 215 =>
        val v722 = v721.asInstanceOf[SequenceNode].children(1)
        val BindNode(v723, v724) = v722
        assert(v723.id == 216)
        val v725 = v721.asInstanceOf[SequenceNode].children(2)
        val BindNode(v726, v727) = v725
        assert(v726.id == 217)
        TerminalUnicodeCategory(v724.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v727.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v721)
    }
    v728
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v729, v730) = node
    val v748 = v729.id match {
      case 274 =>
        val v731 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v732, v733) = v731
        assert(v732.id == 275)
        val v734 = v730.asInstanceOf[SequenceNode].children(4)
        val BindNode(v735, v736) = v734
        assert(v735.id == 345)
        val BindNode(v737, v738) = v736
        assert(v737.id == 346)
        val BindNode(v739, v740) = v738
        assert(v739.id == 273)
        val v741 = v730.asInstanceOf[SequenceNode].children(8)
        val BindNode(v742, v743) = v741
        assert(v742.id == 345)
        val BindNode(v744, v745) = v743
        assert(v744.id == 346)
        val BindNode(v746, v747) = v745
        assert(v746.id == 273)
        TernaryOp(matchBoolOrExpr(v733), matchTernaryExpr(v740), matchTernaryExpr(v747))(nextId(), v730)
      case 275 =>
      matchBoolOrExpr(v730)
    }
    v748
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v749, v750) = node
    val v751 = v749.id match {
      case 109 =>
      matchClassDef(v750)
      case 139 =>
      matchSuperDef(v750)
      case 156 =>
      matchEnumTypeDef(v750)
    }
    v751
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v752, v753) = node
    val v768 = v752.id match {
      case 94 =>
        val v754 = v753.asInstanceOf[SequenceNode].children.head
        val BindNode(v755, v756) = v754
        assert(v755.id == 95)
        val v757 = v753.asInstanceOf[SequenceNode].children(1)
        val BindNode(v758, v759) = v757
        assert(v758.id == 164)
        val BindNode(v760, v761) = v759
        val v767 = v760.id match {
        case 41 =>
        None
        case 165 =>
          val BindNode(v762, v763) = v761
          assert(v762.id == 166)
          val v764 = v763.asInstanceOf[SequenceNode].children(1)
          val BindNode(v765, v766) = v764
          assert(v765.id == 167)
          Some(v766.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      }
        TypeDesc(matchNonNullTypeDesc(v756), v767.isDefined)(nextId(), v753)
    }
    v768
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v769, v770) = node
    val v774 = v769.id match {
      case 49 =>
      TypeName(matchIdNoKeyword(v770))(nextId(), v770)
      case 87 =>
        val v771 = v770.asInstanceOf[SequenceNode].children(1)
        val BindNode(v772, v773) = v771
        assert(v772.id == 51)
        TypeName(matchId(v773))(nextId(), v770)
    }
    v774
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v775, v776) = node
    val v780 = v775.id match {
      case 49 =>
      TypeOrFuncName(matchIdNoKeyword(v776))(nextId(), v776)
      case 87 =>
        val v777 = v776.asInstanceOf[SequenceNode].children(1)
        val BindNode(v778, v779) = v777
        assert(v778.id == 51)
        TypeOrFuncName(matchId(v779))(nextId(), v776)
    }
    v780
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v781, v782) = node
    val v795 = v781.id match {
      case 201 =>
        val v783 = v782.asInstanceOf[SequenceNode].children(2)
        val BindNode(v784, v785) = v783
        assert(v784.id == 202)
        val v786 = v782.asInstanceOf[SequenceNode].children(3)
        val BindNode(v787, v788) = v786
        assert(v787.id == 202)
        val v789 = v782.asInstanceOf[SequenceNode].children(4)
        val BindNode(v790, v791) = v789
        assert(v790.id == 202)
        val v792 = v782.asInstanceOf[SequenceNode].children(5)
        val BindNode(v793, v794) = v792
        assert(v793.id == 202)
        CharUnicode(List(v785.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v788.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v791.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v794.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char))(nextId(), v782)
    }
    v795
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v796, v797) = node
    val v807 = v796.id match {
      case 248 =>
        val v798 = v797.asInstanceOf[SequenceNode].children(2)
        val BindNode(v799, v800) = v798
        assert(v799.id == 255)
        val v801 = v797.asInstanceOf[SequenceNode].children(1)
        val BindNode(v802, v803) = v801
        assert(v802.id == 250)
        val BindNode(v804, v805) = v803
        val v806 = v804.id match {
        case 41 =>
        None
        case 251 =>
        Some(matchCondSymPath(v805))
      }
        ValRef(matchRefIdx(v800), v806)(nextId(), v797)
    }
    v807
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v808, v809) = node
    val v810 = v808.id match {
      case 60 =>
      BooleanType()(nextId(), v809)
      case 68 =>
      CharType()(nextId(), v809)
      case 73 =>
      StringType()(nextId(), v809)
    }
    v810
  }
}
