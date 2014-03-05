name := "chroniq"

version := "1.0"

scalaVersion := "2.10.3"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies ++= Seq(
//  "io.spray" % "spray-can" % "1.2.0",
//  "io.spray" % "spray-http" % "1.2.0",
//  "io.spray" % "spray-util" % "1.2.0",
//  "com.typesafe.akka" %% "akka-cluster" % "2.2.3",
  "io.spray" % "spray-can" % "1.3-RC4",
  "io.spray" % "spray-http" % "1.3-RC4",
  "io.spray" % "spray-util" % "1.3-RC4",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.0-RC4",
//    "com.typesafe.akka" %% "akka-cluster" % "2.3.0-RC3",
  "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.4",
  ("io.vertx" % "vertx-core" % "2.1M5").exclude("log4j","log4j"),
  ("io.vertx" % "vertx-platform" % "2.1M5").exclude("log4j","log4j"),
  "io.vertx" % "lang-scala" % "0.3.0",
  "io.netty" % "netty-all" % "4.0.15.Final",
  "com.typesafe.play" % "play-json_2.10" % "2.2.1",
  "com.github.mauricio" %% "postgresql-async" % "0.2.12",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0",
  "org.elasticsearch" % "elasticsearch" % "1.0.0",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.1",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.1",  // for any java classes looking for this
  "ch.qos.logback" % "logback-classic" % "1.0.3"
)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies repo" at "http://nightlies.spray.io"