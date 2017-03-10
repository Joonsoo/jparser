
resolvers += Resolver.url("sbt-plugin-snapshots", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.8")

addSbtPlugin("com.giyeok.oneswt" % "sbt-oneswt" % "0.0.1")

lazy val oneswtPlugin = uri("https://github.com/Joonsoo/oneswt.git#master")
lazy val root = project.in(file(".")).dependsOn(oneswtPlugin)
