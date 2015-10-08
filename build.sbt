import WebKeys._

import play.sbt.PlayImport._

name := """angularjs-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala,SbtWeb)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  filters,
  cache,
  "org.webjars" % "requirejs" % "2.1.20",
  "org.webjars" % "jquery" % "1.11.3",
  "org.webjars" % "bootstrap" % "3.1.1-2" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.4.7" exclude("org.webjars", "jquery"),
  "org.webjars" % "webjars-play_2.11" % "2.4.0",
  "org.webjars" % "angular-ui-bootstrap" % "0.13.4" exclude("org.webjars", "angularjs"),
  "org.webjars" % "momentjs" % "2.10.6",
  "org.webjars" % "font-awesome" % "4.4.0",
  "org.webjars" % "angular-ui-select" % "0.12.1",
  "javax.inject" % "javax.inject" % "1",
  "com.typesafe.slick" %% "slick" % "3.0.3",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc4",
  "edu.vt.middleware" % "vt-password" % "3.1.2",
  "org.webjars" % "angular-translate" % "2.7.2",
  "org.webjars" % "angular-translate-interpolation-messageformat" % "0.1.2",
  "org.webjars" % "angular-translate-loader-partial" % "2.7.0",
  "org.webjars" % "angular-translate-loader-url" % "0.1.2-1",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test",
  "org.dbunit" % "dbunit" % "2.5.0",
  "org.specs2" % "specs2_2.11" % "3.3.1" % "test",
  "org.specs2" % "specs2-mock_2.11" % "3.3.1" % "test",
  "org.specs2" % "specs2-junit_2.11" % "3.3.1" % "test"
)

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

pipelineStages := Seq(rjs, digest, gzip)

// RequireJS with sbt-rjs (https://github.com/sbt/sbt-rjs#sbt-rjs)

RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))

UglifyKeys.mangle := false

UglifyKeys.compressOptions := Seq("dead_code=false,unused=false")

emojiLogs

JshintKeys.config := Some(file("angularjs-play.jshintrc"))

unmanagedResourceDirectories in Test <+= baseDirectory ( _ /"target/web/public/test" )

resolvers += Resolver.sonatypeRepo("snapshots") 

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

fork := true
