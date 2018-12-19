organization in ThisBuild := "com.giyeok"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.5"
crossPaths in ThisBuild := false

lazy val testDeps = {
    val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.0.1" % "test"
    val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

    val junit: ModuleID = "junit" % "junit" % "4.12" % "test"

    Seq(scalactic, scalatest, junit)
}

lazy val cdg = (project in file("cdg")).
    settings(
        name := "jparser-cdg",
        libraryDependencies ++= testDeps
    )

lazy val core = (project in file("core")).
    settings(
        name := "jparser-core",
        libraryDependencies ++= testDeps
    ).
    dependsOn(cdg % "test->test;compile->compile")

// Examples with no meta grammar
lazy val examples1 = (project in file("examples1")).
    settings(
        name := "jparser-examples1",
        libraryDependencies ++= testDeps
    ).
    dependsOn(cdg % "test->test;compile->compile").
    dependsOn(core % "test->test;test->compile")

lazy val metagrammar = (project in file("metagrammar")).
    settings(
        name := "jparser-metagrammar",
        libraryDependencies ++= testDeps
    ).
    dependsOn(core % "test->test;compile->compile")

// Examples defined using meta grammar
lazy val examples2 = (project in file("examples2")).
    settings(
        name := "jparser-examples2",
        libraryDependencies ++= testDeps
    ).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(examples1 % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile")

lazy val visJavaOptions: Seq[String] = {
    if (sys.props("os.name") == "Mac OS X") Seq("-XstartOnFirstThread", "-d64") else Seq()
}

lazy val visualize = (project in file("visualize")).
    settings(
        name := "jparser-visualize",
        libraryDependencies ++= Seq(
            "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6",
            sbtoneswt.OneSwtPlugin.archDependentSwt.value,
            "swt" % "jface" % "3.0.1"
        ),
        libraryDependencies ++= testDeps,
        javaOptions := visJavaOptions
    ).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile").
    dependsOn(examples1 % "test->test;compile->compile").
    dependsOn(examples2 % "test->test;compile->compile")

lazy val study = (project in file("study")).
    settings(
        name := "jparser-study",
        libraryDependencies ++= testDeps,
        javaOptions := visJavaOptions
    ).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(metagrammar % "test->test;compile->compile").
    dependsOn(examples1 % "test->test;compile->compile").
    dependsOn(examples2 % "test->test;compile->compile")

fork in run := true
