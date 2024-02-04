import org.typelevel.sbt.tpolecat.DevMode
import sbt.Keys._
import sbt._
import org.typelevel.scalacoptions._

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

crossScalaVersions := Seq("2.13.12") //, "3.3.0"

scalaVersion := crossScalaVersions.value.head

playVersion := "3.0.0"

libraryDependencies ++= Seq(
  // play framework cache API
  "org.playframework" %% "play-cache" % playVersion.value % Provided,
  // redis connector
  "io.github.rediscala" %% "rediscala" % "1.14.0-pekko",
  // test framework with mockito extension
  "org.scalatest"       %% "scalatest"                 % "3.2.17"          % Test,
  "org.scalamock"       %% "scalamock"                 % "5.2.0"           % Test,
  // test module for play framework
  "org.playframework" %% "play-test" % playVersion.value % Test,
  // to run integration tests
  "com.dimafeng"        %% "testcontainers-scala-core" % "0.41.2"          % Test,
)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
)

javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "UTF-8")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-unused")

enablePlugins(CustomReleasePlugin)

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"

Test / test := (Test / testOnly).toTask(" * -- -l \"org.scalatest.Ignore\"").value

semanticdbEnabled                      := true
semanticdbOptions += "-P:semanticdb:synthetics:on"
semanticdbVersion                      := scalafixSemanticdb.revision
ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)

wartremoverWarnings ++= Warts.allBut(
  Wart.Any,
  Wart.AnyVal,
  Wart.AsInstanceOf,
  Wart.AutoUnboxing,
  Wart.DefaultArguments,
  Wart.GlobalExecutionContext,
  Wart.ImplicitConversion,
  Wart.ImplicitParameter,
  Wart.IterableOps,
  Wart.NonUnitStatements,
  Wart.Nothing,
  Wart.Null,
  Wart.OptionPartial,
  Wart.Overloading,
  Wart.PlatformDefault,
  Wart.StringPlusAny,
  Wart.Throw,
  Wart.ToString,
  Wart.TryPartial,
  Wart.Var,
)

tpolecatDevModeOptions ~= { opts =>
  opts.filterNot(Set(ScalacOptions.warnError))
}

Test / tpolecatExcludeOptions ++= Set(
  ScalacOptions.warnValueDiscard,
  ScalacOptions.warnNonUnitStatement,
)

ThisBuild / tpolecatCiModeEnvVar       := "CI"
ThisBuild / tpolecatDefaultOptionsMode := DevMode

addCommandAlias("fix", "; scalafixAll; scalafmtAll; scalafmtSbt")
addCommandAlias("lint", "; scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check")
