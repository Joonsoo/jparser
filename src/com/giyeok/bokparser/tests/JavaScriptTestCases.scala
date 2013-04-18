package com.giyeok.bokparser.tests

object JavaScriptTestCases {
	val tests = List(
		// Regular expressions/unicode/comments
		 """|var filter = /a/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|var filter = /about/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 또는 'A' 가 있는 문자열 모두가 TRUE (대소문자 구분 안함)
			|var filter = /a/i;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|/**********
			| *
			| * 'a' 에서 'z' 까지중 하나만 있으면 모두가 TRUE (대소문자 구분)
			| /-\/-\ *********** **/
			|var filter = /[a-z]/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 또는 'b' 또는 'c' 가 있는 문자열 모두가 TRUE (대소문자 구분)
			|var filter = /a|b|c/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 에서 'z' 까지 또는 '0' 에서 '9' 까지중 하나만 있으면 모두가 TRUE (대소문자 구분)
			|var filter = /[a-z]|[0-9]/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 에서 'z' 까지의 문자가 아닌 문자가 있을 경우 TRUE (대소문자 구분)
			|var filter = /[^a-z]/gim;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 에서 'z' 까지의 문자로 시작하는 문자열일 겨우 TRUE (대소문자 구분)
			|var filter = /^[a-z]/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// 'a' 에서 'z' 까지의 문자로 끝나는 문자열일 겨우 TRUE (대소문자 구분)
			|var filter = /[a-z]$/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
			|// '\' 가 있는 문자열일 겨우 TRUE (대소문자 구분)
			|var filter = /\\/;
			|if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }""".stripMargin('|')
	)
	
	val test = tests(0)
}
