name := "Moon Parser"

version := "0.1"

scalaVersion := "2.11.6"


resolvers += "swt-repo" at "https://swt-repo.googlecode.com/svn/repo/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"

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
  "org.eclipse.swt" % artifact % "4.4"
}

libraryDependencies ++= Seq(
  "org.eclipse.draw2d" % "org.eclipse.draw2d" % "3.9.101.201408150207" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.draw2d_3.9.101.201408150207.jar",
  "org.eclipse.zest" % "org.eclipse.zest.core" % "1.5.100.201408150207" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.core_1.5.100.201408150207.jar",
  "org.eclipse.zest" % "org.eclipse.zest.layouts" % "1.1.100.201408150207" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.layouts_1.1.100.201408150207.jar",
  "org.eclipse.zest" % "org.eclipse.zest.layouts.source" % "1.1.100.201408150207" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.layouts.source_1.1.100.201408150207.jar"
)

fork in run := true

javaOptions in run := {
    if (sys.props("os.name") == "Mac OS X") Seq("-XstartOnFirstThread", "-d64") else Seq()
}

crossPaths := false

