organization in ThisBuild := "com.giyeok"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.1"
crossPaths in ThisBuild := false

import Dependencies._

lazy val core = (project in file("core")).
    settings(
        name := "jparser-core",
        libraryDependencies ++= testDeps
    )

lazy val visualize = (project in file("visualize")).
    settings(
        name := "jparser-visualize",
        libraryDependencies ++= visDeps,
        libraryDependencies ++= testDeps
    ).
    dependsOn(core % "test->test;compile->compile")

lazy val study = (project in file("study")).
    settings(
        name := "jparser-study",
        libraryDependencies ++= testDeps,
        resolvers += visResolver,
        javaOptions := visJavaOptions
    ).
    dependsOn(core % "test->test;compile->compile")

fork in run := true
