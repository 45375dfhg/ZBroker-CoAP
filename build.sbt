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
    "dev.zio" %% "zio-config" % "1.0.0-RC26",
)

