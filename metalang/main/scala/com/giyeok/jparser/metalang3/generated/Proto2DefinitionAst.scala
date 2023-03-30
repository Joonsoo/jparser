//package com.giyeok.jparser.metalang3.generated
//
//import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode, TerminalNode}
//import com.giyeok.jparser.{Inputs, NGrammar, ParsingErrors}
//import com.giyeok.jparser.nparser.ParseTreeUtil.{unrollRepeat0, unrollRepeat1}
//import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeUtil, Parser}
//import com.giyeok.jparser.proto.{GrammarProto, GrammarProtobufConverter}
//
//import java.io.FileInputStream
//import scala.util.Using
//
//object Proto2DefinitionAst {
//  val ngrammar: NGrammar = Using(new FileInputStream("C:\\Users\\Joonsoo\\workspace\\jparser\\Proto2DefinitionGrammar.pb")) { inputStream =>
//    GrammarProtobufConverter.convertProtoToNGrammar(GrammarProto.NGrammar.parseFrom(inputStream))
//  }.get
//
//  sealed trait WithParseNode {
//    val parseNode: Node
//  }
//
//  case class BoolConstant(value: BoolLit.Value)(override val parseNode: Node) extends Constant with WithParseNode
//
//  case class BuiltinType(typ: BuiltinTypeEnum.Value)(override val parseNode: Node) extends Type with WithParseNode
//
//  case class CharEscape(value: Char)(override val parseNode: Node) extends CharValue with WithParseNode
//
//  sealed trait CharValue extends WithParseNode
//
//  case class Character(value: Char)(override val parseNode: Node) extends CharValue with WithParseNode
//
//  sealed trait Constant extends WithParseNode
//
//  case class DecimalLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode
//
//  case class DoubleQuoteStrLit(value: List[CharValue])(override val parseNode: Node) extends StrLit with WithParseNode
//
//  case class EmptyStatement()(override val parseNode: Node) extends EnumBodyElem with ExtendBodyElem with MessageBodyElem with OneOfElem with ProtoDefElem with ServiceBodyElem with WithParseNode
//
//  sealed trait EnumBodyElem extends WithParseNode
//
//  case class EnumDef(name: Ident, body: List[EnumBodyElem])(override val parseNode: Node) extends MessageBodyElem with TopLevelDef with WithParseNode
//
//  case class EnumFieldDef(name: Ident, minus: Boolean, value: IntLit, options: Option[List[EnumValueOption]])(override val parseNode: Node) extends EnumBodyElem with WithParseNode
//
//  case class EnumType(firstDot: Boolean, parent: List[Char], name: Ident)(override val parseNode: Node) extends WithParseNode
//
//  case class EnumValueOption(name: OptionName, value: Constant)(override val parseNode: Node) extends WithParseNode
//
//  case class Exponent(sign: Option[Sign.Value], value: String)(override val parseNode: Node) extends WithParseNode
//
//  case class Extend(name: MessageType, body: List[ExtendBodyElem])(override val parseNode: Node) extends MessageBodyElem with TopLevelDef with WithParseNode
//
//  sealed trait ExtendBodyElem extends WithParseNode
//
//  case class Extensions(ranges: Ranges)(override val parseNode: Node) extends MessageBodyElem with WithParseNode
//
//  case class Field(label: Label.Value, typ: Type, name: Ident, fieldNumber: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends ExtendBodyElem with MessageBodyElem with WithParseNode
//
//  case class FieldNames(names: List[Ident])(override val parseNode: Node) extends ReservedBody with WithParseNode
//
//  case class FieldOption(name: OptionName, value: Constant)(override val parseNode: Node) extends WithParseNode
//
//  case class FloatConstant(sign: Option[Sign.Value], value: FloatLit)(override val parseNode: Node) extends Constant with WithParseNode
//
//  sealed trait FloatLit extends WithParseNode
//
//  case class FloatLiteral(integral: String, fractional: String, exponent: Option[Exponent])(override val parseNode: Node) extends FloatLit with WithParseNode
//
//  case class FullIdent(names: List[Ident])(override val parseNode: Node) extends Constant with OptionScope with WithParseNode
//
//  case class Group(label: Label.Value, name: GroupName, number: IntLit, body: List[MessageBodyElem])(override val parseNode: Node) extends ExtendBodyElem with MessageBodyElem with WithParseNode
//
//  case class GroupName(name: String)(override val parseNode: Node) extends WithParseNode
//
//  case class HexEscape(value: String)(override val parseNode: Node) extends CharValue with WithParseNode
//
//  case class HexLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode
//
//  case class Ident(name: String)(override val parseNode: Node) extends OptionScope with WithParseNode
//
//  case class Import(importType: Option[ImportType.Value], target: StrLit)(override val parseNode: Node) extends ProtoDefElem with WithParseNode
//
//  case class Inf()(override val parseNode: Node) extends FloatLit with WithParseNode
//
//  case class IntConstant(sign: Option[Sign.Value], value: IntLit)(override val parseNode: Node) extends Constant with WithParseNode
//
//  sealed trait IntLit extends WithParseNode
//
//  case class MapField(keyType: MapKeyType.Value, valueType: Type, mapName: Ident, number: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends MessageBodyElem with WithParseNode
//
//  case class Message(name: Ident, body: List[MessageBodyElem])(override val parseNode: Node) extends MessageBodyElem with TopLevelDef with WithParseNode
//
//  sealed trait MessageBodyElem extends WithParseNode
//
//  case class MessageOrEnumType(name: MessageType)(override val parseNode: Node) extends Type with WithParseNode
//
//  case class MessageType(firstDot: Boolean, parent: List[Char], name: Ident)(override val parseNode: Node) extends WithParseNode
//
//  case class NaN()(override val parseNode: Node) extends FloatLit with WithParseNode
//
//  case class OctalEscape(value: String)(override val parseNode: Node) extends CharValue with WithParseNode
//
//  case class OctalLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode
//
//  sealed trait OneOfElem extends WithParseNode
//
//  case class OneofDef(name: Ident, elems: List[OneOfElem])(override val parseNode: Node) extends MessageBodyElem with WithParseNode
//
//  case class OneofField(typ: Type, name: Ident, number: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends OneOfElem with WithParseNode
//
//  case class OptionDef(name: OptionName, value: Constant)(override val parseNode: Node) extends EnumBodyElem with MessageBodyElem with OneOfElem with ProtoDefElem with ServiceBodyElem with WithParseNode
//
//  case class OptionName(scope: OptionScope, name: List[Ident])(override val parseNode: Node) extends WithParseNode
//
//  sealed trait OptionScope extends WithParseNode
//
//  case class Package(name: FullIdent)(override val parseNode: Node) extends ProtoDefElem with WithParseNode
//
//  case class Proto3(defs: List[ProtoDefElem])(override val parseNode: Node) extends WithParseNode
//
//  sealed trait ProtoDefElem extends WithParseNode
//
//  case class Range(start: IntLit, end: Option[RangeEnd])(override val parseNode: Node) extends WithParseNode
//
//  sealed trait RangeEnd extends WithParseNode
//
//  case class RangeEndMax()(override val parseNode: Node) extends RangeEnd with WithParseNode
//
//  case class RangeEndValue(value: IntLit)(override val parseNode: Node) extends RangeEnd with WithParseNode
//
//  case class Ranges(values: List[Range])(override val parseNode: Node) extends ReservedBody with WithParseNode
//
//  case class Reserved(value: ReservedBody)(override val parseNode: Node) extends MessageBodyElem with WithParseNode
//
//  sealed trait ReservedBody extends WithParseNode
//
//  case class Rpc(name: Ident, isInputStream: Boolean, inputType: MessageType, isOutputStream: Boolean, outputType: MessageType, options: List[Option[OptionDef]])(override val parseNode: Node) extends ServiceBodyElem with WithParseNode
//
//  case class Service(name: Ident, body: List[ServiceBodyElem])(override val parseNode: Node) extends TopLevelDef with WithParseNode
//
//  sealed trait ServiceBodyElem extends WithParseNode
//
//  case class SingleQuoteStrLit(value: List[CharValue])(override val parseNode: Node) extends StrLit with WithParseNode
//
//  sealed trait StrLit extends WithParseNode
//
//  case class Stream(name: Ident, type1: MessageType, type2: MessageType, options: List[Option[OptionDef]])(override val parseNode: Node) extends ServiceBodyElem with WithParseNode
//
//  case class StringConstant(value: StrLit)(override val parseNode: Node) extends Constant with WithParseNode
//
//  sealed trait TopLevelDef extends ProtoDefElem with WithParseNode
//
//  sealed trait Type extends WithParseNode
//
//  object BoolLit extends Enumeration {
//    val FALSE, TRUE = Value
//  }
//
//  object BuiltinTypeEnum extends Enumeration {
//    val BOOL, BYTES, DOUBLE, FIXED32, FIXED64, FLOAT, INT32, INT64, SFIXED32, SFIXED64, SINT32, SINT64, STRING, UINT32, UINT64 = Value
//  }
//
//  object ImportType extends Enumeration {
//    val PUBLIC, WEAK = Value
//  }
//
//  object Label extends Enumeration {
//    val OPTIONAL, REPEATED, REQUIRED = Value
//  }
//
//  object MapKeyType extends Enumeration {
//    val BOOL, FIXED32, FIXED64, INT32, INT64, SFIXED32, SFIXED64, SINT32, SINT64, STRING, UINT32, UINT64 = Value
//  }
//
//  object Sign extends Enumeration {
//    val MINUS, PLUS = Value
//  }
//
//  def matchBoolLit(node: Node): BoolLit.Value = {
//    val BindNode(v1, v2) = node
//    val v3 = v1.id match {
//      case 173 =>
//        BoolLit.TRUE
//      case 176 =>
//        BoolLit.FALSE
//    }
//    v3
//  }
//
//  def matchBuiltinType(node: Node): BuiltinTypeEnum.Value = {
//    val BindNode(v4, v5) = node
//    val v45 = v4.id match {
//      case 280 =>
//        val v6 = v5.asInstanceOf[SequenceNode].children.head
//        val BindNode(v7, v8) = v6
//        assert(v7.id == 281)
//        val JoinNode(_, v9, _) = v8
//        val BindNode(v10, v11) = v9
//        assert(v10.id == 282)
//        val BindNode(v12, v13) = v11
//        val v44 = v12.id match {
//          case 306 =>
//            val BindNode(v14, v15) = v13
//            assert(v14.id == 307)
//            BuiltinTypeEnum.UINT64
//          case 322 =>
//            val BindNode(v16, v17) = v13
//            assert(v16.id == 323)
//            BuiltinTypeEnum.FIXED64
//          case 291 =>
//            val BindNode(v18, v19) = v13
//            assert(v18.id == 292)
//            BuiltinTypeEnum.INT32
//          case 283 =>
//            val BindNode(v20, v21) = v13
//            assert(v20.id == 284)
//            BuiltinTypeEnum.DOUBLE
//          case 338 =>
//            val BindNode(v22, v23) = v13
//            assert(v22.id == 339)
//            BuiltinTypeEnum.STRING
//          case 330 =>
//            val BindNode(v24, v25) = v13
//            assert(v24.id == 331)
//            BuiltinTypeEnum.SFIXED64
//          case 296 =>
//            val BindNode(v26, v27) = v13
//            assert(v26.id == 297)
//            BuiltinTypeEnum.INT64
//          case 334 =>
//            val BindNode(v28, v29) = v13
//            assert(v28.id == 335)
//            BuiltinTypeEnum.BOOL
//          case 326 =>
//            val BindNode(v30, v31) = v13
//            assert(v30.id == 327)
//            BuiltinTypeEnum.SFIXED32
//          case 318 =>
//            val BindNode(v32, v33) = v13
//            assert(v32.id == 319)
//            BuiltinTypeEnum.FIXED32
//          case 302 =>
//            val BindNode(v34, v35) = v13
//            assert(v34.id == 303)
//            BuiltinTypeEnum.UINT32
//          case 314 =>
//            val BindNode(v36, v37) = v13
//            assert(v36.id == 315)
//            BuiltinTypeEnum.SINT64
//          case 310 =>
//            val BindNode(v38, v39) = v13
//            assert(v38.id == 311)
//            BuiltinTypeEnum.SINT32
//          case 287 =>
//            val BindNode(v40, v41) = v13
//            assert(v40.id == 288)
//            BuiltinTypeEnum.FLOAT
//          case 342 =>
//            val BindNode(v42, v43) = v13
//            assert(v42.id == 343)
//            BuiltinTypeEnum.BYTES
//        }
//        v44
//    }
//    v45
//  }
//
//  def matchCapitalLetter(node: Node): Char = {
//    val BindNode(v46, v47) = node
//    val v51 = v46.id match {
//      case 440 =>
//        val v48 = v47.asInstanceOf[SequenceNode].children.head
//        val BindNode(v49, v50) = v48
//        assert(v49.id == 441)
//        v50.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//    }
//    v51
//  }
//
//  def matchCharEscape(node: Node): CharEscape = {
//    val BindNode(v52, v53) = node
//    val v57 = v52.id match {
//      case 103 =>
//        val v54 = v53.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v55, v56) = v54
//        assert(v55.id == 104)
//        CharEscape(v56.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v53)
//    }
//    v57
//  }
//
//  def matchCharValue(node: Node): CharValue = {
//    val BindNode(v58, v59) = node
//    val v74 = v58.id match {
//      case 87 =>
//        val v60 = v59.asInstanceOf[SequenceNode].children.head
//        val BindNode(v61, v62) = v60
//        assert(v61.id == 88)
//        matchHexEscape(v62)
//      case 95 =>
//        val v63 = v59.asInstanceOf[SequenceNode].children.head
//        val BindNode(v64, v65) = v63
//        assert(v64.id == 96)
//        matchOctEscape(v65)
//      case 101 =>
//        val v66 = v59.asInstanceOf[SequenceNode].children.head
//        val BindNode(v67, v68) = v66
//        assert(v67.id == 102)
//        matchCharEscape(v68)
//      case 105 =>
//        val v69 = v59.asInstanceOf[SequenceNode].children.head
//        val BindNode(v70, v71) = v69
//        assert(v70.id == 106)
//        val BindNode(v72, v73) = v71
//        assert(v72.id == 107)
//        Character(v73.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v59)
//    }
//    v74
//  }
//
//  def matchConstant(node: Node): Constant = {
//    val BindNode(v75, v76) = node
//    val v106 = v75.id match {
//      case 235 =>
//        val v77 = v76.asInstanceOf[SequenceNode].children.head
//        val BindNode(v78, v79) = v77
//        assert(v78.id == 82)
//        StringConstant(matchStrLit(v79))(v76)
//      case 236 =>
//        val v80 = v76.asInstanceOf[SequenceNode].children.head
//        val BindNode(v81, v82) = v80
//        assert(v81.id == 172)
//        BoolConstant(matchBoolLit(v82))(v76)
//      case 170 =>
//        val v83 = v76.asInstanceOf[SequenceNode].children.head
//        val BindNode(v84, v85) = v83
//        assert(v84.id == 171)
//        val BindNode(v86, v87) = v85
//        assert(v86.id == 118)
//        matchFullIdent(v87)
//      case 180 =>
//        val v88 = v76.asInstanceOf[SequenceNode].children.head
//        val BindNode(v89, v90) = v88
//        assert(v89.id == 181)
//        val BindNode(v91, v92) = v90
//        val v93 = v91.id match {
//          case 81 =>
//            None
//          case 182 =>
//            Some(matchSign(v92))
//        }
//        val v94 = v76.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v95, v96) = v94
//        assert(v95.id == 187)
//        IntConstant(v93, matchIntLit(v96))(v76)
//      case 215 =>
//        val v97 = v76.asInstanceOf[SequenceNode].children.head
//        val BindNode(v98, v99) = v97
//        assert(v98.id == 181)
//        val BindNode(v100, v101) = v99
//        val v102 = v100.id match {
//          case 81 =>
//            None
//          case 182 =>
//            Some(matchSign(v101))
//        }
//        val v103 = v76.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v104, v105) = v103
//        assert(v104.id == 216)
//        FloatConstant(v102, matchFloatLit(v105))(v76)
//    }
//    v106
//  }
//
//  def matchDecimalDigit(node: Node): Char = {
//    val BindNode(v107, v108) = node
//    val v112 = v107.id match {
//      case 137 =>
//        val v109 = v108.asInstanceOf[SequenceNode].children.head
//        val BindNode(v110, v111) = v109
//        assert(v110.id == 138)
//        v111.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//    }
//    v112
//  }
//
//  def matchDecimalLit(node: Node): DecimalLit = {
//    val BindNode(v113, v114) = node
//    val v122 = v113.id match {
//      case 199 =>
//        val v115 = v114.asInstanceOf[SequenceNode].children.head
//        val BindNode(v116, v117) = v115
//        assert(v116.id == 200)
//        val v118 = v114.asInstanceOf[SequenceNode].children(1)
//        val v119 = unrollRepeat0(v118).map { elem =>
//          val BindNode(v120, v121) = elem
//          assert(v120.id == 136)
//          matchDecimalDigit(v121)
//        }
//        DecimalLit(v117.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v119.map(x => x.toString).mkString(""))(v114)
//    }
//    v122
//  }
//
//  def matchDecimals(node: Node): String = {
//    val BindNode(v123, v124) = node
//    val v129 = v123.id match {
//      case 219 =>
//        val v125 = v124.asInstanceOf[SequenceNode].children.head
//        val v126 = unrollRepeat1(v125).map { elem =>
//          val BindNode(v127, v128) = elem
//          assert(v127.id == 136)
//          matchDecimalDigit(v128)
//        }
//        v126.map(x => x.toString).mkString("")
//    }
//    v129
//  }
//
//  def matchEmptyStatement(node: Node): EmptyStatement = {
//    val BindNode(v130, v131) = node
//    val v132 = v130.id match {
//      case 411 =>
//        EmptyStatement()(v131)
//    }
//    v132
//  }
//
//  def matchEnum(node: Node): EnumDef = {
//    val BindNode(v133, v134) = node
//    val v141 = v133.id match {
//      case 377 =>
//        val v135 = v134.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v136, v137) = v135
//        assert(v136.id == 381)
//        val v138 = v134.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v139, v140) = v138
//        assert(v139.id == 382)
//        EnumDef(matchEnumName(v137), matchEnumBody(v140))(v134)
//    }
//    v141
//  }
//
//  def matchEnumBody(node: Node): List[EnumBodyElem] = {
//    val BindNode(v142, v143) = node
//    val v174 = v142.id match {
//      case 383 =>
//        val v144 = v143.asInstanceOf[SequenceNode].children(1)
//        val v145 = unrollRepeat0(v144).map { elem =>
//          val BindNode(v146, v147) = elem
//          assert(v146.id == 386)
//          val BindNode(v148, v149) = v147
//          val v173 = v148.id match {
//            case 387 =>
//              val BindNode(v150, v151) = v149
//              assert(v150.id == 388)
//              val v152 = v151.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v153, v154) = v152
//              assert(v153.id == 389)
//              val BindNode(v155, v156) = v154
//              val v172 = v155.id match {
//                case 148 =>
//                  val BindNode(v157, v158) = v156
//                  assert(v157.id == 149)
//                  val v159 = v158.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v160, v161) = v159
//                  assert(v160.id == 150)
//                  matchOption(v161)
//                case 390 =>
//                  val BindNode(v162, v163) = v156
//                  assert(v162.id == 391)
//                  val v164 = v163.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v165, v166) = v164
//                  assert(v165.id == 392)
//                  matchEnumField(v166)
//                case 408 =>
//                  val BindNode(v167, v168) = v156
//                  assert(v167.id == 409)
//                  val v169 = v168.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v170, v171) = v169
//                  assert(v170.id == 410)
//                  matchEmptyStatement(v171)
//              }
//              v172
//          }
//          v173
//        }
//        v145
//    }
//    v174
//  }
//
//  def matchEnumField(node: Node): EnumFieldDef = {
//    val BindNode(v175, v176) = node
//    val v223 = v175.id match {
//      case 393 =>
//        val v177 = v176.asInstanceOf[SequenceNode].children.head
//        val BindNode(v178, v179) = v177
//        assert(v178.id == 120)
//        val v180 = v176.asInstanceOf[SequenceNode].children(3)
//        val BindNode(v181, v182) = v180
//        assert(v181.id == 394)
//        val BindNode(v183, v184) = v182
//        val v193 = v183.id match {
//          case 81 =>
//            None
//          case 395 =>
//            val BindNode(v185, v186) = v184
//            val v192 = v185.id match {
//              case 396 =>
//                val BindNode(v187, v188) = v186
//                assert(v187.id == 397)
//                val v189 = v188.asInstanceOf[SequenceNode].children(1)
//                val BindNode(v190, v191) = v189
//                assert(v190.id == 186)
//                v191.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//            }
//            Some(v192)
//        }
//        val v194 = v176.asInstanceOf[SequenceNode].children(5)
//        val BindNode(v195, v196) = v194
//        assert(v195.id == 187)
//        val v197 = v176.asInstanceOf[SequenceNode].children(6)
//        val BindNode(v198, v199) = v197
//        assert(v198.id == 398)
//        val BindNode(v200, v201) = v199
//        val v222 = v200.id match {
//          case 81 =>
//            None
//          case 399 =>
//            val BindNode(v202, v203) = v201
//            val v221 = v202.id match {
//              case 400 =>
//                val BindNode(v204, v205) = v203
//                assert(v204.id == 401)
//                val v206 = v205.asInstanceOf[SequenceNode].children(3)
//                val BindNode(v207, v208) = v206
//                assert(v207.id == 402)
//                val v209 = v205.asInstanceOf[SequenceNode].children(4)
//                val v210 = unrollRepeat0(v209).map { elem =>
//                  val BindNode(v211, v212) = elem
//                  assert(v211.id == 405)
//                  val BindNode(v213, v214) = v212
//                  val v220 = v213.id match {
//                    case 406 =>
//                      val BindNode(v215, v216) = v214
//                      assert(v215.id == 407)
//                      val v217 = v216.asInstanceOf[SequenceNode].children(3)
//                      val BindNode(v218, v219) = v217
//                      assert(v218.id == 402)
//                      matchEnumValueOption(v219)
//                  }
//                  v220
//                }
//                List(matchEnumValueOption(v208)) ++ v210
//            }
//            Some(v221)
//        }
//        EnumFieldDef(matchIdent(v179), v193.isDefined, matchIntLit(v196), v222)(v176)
//    }
//    v223
//  }
//
//  def matchEnumName(node: Node): Ident = {
//    val BindNode(v224, v225) = node
//    val v229 = v224.id match {
//      case 159 =>
//        val v226 = v225.asInstanceOf[SequenceNode].children.head
//        val BindNode(v227, v228) = v226
//        assert(v227.id == 120)
//        matchIdent(v228)
//    }
//    v229
//  }
//
//  def matchEnumValueOption(node: Node): EnumValueOption = {
//    val BindNode(v230, v231) = node
//    val v238 = v230.id match {
//      case 367 =>
//        val v232 = v231.asInstanceOf[SequenceNode].children.head
//        val BindNode(v233, v234) = v232
//        assert(v233.id == 155)
//        val v235 = v231.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v236, v237) = v235
//        assert(v236.id == 169)
//        EnumValueOption(matchOptionName(v234), matchConstant(v237))(v231)
//    }
//    v238
//  }
//
//  def matchExponent(node: Node): Exponent = {
//    val BindNode(v239, v240) = node
//    val v250 = v239.id match {
//      case 225 =>
//        val v241 = v240.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v242, v243) = v241
//        assert(v242.id == 181)
//        val BindNode(v244, v245) = v243
//        val v246 = v244.id match {
//          case 81 =>
//            None
//          case 182 =>
//            Some(matchSign(v245))
//        }
//        val v247 = v240.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v248, v249) = v247
//        assert(v248.id == 218)
//        Exponent(v246, matchDecimals(v249))(v240)
//    }
//    v250
//  }
//
//  def matchExtend(node: Node): Extend = {
//    val BindNode(v251, v252) = node
//    val v286 = v251.id match {
//      case 415 =>
//        val v253 = v252.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v254, v255) = v253
//        assert(v254.id == 348)
//        val v256 = v252.asInstanceOf[SequenceNode].children(5)
//        val v257 = unrollRepeat0(v256).map { elem =>
//          val BindNode(v258, v259) = elem
//          assert(v258.id == 421)
//          val BindNode(v260, v261) = v259
//          val v285 = v260.id match {
//            case 422 =>
//              val BindNode(v262, v263) = v261
//              assert(v262.id == 423)
//              val v264 = v263.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v265, v266) = v264
//              assert(v265.id == 424)
//              val BindNode(v267, v268) = v266
//              val v284 = v267.id match {
//                case 425 =>
//                  val BindNode(v269, v270) = v268
//                  assert(v269.id == 256)
//                  val v271 = v270.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v272, v273) = v271
//                  assert(v272.id == 257)
//                  matchField(v273)
//                case 426 =>
//                  val BindNode(v274, v275) = v268
//                  assert(v274.id == 427)
//                  val v276 = v275.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v277, v278) = v276
//                  assert(v277.id == 428)
//                  matchGroup(v278)
//                case 408 =>
//                  val BindNode(v279, v280) = v268
//                  assert(v279.id == 409)
//                  val v281 = v280.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v282, v283) = v281
//                  assert(v282.id == 410)
//                  matchEmptyStatement(v283)
//              }
//              v284
//          }
//          v285
//        }
//        Extend(matchMessageType(v255), v257)(v252)
//    }
//    v286
//  }
//
//  def matchExtensions(node: Node): Extensions = {
//    val BindNode(v287, v288) = node
//    val v292 = v287.id match {
//      case 444 =>
//        val v289 = v288.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v290, v291) = v289
//        assert(v290.id == 448)
//        Extensions(matchRanges(v291))(v288)
//    }
//    v292
//  }
//
//  def matchField(node: Node): Field = {
//    val BindNode(v293, v294) = node
//    val v321 = v293.id match {
//      case 258 =>
//        val v295 = v294.asInstanceOf[SequenceNode].children.head
//        val BindNode(v296, v297) = v295
//        assert(v296.id == 259)
//        val v298 = v294.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v299, v300) = v298
//        assert(v299.id == 277)
//        val v301 = v294.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v302, v303) = v301
//        assert(v302.id == 356)
//        val v304 = v294.asInstanceOf[SequenceNode].children(8)
//        val BindNode(v305, v306) = v304
//        assert(v305.id == 357)
//        val v307 = v294.asInstanceOf[SequenceNode].children(9)
//        val BindNode(v308, v309) = v307
//        assert(v308.id == 359)
//        val BindNode(v310, v311) = v309
//        val v320 = v310.id match {
//          case 81 =>
//            None
//          case 360 =>
//            val BindNode(v312, v313) = v311
//            val v319 = v312.id match {
//              case 361 =>
//                val BindNode(v314, v315) = v313
//                assert(v314.id == 362)
//                val v316 = v315.asInstanceOf[SequenceNode].children(3)
//                val BindNode(v317, v318) = v316
//                assert(v317.id == 364)
//                matchFieldOptions(v318)
//            }
//            Some(v319)
//        }
//        Field(matchLabel(v297), matchType(v300), matchFieldName(v303), matchFieldNumber(v306), v320)(v294)
//    }
//    v321
//  }
//
//  def matchFieldName(node: Node): Ident = {
//    val BindNode(v322, v323) = node
//    val v327 = v322.id match {
//      case 159 =>
//        val v324 = v323.asInstanceOf[SequenceNode].children.head
//        val BindNode(v325, v326) = v324
//        assert(v325.id == 120)
//        matchIdent(v326)
//    }
//    v327
//  }
//
//  def matchFieldNames(node: Node): FieldNames = {
//    val BindNode(v328, v329) = node
//    val v345 = v328.id match {
//      case 512 =>
//        val v330 = v329.asInstanceOf[SequenceNode].children.head
//        val BindNode(v331, v332) = v330
//        assert(v331.id == 356)
//        val v333 = v329.asInstanceOf[SequenceNode].children(1)
//        val v334 = unrollRepeat0(v333).map { elem =>
//          val BindNode(v335, v336) = elem
//          assert(v335.id == 515)
//          val BindNode(v337, v338) = v336
//          val v344 = v337.id match {
//            case 516 =>
//              val BindNode(v339, v340) = v338
//              assert(v339.id == 517)
//              val v341 = v340.asInstanceOf[SequenceNode].children(3)
//              val BindNode(v342, v343) = v341
//              assert(v342.id == 356)
//              matchFieldName(v343)
//          }
//          v344
//        }
//        FieldNames(List(matchFieldName(v332)) ++ v334)(v329)
//    }
//    v345
//  }
//
//  def matchFieldNumber(node: Node): IntLit = {
//    val BindNode(v346, v347) = node
//    val v351 = v346.id match {
//      case 358 =>
//        val v348 = v347.asInstanceOf[SequenceNode].children.head
//        val BindNode(v349, v350) = v348
//        assert(v349.id == 187)
//        matchIntLit(v350)
//    }
//    v351
//  }
//
//  def matchFieldOption(node: Node): FieldOption = {
//    val BindNode(v352, v353) = node
//    val v360 = v352.id match {
//      case 367 =>
//        val v354 = v353.asInstanceOf[SequenceNode].children.head
//        val BindNode(v355, v356) = v354
//        assert(v355.id == 155)
//        val v357 = v353.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v358, v359) = v357
//        assert(v358.id == 169)
//        FieldOption(matchOptionName(v356), matchConstant(v359))(v353)
//    }
//    v360
//  }
//
//  def matchFieldOptions(node: Node): List[FieldOption] = {
//    val BindNode(v361, v362) = node
//    val v378 = v361.id match {
//      case 365 =>
//        val v363 = v362.asInstanceOf[SequenceNode].children.head
//        val BindNode(v364, v365) = v363
//        assert(v364.id == 366)
//        val v366 = v362.asInstanceOf[SequenceNode].children(1)
//        val v367 = unrollRepeat0(v366).map { elem =>
//          val BindNode(v368, v369) = elem
//          assert(v368.id == 370)
//          val BindNode(v370, v371) = v369
//          val v377 = v370.id match {
//            case 371 =>
//              val BindNode(v372, v373) = v371
//              assert(v372.id == 372)
//              val v374 = v373.asInstanceOf[SequenceNode].children(3)
//              val BindNode(v375, v376) = v374
//              assert(v375.id == 366)
//              matchFieldOption(v376)
//          }
//          v377
//        }
//        List(matchFieldOption(v365)) ++ v367
//    }
//    v378
//  }
//
//  def matchFloatLit(node: Node): FloatLit = {
//    val BindNode(v379, v380) = node
//    val v412 = v379.id match {
//      case 228 =>
//        val v381 = v380.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v382, v383) = v381
//        assert(v382.id == 218)
//        val v384 = v380.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v385, v386) = v384
//        assert(v385.id == 223)
//        val BindNode(v387, v388) = v386
//        val v389 = v387.id match {
//          case 81 =>
//            None
//          case 224 =>
//            Some(matchExponent(v388))
//        }
//        FloatLiteral("", matchDecimals(v383), v389)(v380)
//      case 227 =>
//        val v390 = v380.asInstanceOf[SequenceNode].children.head
//        val BindNode(v391, v392) = v390
//        assert(v391.id == 218)
//        val v393 = v380.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v394, v395) = v393
//        assert(v394.id == 224)
//        FloatLiteral(matchDecimals(v392), "", Some(matchExponent(v395)))(v380)
//      case 217 =>
//        val v396 = v380.asInstanceOf[SequenceNode].children.head
//        val BindNode(v397, v398) = v396
//        assert(v397.id == 218)
//        val v400 = v380.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v401, v402) = v400
//        assert(v401.id == 222)
//        val BindNode(v403, v404) = v402
//        val v405 = v403.id match {
//          case 81 =>
//            None
//          case 218 =>
//            Some(matchDecimals(v404))
//        }
//        val v399 = v405
//        val v406 = v380.asInstanceOf[SequenceNode].children(3)
//        val BindNode(v407, v408) = v406
//        assert(v407.id == 223)
//        val BindNode(v409, v410) = v408
//        val v411 = v409.id match {
//          case 81 =>
//            None
//          case 224 =>
//            Some(matchExponent(v410))
//        }
//        FloatLiteral(matchDecimals(v398), if (v399.isDefined) v399.get else "", v411)(v380)
//      case 229 =>
//        Inf()(v380)
//      case 232 =>
//        NaN()(v380)
//    }
//    v412
//  }
//
//  def matchFullIdent(node: Node): FullIdent = {
//    val BindNode(v413, v414) = node
//    val v430 = v413.id match {
//      case 119 =>
//        val v415 = v414.asInstanceOf[SequenceNode].children.head
//        val BindNode(v416, v417) = v415
//        assert(v416.id == 120)
//        val v418 = v414.asInstanceOf[SequenceNode].children(1)
//        val v419 = unrollRepeat0(v418).map { elem =>
//          val BindNode(v420, v421) = elem
//          assert(v420.id == 144)
//          val BindNode(v422, v423) = v421
//          val v429 = v422.id match {
//            case 145 =>
//              val BindNode(v424, v425) = v423
//              assert(v424.id == 146)
//              val v426 = v425.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v427, v428) = v426
//              assert(v427.id == 120)
//              matchIdent(v428)
//          }
//          v429
//        }
//        FullIdent(List(matchIdent(v417)) ++ v419)(v414)
//    }
//    v430
//  }
//
//  def matchGroup(node: Node): Group = {
//    val BindNode(v431, v432) = node
//    val v445 = v431.id match {
//      case 429 =>
//        val v433 = v432.asInstanceOf[SequenceNode].children.head
//        val BindNode(v434, v435) = v433
//        assert(v434.id == 259)
//        val v436 = v432.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v437, v438) = v436
//        assert(v437.id == 433)
//        val v439 = v432.asInstanceOf[SequenceNode].children(8)
//        val BindNode(v440, v441) = v439
//        assert(v440.id == 357)
//        val v442 = v432.asInstanceOf[SequenceNode].children(10)
//        val BindNode(v443, v444) = v442
//        assert(v443.id == 247)
//        Group(matchLabel(v435), matchGroupName(v438), matchFieldNumber(v441), matchMessageBody(v444))(v432)
//    }
//    v445
//  }
//
//  def matchGroupName(node: Node): GroupName = {
//    val BindNode(v446, v447) = node
//    val v483 = v446.id match {
//      case 434 =>
//        val v448 = v447.asInstanceOf[SequenceNode].children.head
//        val BindNode(v449, v450) = v448
//        assert(v449.id == 435)
//        val BindNode(v451, v452) = v450
//        assert(v451.id == 436)
//        val BindNode(v453, v454) = v452
//        val v482 = v453.id match {
//          case 437 =>
//            val BindNode(v455, v456) = v454
//            assert(v455.id == 438)
//            val v457 = v456.asInstanceOf[SequenceNode].children.head
//            val BindNode(v458, v459) = v457
//            assert(v458.id == 439)
//            val v460 = v456.asInstanceOf[SequenceNode].children(1)
//            val v461 = unrollRepeat0(v460).map { elem =>
//              val BindNode(v462, v463) = elem
//              assert(v462.id == 131)
//              val BindNode(v464, v465) = v463
//              val v481 = v464.id match {
//                case 132 =>
//                  val BindNode(v466, v467) = v465
//                  assert(v466.id == 133)
//                  val v468 = v467.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v469, v470) = v468
//                  assert(v469.id == 126)
//                  matchLetter(v470)
//                case 134 =>
//                  val BindNode(v471, v472) = v465
//                  assert(v471.id == 135)
//                  val v473 = v472.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v474, v475) = v473
//                  assert(v474.id == 136)
//                  matchDecimalDigit(v475)
//                case 139 =>
//                  val BindNode(v476, v477) = v465
//                  assert(v476.id == 140)
//                  val v478 = v477.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v479, v480) = v478
//                  assert(v479.id == 141)
//                  v480.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//              }
//              v481
//            }
//            matchCapitalLetter(v459).toString + v461.map(x => x.toString).mkString("")
//        }
//        GroupName(v482)(v447)
//    }
//    v483
//  }
//
//  def matchHexDigit(node: Node): Char = {
//    val BindNode(v484, v485) = node
//    val v489 = v484.id match {
//      case 93 =>
//        val v486 = v485.asInstanceOf[SequenceNode].children.head
//        val BindNode(v487, v488) = v486
//        assert(v487.id == 94)
//        v488.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//    }
//    v489
//  }
//
//  def matchHexEscape(node: Node): HexEscape = {
//    val BindNode(v490, v491) = node
//    val v498 = v490.id match {
//      case 89 =>
//        val v492 = v491.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v493, v494) = v492
//        assert(v493.id == 92)
//        val v495 = v491.asInstanceOf[SequenceNode].children(3)
//        val BindNode(v496, v497) = v495
//        assert(v496.id == 92)
//        HexEscape(matchHexDigit(v494).toString + matchHexDigit(v497).toString)(v491)
//    }
//    v498
//  }
//
//  def matchHexLit(node: Node): HexLit = {
//    val BindNode(v499, v500) = node
//    val v505 = v499.id match {
//      case 212 =>
//        val v501 = v500.asInstanceOf[SequenceNode].children(2)
//        val v502 = unrollRepeat1(v501).map { elem =>
//          val BindNode(v503, v504) = elem
//          assert(v503.id == 92)
//          matchHexDigit(v504)
//        }
//        HexLit(v502.map(x => x.toString).mkString(""))(v500)
//    }
//    v505
//  }
//
//  def matchIdent(node: Node): Ident = {
//    val BindNode(v506, v507) = node
//    val v543 = v506.id match {
//      case 121 =>
//        val v508 = v507.asInstanceOf[SequenceNode].children.head
//        val BindNode(v509, v510) = v508
//        assert(v509.id == 122)
//        val BindNode(v511, v512) = v510
//        assert(v511.id == 123)
//        val BindNode(v513, v514) = v512
//        val v542 = v513.id match {
//          case 124 =>
//            val BindNode(v515, v516) = v514
//            assert(v515.id == 125)
//            val v517 = v516.asInstanceOf[SequenceNode].children.head
//            val BindNode(v518, v519) = v517
//            assert(v518.id == 126)
//            val v520 = v516.asInstanceOf[SequenceNode].children(1)
//            val v521 = unrollRepeat0(v520).map { elem =>
//              val BindNode(v522, v523) = elem
//              assert(v522.id == 131)
//              val BindNode(v524, v525) = v523
//              val v541 = v524.id match {
//                case 132 =>
//                  val BindNode(v526, v527) = v525
//                  assert(v526.id == 133)
//                  val v528 = v527.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v529, v530) = v528
//                  assert(v529.id == 126)
//                  matchLetter(v530)
//                case 134 =>
//                  val BindNode(v531, v532) = v525
//                  assert(v531.id == 135)
//                  val v533 = v532.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v534, v535) = v533
//                  assert(v534.id == 136)
//                  matchDecimalDigit(v535)
//                case 139 =>
//                  val BindNode(v536, v537) = v525
//                  assert(v536.id == 140)
//                  val v538 = v537.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v539, v540) = v538
//                  assert(v539.id == 141)
//                  v540.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//              }
//              v541
//            }
//            matchLetter(v519).toString + v521.map(x => x.toString).mkString("")
//        }
//        Ident(v542)(v507)
//    }
//    v543
//  }
//
//  def matchImport(node: Node): Import = {
//    val BindNode(v544, v545) = node
//    val v562 = v544.id match {
//      case 56 =>
//        val v546 = v545.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v547, v548) = v546
//        assert(v547.id == 62)
//        val BindNode(v549, v550) = v548
//        val v558 = v549.id match {
//          case 81 =>
//            None
//          case 63 =>
//            val BindNode(v551, v552) = v550
//            val v557 = v551.id match {
//              case 64 =>
//                val BindNode(v553, v554) = v552
//                assert(v553.id == 65)
//                ImportType.WEAK
//              case 72 =>
//                val BindNode(v555, v556) = v552
//                assert(v555.id == 73)
//                ImportType.PUBLIC
//            }
//            Some(v557)
//        }
//        val v559 = v545.asInstanceOf[SequenceNode].children(3)
//        val BindNode(v560, v561) = v559
//        assert(v560.id == 82)
//        Import(v558, matchStrLit(v561))(v545)
//    }
//    v562
//  }
//
//  def matchIntLit(node: Node): IntLit = {
//    val BindNode(v563, v564) = node
//    val v593 = v563.id match {
//      case 188 =>
//        val v565 = v564.asInstanceOf[SequenceNode].children.head
//        val BindNode(v566, v567) = v565
//        assert(v566.id == 189)
//        val BindNode(v568, v569) = v567
//        assert(v568.id == 190)
//        val BindNode(v570, v571) = v569
//        val v592 = v570.id match {
//          case 191 =>
//            val BindNode(v572, v573) = v571
//            assert(v572.id == 192)
//            val v574 = v573.asInstanceOf[SequenceNode].children.head
//            val BindNode(v575, v576) = v574
//            assert(v575.id == 193)
//            matchZeroLit(v576)
//          case 196 =>
//            val BindNode(v577, v578) = v571
//            assert(v577.id == 197)
//            val v579 = v578.asInstanceOf[SequenceNode].children.head
//            val BindNode(v580, v581) = v579
//            assert(v580.id == 198)
//            matchDecimalLit(v581)
//          case 203 =>
//            val BindNode(v582, v583) = v571
//            assert(v582.id == 204)
//            val v584 = v583.asInstanceOf[SequenceNode].children.head
//            val BindNode(v585, v586) = v584
//            assert(v585.id == 205)
//            matchOctalLit(v586)
//          case 209 =>
//            val BindNode(v587, v588) = v571
//            assert(v587.id == 210)
//            val v589 = v588.asInstanceOf[SequenceNode].children.head
//            val BindNode(v590, v591) = v589
//            assert(v590.id == 211)
//            matchHexLit(v591)
//        }
//        v592
//    }
//    v593
//  }
//
//  def matchKeyType(node: Node): MapKeyType.Value = {
//    val BindNode(v594, v595) = node
//    val v629 = v594.id match {
//      case 494 =>
//        val v596 = v595.asInstanceOf[SequenceNode].children.head
//        val BindNode(v597, v598) = v596
//        assert(v597.id == 495)
//        val JoinNode(_, v599, _) = v598
//        val BindNode(v600, v601) = v599
//        assert(v600.id == 496)
//        val BindNode(v602, v603) = v601
//        val v628 = v602.id match {
//          case 306 =>
//            val BindNode(v604, v605) = v603
//            assert(v604.id == 307)
//            MapKeyType.UINT64
//          case 322 =>
//            val BindNode(v606, v607) = v603
//            assert(v606.id == 323)
//            MapKeyType.FIXED64
//          case 291 =>
//            val BindNode(v608, v609) = v603
//            assert(v608.id == 292)
//            MapKeyType.INT32
//          case 338 =>
//            val BindNode(v610, v611) = v603
//            assert(v610.id == 339)
//            MapKeyType.STRING
//          case 330 =>
//            val BindNode(v612, v613) = v603
//            assert(v612.id == 331)
//            MapKeyType.SFIXED64
//          case 296 =>
//            val BindNode(v614, v615) = v603
//            assert(v614.id == 297)
//            MapKeyType.INT64
//          case 334 =>
//            val BindNode(v616, v617) = v603
//            assert(v616.id == 335)
//            MapKeyType.BOOL
//          case 326 =>
//            val BindNode(v618, v619) = v603
//            assert(v618.id == 327)
//            MapKeyType.SFIXED32
//          case 318 =>
//            val BindNode(v620, v621) = v603
//            assert(v620.id == 319)
//            MapKeyType.FIXED32
//          case 302 =>
//            val BindNode(v622, v623) = v603
//            assert(v622.id == 303)
//            MapKeyType.UINT32
//          case 314 =>
//            val BindNode(v624, v625) = v603
//            assert(v624.id == 315)
//            MapKeyType.SINT64
//          case 310 =>
//            val BindNode(v626, v627) = v603
//            assert(v626.id == 311)
//            MapKeyType.SINT32
//        }
//        v628
//    }
//    v629
//  }
//
//  def matchLabel(node: Node): Label.Value = {
//    val BindNode(v630, v631) = node
//    val v647 = v630.id match {
//      case 260 =>
//        val v632 = v631.asInstanceOf[SequenceNode].children.head
//        val BindNode(v633, v634) = v632
//        assert(v633.id == 261)
//        val JoinNode(_, v635, _) = v634
//        val BindNode(v636, v637) = v635
//        assert(v636.id == 262)
//        val BindNode(v638, v639) = v637
//        val v646 = v638.id match {
//          case 263 =>
//            val BindNode(v640, v641) = v639
//            assert(v640.id == 264)
//            Label.REQUIRED
//          case 269 =>
//            val BindNode(v642, v643) = v639
//            assert(v642.id == 270)
//            Label.OPTIONAL
//          case 273 =>
//            val BindNode(v644, v645) = v639
//            assert(v644.id == 274)
//            Label.REPEATED
//        }
//        v646
//    }
//    v647
//  }
//
//  def matchLetter(node: Node): Char = {
//    val BindNode(v648, v649) = node
//    val v653 = v648.id match {
//      case 127 =>
//        val v650 = v649.asInstanceOf[SequenceNode].children.head
//        val BindNode(v651, v652) = v650
//        assert(v651.id == 128)
//        v652.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//    }
//    v653
//  }
//
//  def matchMapField(node: Node): MapField = {
//    val BindNode(v654, v655) = node
//    val v682 = v654.id match {
//      case 488 =>
//        val v656 = v655.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v657, v658) = v656
//        assert(v657.id == 493)
//        val v659 = v655.asInstanceOf[SequenceNode].children(8)
//        val BindNode(v660, v661) = v659
//        assert(v660.id == 277)
//        val v662 = v655.asInstanceOf[SequenceNode].children(12)
//        val BindNode(v663, v664) = v662
//        assert(v663.id == 498)
//        val v665 = v655.asInstanceOf[SequenceNode].children(16)
//        val BindNode(v666, v667) = v665
//        assert(v666.id == 357)
//        val v668 = v655.asInstanceOf[SequenceNode].children(17)
//        val BindNode(v669, v670) = v668
//        assert(v669.id == 359)
//        val BindNode(v671, v672) = v670
//        val v681 = v671.id match {
//          case 81 =>
//            None
//          case 360 =>
//            val BindNode(v673, v674) = v672
//            val v680 = v673.id match {
//              case 361 =>
//                val BindNode(v675, v676) = v674
//                assert(v675.id == 362)
//                val v677 = v676.asInstanceOf[SequenceNode].children(3)
//                val BindNode(v678, v679) = v677
//                assert(v678.id == 364)
//                matchFieldOptions(v679)
//            }
//            Some(v680)
//        }
//        MapField(matchKeyType(v658), matchType(v661), matchMapName(v664), matchFieldNumber(v667), v681)(v655)
//    }
//    v682
//  }
//
//  def matchMapName(node: Node): Ident = {
//    val BindNode(v683, v684) = node
//    val v688 = v683.id match {
//      case 159 =>
//        val v685 = v684.asInstanceOf[SequenceNode].children.head
//        val BindNode(v686, v687) = v685
//        assert(v686.id == 120)
//        matchIdent(v687)
//    }
//    v688
//  }
//
//  def matchMessage(node: Node): Message = {
//    val BindNode(v689, v690) = node
//    val v697 = v689.id match {
//      case 242 =>
//        val v691 = v690.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v692, v693) = v691
//        assert(v692.id == 246)
//        val v694 = v690.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v695, v696) = v694
//        assert(v695.id == 247)
//        Message(matchMessageName(v693), matchMessageBody(v696))(v690)
//    }
//    v697
//  }
//
//  def matchMessageBody(node: Node): List[MessageBodyElem] = {
//    val BindNode(v698, v699) = node
//    val v712 = v698.id match {
//      case 248 =>
//        val v700 = v699.asInstanceOf[SequenceNode].children(1)
//        val v701 = unrollRepeat0(v700).map { elem =>
//          val BindNode(v702, v703) = elem
//          assert(v702.id == 252)
//          val BindNode(v704, v705) = v703
//          val v711 = v704.id match {
//            case 253 =>
//              val BindNode(v706, v707) = v705
//              assert(v706.id == 254)
//              val v708 = v707.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v709, v710) = v708
//              assert(v709.id == 255)
//              matchMessageBodyElem(v710)
//          }
//          v711
//        }
//        v701
//    }
//    v712
//  }
//
//  def matchMessageBodyElem(node: Node): MessageBodyElem = {
//    val BindNode(v713, v714) = node
//    val v748 = v713.id match {
//      case 442 =>
//        val v715 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v716, v717) = v715
//        assert(v716.id == 443)
//        matchExtensions(v717)
//      case 499 =>
//        val v718 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v719, v720) = v718
//        assert(v719.id == 500)
//        matchReserved(v720)
//      case 486 =>
//        val v721 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v722, v723) = v721
//        assert(v722.id == 487)
//        matchMapField(v723)
//      case 413 =>
//        val v724 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v725, v726) = v724
//        assert(v725.id == 414)
//        matchExtend(v726)
//      case 427 =>
//        val v727 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v728, v729) = v727
//        assert(v728.id == 428)
//        matchGroup(v729)
//      case 375 =>
//        val v730 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v731, v732) = v730
//        assert(v731.id == 376)
//        matchEnum(v732)
//      case 409 =>
//        val v733 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v734, v735) = v733
//        assert(v734.id == 410)
//        matchEmptyStatement(v735)
//      case 240 =>
//        val v736 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v737, v738) = v736
//        assert(v737.id == 241)
//        matchMessage(v738)
//      case 149 =>
//        val v739 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v740, v741) = v739
//        assert(v740.id == 150)
//        matchOption(v741)
//      case 469 =>
//        val v742 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v743, v744) = v742
//        assert(v743.id == 470)
//        matchOneof(v744)
//      case 256 =>
//        val v745 = v714.asInstanceOf[SequenceNode].children.head
//        val BindNode(v746, v747) = v745
//        assert(v746.id == 257)
//        matchField(v747)
//    }
//    v748
//  }
//
//  def matchMessageName(node: Node): Ident = {
//    val BindNode(v749, v750) = node
//    val v754 = v749.id match {
//      case 159 =>
//        val v751 = v750.asInstanceOf[SequenceNode].children.head
//        val BindNode(v752, v753) = v751
//        assert(v752.id == 120)
//        matchIdent(v753)
//    }
//    v754
//  }
//
//  def matchMessageType(node: Node): MessageType = {
//    val BindNode(v755, v756) = node
//    val v778 = v755.id match {
//      case 349 =>
//        val v757 = v756.asInstanceOf[SequenceNode].children.head
//        val BindNode(v758, v759) = v757
//        assert(v758.id == 350)
//        val BindNode(v760, v761) = v759
//        val v762 = v760.id match {
//          case 81 =>
//            None
//          case 147 =>
//            Some(v761.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
//        }
//        val v763 = v756.asInstanceOf[SequenceNode].children(1)
//        val v764 = unrollRepeat0(v763).map { elem =>
//          val BindNode(v765, v766) = elem
//          assert(v765.id == 353)
//          val BindNode(v767, v768) = v766
//          val v774 = v767.id match {
//            case 354 =>
//              val BindNode(v769, v770) = v768
//              assert(v769.id == 355)
//              val v771 = v770.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v772, v773) = v771
//              assert(v772.id == 147)
//              v773.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//          }
//          v774
//        }
//        val v775 = v756.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v776, v777) = v775
//        assert(v776.id == 246)
//        MessageType(v762.isDefined, v764, matchMessageName(v777))(v756)
//    }
//    v778
//  }
//
//  def matchOctEscape(node: Node): OctalEscape = {
//    val BindNode(v779, v780) = node
//    val v790 = v779.id match {
//      case 97 =>
//        val v781 = v780.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v782, v783) = v781
//        assert(v782.id == 98)
//        val v784 = v780.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v785, v786) = v784
//        assert(v785.id == 98)
//        val v787 = v780.asInstanceOf[SequenceNode].children(3)
//        val BindNode(v788, v789) = v787
//        assert(v788.id == 98)
//        OctalEscape(matchOctalDigit(v783).toString + matchOctalDigit(v786).toString + matchOctalDigit(v789).toString)(v780)
//    }
//    v790
//  }
//
//  def matchOctalDigit(node: Node): Char = {
//    val BindNode(v791, v792) = node
//    val v796 = v791.id match {
//      case 99 =>
//        val v793 = v792.asInstanceOf[SequenceNode].children.head
//        val BindNode(v794, v795) = v793
//        assert(v794.id == 100)
//        v795.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//    }
//    v796
//  }
//
//  def matchOctalLit(node: Node): OctalLit = {
//    val BindNode(v797, v798) = node
//    val v806 = v797.id match {
//      case 206 =>
//        val v799 = v798.asInstanceOf[SequenceNode].children.head
//        val BindNode(v800, v801) = v799
//        assert(v800.id == 195)
//        val v802 = v798.asInstanceOf[SequenceNode].children(1)
//        val v803 = unrollRepeat1(v802).map { elem =>
//          val BindNode(v804, v805) = elem
//          assert(v804.id == 98)
//          matchOctalDigit(v805)
//        }
//        OctalLit(v801.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v803.map(x => x.toString).mkString(""))(v798)
//    }
//    v806
//  }
//
//  def matchOneof(node: Node): OneofDef = {
//    val BindNode(v807, v808) = node
//    val v842 = v807.id match {
//      case 471 =>
//        val v809 = v808.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v810, v811) = v809
//        assert(v810.id == 475)
//        val v812 = v808.asInstanceOf[SequenceNode].children(5)
//        val v813 = unrollRepeat0(v812).map { elem =>
//          val BindNode(v814, v815) = elem
//          assert(v814.id == 478)
//          val BindNode(v816, v817) = v815
//          val v841 = v816.id match {
//            case 479 =>
//              val BindNode(v818, v819) = v817
//              assert(v818.id == 480)
//              val v820 = v819.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v821, v822) = v820
//              assert(v821.id == 481)
//              val BindNode(v823, v824) = v822
//              val v840 = v823.id match {
//                case 148 =>
//                  val BindNode(v825, v826) = v824
//                  assert(v825.id == 149)
//                  val v827 = v826.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v828, v829) = v827
//                  assert(v828.id == 150)
//                  matchOption(v829)
//                case 482 =>
//                  val BindNode(v830, v831) = v824
//                  assert(v830.id == 483)
//                  val v832 = v831.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v833, v834) = v832
//                  assert(v833.id == 484)
//                  matchOneofField(v834)
//                case 408 =>
//                  val BindNode(v835, v836) = v824
//                  assert(v835.id == 409)
//                  val v837 = v836.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v838, v839) = v837
//                  assert(v838.id == 410)
//                  matchEmptyStatement(v839)
//              }
//              v840
//          }
//          v841
//        }
//        OneofDef(matchOneofName(v811), v813)(v808)
//    }
//    v842
//  }
//
//  def matchOneofField(node: Node): OneofField = {
//    val BindNode(v843, v844) = node
//    val v868 = v843.id match {
//      case 485 =>
//        val v845 = v844.asInstanceOf[SequenceNode].children.head
//        val BindNode(v846, v847) = v845
//        assert(v846.id == 277)
//        val v848 = v844.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v849, v850) = v848
//        assert(v849.id == 356)
//        val v851 = v844.asInstanceOf[SequenceNode].children(6)
//        val BindNode(v852, v853) = v851
//        assert(v852.id == 357)
//        val v854 = v844.asInstanceOf[SequenceNode].children(7)
//        val BindNode(v855, v856) = v854
//        assert(v855.id == 359)
//        val BindNode(v857, v858) = v856
//        val v867 = v857.id match {
//          case 81 =>
//            None
//          case 360 =>
//            val BindNode(v859, v860) = v858
//            val v866 = v859.id match {
//              case 361 =>
//                val BindNode(v861, v862) = v860
//                assert(v861.id == 362)
//                val v863 = v862.asInstanceOf[SequenceNode].children(3)
//                val BindNode(v864, v865) = v863
//                assert(v864.id == 364)
//                matchFieldOptions(v865)
//            }
//            Some(v866)
//        }
//        OneofField(matchType(v847), matchFieldName(v850), matchFieldNumber(v853), v867)(v844)
//    }
//    v868
//  }
//
//  def matchOneofName(node: Node): Ident = {
//    val BindNode(v869, v870) = node
//    val v874 = v869.id match {
//      case 159 =>
//        val v871 = v870.asInstanceOf[SequenceNode].children.head
//        val BindNode(v872, v873) = v871
//        assert(v872.id == 120)
//        matchIdent(v873)
//    }
//    v874
//  }
//
//  def matchOption(node: Node): OptionDef = {
//    val BindNode(v875, v876) = node
//    val v883 = v875.id match {
//      case 151 =>
//        val v877 = v876.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v878, v879) = v877
//        assert(v878.id == 155)
//        val v880 = v876.asInstanceOf[SequenceNode].children(6)
//        val BindNode(v881, v882) = v880
//        assert(v881.id == 169)
//        OptionDef(matchOptionName(v879), matchConstant(v882))(v876)
//    }
//    v883
//  }
//
//  def matchOptionName(node: Node): OptionName = {
//    val BindNode(v884, v885) = node
//    val v914 = v884.id match {
//      case 156 =>
//        val v886 = v885.asInstanceOf[SequenceNode].children.head
//        val BindNode(v887, v888) = v886
//        assert(v887.id == 157)
//        val BindNode(v889, v890) = v888
//        val v901 = v889.id match {
//          case 158 =>
//            val BindNode(v891, v892) = v890
//            assert(v891.id == 159)
//            val v893 = v892.asInstanceOf[SequenceNode].children.head
//            val BindNode(v894, v895) = v893
//            assert(v894.id == 120)
//            matchIdent(v895)
//          case 160 =>
//            val BindNode(v896, v897) = v890
//            assert(v896.id == 161)
//            val v898 = v897.asInstanceOf[SequenceNode].children(2)
//            val BindNode(v899, v900) = v898
//            assert(v899.id == 118)
//            matchFullIdent(v900)
//        }
//        val v902 = v885.asInstanceOf[SequenceNode].children(1)
//        val v903 = unrollRepeat0(v902).map { elem =>
//          val BindNode(v904, v905) = elem
//          assert(v904.id == 166)
//          val BindNode(v906, v907) = v905
//          val v913 = v906.id match {
//            case 167 =>
//              val BindNode(v908, v909) = v907
//              assert(v908.id == 168)
//              val v910 = v909.asInstanceOf[SequenceNode].children(2)
//              val BindNode(v911, v912) = v910
//              assert(v911.id == 120)
//              matchIdent(v912)
//          }
//          v913
//        }
//        OptionName(v901, v903)(v885)
//    }
//    v914
//  }
//
//  def matchPackage(node: Node): Package = {
//    val BindNode(v915, v916) = node
//    val v920 = v915.id match {
//      case 113 =>
//        val v917 = v916.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v918, v919) = v917
//        assert(v918.id == 118)
//        Package(matchFullIdent(v919))(v916)
//    }
//    v920
//  }
//
//  def matchProto2(node: Node): Proto3 = {
//    val BindNode(v921, v922) = node
//    val v963 = v921.id match {
//      case 3 =>
//        val v923 = v922.asInstanceOf[SequenceNode].children(2)
//        val v924 = unrollRepeat0(v923).map { elem =>
//          val BindNode(v925, v926) = elem
//          assert(v925.id == 49)
//          val BindNode(v927, v928) = v926
//          val v962 = v927.id match {
//            case 50 =>
//              val BindNode(v929, v930) = v928
//              assert(v929.id == 51)
//              val v931 = v930.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v932, v933) = v931
//              assert(v932.id == 52)
//              val BindNode(v934, v935) = v933
//              val v961 = v934.id match {
//                case 110 =>
//                  val BindNode(v936, v937) = v935
//                  assert(v936.id == 111)
//                  val v938 = v937.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v939, v940) = v938
//                  assert(v939.id == 112)
//                  matchPackage(v940)
//                case 148 =>
//                  val BindNode(v941, v942) = v935
//                  assert(v941.id == 149)
//                  val v943 = v942.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v944, v945) = v943
//                  assert(v944.id == 150)
//                  matchOption(v945)
//                case 237 =>
//                  val BindNode(v946, v947) = v935
//                  assert(v946.id == 238)
//                  val v948 = v947.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v949, v950) = v948
//                  assert(v949.id == 239)
//                  matchTopLevelDef(v950)
//                case 53 =>
//                  val BindNode(v951, v952) = v935
//                  assert(v951.id == 54)
//                  val v953 = v952.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v954, v955) = v953
//                  assert(v954.id == 55)
//                  matchImport(v955)
//                case 408 =>
//                  val BindNode(v956, v957) = v935
//                  assert(v956.id == 409)
//                  val v958 = v957.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v959, v960) = v958
//                  assert(v959.id == 410)
//                  matchEmptyStatement(v960)
//              }
//              v961
//          }
//          v962
//        }
//        Proto3(v924)(v922)
//    }
//    v963
//  }
//
//  def matchRange(node: Node): Range = {
//    val BindNode(v964, v965) = node
//    val v983 = v964.id match {
//      case 451 =>
//        val v966 = v965.asInstanceOf[SequenceNode].children.head
//        val BindNode(v967, v968) = v966
//        assert(v967.id == 187)
//        val v969 = v965.asInstanceOf[SequenceNode].children(1)
//        val BindNode(v970, v971) = v969
//        assert(v970.id == 452)
//        val BindNode(v972, v973) = v971
//        val v982 = v972.id match {
//          case 81 =>
//            None
//          case 453 =>
//            val BindNode(v974, v975) = v973
//            val v981 = v974.id match {
//              case 454 =>
//                val BindNode(v976, v977) = v975
//                assert(v976.id == 455)
//                val v978 = v977.asInstanceOf[SequenceNode].children(3)
//                val BindNode(v979, v980) = v978
//                assert(v979.id == 459)
//                matchRangeEnd(v980)
//            }
//            Some(v981)
//        }
//        Range(matchIntLit(v968), v982)(v965)
//    }
//    v983
//  }
//
//  def matchRangeEnd(node: Node): RangeEnd = {
//    val BindNode(v984, v985) = node
//    val v989 = v984.id match {
//      case 358 =>
//        val v986 = v985.asInstanceOf[SequenceNode].children.head
//        val BindNode(v987, v988) = v986
//        assert(v987.id == 187)
//        RangeEndValue(matchIntLit(v988))(v985)
//      case 460 =>
//        RangeEndMax()(v985)
//    }
//    v989
//  }
//
//  def matchRanges(node: Node): Ranges = {
//    val BindNode(v990, v991) = node
//    val v1007 = v990.id match {
//      case 449 =>
//        val v992 = v991.asInstanceOf[SequenceNode].children.head
//        val BindNode(v993, v994) = v992
//        assert(v993.id == 450)
//        val v995 = v991.asInstanceOf[SequenceNode].children(1)
//        val v996 = unrollRepeat0(v995).map { elem =>
//          val BindNode(v997, v998) = elem
//          assert(v997.id == 466)
//          val BindNode(v999, v1000) = v998
//          val v1006 = v999.id match {
//            case 467 =>
//              val BindNode(v1001, v1002) = v1000
//              assert(v1001.id == 468)
//              val v1003 = v1002.asInstanceOf[SequenceNode].children(3)
//              val BindNode(v1004, v1005) = v1003
//              assert(v1004.id == 450)
//              matchRange(v1005)
//          }
//          v1006
//        }
//        Ranges(List(matchRange(v994)) ++ v996)(v991)
//    }
//    v1007
//  }
//
//  def matchReserved(node: Node): Reserved = {
//    val BindNode(v1008, v1009) = node
//    val v1026 = v1008.id match {
//      case 501 =>
//        val v1010 = v1009.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v1011, v1012) = v1010
//        assert(v1011.id == 506)
//        val BindNode(v1013, v1014) = v1012
//        val v1025 = v1013.id match {
//          case 507 =>
//            val BindNode(v1015, v1016) = v1014
//            assert(v1015.id == 508)
//            val v1017 = v1016.asInstanceOf[SequenceNode].children.head
//            val BindNode(v1018, v1019) = v1017
//            assert(v1018.id == 448)
//            matchRanges(v1019)
//          case 509 =>
//            val BindNode(v1020, v1021) = v1014
//            assert(v1020.id == 510)
//            val v1022 = v1021.asInstanceOf[SequenceNode].children.head
//            val BindNode(v1023, v1024) = v1022
//            assert(v1023.id == 511)
//            matchFieldNames(v1024)
//        }
//        Reserved(v1025)(v1009)
//    }
//    v1026
//  }
//
//  def matchRpc(node: Node): Rpc = {
//    val BindNode(v1027, v1028) = node
//    val v1071 = v1027.id match {
//      case 534 =>
//        val v1029 = v1028.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v1030, v1031) = v1029
//        assert(v1030.id == 538)
//        val v1032 = v1028.asInstanceOf[SequenceNode].children(5)
//        val BindNode(v1033, v1034) = v1032
//        assert(v1033.id == 539)
//        val BindNode(v1035, v1036) = v1034
//        val v1046 = v1035.id match {
//          case 81 =>
//            None
//          case 540 =>
//            val BindNode(v1037, v1038) = v1036
//            val v1045 = v1037.id match {
//              case 541 =>
//                val BindNode(v1039, v1040) = v1038
//                assert(v1039.id == 542)
//                val v1041 = v1040.asInstanceOf[SequenceNode].children(1)
//                val BindNode(v1042, v1043) = v1041
//                assert(v1042.id == 543)
//                val JoinNode(_, v1044, _) = v1043
//                "stream"
//            }
//            Some(v1045)
//        }
//        val v1047 = v1028.asInstanceOf[SequenceNode].children(7)
//        val BindNode(v1048, v1049) = v1047
//        assert(v1048.id == 348)
//        val v1050 = v1028.asInstanceOf[SequenceNode].children(14)
//        val BindNode(v1051, v1052) = v1050
//        assert(v1051.id == 539)
//        val BindNode(v1053, v1054) = v1052
//        val v1064 = v1053.id match {
//          case 81 =>
//            None
//          case 540 =>
//            val BindNode(v1055, v1056) = v1054
//            val v1063 = v1055.id match {
//              case 541 =>
//                val BindNode(v1057, v1058) = v1056
//                assert(v1057.id == 542)
//                val v1059 = v1058.asInstanceOf[SequenceNode].children(1)
//                val BindNode(v1060, v1061) = v1059
//                assert(v1060.id == 543)
//                val JoinNode(_, v1062, _) = v1061
//                "stream"
//            }
//            Some(v1063)
//        }
//        val v1065 = v1028.asInstanceOf[SequenceNode].children(16)
//        val BindNode(v1066, v1067) = v1065
//        assert(v1066.id == 348)
//        val v1068 = v1028.asInstanceOf[SequenceNode].children(20)
//        val BindNode(v1069, v1070) = v1068
//        assert(v1069.id == 549)
//        Rpc(matchRpcName(v1031), v1046.isDefined, matchMessageType(v1049), v1064.isDefined, matchMessageType(v1067), matchRpcEnding(v1070))(v1028)
//    }
//    v1071
//  }
//
//  def matchRpcEnding(node: Node): List[Option[OptionDef]] = {
//    val BindNode(v1072, v1073) = node
//    val v1096 = v1072.id match {
//      case 550 =>
//        val v1074 = v1073.asInstanceOf[SequenceNode].children(1)
//        val v1075 = unrollRepeat0(v1074).map { elem =>
//          val BindNode(v1076, v1077) = elem
//          assert(v1076.id == 553)
//          val BindNode(v1078, v1079) = v1077
//          val v1095 = v1078.id match {
//            case 554 =>
//              val BindNode(v1080, v1081) = v1079
//              assert(v1080.id == 555)
//              val v1082 = v1081.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v1083, v1084) = v1082
//              assert(v1083.id == 556)
//              val BindNode(v1085, v1086) = v1084
//              val v1094 = v1085.id match {
//                case 148 =>
//                  val BindNode(v1087, v1088) = v1086
//                  assert(v1087.id == 149)
//                  val v1089 = v1088.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1090, v1091) = v1089
//                  assert(v1090.id == 150)
//                  Some(matchOption(v1091))
//                case 408 =>
//                  val BindNode(v1092, v1093) = v1086
//                  assert(v1092.id == 409)
//                  None
//              }
//              v1094
//          }
//          v1095
//        }
//        v1075
//      case 411 =>
//        List()
//    }
//    v1096
//  }
//
//  def matchRpcName(node: Node): Ident = {
//    val BindNode(v1097, v1098) = node
//    val v1102 = v1097.id match {
//      case 159 =>
//        val v1099 = v1098.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1100, v1101) = v1099
//        assert(v1100.id == 120)
//        matchIdent(v1101)
//    }
//    v1102
//  }
//
//  def matchService(node: Node): Service = {
//    val BindNode(v1103, v1104) = node
//    val v1143 = v1103.id match {
//      case 520 =>
//        val v1105 = v1104.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v1106, v1107) = v1105
//        assert(v1106.id == 524)
//        val v1108 = v1104.asInstanceOf[SequenceNode].children(5)
//        val v1109 = unrollRepeat0(v1108).map { elem =>
//          val BindNode(v1110, v1111) = elem
//          assert(v1110.id == 527)
//          val BindNode(v1112, v1113) = v1111
//          val v1142 = v1112.id match {
//            case 528 =>
//              val BindNode(v1114, v1115) = v1113
//              assert(v1114.id == 529)
//              val v1116 = v1115.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v1117, v1118) = v1116
//              assert(v1117.id == 530)
//              val BindNode(v1119, v1120) = v1118
//              val v1141 = v1119.id match {
//                case 148 =>
//                  val BindNode(v1121, v1122) = v1120
//                  assert(v1121.id == 149)
//                  val v1123 = v1122.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1124, v1125) = v1123
//                  assert(v1124.id == 150)
//                  matchOption(v1125)
//                case 531 =>
//                  val BindNode(v1126, v1127) = v1120
//                  assert(v1126.id == 532)
//                  val v1128 = v1127.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1129, v1130) = v1128
//                  assert(v1129.id == 533)
//                  matchRpc(v1130)
//                case 557 =>
//                  val BindNode(v1131, v1132) = v1120
//                  assert(v1131.id == 558)
//                  val v1133 = v1132.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1134, v1135) = v1133
//                  assert(v1134.id == 559)
//                  matchStream(v1135)
//                case 408 =>
//                  val BindNode(v1136, v1137) = v1120
//                  assert(v1136.id == 409)
//                  val v1138 = v1137.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1139, v1140) = v1138
//                  assert(v1139.id == 410)
//                  matchEmptyStatement(v1140)
//              }
//              v1141
//          }
//          v1142
//        }
//        Service(matchServiceName(v1107), v1109)(v1104)
//    }
//    v1143
//  }
//
//  def matchServiceName(node: Node): Ident = {
//    val BindNode(v1144, v1145) = node
//    val v1149 = v1144.id match {
//      case 159 =>
//        val v1146 = v1145.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1147, v1148) = v1146
//        assert(v1147.id == 120)
//        matchIdent(v1148)
//    }
//    v1149
//  }
//
//  def matchSign(node: Node): Sign.Value = {
//    val BindNode(v1150, v1151) = node
//    val v1152 = v1150.id match {
//      case 183 =>
//        Sign.PLUS
//      case 185 =>
//        Sign.MINUS
//    }
//    v1152
//  }
//
//  def matchStrLit(node: Node): StrLit = {
//    val BindNode(v1153, v1154) = node
//    val v1163 = v1153.id match {
//      case 83 =>
//        val v1155 = v1154.asInstanceOf[SequenceNode].children(1)
//        val v1156 = unrollRepeat0(v1155).map { elem =>
//          val BindNode(v1157, v1158) = elem
//          assert(v1157.id == 86)
//          matchCharValue(v1158)
//        }
//        SingleQuoteStrLit(v1156)(v1154)
//      case 109 =>
//        val v1159 = v1154.asInstanceOf[SequenceNode].children(1)
//        val v1160 = unrollRepeat0(v1159).map { elem =>
//          val BindNode(v1161, v1162) = elem
//          assert(v1161.id == 86)
//          matchCharValue(v1162)
//        }
//        DoubleQuoteStrLit(v1160)(v1154)
//    }
//    v1163
//  }
//
//  def matchStream(node: Node): Stream = {
//    val BindNode(v1164, v1165) = node
//    val v1178 = v1164.id match {
//      case 560 =>
//        val v1166 = v1165.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v1167, v1168) = v1166
//        assert(v1167.id == 561)
//        val v1169 = v1165.asInstanceOf[SequenceNode].children(6)
//        val BindNode(v1170, v1171) = v1169
//        assert(v1170.id == 348)
//        val v1172 = v1165.asInstanceOf[SequenceNode].children(10)
//        val BindNode(v1173, v1174) = v1172
//        assert(v1173.id == 348)
//        val v1175 = v1165.asInstanceOf[SequenceNode].children(14)
//        val BindNode(v1176, v1177) = v1175
//        assert(v1176.id == 562)
//        Stream(matchStreamName(v1168), matchMessageType(v1171), matchMessageType(v1174), matchStreamEnding(v1177))(v1165)
//    }
//    v1178
//  }
//
//  def matchStreamEnding(node: Node): List[Option[OptionDef]] = {
//    val BindNode(v1179, v1180) = node
//    val v1203 = v1179.id match {
//      case 550 =>
//        val v1181 = v1180.asInstanceOf[SequenceNode].children(1)
//        val v1182 = unrollRepeat0(v1181).map { elem =>
//          val BindNode(v1183, v1184) = elem
//          assert(v1183.id == 553)
//          val BindNode(v1185, v1186) = v1184
//          val v1202 = v1185.id match {
//            case 554 =>
//              val BindNode(v1187, v1188) = v1186
//              assert(v1187.id == 555)
//              val v1189 = v1188.asInstanceOf[SequenceNode].children(1)
//              val BindNode(v1190, v1191) = v1189
//              assert(v1190.id == 556)
//              val BindNode(v1192, v1193) = v1191
//              val v1201 = v1192.id match {
//                case 148 =>
//                  val BindNode(v1194, v1195) = v1193
//                  assert(v1194.id == 149)
//                  val v1196 = v1195.asInstanceOf[SequenceNode].children.head
//                  val BindNode(v1197, v1198) = v1196
//                  assert(v1197.id == 150)
//                  Some(matchOption(v1198))
//                case 408 =>
//                  val BindNode(v1199, v1200) = v1193
//                  assert(v1199.id == 409)
//                  None
//              }
//              v1201
//          }
//          v1202
//        }
//        v1182
//      case 411 =>
//        List()
//    }
//    v1203
//  }
//
//  def matchStreamName(node: Node): Ident = {
//    val BindNode(v1204, v1205) = node
//    val v1209 = v1204.id match {
//      case 159 =>
//        val v1206 = v1205.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1207, v1208) = v1206
//        assert(v1207.id == 120)
//        matchIdent(v1208)
//    }
//    v1209
//  }
//
//  def matchTopLevelDef(node: Node): TopLevelDef = {
//    val BindNode(v1210, v1211) = node
//    val v1224 = v1210.id match {
//      case 240 =>
//        val v1212 = v1211.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1213, v1214) = v1212
//        assert(v1213.id == 241)
//        matchMessage(v1214)
//      case 375 =>
//        val v1215 = v1211.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1216, v1217) = v1215
//        assert(v1216.id == 376)
//        matchEnum(v1217)
//      case 413 =>
//        val v1218 = v1211.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1219, v1220) = v1218
//        assert(v1219.id == 414)
//        matchExtend(v1220)
//      case 518 =>
//        val v1221 = v1211.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1222, v1223) = v1221
//        assert(v1222.id == 519)
//        matchService(v1223)
//    }
//    v1224
//  }
//
//  def matchType(node: Node): Type = {
//    val BindNode(v1225, v1226) = node
//    val v1235 = v1225.id match {
//      case 278 =>
//        val v1227 = v1226.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1228, v1229) = v1227
//        assert(v1228.id == 279)
//        BuiltinType(matchBuiltinType(v1229))(v1226)
//      case 346 =>
//        val v1230 = v1226.asInstanceOf[SequenceNode].children.head
//        val BindNode(v1231, v1232) = v1230
//        assert(v1231.id == 347)
//        val BindNode(v1233, v1234) = v1232
//        assert(v1233.id == 348)
//        MessageOrEnumType(matchMessageType(v1234))(v1226)
//    }
//    v1235
//  }
//
//  def matchZeroLit(node: Node): DecimalLit = {
//    val BindNode(v1236, v1237) = node
//    val v1238 = v1236.id match {
//      case 194 =>
//        DecimalLit("0")(v1237)
//    }
//    v1238
//  }
//
//  def matchStart(node: Node): Proto3 = {
//    val BindNode(start, BindNode(_, body)) = node
//    assert(start.id == 1)
//    matchProto2(body)
//  }
//
//  val naiveParser = new NaiveParser(ngrammar)
//
//  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
//    naiveParser.parse(text)
//
//  def parseAst(text: String): Either[Proto3, ParsingErrors.ParsingError] =
//    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
//
//  def main(args: Array[String]): Unit = {
//    println(parseAst(
//      """syntax = "proto2";
//        |package foo.bar.bar.goo;
//        |import public "other.proto";
//        |option java_package = "com.example.foo";
//        |enum EnumAllowingAlias {
//        |  option allow_alias = true;
//        |  UNKNOWN = 0;
//        |  STARTED = 1;
//        |  RUNNING = 2 [(custom_option) = "hello world"];
//        |}
//        |message Outer {
//        |  option (my_option).a = true;
//        |  message Inner {
//        |    required int64 ival = 1;
//        |  }
//        |  repeated Inner inner_message = 2;
//        |  optional EnumAllowingAlias enum_field = 3;
//        |  map<int32, string> my_map = 4;
//        |  map<int32, Foo> my_map = 4;
//        |  extensions 20 to 30;
//        |}
//        |message Foo {
//        |  repeated group Result = 1 {
//        |    required string url = 2;
//        |    optional string title = 3;
//        |    repeated string snippets = 4;
//        |  }
//        |  oneof foo {
//        |    string name = 4;
//        |    SubMessage sub_message = 9;
//        |  }
//        |}
//        |""".stripMargin))
//  }
//}
