package com.giyeok.jparser.examples1

object JavaScriptSamples1 extends ExampleGrammarSet {
    val examples = Set(
        (JavaScriptGrammar, new StringSamples {
            val validInputs = Set(
                "",
                "var x = 1;",
                "varx = 1;",
                "iff=1;",
                "a=b\nc=d;",
                "abc=  function(a){return a+1;}(1);",
                "console.log(function(a){return a+1;}(1));",
                "function x(a) { return a + 1; }",
                "{return a}",
                "var vara = function ifx(a){return(function(y){return (y+1);})(a)};")
            val invalidInputs = Set()
        })
    )
}
