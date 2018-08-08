addSbtPlugin("com.geirsson"        % "sbt-scalafmt"        % "1.4.0")
// Yes it's weird to do the following, but it's what is mandated by the scalapb documentation
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.1"