import org.typelevel.sbt.tpolecat.DevMode
import sbt.Keys._
import sbt._
import org.typelevel.scalacoptions._

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

crossScalaVersions := Seq("2.13.18", "3.3.9")

scalaVersion := crossScalaVersions.value.head

playVersion := "3.0.12"

libraryDependencies ++= Seq(
  // play framework cache API
  "org.playframework" %% "play-cache"                % playVersion.value % Provided,
  // redis connector
  "io.lettuce"         % "lettuce-core"              % "7.9.0.RELEASE",
  // test framework with mockito extension
  "org.scalatest"     %% "scalatest"                 % "3.2.20"          % Test,
  "org.scalamock"     %% "scalamock"                 % "7.5.5"           % Test,
  // test module for play framework
  "org.playframework" %% "play-test"                 % playVersion.value % Test,
  // to run integration tests
  "com.dimafeng"      %% "testcontainers-scala-core" % "0.44.1"          % Test,
)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
)

javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "UTF-8", "--release", "11")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-release", "11")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.")) Seq("-Ywarn-unused") else Seq.empty
}

enablePlugins(CustomReleasePlugin)

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"

Test / fork               := true
// sbt 2 runs forked suites in parallel; the integration tests bind fixed ports
Test / testForkedParallel := false
Test / test               := (Test / testOnly).toTask(" * -- -l \"org.scalatest.Ignore\"").value
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision

wartremoverWarnings ++= Warts.allBut(
  Wart.Any,
  Wart.AnyVal,
  Wart.AsInstanceOf,
  Wart.AutoUnboxing,
  Wart.DefaultArguments,
  Wart.FinalVal,
  Wart.GlobalExecutionContext,
  Wart.ImplicitConversion,
  Wart.ImplicitParameter,
  Wart.IterableOps,
  Wart.NonUnitStatements,
  Wart.Nothing,
  Wart.Null,
  Wart.OptionPartial,
  Wart.Overloading,
  Wart.PartialFunctionApply,
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
