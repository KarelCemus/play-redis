import sbt.Keys._
import sbt._

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

crossScalaVersions := Seq("2.13.12") //, "3.3.0"

scalaVersion := crossScalaVersions.value.head

playVersion := "2.9.0"

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion.value % Provided,
  // redis connector
  "io.github.rediscala" %% "rediscala" % "1.14.0-akka",
  // test framework with mockito extension
  "org.specs2" %% "specs2-mock" % "4.20.3" % Test,
  // test module for play framework
  "com.typesafe.play" %% "play-test" % playVersion.value % Test,
  // to run integration tests
  "com.dimafeng" %% "testcontainers-scala-core" % "0.41.0" % Test
)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "UTF-8")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

enablePlugins(CustomReleasePlugin)

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"
