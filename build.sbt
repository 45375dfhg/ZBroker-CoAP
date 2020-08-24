scalaVersion := "2.13.3"

name := "gateway"
organization := "de.hsma.scala"
version := "0.1"

run / fork := true

libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.0",
    "dev.zio" %% "zio-streams" % "1.0.0",
    "dev.zio" %% "zio-nio" % "1.0.0-RC9",
    "dev.zio" %% "zio-nio-core" % "1.0.0-RC9",
)

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
    "-Ywarn-unused"
)
