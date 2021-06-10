ThisBuild/organization := "com.giyeok"
ThisBuild/version := "0.2.3"
ThisBuild/scalaVersion := "2.13.5"
ThisBuild/crossPaths := false

ThisBuild/javacOptions ++= Seq("-encoding", "UTF-8")

lazy val testDeps = {
  // val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.0.1" % "test"
  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.2.2" % "test"

  val junit: ModuleID = "junit" % "junit" % "4.12" % "test"

  Seq(scalatest, junit)
}

lazy val protobufDep = "com.google.protobuf" % "protobuf-java" % "3.14.0"
lazy val javaFormatDep = "com.google.googlejavaformat" % "google-java-format" % "1.9"

lazy val utils = (project in file("utils")).
  settings(
    name := "jparser-utils",
    libraryDependencies ++= testDeps)

lazy val base = (project in file("base")).
  settings(
    name := "jparser-base",
    libraryDependencies ++= testDeps,
    libraryDependencies += protobufDep).
  dependsOn(utils % "test->test;compile->compile")

lazy val examples = (project in file("examples")).
  settings(
    name := "jparser-examples").
  dependsOn(base % "test->test;compile->compile")

lazy val naive = (project in file("naive")).
  settings(
    name := "jparser-naive",
    libraryDependencies ++= testDeps).
  dependsOn(base % "test->test;compile->compile").
  dependsOn(examples % "compile->test")

lazy val metalang = (project in file("metalang")).
  settings(
    name := "jparser-metalang",
    libraryDependencies += protobufDep).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile").
  dependsOn(examples % "compile->test")

lazy val fast = (project in file("fast")).
  settings(
    name := "jparser-fast",
    libraryDependencies += javaFormatDep,
    libraryDependencies ++= testDeps,
    javacOptions ++= Seq("-encoding", "UTF-8")).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile").
  dependsOn(examples % "test->test;compile->compile")

lazy val visJavaOptions: Seq[String] = {
  if (sys.props("os.name") == "Mac OS X") Seq("-XstartOnFirstThread", "-d64") else Seq()
}

lazy val visualize = (project in file("visualize")).
  settings(
    name := "jparser-visualize",
    resolvers += "swt-repo" at "https://maven-eclipse.github.io/maven",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "swt" % "jface" % "3.0.1"
    ),
    libraryDependencies += {
      val os = (sys.props("os.name"), sys.props("os.arch")) match {
        case ("Linux", "amd64" | "x86_64") => "gtk.linux.x86_64"
        case ("Linux", _) => "gtk.linux.x86"
        case ("Mac OS X", "amd64" | "x86_64") => "cocoa.macosx.x86_64"
        case ("Mac OS X", _) => "cocoa.macosx.x86"
        case (os, "amd64") if os.startsWith("Windows") => "win32.win32.x86_64"
        case (os, _) if os.startsWith("Windows") => "win32.win32.x86"
        case (os, arch) => sys.error("Cannot obtain lib for OS '" + os + "' and architecture '" + arch + "'")
      }
      val artifact = "org.eclipse.swt." + os
      "org.eclipse.swt" % artifact % "4.6.1"
    },
    libraryDependencies ++= testDeps,
    libraryDependencies += "io.reactivex.rxjava3" % "rxjava" % "3.0.8",
    javaOptions := visJavaOptions).
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

lazy val cli = (project in file("cli")).
  settings(
    name := "jparser-cli",
    libraryDependencies ++= testDeps,
    libraryDependencies += "info.picocli" % "picocli" % "4.6.1"
  ).
  dependsOn(naive % "test->test;compile->compile").
  dependsOn(utils % "test->test;compile->compile").
  dependsOn(metalang % "test->test;compile->compile").
  dependsOn(fast % "test->test;compile->compile")

run/fork := true
Test/fork := true
