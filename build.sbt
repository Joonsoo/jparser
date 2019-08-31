organization in ThisBuild := "com.giyeok"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.13.0"
crossPaths in ThisBuild := false

javacOptions in ThisBuild ++= Seq("-encoding", "UTF-8")

lazy val testDeps = {
    // val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.0.1" % "test"
    val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.8" % "test"

    val junit: ModuleID = "junit" % "junit" % "4.12" % "test"

    Seq(scalatest, junit)
}

lazy val utils = (project in file("utils")).
    settings(
        name := "jparser-utils",
        libraryDependencies ++= testDeps)

lazy val types = (project in file("types")).
	settings(
		name := "jparser-types",
		libraryDependencies ++= testDeps).
    dependsOn(utils % "test->test;compile->compile")

lazy val core = (project in file("core")).
    settings(
        name := "jparser-core",
        libraryDependencies ++= testDeps).
    dependsOn(types % "test->test;compile->compile")

lazy val metagrammar = (project in file("metagrammar")).
    settings(
        name := "jparser-metagrammar").
    dependsOn(core % "test->test;compile->compile").
    dependsOn(utils % "test->test;compile->compile")

lazy val examples = (project in file("examples")).
    settings(
        name := "jparser-examples").
    dependsOn(types % "test->test;compile->compile").
    dependsOn(core % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile")

lazy val visJavaOptions: Seq[String] = {
    if (sys.props("os.name") == "Mac OS X") Seq("-XstartOnFirstThread", "-d64") else Seq()
}

lazy val visualize = (project in file("visualize")).
    settings(
        name := "jparser-visualize",
        libraryDependencies ++= Seq(
            "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
            sbtoneswt.OneSwtPlugin.archDependentSwt.value,
            "swt" % "jface" % "3.0.1"
        ),
        libraryDependencies ++= testDeps,
        javaOptions := visJavaOptions).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(utils % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile").
    dependsOn(examples % "test->test;compile->compile")

lazy val parsergen = (project in file("parsergen")).
    settings(
        name := "jparser-parsergen",
        libraryDependencies += "com.google.googlejavaformat" % "google-java-format" % "1.6",
        libraryDependencies ++= testDeps,
        javacOptions ++= Seq("-encoding", "UTF-8")).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(examples % "test->test;compile->compile").
    dependsOn(visualize % "test->test;compile->compile")

lazy val study = (project in file("study")).
    settings(
        name := "jparser-study",
        libraryDependencies ++= testDeps,
        javaOptions := visJavaOptions).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(utils % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile").
    dependsOn(examples % "test->test;compile->compile").
    dependsOn(visualize % "test->test;compile->compile").
    dependsOn(parsergen % "test->test;compile->compile")

fork in run := true
