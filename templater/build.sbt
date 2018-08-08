
name := "rchain-perf-harness"

scalaVersion := "2.12.6"

lazy val projectSettings = Seq(
  organization := "coop.rchain",
  scalaVersion := "2.12.6",
  version := "0.1.0-SNAPSHOT",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")),
  scalafmtOnCompile := true
)

val scalapbRuntime      = "com.thesamet.scalapb"       %% "scalapb-runtime"           % scalapb.compiler.Version.scalapbVersion % "protobuf"
val scalapbRuntimeLib   = "com.thesamet.scalapb"       %% "scalapb-runtime"           % scalapb.compiler.Version.scalapbVersion
val scalapbRuntimegGrpc = "com.thesamet.scalapb"       %% "scalapb-runtime-grpc"      % scalapb.compiler.Version.scalapbVersion

val protobufDependencies: Seq[ModuleID] =
  Seq(scalapbRuntime)

val protobufLibDependencies: Seq[ModuleID] =
  Seq(scalapbRuntimeLib)

lazy val commonSettings = projectSettings

lazy val templater = (project in file("templater"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.clapper" %% "scalasti" % "3.0.1",
      "org.rogach" %% "scallop" % "3.1.3"
    ),
    mainClass := Some("coop.rchain.templater.Templater")
  )

lazy val runner = (project in file("runner"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= protobufDependencies ++ protobufLibDependencies ++ Seq()
  )