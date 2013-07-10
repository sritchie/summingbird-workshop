package summingdemo

import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  implicit def dependencyFilterer(deps: Seq[ModuleID]) = new Object {
    def excluding(group: String, artifactId: String) =
      deps.map(_.exclude(group, artifactId))
  }

  val sharedSettings = Project.defaultSettings ++ Seq(
    organization := "com.twitter",
    version := "0.0.1",
    scalaVersion := "2.10.0",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
      "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
    ),
    resolvers ++= Seq(
      "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "releases"  at "http://oss.sonatype.org/content/repositories/releases",
      "Clojars Repository" at "http://clojars.org/repo",
      "Conjars Repository" at "http://conjars.org/repo",
      "Twitter Maven" at "http://maven.twttr.com",
      Resolver.file("Local repo", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    )
  )

  def module(name: String) = {
    val id = "summingdemo-%s".format(name)
    Project(id = id, base = file(id), settings = sharedSettings ++ Seq(
      Keys.name := id
    ))
  }

  val summingbirdVersion = "0.1.0-SNAPSHOT"
  val algebirdVersion = "0.1.13"
  val bijectionVersion = "0.4.0"
  val storehausVersion = "0.4.0"
  val tormentaVersion = "0.5.1"
  val chillVersion = "0.2.3"

  lazy val summingdemo = play.Project(
    "summingdemo",
    settings = sharedSettings
  ).settings(
    test := { }
  ).aggregate(main, common, storm)

  lazy val common = module("common").settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "algebird-core" % algebirdVersion,
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "bijection-netty" % bijectionVersion,
      "com.twitter" %% "chill" % chillVersion,
      "com.twitter" %% "summingbird-batch" % summingbirdVersion,
      "com.twitter" %% "storehaus-memcache" % storehausVersion,
      "org.twitter4j" % "twitter4j-stream" % "3.0.3"
    ).excluding("org.slf4j", "slf4j-jdk14")
      .excluding("ch.qos.logback", "logback-classic")
  )

  lazy val storm = module("storm").settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "summingbird-storm" % summingbirdVersion,
      "com.twitter" %% "tormenta-core" % tormentaVersion,
      "com.twitter" %% "tormenta-twitter" % tormentaVersion
    ).excluding("com.twitter", "kafka_2.9.2")
      .excluding("org.slf4j", "slf4j-api")
  ).dependsOn(common)

  lazy val main = {
    val id = "summingdemo-client"
    play.Project(id, path = file(id), settings = sharedSettings).settings (
      libraryDependencies ++= Seq(
        "com.twitter" %% "chill" % chillVersion,
        "com.twitter" %% "summingbird-client" % summingbirdVersion
      )
    ).dependsOn(common)
  }
}
