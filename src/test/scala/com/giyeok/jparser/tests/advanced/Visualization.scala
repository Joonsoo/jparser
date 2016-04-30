package com.giyeok.jparser.tests.advanced

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.javascript.JavaScriptGrammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        BackupGrammars.grammars,
        JoinGrammars.grammars,
        LongestMatchGrammars.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
