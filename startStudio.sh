sbt "project visualize" "set javaOptions += \"-XstartOnFirstThread\"" "set fork := true" "test:run-main com.giyeok.jparser.tests.ParserStudioMain"
