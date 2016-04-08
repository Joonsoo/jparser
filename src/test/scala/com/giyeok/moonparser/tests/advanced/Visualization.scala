package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.javascript.JavaScriptGrammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import com.giyeok.moonparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        BackupGrammars.grammars,
        JoinGrammars.grammars,
        LongestMatchGrammars.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
