import org.typelevel.sbt.tpolecat.DevMode
import sbt.Keys._
import sbt._
import org.typelevel.scalacoptions._

normalizedName := "play-redis"

name := "Redis Cache for Play"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

crossScalaVersions := Seq("2.13.14", "3.3.3")

scalaVersion := crossScalaVersions.value.head

playVersion := "3.0.2"

libraryDependencies ++= Seq(
  // play framework cache API
  "org.playframework" %% "play-cache"                % playVersion.value % Provided,
  // redis connector
  "io.lettuce"         % "lettuce-core"              % "6.3.2.RELEASE",
  // test framework with mockito extension
  "org.scalatest"     %% "scalatest"                 % "3.2.18"          % Test,
  "org.scalamock"     %% "scalamock"                 % "6.0.0"           % Test,
  // test module for play framework
  "org.playframework" %% "play-test"                 % playVersion.value % Test,
  // to run integration tests
  "com.dimafeng"      %% "testcontainers-scala-core" % "0.41.3"          % Test,
)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
)

javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "UTF-8")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.")) Seq("-Ywarn-unused") else Seq.empty
}

ThisBuild / version := "4.0.2"

//enablePlugins(CustomReleasePlugin)

// exclude from tests coverage
coverageExcludedFiles := ".*exceptions.*"

Test / fork := true
Test / test := (Test / testOnly).toTask(" * -- -l \"org.scalatest.Ignore\"").value
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

semanticdbEnabled                      := true
semanticdbVersion                      := scalafixSemanticdb.revision
ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)

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
