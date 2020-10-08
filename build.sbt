scalaVersion := "2.13.3"

name := "gateway"
organization := "de.hsma.scala"
version := "0.1"

run / fork := true

PB.targets in Compile := Seq(
    scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
    scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value,
)

val zioVersion = "1.0.1"

val zio = Seq(
    "dev.zio" %% "zio"         % zioVersion,
    "dev.zio" %% "zio-streams" % zioVersion,
)

val zio_nio = Seq(
    "dev.zio" %% "zio-nio"      % "1.0.0-RC9",
    "dev.zio" %% "zio-nio-core" % "1.0.0-RC9",
)

val zio_grpc = Seq(
    "io.grpc" % "grpc-netty" % "1.31.1",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
)

val newtype = Seq(
    "io.estatico" %% "newtype" % "0.4.4",
)

val spec = Seq(
    "dev.zio" %% "zio-test"     % zioVersion % "test",
    "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
)

libraryDependencies ++= zio ++ zio_nio ++ zio_grpc ++ newtype ++ spec

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

scalacOptions ++= Seq(
    "-target:jvm-1.11",
    "-encoding", "UTF-8", // Specify character encoding used by source files.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-deprecation",
    "-explaintypes", // Explain type errors in more detail
    "-language:higherKinds", // Allow higher-kinded types
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused",
    "-Ymacro-annotations", // required for NewType in 2.12.1+
    // "-language:implicitConversions" // used to e.g. extend the PublisherResponse object
)
