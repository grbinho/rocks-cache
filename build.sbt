name := "rocks-cache"

version := "0.3"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "org.rocksdb" % "rocksdbjni" % "5.15.10",
  "com.softwaremill.sttp" %% "core" % "1.5.0",
  "com.softwaremill.sttp" %% "circe" % "1.5.0",
  "org.apache.avro" % "avro" % "1.8.2",
  "org.apache.avro" % "avro-ipc" % "1.8.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
)

val circeVersion = "0.10.0"

name := "rocks-cache"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

enablePlugins(JavaAppPackaging)

// default generated class path is problematic in windows launch script.
scriptClasspath := Seq("*")

(sourceDirectory in AvroConfig) := baseDirectory.value / "src/main/avro"
(javaSource in AvroConfig) := baseDirectory.value / "src/main/scala/"
(stringType in AvroConfig) := "String"
(version in AvroConfig) := "1.8.2"


