package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.giyeok.moonparser.grammars.JavaScriptParser

@RunWith(classOf[JUnitRunner])
class JavaScriptTests extends FunSuite {
    test("Regular expressions/unicode/comments") {
        val text = """|var filter = /a/;
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
           |if (filter.test("some test words") == true) { alert("ok"); } else { alert("fail"); }""".stripMargin('|')

        val parser = JavaScriptParser.getBlackboxParser

        parser.parse(text)
    }
}
