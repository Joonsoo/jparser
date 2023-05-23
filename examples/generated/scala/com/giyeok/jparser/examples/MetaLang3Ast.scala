package com.giyeok.jparser.examples

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

  sealed trait WithIdAndParseNode {
    val id: Int;
    val parseNode: Node
  }

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

  object CondSymDir extends Enumeration {
    val BODY, COND = Value
  }

  object KeyWord extends Enumeration {
    val BOOLEAN, CHAR, FALSE, NULL, STRING, TRUE = Value
  }

  object Op extends Enumeration {
    val ADD, AND, EQ, NE, OR = Value
  }

  object PreOp extends Enumeration {
    val NOT = Value
  }


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
      case 283 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 188)
        val v6 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v7, v8) = v6
        assert(v7.id == 284)
        val v9 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v10, v11) = v9
        assert(v10.id == 282)
        BinOp(Op.ADD, matchPrefixNotExpr(v8), matchAdditiveExpr(v11))(nextId(), v2)
      case 284 =>
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
    val v48 = v16.id match {
      case 315 =>
        val BindNode(v19, v20) = v17
        val v47 = v19.id match {
          case 41 =>
            None
          case 308 =>
            val v21 = v17.asInstanceOf[SequenceNode].children(2)
            val BindNode(v22, v23) = v21
            assert(v22.id == 307)
            val BindNode(v24, v25) = v23
            assert(v24.id == 308)
            val BindNode(v26, v27) = v25
            assert(v26.id == 309)
            val v28 = v27.asInstanceOf[SequenceNode].children.head
            val BindNode(v29, v30) = v28
            assert(v29.id == 270)
            val v31 = v17.asInstanceOf[SequenceNode].children(2)
            val BindNode(v32, v33) = v31
            assert(v32.id == 307)
            val BindNode(v34, v35) = v33
            assert(v34.id == 308)
            val BindNode(v36, v37) = v35
            assert(v36.id == 309)
            val v38 = v37.asInstanceOf[SequenceNode].children(1)
            val v39 = unrollRepeat0(v38).map { elem =>
              val BindNode(v40, v41) = elem
              assert(v40.id == 312)
              val BindNode(v42, v43) = v41
              assert(v42.id == 313)
              val v44 = v43.asInstanceOf[SequenceNode].children(3)
              val BindNode(v45, v46) = v44
              assert(v45.id == 270)
              matchPExpr(v46)
            }
            Some(List(matchPExpr(v30)) ++ v39)
        }
        val v18 = v47
        ArrayExpr(if (v18.isDefined) v18.get else List())(nextId(), v17)
    }
    v48
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v49, v50) = node
    val v54 = v49.id match {
      case 245 =>
        matchRef(v50)
      case 287 =>
        matchBindExpr(v50)
      case 290 =>
        matchNamedConstructExpr(v50)
      case 302 =>
        matchFuncCallOrConstructExpr(v50)
      case 314 =>
        matchArrayExpr(v50)
      case 316 =>
        matchLiteral(v50)
      case 324 =>
        matchEnumValue(v50)
      case 332 =>
        val v51 = v50.asInstanceOf[SequenceNode].children(2)
        val BindNode(v52, v53) = v51
        assert(v52.id == 270)
        ExprParen(matchPExpr(v53))(nextId(), v50)
    }
    v54
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v55, v56) = node
    val v60 = v55.id match {
      case 47 =>
        matchNonterminal(v56)
      case 190 =>
        matchTerminal(v56)
      case 202 =>
        matchTerminalChoice(v56)
      case 221 =>
        matchStringSymbol(v56)
      case 231 =>
        val v57 = v56.asInstanceOf[SequenceNode].children(2)
        val BindNode(v58, v59) = v57
        assert(v58.id == 232)
        matchInPlaceChoices(v59)
      case 239 =>
        matchLongest(v56)
      case 241 =>
        matchEmptySequence(v56)
    }
    v60
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v61, v62) = node
    val v75 = v61.id match {
      case 177 =>
        val v63 = v62.asInstanceOf[SequenceNode].children.head
        val BindNode(v64, v65) = v63
        assert(v64.id == 176)
        val v66 = v62.asInstanceOf[SequenceNode].children(4)
        val BindNode(v67, v68) = v66
        assert(v67.id == 179)
        JoinSymbol(matchBinSymbol(v65), matchPreUnSymbol(v68))(nextId(), v62)
      case 179 =>
        matchPreUnSymbol(v62)
      case 243 =>
        val v69 = v62.asInstanceOf[SequenceNode].children.head
        val BindNode(v70, v71) = v69
        assert(v70.id == 176)
        val v72 = v62.asInstanceOf[SequenceNode].children(4)
        val BindNode(v73, v74) = v72
        assert(v73.id == 179)
        ExceptSymbol(matchBinSymbol(v71), matchPreUnSymbol(v74))(nextId(), v62)
    }
    v75
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v76, v77) = node
    val v84 = v76.id match {
      case 288 =>
        val v78 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v79, v80) = v78
        assert(v79.id == 246)
        val v81 = v77.asInstanceOf[SequenceNode].children(1)
        val BindNode(v82, v83) = v81
        assert(v82.id == 289)
        BindExpr(matchValRef(v80), matchBinderExpr(v83))(nextId(), v77)
    }
    v84
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v85, v86) = node
    val v87 = v85.id match {
      case 245 =>
        matchRef(v86)
      case 268 =>
        matchPExprBlock(v86)
      case 287 =>
        matchBindExpr(v86)
    }
    v87
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v88, v89) = node
    val v96 = v88.id match {
      case 277 =>
        val v90 = v89.asInstanceOf[SequenceNode].children.head
        val BindNode(v91, v92) = v90
        assert(v91.id == 278)
        val v93 = v89.asInstanceOf[SequenceNode].children(4)
        val BindNode(v94, v95) = v93
        assert(v94.id == 276)
        BinOp(Op.OR, matchBoolEqExpr(v92), matchBoolAndExpr(v95))(nextId(), v89)
      case 278 =>
        matchBoolEqExpr(v89)
    }
    v96
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v97, v98) = node
    val v111 = v97.id match {
      case 279 =>
        val v99 = v98.asInstanceOf[SequenceNode].children(2)
        val BindNode(v100, v101) = v99
        assert(v100.id == 335)
        val BindNode(v102, v103) = v101
        val v104 = v102.id match {
          case 336 =>
            Op.EQ
          case 338 =>
            Op.NE
        }
        val v105 = v98.asInstanceOf[SequenceNode].children.head
        val BindNode(v106, v107) = v105
        assert(v106.id == 280)
        val v108 = v98.asInstanceOf[SequenceNode].children(4)
        val BindNode(v109, v110) = v108
        assert(v109.id == 278)
        BinOp(v104, matchElvisExpr(v107), matchBoolEqExpr(v110))(nextId(), v98)
      case 280 =>
        matchElvisExpr(v98)
    }
    v111
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v112, v113) = node
    val v120 = v112.id match {
      case 275 =>
        val v114 = v113.asInstanceOf[SequenceNode].children.head
        val BindNode(v115, v116) = v114
        assert(v115.id == 276)
        val v117 = v113.asInstanceOf[SequenceNode].children(4)
        val BindNode(v118, v119) = v117
        assert(v118.id == 274)
        BinOp(Op.AND, matchBoolAndExpr(v116), matchBoolOrExpr(v119))(nextId(), v113)
      case 276 =>
        matchBoolAndExpr(v113)
    }
    v120
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v121, v122) = node
    val v144 = v121.id match {
      case 306 =>
        val v124 = v122.asInstanceOf[SequenceNode].children(2)
        val BindNode(v125, v126) = v124
        assert(v125.id == 307)
        val BindNode(v127, v128) = v126
        val v143 = v127.id match {
          case 41 =>
            None
          case 308 =>
            val BindNode(v129, v130) = v128
            assert(v129.id == 309)
            val v131 = v130.asInstanceOf[SequenceNode].children.head
            val BindNode(v132, v133) = v131
            assert(v132.id == 270)
            val v134 = v130.asInstanceOf[SequenceNode].children(1)
            val v135 = unrollRepeat0(v134).map { elem =>
              val BindNode(v136, v137) = elem
              assert(v136.id == 312)
              val BindNode(v138, v139) = v137
              assert(v138.id == 313)
              val v140 = v139.asInstanceOf[SequenceNode].children(3)
              val BindNode(v141, v142) = v140
              assert(v141.id == 270)
              matchPExpr(v142)
            }
            Some(List(matchPExpr(v133)) ++ v135)
        }
        val v123 = v143
        if (v123.isDefined) v123.get else List()
    }
    v144
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v145, v146) = node
    val v153 = v145.id match {
      case 328 =>
        val v147 = v146.asInstanceOf[SequenceNode].children.head
        val BindNode(v148, v149) = v147
        assert(v148.id == 105)
        val v150 = v146.asInstanceOf[SequenceNode].children(2)
        val BindNode(v151, v152) = v150
        assert(v151.id == 329)
        CanonicalEnumValue(matchEnumTypeName(v149), matchEnumValueName(v152))(nextId(), v146)
    }
    v153
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v154, v155) = node
    val v156 = v154.id match {
      case 193 =>
        matchTerminalChar(v155)
    }
    v156
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v157, v158) = node
    val v180 = v157.id match {
      case 110 =>
        val v159 = v158.asInstanceOf[SequenceNode].children.head
        val BindNode(v160, v161) = v159
        assert(v160.id == 96)
        val v162 = v158.asInstanceOf[SequenceNode].children(2)
        val BindNode(v163, v164) = v162
        assert(v163.id == 111)
        AbstractClassDef(matchTypeName(v161), matchSuperTypes(v164))(nextId(), v158)
      case 123 =>
        val v165 = v158.asInstanceOf[SequenceNode].children.head
        val BindNode(v166, v167) = v165
        assert(v166.id == 96)
        val v168 = v158.asInstanceOf[SequenceNode].children(2)
        val BindNode(v169, v170) = v168
        assert(v169.id == 124)
        ConcreteClassDef(matchTypeName(v167), None, matchClassParamsDef(v170))(nextId(), v158)
      case 138 =>
        val v171 = v158.asInstanceOf[SequenceNode].children.head
        val BindNode(v172, v173) = v171
        assert(v172.id == 96)
        val v174 = v158.asInstanceOf[SequenceNode].children(2)
        val BindNode(v175, v176) = v174
        assert(v175.id == 111)
        val v177 = v158.asInstanceOf[SequenceNode].children(4)
        val BindNode(v178, v179) = v177
        assert(v178.id == 124)
        ConcreteClassDef(matchTypeName(v173), Some(matchSuperTypes(v176)), matchClassParamsDef(v179))(nextId(), v158)
    }
    v180
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v181, v182) = node
    val v197 = v181.id match {
      case 131 =>
        val v183 = v182.asInstanceOf[SequenceNode].children.head
        val BindNode(v184, v185) = v183
        assert(v184.id == 132)
        val v186 = v182.asInstanceOf[SequenceNode].children(1)
        val BindNode(v187, v188) = v186
        assert(v187.id == 89)
        val BindNode(v189, v190) = v188
        val v196 = v189.id match {
          case 41 =>
            None
          case 90 =>
            val BindNode(v191, v192) = v190
            assert(v191.id == 91)
            val v193 = v192.asInstanceOf[SequenceNode].children(3)
            val BindNode(v194, v195) = v193
            assert(v194.id == 93)
            Some(matchTypeDesc(v195))
        }
        ClassParamDef(matchParamName(v185), v196)(nextId(), v182)
    }
    v197
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v198, v199) = node
    val v221 = v198.id match {
      case 125 =>
        val v201 = v199.asInstanceOf[SequenceNode].children(2)
        val BindNode(v202, v203) = v201
        assert(v202.id == 127)
        val BindNode(v204, v205) = v203
        val v220 = v204.id match {
          case 41 =>
            None
          case 128 =>
            val BindNode(v206, v207) = v205
            assert(v206.id == 129)
            val v208 = v207.asInstanceOf[SequenceNode].children.head
            val BindNode(v209, v210) = v208
            assert(v209.id == 130)
            val v211 = v207.asInstanceOf[SequenceNode].children(1)
            val v212 = unrollRepeat0(v211).map { elem =>
              val BindNode(v213, v214) = elem
              assert(v213.id == 135)
              val BindNode(v215, v216) = v214
              assert(v215.id == 136)
              val v217 = v216.asInstanceOf[SequenceNode].children(3)
              val BindNode(v218, v219) = v217
              assert(v218.id == 130)
              matchClassParamDef(v219)
            }
            Some(List(matchClassParamDef(v210)) ++ v212)
        }
        val v200 = v220
        if (v200.isDefined) v200.get else List()
    }
    v221
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v222, v223) = node
    val v230 = v222.id match {
      case 251 =>
        val v224 = unrollRepeat1NoUnbind(253, 252, v223).map { elem =>
          val BindNode(v225, v226) = elem
          assert(v225.id == 252)
          val BindNode(v227, v228) = v226
          val v229 = v227.id match {
            case 113 =>
              CondSymDir.BODY
            case 122 =>
              CondSymDir.COND
          }
          v229
        }
        v224
    }
    v230
  }

  def matchDef(node: Node): Def = {
    val BindNode(v231, v232) = node
    val v233 = v231.id match {
      case 43 =>
        matchRule(v232)
      case 108 =>
        matchTypeDef(v232)
    }
    v233
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v234, v235) = node
    val v236 = v234.id match {
      case 175 =>
        matchSymbol(v235)
      case 244 =>
        matchProcessor(v235)
    }
    v236
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v237, v238) = node
    val v245 = v237.id match {
      case 281 =>
        val v239 = v238.asInstanceOf[SequenceNode].children.head
        val BindNode(v240, v241) = v239
        assert(v240.id == 282)
        val v242 = v238.asInstanceOf[SequenceNode].children(4)
        val BindNode(v243, v244) = v242
        assert(v243.id == 280)
        ElvisOp(matchAdditiveExpr(v241), matchElvisExpr(v244))(nextId(), v238)
      case 282 =>
        matchAdditiveExpr(v238)
    }
    v245
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v246, v247) = node
    val v248 = v246.id match {
      case 242 =>
        EmptySeq()(nextId(), v247)
    }
    v248
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v249, v250) = node
    val v271 = v249.id match {
      case 157 =>
        val v251 = v250.asInstanceOf[SequenceNode].children.head
        val BindNode(v252, v253) = v251
        assert(v252.id == 105)
        val v254 = v250.asInstanceOf[SequenceNode].children(4)
        val BindNode(v255, v256) = v254
        assert(v255.id == 158)
        val BindNode(v257, v258) = v256
        assert(v257.id == 159)
        val v259 = v258.asInstanceOf[SequenceNode].children.head
        val BindNode(v260, v261) = v259
        assert(v260.id == 51)
        val v262 = v258.asInstanceOf[SequenceNode].children(1)
        val v263 = unrollRepeat0(v262).map { elem =>
          val BindNode(v264, v265) = elem
          assert(v264.id == 162)
          val BindNode(v266, v267) = v265
          assert(v266.id == 163)
          val v268 = v267.asInstanceOf[SequenceNode].children(3)
          val BindNode(v269, v270) = v268
          assert(v269.id == 51)
          matchId(v270)
        }
        EnumTypeDef(matchEnumTypeName(v253), List(matchId(v261)) ++ v263)(nextId(), v250)
    }
    v271
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v272, v273) = node
    val v277 = v272.id match {
      case 106 =>
        val v274 = v273.asInstanceOf[SequenceNode].children(1)
        val BindNode(v275, v276) = v274
        assert(v275.id == 51)
        EnumTypeName(matchId(v276))(nextId(), v273)
    }
    v277
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v278, v279) = node
    val v285 = v278.id match {
      case 325 =>
        val BindNode(v280, v281) = v279
        assert(v280.id == 326)
        val BindNode(v282, v283) = v281
        val v284 = v282.id match {
          case 327 =>
            matchCanonicalEnumValue(v283)
          case 330 =>
            matchShortenedEnumValue(v283)
        }
        v284
    }
    v285
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v286, v287) = node
    val v288 = v286.id match {
      case 51 =>
        EnumValueName(matchId(v287))(nextId(), v287)
    }
    v288
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v289, v290) = node
    val v297 = v289.id match {
      case 303 =>
        val v291 = v290.asInstanceOf[SequenceNode].children.head
        val BindNode(v292, v293) = v291
        assert(v292.id == 304)
        val v294 = v290.asInstanceOf[SequenceNode].children(2)
        val BindNode(v295, v296) = v294
        assert(v295.id == 305)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v293), matchCallParams(v296))(nextId(), v290)
    }
    v297
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v298, v299) = node
    val v312 = v298.id match {
      case 3 =>
        val v300 = v299.asInstanceOf[SequenceNode].children(1)
        val BindNode(v301, v302) = v300
        assert(v301.id == 42)
        val v303 = v299.asInstanceOf[SequenceNode].children(2)
        val v304 = unrollRepeat0(v303).map { elem =>
          val BindNode(v305, v306) = elem
          assert(v305.id == 356)
          val BindNode(v307, v308) = v306
          assert(v307.id == 357)
          val v309 = v308.asInstanceOf[SequenceNode].children(1)
          val BindNode(v310, v311) = v309
          assert(v310.id == 42)
          matchDef(v311)
        }
        Grammar(List(matchDef(v302)) ++ v304)(nextId(), v299)
    }
    v312
  }

  def matchId(node: Node): String = {
    val BindNode(v313, v314) = node
    val v326 = v313.id match {
      case 52 =>
        val BindNode(v315, v316) = v314
        assert(v315.id == 53)
        val BindNode(v317, v318) = v316
        assert(v317.id == 54)
        val v319 = v318.asInstanceOf[SequenceNode].children.head
        val BindNode(v320, v321) = v319
        assert(v320.id == 55)
        val v322 = v318.asInstanceOf[SequenceNode].children(1)
        val v323 = unrollRepeat0(v322).map { elem =>
          val BindNode(v324, v325) = elem
          assert(v324.id == 58)
          v325.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v321.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v323.map(x => x.toString).mkString("")
    }
    v326
  }

  def matchIdNoKeyword(node: Node): String = {
    val BindNode(v327, v328) = node
    val v331 = v327.id match {
      case 50 =>
        val BindNode(v329, v330) = v328
        assert(v329.id == 51)
        matchId(v330)
    }
    v331
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v332, v333) = node
    val v346 = v332.id match {
      case 233 =>
        val v334 = v333.asInstanceOf[SequenceNode].children.head
        val BindNode(v335, v336) = v334
        assert(v335.id == 172)
        val v337 = v333.asInstanceOf[SequenceNode].children(1)
        val v338 = unrollRepeat0(v337).map { elem =>
          val BindNode(v339, v340) = elem
          assert(v339.id == 236)
          val BindNode(v341, v342) = v340
          assert(v341.id == 237)
          val v343 = v342.asInstanceOf[SequenceNode].children(3)
          val BindNode(v344, v345) = v343
          assert(v344.id == 172)
          matchSequence(v345)
        }
        InPlaceChoices(List(matchSequence(v336)) ++ v338)(nextId(), v333)
    }
    v346
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v347, v348) = node
    val v363 = v347.id match {
      case 46 =>
        val v349 = v348.asInstanceOf[SequenceNode].children.head
        val BindNode(v350, v351) = v349
        assert(v350.id == 47)
        val v352 = v348.asInstanceOf[SequenceNode].children(1)
        val BindNode(v353, v354) = v352
        assert(v353.id == 89)
        val BindNode(v355, v356) = v354
        val v362 = v355.id match {
          case 41 =>
            None
          case 90 =>
            val BindNode(v357, v358) = v356
            assert(v357.id == 91)
            val v359 = v358.asInstanceOf[SequenceNode].children(3)
            val BindNode(v360, v361) = v359
            assert(v360.id == 93)
            Some(matchTypeDesc(v361))
        }
        LHS(matchNonterminal(v351), v362)(nextId(), v348)
    }
    v363
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v364, v365) = node
    val v376 = v364.id match {
      case 85 =>
        NullLiteral()(nextId(), v365)
      case 317 =>
        val BindNode(v366, v367) = v365
        val v368 = v366.id match {
          case 79 =>
            true
          case 82 =>
            false
        }
        BoolLiteral(v368)(nextId(), v365)
      case 318 =>
        val v369 = v365.asInstanceOf[SequenceNode].children(1)
        val BindNode(v370, v371) = v369
        assert(v370.id == 319)
        CharLiteral(matchCharChar(v371))(nextId(), v365)
      case 320 =>
        val v372 = v365.asInstanceOf[SequenceNode].children(1)
        val v373 = unrollRepeat0(v372).map { elem =>
          val BindNode(v374, v375) = elem
          assert(v374.id == 323)
          matchStrChar(v375)
        }
        StrLiteral(v373)(nextId(), v365)
    }
    v376
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v377, v378) = node
    val v382 = v377.id match {
      case 240 =>
        val v379 = v378.asInstanceOf[SequenceNode].children(2)
        val BindNode(v380, v381) = v379
        assert(v380.id == 232)
        Longest(matchInPlaceChoices(v381))(nextId(), v378)
    }
    v382
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v383, v384) = node
    val v402 = v383.id match {
      case 291 =>
        val v385 = v384.asInstanceOf[SequenceNode].children.head
        val BindNode(v386, v387) = v385
        assert(v386.id == 96)
        val v388 = v384.asInstanceOf[SequenceNode].children(3)
        val BindNode(v389, v390) = v388
        assert(v389.id == 292)
        val v391 = v384.asInstanceOf[SequenceNode].children(1)
        val BindNode(v392, v393) = v391
        assert(v392.id == 141)
        val BindNode(v394, v395) = v393
        val v401 = v394.id match {
          case 41 =>
            None
          case 142 =>
            val BindNode(v396, v397) = v395
            assert(v396.id == 143)
            val v398 = v397.asInstanceOf[SequenceNode].children(1)
            val BindNode(v399, v400) = v398
            assert(v399.id == 111)
            Some(matchSuperTypes(v400))
        }
        NamedConstructExpr(matchTypeName(v387), matchNamedConstructParams(v390), v401)(nextId(), v384)
    }
    v402
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v403, v404) = node
    val v422 = v403.id match {
      case 293 =>
        val v405 = v404.asInstanceOf[SequenceNode].children(2)
        val BindNode(v406, v407) = v405
        assert(v406.id == 294)
        val BindNode(v408, v409) = v407
        assert(v408.id == 295)
        val v410 = v409.asInstanceOf[SequenceNode].children.head
        val BindNode(v411, v412) = v410
        assert(v411.id == 296)
        val v413 = v409.asInstanceOf[SequenceNode].children(1)
        val v414 = unrollRepeat0(v413).map { elem =>
          val BindNode(v415, v416) = elem
          assert(v415.id == 300)
          val BindNode(v417, v418) = v416
          assert(v417.id == 301)
          val v419 = v418.asInstanceOf[SequenceNode].children(3)
          val BindNode(v420, v421) = v419
          assert(v420.id == 296)
          matchNamedParam(v421)
        }
        List(matchNamedParam(v412)) ++ v414
    }
    v422
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v423, v424) = node
    val v442 = v423.id match {
      case 297 =>
        val v425 = v424.asInstanceOf[SequenceNode].children.head
        val BindNode(v426, v427) = v425
        assert(v426.id == 132)
        val v428 = v424.asInstanceOf[SequenceNode].children(1)
        val BindNode(v429, v430) = v428
        assert(v429.id == 89)
        val BindNode(v431, v432) = v430
        val v438 = v431.id match {
          case 41 =>
            None
          case 90 =>
            val BindNode(v433, v434) = v432
            assert(v433.id == 91)
            val v435 = v434.asInstanceOf[SequenceNode].children(3)
            val BindNode(v436, v437) = v435
            assert(v436.id == 93)
            Some(matchTypeDesc(v437))
        }
        val v439 = v424.asInstanceOf[SequenceNode].children(5)
        val BindNode(v440, v441) = v439
        assert(v440.id == 270)
        NamedParam(matchParamName(v427), v438, matchPExpr(v441))(nextId(), v424)
    }
    v442
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v443, v444) = node
    val v448 = v443.id match {
      case 96 =>
        matchTypeName(v444)
      case 97 =>
        val v445 = v444.asInstanceOf[SequenceNode].children(2)
        val BindNode(v446, v447) = v445
        assert(v446.id == 93)
        ArrayTypeDesc(matchTypeDesc(v447))(nextId(), v444)
      case 100 =>
        matchValueType(v444)
      case 101 =>
        matchAnyType(v444)
      case 105 =>
        matchEnumTypeName(v444)
      case 108 =>
        matchTypeDef(v444)
    }
    v448
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v449, v450) = node
    val v451 = v449.id match {
      case 48 =>
        Nonterminal(matchNonterminalName(v450))(nextId(), v450)
    }
    v451
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v452, v453) = node
    val v457 = v452.id match {
      case 49 =>
        NonterminalName(matchIdNoKeyword(v453))(nextId(), v453)
      case 87 =>
        val v454 = v453.asInstanceOf[SequenceNode].children(1)
        val BindNode(v455, v456) = v454
        assert(v455.id == 51)
        NonterminalName(matchId(v456))(nextId(), v453)
    }
    v457
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v458, v459) = node
    val v466 = v458.id match {
      case 271 =>
        val v460 = v459.asInstanceOf[SequenceNode].children.head
        val BindNode(v461, v462) = v460
        assert(v461.id == 272)
        val v463 = v459.asInstanceOf[SequenceNode].children(4)
        val BindNode(v464, v465) = v463
        assert(v464.id == 93)
        TypedPExpr(matchTernaryExpr(v462), matchTypeDesc(v465))(nextId(), v459)
      case 272 =>
        matchTernaryExpr(v459)
    }
    v466
  }

  def matchPExprBlock(node: Node): ProcessorBlock = {
    val BindNode(v467, v468) = node
    val v472 = v467.id match {
      case 269 =>
        val v469 = v468.asInstanceOf[SequenceNode].children(2)
        val BindNode(v470, v471) = v469
        assert(v470.id == 270)
        ProcessorBlock(matchPExpr(v471))(nextId(), v468)
    }
    v472
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v473, v474) = node
    val v478 = v473.id match {
      case 49 =>
        ParamName(matchIdNoKeyword(v474))(nextId(), v474)
      case 87 =>
        val v475 = v474.asInstanceOf[SequenceNode].children(1)
        val BindNode(v476, v477) = v475
        assert(v476.id == 51)
        ParamName(matchId(v477))(nextId(), v474)
    }
    v478
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v479, v480) = node
    val v490 = v479.id match {
      case 185 =>
        val v481 = v480.asInstanceOf[SequenceNode].children.head
        val BindNode(v482, v483) = v481
        assert(v482.id == 184)
        Optional(matchPostUnSymbol(v483))(nextId(), v480)
      case 186 =>
        val v484 = v480.asInstanceOf[SequenceNode].children.head
        val BindNode(v485, v486) = v484
        assert(v485.id == 184)
        RepeatFromZero(matchPostUnSymbol(v486))(nextId(), v480)
      case 187 =>
        val v487 = v480.asInstanceOf[SequenceNode].children.head
        val BindNode(v488, v489) = v487
        assert(v488.id == 184)
        RepeatFromOne(matchPostUnSymbol(v489))(nextId(), v480)
      case 189 =>
        matchAtomSymbol(v480)
    }
    v490
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v491, v492) = node
    val v499 = v491.id match {
      case 180 =>
        val v493 = v492.asInstanceOf[SequenceNode].children(2)
        val BindNode(v494, v495) = v493
        assert(v494.id == 179)
        FollowedBy(matchPreUnSymbol(v495))(nextId(), v492)
      case 182 =>
        val v496 = v492.asInstanceOf[SequenceNode].children(2)
        val BindNode(v497, v498) = v496
        assert(v497.id == 179)
        NotFollowedBy(matchPreUnSymbol(v498))(nextId(), v492)
      case 184 =>
        matchPostUnSymbol(v492)
    }
    v499
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v500, v501) = node
    val v505 = v500.id match {
      case 285 =>
        val v502 = v501.asInstanceOf[SequenceNode].children(2)
        val BindNode(v503, v504) = v502
        assert(v503.id == 284)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v504))(nextId(), v501)
      case 286 =>
        matchAtom(v501)
    }
    v505
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v506, v507) = node
    val v508 = v506.id match {
      case 245 =>
        matchRef(v507)
      case 268 =>
        matchPExprBlock(v507)
    }
    v508
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v509, v510) = node
    val v511 = v509.id match {
      case 172 =>
        matchSequence(v510)
    }
    v511
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v512, v513) = node
    val v523 = v512.id match {
      case 265 =>
        val v514 = v513.asInstanceOf[SequenceNode].children(2)
        val BindNode(v515, v516) = v514
        assert(v515.id == 254)
        val v517 = v513.asInstanceOf[SequenceNode].children(1)
        val BindNode(v518, v519) = v517
        assert(v518.id == 249)
        val BindNode(v520, v521) = v519
        val v522 = v520.id match {
          case 41 =>
            None
          case 250 =>
            Some(matchCondSymPath(v521))
        }
        RawRef(matchRefIdx(v516), v522)(nextId(), v513)
    }
    v523
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v524, v525) = node
    val v526 = v524.id match {
      case 246 =>
        matchValRef(v525)
      case 264 =>
        matchRawRef(v525)
    }
    v526
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v527, v528) = node
    val v543 = v527.id match {
      case 255 =>
        val BindNode(v529, v530) = v528
        assert(v529.id == 256)
        val BindNode(v531, v532) = v530
        val v542 = v531.id match {
          case 257 =>
            v532.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString
          case 258 =>
            val BindNode(v533, v534) = v532
            assert(v533.id == 259)
            val v535 = v534.asInstanceOf[SequenceNode].children.head
            val BindNode(v536, v537) = v535
            assert(v536.id == 260)
            val v538 = v534.asInstanceOf[SequenceNode].children(1)
            val v539 = unrollRepeat0(v538).map { elem =>
              val BindNode(v540, v541) = elem
              assert(v540.id == 263)
              v541.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            v537.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v539.map(x => x.toString).mkString("")
        }
        v542
    }
    v543
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v544, v545) = node
    val v566 = v544.id match {
      case 44 =>
        val v546 = v545.asInstanceOf[SequenceNode].children.head
        val BindNode(v547, v548) = v546
        assert(v547.id == 45)
        val v549 = v545.asInstanceOf[SequenceNode].children(4)
        val BindNode(v550, v551) = v549
        assert(v550.id == 169)
        val BindNode(v552, v553) = v551
        assert(v552.id == 170)
        val v554 = v553.asInstanceOf[SequenceNode].children.head
        val BindNode(v555, v556) = v554
        assert(v555.id == 171)
        val v557 = v553.asInstanceOf[SequenceNode].children(1)
        val v558 = unrollRepeat0(v557).map { elem =>
          val BindNode(v559, v560) = elem
          assert(v559.id == 352)
          val BindNode(v561, v562) = v560
          assert(v561.id == 353)
          val v563 = v562.asInstanceOf[SequenceNode].children(3)
          val BindNode(v564, v565) = v563
          assert(v564.id == 171)
          matchRHS(v565)
        }
        Rule(matchLHS(v548), List(matchRHS(v556)) ++ v558)(nextId(), v545)
    }
    v566
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v567, v568) = node
    val v581 = v567.id match {
      case 173 =>
        val v569 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v570, v571) = v569
        assert(v570.id == 174)
        val v572 = v568.asInstanceOf[SequenceNode].children(1)
        val v573 = unrollRepeat0(v572).map { elem =>
          val BindNode(v574, v575) = elem
          assert(v574.id == 348)
          val BindNode(v576, v577) = v575
          assert(v576.id == 349)
          val v578 = v577.asInstanceOf[SequenceNode].children(1)
          val BindNode(v579, v580) = v578
          assert(v579.id == 174)
          matchElem(v580)
        }
        Sequence(List(matchElem(v571)) ++ v573)(nextId(), v568)
    }
    v581
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v582, v583) = node
    val v587 = v582.id match {
      case 331 =>
        val v584 = v583.asInstanceOf[SequenceNode].children(1)
        val BindNode(v585, v586) = v584
        assert(v585.id == 329)
        ShortenedEnumValue(matchEnumValueName(v586))(nextId(), v583)
    }
    v587
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v588, v589) = node
    val v590 = v588.id match {
      case 226 =>
        matchStringChar(v589)
    }
    v590
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v591, v592) = node
    val v598 = v591.id match {
      case 198 =>
        matchUnicodeChar(v592)
      case 227 =>
        val BindNode(v593, v594) = v592
        assert(v593.id == 20)
        CharAsIs(v594.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v592)
      case 229 =>
        val v595 = v592.asInstanceOf[SequenceNode].children(1)
        val BindNode(v596, v597) = v595
        assert(v596.id == 230)
        CharEscaped(v597.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v592)
    }
    v598
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v599, v600) = node
    val v605 = v599.id match {
      case 222 =>
        val v601 = v600.asInstanceOf[SequenceNode].children(1)
        val v602 = unrollRepeat0(v601).map { elem =>
          val BindNode(v603, v604) = elem
          assert(v603.id == 226)
          matchStringChar(v604)
        }
        StringSymbol(v602)(nextId(), v600)
    }
    v605
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v606, v607) = node
    val v608 = v606.id match {
      case 96 =>
        matchTypeName(v607)
      case 109 =>
        matchClassDef(v607)
      case 139 =>
        matchSuperDef(v607)
    }
    v608
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v609, v610) = node
    val v623 = v609.id match {
      case 149 =>
        val v611 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v612, v613) = v611
        assert(v612.id == 150)
        val v614 = v610.asInstanceOf[SequenceNode].children(1)
        val v615 = unrollRepeat0(v614).map { elem =>
          val BindNode(v616, v617) = elem
          assert(v616.id == 153)
          val BindNode(v618, v619) = v617
          assert(v618.id == 154)
          val v620 = v619.asInstanceOf[SequenceNode].children(3)
          val BindNode(v621, v622) = v620
          assert(v621.id == 150)
          matchSubType(v622)
        }
        List(matchSubType(v613)) ++ v615
    }
    v623
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v624, v625) = node
    val v651 = v624.id match {
      case 140 =>
        val v626 = v625.asInstanceOf[SequenceNode].children.head
        val BindNode(v627, v628) = v626
        assert(v627.id == 96)
        val v629 = v625.asInstanceOf[SequenceNode].children(4)
        val BindNode(v630, v631) = v629
        assert(v630.id == 145)
        val BindNode(v632, v633) = v631
        val v639 = v632.id match {
          case 41 =>
            None
          case 146 =>
            val BindNode(v634, v635) = v633
            assert(v634.id == 147)
            val v636 = v635.asInstanceOf[SequenceNode].children(1)
            val BindNode(v637, v638) = v636
            assert(v637.id == 148)
            Some(matchSubTypes(v638))
        }
        val v640 = v625.asInstanceOf[SequenceNode].children(1)
        val BindNode(v641, v642) = v640
        assert(v641.id == 141)
        val BindNode(v643, v644) = v642
        val v650 = v643.id match {
          case 41 =>
            None
          case 142 =>
            val BindNode(v645, v646) = v644
            assert(v645.id == 143)
            val v647 = v646.asInstanceOf[SequenceNode].children(1)
            val BindNode(v648, v649) = v647
            assert(v648.id == 111)
            Some(matchSuperTypes(v649))
        }
        SuperDef(matchTypeName(v628), v639, v650)(nextId(), v625)
    }
    v651
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v652, v653) = node
    val v675 = v652.id match {
      case 112 =>
        val v655 = v653.asInstanceOf[SequenceNode].children(2)
        val BindNode(v656, v657) = v655
        assert(v656.id == 114)
        val BindNode(v658, v659) = v657
        val v674 = v658.id match {
          case 41 =>
            None
          case 115 =>
            val BindNode(v660, v661) = v659
            assert(v660.id == 116)
            val v662 = v661.asInstanceOf[SequenceNode].children.head
            val BindNode(v663, v664) = v662
            assert(v663.id == 96)
            val v665 = v661.asInstanceOf[SequenceNode].children(1)
            val v666 = unrollRepeat0(v665).map { elem =>
              val BindNode(v667, v668) = elem
              assert(v667.id == 119)
              val BindNode(v669, v670) = v668
              assert(v669.id == 120)
              val v671 = v670.asInstanceOf[SequenceNode].children(3)
              val BindNode(v672, v673) = v671
              assert(v672.id == 96)
              matchTypeName(v673)
            }
            Some(List(matchTypeName(v664)) ++ v666)
        }
        val v654 = v674
        if (v654.isDefined) v654.get else List()
    }
    v675
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v676, v677) = node
    val v678 = v676.id match {
      case 176 =>
        matchBinSymbol(v677)
    }
    v678
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v679, v680) = node
    val v684 = v679.id match {
      case 191 =>
        val v681 = v680.asInstanceOf[SequenceNode].children(1)
        val BindNode(v682, v683) = v681
        assert(v682.id == 193)
        matchTerminalChar(v683)
      case 201 =>
        AnyTerminal()(nextId(), v680)
    }
    v684
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v685, v686) = node
    val v692 = v685.id match {
      case 194 =>
        val BindNode(v687, v688) = v686
        assert(v687.id == 20)
        CharAsIs(v688.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v686)
      case 196 =>
        val v689 = v686.asInstanceOf[SequenceNode].children(1)
        val BindNode(v690, v691) = v689
        assert(v690.id == 197)
        CharEscaped(v691.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v686)
      case 198 =>
        matchUnicodeChar(v686)
    }
    v692
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v693, v694) = node
    val v705 = v693.id match {
      case 203 =>
        val v695 = v694.asInstanceOf[SequenceNode].children(1)
        val BindNode(v696, v697) = v695
        assert(v696.id == 204)
        val v698 = v694.asInstanceOf[SequenceNode].children(2)
        val v699 = unrollRepeat1(v698).map { elem =>
          val BindNode(v700, v701) = elem
          assert(v700.id == 204)
          matchTerminalChoiceElem(v701)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v697)) ++ v699)(nextId(), v694)
      case 220 =>
        val v702 = v694.asInstanceOf[SequenceNode].children(1)
        val BindNode(v703, v704) = v702
        assert(v703.id == 210)
        TerminalChoice(List(matchTerminalChoiceRange(v704)))(nextId(), v694)
    }
    v705
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v706, v707) = node
    val v713 = v706.id match {
      case 198 =>
        matchUnicodeChar(v707)
      case 206 =>
        val BindNode(v708, v709) = v707
        assert(v708.id == 20)
        CharAsIs(v709.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v707)
      case 208 =>
        val v710 = v707.asInstanceOf[SequenceNode].children(1)
        val BindNode(v711, v712) = v710
        assert(v711.id == 209)
        CharEscaped(v712.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v707)
    }
    v713
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v714, v715) = node
    val v716 = v714.id match {
      case 205 =>
        matchTerminalChoiceChar(v715)
      case 210 =>
        matchTerminalChoiceRange(v715)
      case 213 =>
        matchTerminalUnicodeCategory(v715)
    }
    v716
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v717, v718) = node
    val v725 = v717.id match {
      case 211 =>
        val v719 = v718.asInstanceOf[SequenceNode].children.head
        val BindNode(v720, v721) = v719
        assert(v720.id == 205)
        val v722 = v718.asInstanceOf[SequenceNode].children(2)
        val BindNode(v723, v724) = v722
        assert(v723.id == 205)
        TerminalChoiceRange(matchTerminalChoiceChar(v721), matchTerminalChoiceChar(v724))(nextId(), v718)
    }
    v725
  }

  def matchTerminalUnicodeCategory(node: Node): TerminalUnicodeCategory = {
    val BindNode(v726, v727) = node
    val v734 = v726.id match {
      case 214 =>
        val v728 = v727.asInstanceOf[SequenceNode].children(1)
        val BindNode(v729, v730) = v728
        assert(v729.id == 215)
        val v731 = v727.asInstanceOf[SequenceNode].children(2)
        val BindNode(v732, v733) = v731
        assert(v732.id == 216)
        TerminalUnicodeCategory(v730.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v733.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v727)
    }
    v734
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v735, v736) = node
    val v754 = v735.id match {
      case 273 =>
        val v737 = v736.asInstanceOf[SequenceNode].children.head
        val BindNode(v738, v739) = v737
        assert(v738.id == 274)
        val v740 = v736.asInstanceOf[SequenceNode].children(4)
        val BindNode(v741, v742) = v740
        assert(v741.id == 344)
        val BindNode(v743, v744) = v742
        assert(v743.id == 345)
        val BindNode(v745, v746) = v744
        assert(v745.id == 272)
        val v747 = v736.asInstanceOf[SequenceNode].children(8)
        val BindNode(v748, v749) = v747
        assert(v748.id == 344)
        val BindNode(v750, v751) = v749
        assert(v750.id == 345)
        val BindNode(v752, v753) = v751
        assert(v752.id == 272)
        TernaryOp(matchBoolOrExpr(v739), matchTernaryExpr(v746), matchTernaryExpr(v753))(nextId(), v736)
      case 274 =>
        matchBoolOrExpr(v736)
    }
    v754
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v755, v756) = node
    val v757 = v755.id match {
      case 109 =>
        matchClassDef(v756)
      case 139 =>
        matchSuperDef(v756)
      case 156 =>
        matchEnumTypeDef(v756)
    }
    v757
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v758, v759) = node
    val v774 = v758.id match {
      case 94 =>
        val v760 = v759.asInstanceOf[SequenceNode].children.head
        val BindNode(v761, v762) = v760
        assert(v761.id == 95)
        val v763 = v759.asInstanceOf[SequenceNode].children(1)
        val BindNode(v764, v765) = v763
        assert(v764.id == 164)
        val BindNode(v766, v767) = v765
        val v773 = v766.id match {
          case 41 =>
            None
          case 165 =>
            val BindNode(v768, v769) = v767
            assert(v768.id == 166)
            val v770 = v769.asInstanceOf[SequenceNode].children(1)
            val BindNode(v771, v772) = v770
            assert(v771.id == 167)
            Some(v772.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
        }
        TypeDesc(matchNonNullTypeDesc(v762), v773.isDefined)(nextId(), v759)
    }
    v774
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v775, v776) = node
    val v780 = v775.id match {
      case 49 =>
        TypeName(matchIdNoKeyword(v776))(nextId(), v776)
      case 87 =>
        val v777 = v776.asInstanceOf[SequenceNode].children(1)
        val BindNode(v778, v779) = v777
        assert(v778.id == 51)
        TypeName(matchId(v779))(nextId(), v776)
    }
    v780
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v781, v782) = node
    val v786 = v781.id match {
      case 49 =>
        TypeOrFuncName(matchIdNoKeyword(v782))(nextId(), v782)
      case 87 =>
        val v783 = v782.asInstanceOf[SequenceNode].children(1)
        val BindNode(v784, v785) = v783
        assert(v784.id == 51)
        TypeOrFuncName(matchId(v785))(nextId(), v782)
    }
    v786
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v787, v788) = node
    val v801 = v787.id match {
      case 199 =>
        val v789 = v788.asInstanceOf[SequenceNode].children(2)
        val BindNode(v790, v791) = v789
        assert(v790.id == 200)
        val v792 = v788.asInstanceOf[SequenceNode].children(3)
        val BindNode(v793, v794) = v792
        assert(v793.id == 200)
        val v795 = v788.asInstanceOf[SequenceNode].children(4)
        val BindNode(v796, v797) = v795
        assert(v796.id == 200)
        val v798 = v788.asInstanceOf[SequenceNode].children(5)
        val BindNode(v799, v800) = v798
        assert(v799.id == 200)
        CharUnicode(List(v791.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v794.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v797.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v800.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char))(nextId(), v788)
    }
    v801
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v802, v803) = node
    val v813 = v802.id match {
      case 247 =>
        val v804 = v803.asInstanceOf[SequenceNode].children(2)
        val BindNode(v805, v806) = v804
        assert(v805.id == 254)
        val v807 = v803.asInstanceOf[SequenceNode].children(1)
        val BindNode(v808, v809) = v807
        assert(v808.id == 249)
        val BindNode(v810, v811) = v809
        val v812 = v810.id match {
          case 41 =>
            None
          case 250 =>
            Some(matchCondSymPath(v811))
        }
        ValRef(matchRefIdx(v806), v812)(nextId(), v803)
    }
    v813
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v814, v815) = node
    val v816 = v814.id match {
      case 60 =>
        BooleanType()(nextId(), v815)
      case 68 =>
        CharType()(nextId(), v815)
      case 73 =>
        StringType()(nextId(), v815)
    }
    v816
  }
}
