import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._

name := "play-redis"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.11.7"

crossScalaVersions := Seq( scalaVersion.value )

val playVersion = "2.4.3"

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion % "provided" exclude("net.sf.ehcache", "ehcache-core"),
  // redis connector - NOTE: not published yet
  // "com.digital-achiever" %% "brando" % "3.0.3-SNAPSHOT",
  // test framework
  "org.specs2" %% "specs2-core" % "3.6.5" % "test",
  // test module for play framework
  "com.typesafe.play" %% "play-test" % playVersion % "test"
)

resolvers ++= Seq(
  "Brando Repository" at "http://chrisdinn.github.io/releases/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq( "-source", "1.8", "-target", "1.68", "-Xlint:unchecked", "-encoding", "UTF-8" )

scalacOptions ++= Seq( "-deprecation", "-feature", "-unchecked" )

homepage := Some( url( "https://github.com/karelcemus/play-redis" ) )

licenses := Seq( "Apache 2" -> url( "http://www.apache.org/licenses/LICENSE-2.0" ) )

publishMavenStyle := true

pomIncludeRepository := { _ => false}

pomExtra :=
    <scm>
      <url>git@github.com:KarelCemus/play-redis.git</url>
      <connection>scm:git@github.com:KarelCemus/play-redis.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Karel Cemus</name>
      </developer>
    </developers>

// Release plugin settings
releaseCrossBuild := true
releaseTagName := ( version in ThisBuild ).value
releasePublishArtifactsAction := PgpKeys.publishSigned.value

// Publish settings
publishTo := {
  if (isSnapshot.value) Some(Opts.resolver.sonatypeSnapshots)
  else Some( Opts.resolver.sonatypeStaging )
}

// brando source repository, latest snapshot version
lazy val brando = RootProject( uri( "git://github.com/chrisdinn/brando.git" ) )

// as we need brando in its latest snapshot compile it from sources
lazy val root = ( project in file( "." ) ).dependsOn( brando )
