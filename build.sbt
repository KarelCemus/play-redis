import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._
import com.typesafe.sbt.pgp.PgpKeys

name := "play-redis"

description := "Redis cache plugin for the Play framework 2"

organization := "com.github.karelcemus"

scalaVersion := "2.11.4"

crossScalaVersions := Seq( scalaVersion.value, "2.10.4" )

libraryDependencies ++= Seq(
  // play framework cache API
  "com.typesafe.play" %% "play-cache" % "2.3.8" % "provided",
  // redis connector
  "com.digital-achiever" %% "brando" % "2.0.2",
  // test framework
  "org.specs2" %% "specs2-core" % "2.4.15" % "test",
  // test module for play framework
  "com.typesafe.play" %% "play-test" % "2.3.8" % "test"
)

resolvers ++= Seq(
  "Brando Repository" at "http://chrisdinn.github.io/releases/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

javacOptions ++= Seq( "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-encoding", "UTF-8" )

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

// Release settings
releaseSettings

ReleaseKeys.crossBuild := true

ReleaseKeys.tagName := ( version in ThisBuild ).value

ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value

// Publish settings
publishTo := {
  if (isSnapshot.value) Some(Opts.resolver.sonatypeSnapshots)
  else Some( Opts.resolver.sonatypeStaging )
}