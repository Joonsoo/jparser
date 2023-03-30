//package com.giyeok.jparser.tests.metalang3a.codegen
//
//import com.giyeok.jparser.metalang3.generated.{ExpressionGrammarAst, MetaLang3Ast}
//import com.giyeok.jparser.proto.GrammarProtobufConverter
//import org.junit.jupiter.api.Test
//
//class GrammarProtobufConverterTest {
//  @Test
//  def testConvert(): Unit = {
//    val original = MetaLang3Ast.ngrammar
//    val converted = GrammarProtobufConverter.convertNGrammarToProto(original)
//    val rev = GrammarProtobufConverter.convertProtoToNGrammar(converted)
//
//    original.nsymbols.toList.sortBy(_._1).zip(rev.nsymbols.toList.sortBy(_._1)).filter(p => p._1 != p._2).foreach { pair =>
//      println(s"${pair._1._1} ${pair._1._2} ${pair._2._2}")
//    }
//    original.nsequences.toList.sortBy(_._1).zip(rev.nsequences.toList.sortBy(_._1)).filter(p => p._1 != p._2).foreach { pair =>
//      println(s"${pair._1._1} ${pair._1._2} ${pair._2._2}")
//    }
//    assert(original.nsymbols == rev.nsymbols)
//    assert(original.nsequences == rev.nsequences)
//    assert(original.startSymbol == rev.startSymbol)
//  }
//}
