ThisBuild / organization := "com.giyeok"
ThisBuild / version := "0.2.3"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / crossPaths := false

ThisBuild / javacOptions ++= Seq("-encoding", "UTF-8")

lazy val testDeps = {
  // val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.0.1" % "test"
  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.2.12" % "test"

  val junit: ModuleID = "org.junit.jupiter" % "junit-jupiter-api" % "5.8.2" % "test"

  Seq(scalatest, junit)
}

lazy val protobufDep = "com.google.protobuf" % "protobuf-java" % "3.21.1"
lazy val javaFormatDep = "com.google.googlejavaformat" % "google-java-format" % "1.15.0"

lazy val base = (project in file("base")).
  settings(
    name := "jparser-base",
    libraryDependencies ++= testDeps,
    libraryDependencies += protobufDep)

lazy val utils = (project in file("utils")).
  settings(
    name := "jparser-utils",
    libraryDependencies ++= testDeps).
  dependsOn(base % "test->test;compile->compile")

lazy val naive = (project in file("naive")).
  settings(
      name := "jparser-naive",
      libraryDependencies ++= testDeps).
  dependsOn(base % "test->test;compile->compile")

lazy val metalang = (project in file("metalang")).
  settings(
    name := "jparser-metalang",
    libraryDependencies += protobufDep,
  ).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile")

lazy val fast = (project in file("fast")).
  settings(
    name := "jparser-fast",
    libraryDependencies += javaFormatDep,
    libraryDependencies ++= testDeps,
    javacOptions ++= Seq("-encoding", "UTF-8")).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile")

lazy val cli = (project in file("cli")).
  settings(
    name := "jparser-cli",
    libraryDependencies ++= testDeps,
    libraryDependencies += "info.picocli" % "picocli" % "4.6.3",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "generated" / "scala",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "generated" / "resources",
  ).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile").
  dependsOn(fast % "test->test;compile->compile")

lazy val examples = (project in file("examples")).
  settings(
    name := "jparser-examples").
  dependsOn(base % "compile->compile;compile->compile").
  dependsOn(naive % "compile->compile").
  dependsOn(metalang % "compile->compile")

// TODO naive_test, fast_test -> naive와 fast의 test에 들어있지 않은 metalang으로 정의된 테스트 돌리기

lazy val visJavaOptions: Seq[String] = {
  if (sys.props("os.name") == "Mac OS X") Seq("-XstartOnFirstThread", "-d64") else Seq()
}

lazy val visualize = (project in file("visualize")).
  settings(
    name := "jparser-visualize",
    libraryDependencies ++= testDeps,
    libraryDependencies += "io.reactivex.rxjava3" % "rxjava" % "3.1.5",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
    // libraryDependencies += "org.eclipse" % "jface" % "3.3.0-I20070606-0010",
    libraryDependencies += "org.jetbrains.kotlin" % "kotlin-stdlib-jdk8" % "1.7.10",
    libraryDependencies += "org.jetbrains.kotlinx" % "kotlinx-coroutines-core" % "1.6.2",
    libraryDependencies += "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8" % "1.6.2",
    javaOptions := visJavaOptions,
    unmanagedBase := baseDirectory.value / "lib").
  dependsOn(base % "test->test;compile->compile").
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile").
  dependsOn(fast % "test->test;compile->compile").
  dependsOn(examples % "test->test;compile->compile")

lazy val study = (project in file("study")).
  settings(
    name := "jparser-study",
    libraryDependencies ++= testDeps,
    javaOptions := visJavaOptions).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile").
  dependsOn(examples % "test->test;compile->compile").
  dependsOn(visualize % "test->test;compile->compile").
  dependsOn(fast % "test->test;compile->compile")

lazy val bibix = (project in file("bibix")).
  settings(name := "jparser-bibix")

run / fork := true
Test / fork := true
