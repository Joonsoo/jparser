package com.giyeok.moonparser.tests

object JavaScriptTestCases {
    val tests = List(
        // Regular expressions/unicode/comments
        """|var filter = /a/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /about/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /a/i;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |/**********
           | *
           | /-\/-\ *********** **/
           |var filter = /[a-z]/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /a|b|c/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /[a-z]|[0-9]/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /[^a-z]/gim;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /^[a-z]/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /[a-z]$/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }
           |var filter = /\\/;
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }""".stripMargin('|'))

    val test = tests(0)
}
