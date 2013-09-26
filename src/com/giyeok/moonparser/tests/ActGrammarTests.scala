package com.giyeok.moonparser.tests

import com.giyeok.moonparser.ActGrammar
import com.giyeok.moonparser.dynamic.BasicBlackboxParser

object ActGrammarTests {
	def main(args: Array[String]) = {
		val parser = new BasicBlackboxParser(TestGrammar)
		
		println(TestGrammar.actions)
		test(TestGrammar.process(parser.parse("456").parsed.get), 456)
		test(TestGrammar.process(parser.parse("a").parsed.get), "A")
		test(TestGrammar.process(parser.parse("c").parsed.get), "C")
		
		test(TestGrammar.process(parser.parse("a1").parsed.get), "T1")
		test(TestGrammar.process(parser.parse("a2").parsed.get), "T2")
		test(TestGrammar.process(parser.parse("a3").parsed.get), "T3")
	}
	
	def test(result: Any, correct: Any) = {
		println(result)
		if (result != correct) println("  => Error")
	}
}

object TestGrammar extends ActGrammar {
	val name = "Test"
	val startSymbol: String = "Start"
	
	val rulesWithAct = Map(
		"Start" -> List(
			seq(n("Q") act ((_, objs) => objs(0).asInstanceOf[Int] * 100),
				n("Q") act ((_, objs) => objs(0).asInstanceOf[Int] * 10),
				n("Q") act ((_, objs) => objs(0).asInstanceOf[Int] * 1)) act ((_, objs) =>
					objs(2).asInstanceOf[Int] + objs(1).asInstanceOf[Int] + objs(0).asInstanceOf[Int]
				),
			n("A"), n("B"), n("C"), n("D"),
			n("T")
		),
		
		"Q" -> List(c("0123456789") act ((sym, _) => Integer.parseInt(sym.text))),
		
		"A" -> List(seq(i("a"), n("K") act ((_, _) => "A")) act ((_, objs) => objs(1))),
		"B" -> List(seq(i("b"), n("K") act ((_, _) => "B")) act ((_, objs) => objs(1))),
		"C" -> List(seq(i("c"), n("K") act ((_, _) => "C")) act ((_, objs) => objs(1))),
		"D" -> List(seq(i("d"), n("K") act ((_, _) => "D")) act ((_, objs) => objs(1))),

		"K" -> List(seq()),
		
		"T" -> List(n("T1"), n("T2"), n("T3")),
		"T1" -> List(seq(seq(i("a"), n("T0") act ((_, _) => "T1")) act ((_, objs) => objs(1)), i("1")) act ((_, objs) => objs(0))),
		"T2" -> List(seq(seq(i("a"), n("T0") act ((_, _) => "T2")) act ((_, objs) => objs(1)), i("2")) act ((_, objs) => objs(0))),
		"T3" -> List(seq(seq(i("a"), n("T0") act ((_, _) => "T3")) act ((_, objs) => objs(1)), i("3")) act ((_, objs) => objs(0))),
		"T0" -> List(seq())
	)
}
