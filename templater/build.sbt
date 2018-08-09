
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

val bouncyCastle        = "org.bouncycastle"            % "bcprov-jdk15on"            % "1.59"
val catsCore            = "org.typelevel"              %% "cats-core"                 % "1.1.0"
val catsEffect          = "org.typelevel"              %% "cats-effect"               % "1.0.0-RC2"
val catsMtl             = "org.typelevel"              %% "cats-mtl-core"             % "0.2.3"
val monix               = "io.monix"                   %% "monix"                     % "3.0.0-RC1"
val scalapbRuntime      = "com.thesamet.scalapb"       %% "scalapb-runtime"           % scalapb.compiler.Version.scalapbVersion % "protobuf"
val scalapbRuntimeLib   = "com.thesamet.scalapb"       %% "scalapb-runtime"           % scalapb.compiler.Version.scalapbVersion
val scalapbRuntimegGrpc = "com.thesamet.scalapb"       %% "scalapb-runtime-grpc"      % scalapb.compiler.Version.scalapbVersion
val scalacheck          = "org.scalacheck"             %% "scalacheck"                % "1.13.4"
val gatling             = "io.gatling.highcharts"       % "gatling-charts-highcharts" % "2.3.1"
val grpcNetty           = "io.grpc"                     % "grpc-netty"                % scalapb.compiler.Version.grpcJavaVersion

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
    libraryDependencies ++= protobufDependencies ++ protobufLibDependencies ++ Seq(
      scalapbRuntimegGrpc,
      catsCore,
      monix,
      bouncyCastle,
      scalacheck,
      gatling,
      grpcNetty
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
    )
  )