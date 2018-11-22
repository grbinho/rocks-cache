name := "rocks-cache"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finchx-core" % "0.26.0",
  "com.github.finagle" %% "finchx-circe" % "0.26.0",
  "org.rocksdb" % "rocksdbjni" % "5.15.10"
)

val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)