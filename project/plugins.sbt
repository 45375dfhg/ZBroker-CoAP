addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC1")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.4.0"
)