addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.6")
addSbtPlugin("com.cavorite" % "sbt-avro-1-8" % "1.1.5")

resolvers ++= Seq(
  Resolver.url("typesafe-ivy-repo", url("http://typesafe.artifactoryonline.com/typesafe/releases"))(Resolver.ivyStylePatterns),
  "spray repo" at "http://repo.spray.io/"
)