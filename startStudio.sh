sbt "project visualize" "set javaOptions += \"-XstartOnFirstThread\"" "set fork := true" "test:runMain com.giyeok.jparser.tests.ParserStudioMain"
