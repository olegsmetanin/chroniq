name := "chroniq"

version := "1.0"

scalaVersion := "2.10.3"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

javacOptions ++= Seq("-encoding", "UTF-8")

net.virtualvoid.sbt.graph.Plugin.graphSettings

seq(Revolver.settings: _*)

seq(Twirl.settings: _*)

unmanagedResourceDirectories in Compile += baseDirectory.value / "src" / "main" / "webapp"

libraryDependencies ++= Seq(
  "io.spray" % "spray-can" % "1.3.0",
  "io.spray" % "spray-http" % "1.3.0",
  "io.spray" % "spray-util" % "1.3.0",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.0",
  "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.4",
  ("io.vertx" % "vertx-core" % "2.1M5").exclude("log4j","log4j"),
  ("io.vertx" % "vertx-platform" % "2.1M5").exclude("log4j","log4j"),
  "io.vertx" % "lang-scala" % "0.3.0",
  "io.netty" % "netty-all" % "4.0.15.Final",
  "com.typesafe.play" % "play-json_2.10" % "2.2.1",
  "com.github.mauricio" %% "postgresql-async" % "0.2.12",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0",
  "org.elasticsearch" % "elasticsearch" % "1.0.0",
  "io.spray"  %%  "twirl-api" % "0.6.2",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.1",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.1",  // for any java classes looking for this
  "ch.qos.logback" % "logback-classic" % "1.0.3"
)

resolvers ++= Seq(
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "spray repo" at "http://repo.spray.io",
    "spray nightlies repo" at "http://nightlies.spray.io"
)

sourceGenerators in Compile <+= (sourceManaged in Compile, version) map {
    (dir, version) =>
        val file = dir / "sw" / "platform" / "utils" / "Templates.scala"
        IO.write(file, Helpers.generate)
        Seq(file)
}
