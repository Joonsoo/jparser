name := "Moon Parser"

version := "0.1"

scalaVersion := "2.10.2"


resolvers += "swt-repo" at "https://swt-repo.googlecode.com/svn/repo/"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

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
  "org.eclipse.swt" % artifact % "4.3"
}

libraryDependencies ++= Seq(
  "org.eclipse.draw2d" % "org.eclipse.draw2d" % "3.9.0" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.draw2d_3.9.0.201308190730.jar",
  "org.eclipse.zest" % "org.eclipse.zest.core" % "1.5.0" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.core.source_1.5.0.201308190730.jar",
  "org.eclipse.zest" % "org.eclipse.zest.layouts" % "1.1.0" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.layouts_1.1.0.201308190730.jar",
  "org.eclipse.zest" % "org.eclipse.zest.layouts.source" % "1.1.0" from "http://download.eclipse.org/tools/gef/updates/releases/plugins/org.eclipse.zest.layouts.source_1.1.0.201308190730.jar"
)
