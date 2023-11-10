import sbt.Keys._
import sbt._

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.13.8"

crossScalaVersions := Seq("2.12.15", scalaVersion.value)

playVersion := "2.8.13"

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % playVersion.value % Provided,
  // redis connector
  "com.github.karelcemus" %% "rediscala" % "1.9.1",
  // test framework with mockito extension
  "org.specs2" %% "specs2-mock" % "4.13.2" % Test,
  // test module for play framework
  "com.typesafe.play" %% "play-specs2" % playVersion.value % Test
)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "UTF-8")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

homepage := Some(url("https://github.com/karelcemus/play-redis"))

licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0"))

enablePlugins(CustomReleasePlugin)

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"
