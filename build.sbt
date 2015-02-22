name := "play-redis"

organization := "com.github.karelcemus"

scalaVersion := "2.11.4"

crossScalaVersions := Seq( scalaVersion.value, "2.10.4" )

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-cache" % "2.3.8" % "provided",
  "org.specs2" %% "specs2-core" % "2.4.15" % "test"
)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

javacOptions ++= Seq( "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-encoding", "UTF-8" )

scalacOptions ++= Seq( "-deprecation", "-feature" )

homepage := Some( url( "https://github.com/KarelCemus/play-redis" ) )

licenses := Seq( "Apache 2" -> url( "http://www.apache.org/licenses/LICENSE-2.0" ) )

pomIncludeRepository := { _ => false}

// Release settings
releaseSettings

ReleaseKeys.crossBuild := true

ReleaseKeys.tagName := ( version in ThisBuild ).value

// Publish settings
publishTo := {
  if ( isSnapshot.value ) None
  else Some( Opts.resolver.sonatypeStaging )
}
