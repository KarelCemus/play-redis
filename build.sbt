import sbt._
import sbt.Keys._
import com.typesafe.sbt.pgp.PgpKeys

name := "play-redis"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.11.7"

crossScalaVersions := Seq( scalaVersion.value )

val playVersion = "2.4.6"

val brandoVersion = "3.0.3"

val specs2Version = "3.6.6"

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion % "provided" exclude("net.sf.ehcache", "ehcache-core"),
  // redis connector - NOTE: not published yet
  "com.digital-achiever" %% "brando" % brandoVersion,
  // test framework
  "org.specs2" %% "specs2-core" % specs2Version % "test",
  // test module for play framework
  "com.typesafe.play" %% "play-test" % playVersion % "test"
)

resolvers ++= Seq(
  "Brando Repository" at "http://chrisdinn.github.io/releases/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq( "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-encoding", "UTF-8" )

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
