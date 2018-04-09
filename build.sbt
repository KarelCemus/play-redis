import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.12.5"

crossScalaVersions := Seq( "2.11.12", scalaVersion.value )

val playVersion = "2.6.13"

val connectorVersion = "1.8.3"

val specs2Version = "4.0.3"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion % Provided,
  // redis connector
  "com.github.Ma27" %% "rediscala" % connectorVersion,
  // test framework
  "org.specs2" %% "specs2-core" % specs2Version % Test,
  // test module for play framework
  "com.typesafe.play" %% "play-specs2" % playVersion % Test,
  // mockito framework
  "org.mockito" % "mockito-core" % "2.18.0"
)

resolvers ++= Seq(
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

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"
