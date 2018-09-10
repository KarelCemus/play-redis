import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.12.6"

crossScalaVersions := Seq( "2.11.12", scalaVersion.value )

playVersion := "2.6.19"

connectorVersion := "1.8.4"

specs2Version := "4.3.4"

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion.value % Provided,
  // redis connector
  "com.github.Ma27" %% "rediscala" % connectorVersion.value,
  // test framework
  "org.specs2" %% "specs2-core" % specs2Version.value % Test,
  // with mockito extension
  "org.specs2" %% "specs2-mock" % specs2Version.value % Test,
  // test module for play framework
  "com.typesafe.play" %% "play-specs2" % playVersion.value % Test
)

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq( "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-encoding", "UTF-8" )

scalacOptions ++= Seq( "-deprecation", "-feature", "-unchecked" )

homepage := Some( url( "https://github.com/karelcemus/play-redis" ) )

licenses := Seq( "Apache 2" -> url( "http://www.apache.org/licenses/LICENSE-2.0" ) )

vcsScm := "git@github.com:KarelCemus/play-redis.git"

authors := Seq( "Karel Čemus" )

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
