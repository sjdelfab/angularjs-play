import WebKeys._

import play.sbt.PlayImport._

name := """angularjs-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala,SbtWeb)

scalaVersion := "2.11.8"

EclipseKeys.withSource := true

libraryDependencies ++= Seq(
  filters,
  cache,
  "org.webjars" % "requirejs" % "2.3.2",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "bootstrap" % "3.3.7" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.5.8" exclude("org.webjars", "jquery"),
  "org.webjars" % "webjars-play_2.11" % "2.4.0-2",
  "org.webjars" % "angular-ui-bootstrap" % "1.3.3" exclude("org.webjars", "angularjs"),
  "org.webjars" % "momentjs" % "2.15.0",
  "org.webjars" % "font-awesome" % "4.6.3",
  "org.webjars" % "angular-ui-select" % "0.17.1",
  "javax.inject" % "javax.inject" % "1",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc4",
  "edu.vt.middleware" % "vt-password" % "3.1.2",
  "org.webjars" % "angular-translate" % "2.11.0",
  "org.webjars" % "angular-translate-interpolation-messageformat" % "0.1.2",
  "org.webjars" % "angular-translate-loader-partial" % "2.8.1",
  "org.webjars" % "angular-translate-loader-url" % "0.1.2-1",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test",
  "org.dbunit" % "dbunit" % "2.5.0",
  "org.specs2" % "specs2_2.11" % "3.7" % "test",
  "org.specs2" % "specs2-mock_2.11" % "3.7" % "test",
  "org.specs2" % "specs2-junit_2.11" % "3.7" % "test",
  "com.typesafe.play" % "play-specs2_2.11" % "2.5.9" % "test"
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

lazy val setVersionTask = TaskKey[Unit]("setRevisionRJS", "Set revision number in main.js")

setVersionTask := {
    val v = System.currentTimeMillis().toString()
	var mainFile = scala.io.Source.fromFile("app/assets/javascripts/main.js").mkString
	mainFile = mainFile.replaceAll("@Revision@",v)
    Some(new java.io.PrintWriter("app/assets/javascripts/main.js")).foreach{p => p.write(mainFile); p.close}
}

packageBin in Universal <<= (packageBin in Universal).dependsOn(setVersionTask)

pipelineStages := Seq(rjs, digest, gzip, filter)

includeFilter in filter := "*.js.map" || "*.js"

excludeFilter in filter := "*main.js" || "*require.js"

// RequireJS with sbt-rjs (https://github.com/sbt/sbt-rjs#sbt-rjs)
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))

// http://stackoverflow.com/questions/31368029/paths-fallback-not-supported-in-optimizer-reloaded
// Disable CDN See https://github.com/sbt/sbt-rjs/issues/45
//RjsKeys.webJarCdns := Map.empty
RjsKeys.webJarCdns := Map("org.webjars" -> "/webjars")

UglifyKeys.mangle := false

UglifyKeys.compressOptions := Seq("dead_code=false,unused=false")

emojiLogs

JshintKeys.config := Some(file("angularjs-play.jshintrc"))

unmanagedResourceDirectories in Test <+= baseDirectory ( _ /"target/web/public/test" )

resolvers += Resolver.sonatypeRepo("snapshots") 

resolvers ++= Seq(
	"Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
	"Atlassian Releases" at "https://maven.atlassian.com/public/",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    Resolver.jcenterRepo
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

fork := true
