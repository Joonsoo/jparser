lazy val commonSettings = Seq(
  organization := "com.giyeok",
  version := "0.1",
  scalaVersion := "2.12.1",
  crossPaths := false
)

lazy val parser = (project in file("core")).
  settings(commonSettings: _*).
  settings(name := "jparser-core")

lazy val visualize = (project in file("visualize")).
  settings(commonSettings: _*).
  settings(name := "jparser-visualize").
  dependsOn(parser % "test->test;compile->compile")

lazy val study = (project in file("study")).
  settings(commonSettings: _*).
  settings(name := "jparser-study").
  dependsOn(parser % "test->test;compile->compile")

fork in run := true
