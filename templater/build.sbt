name := "templater"

scalaVersion := "2.12.6"

//libraryDependencies += "org.jtwig" % "jtwig-core" % "5.87.0.RELEASE"
libraryDependencies += "org.clapper" %% "scalasti" % "3.0.1"

libraryDependencies += "org.rogach" %% "scallop" % "3.1.3"

mainClass := Some("coop.rchain.templater.Templater")