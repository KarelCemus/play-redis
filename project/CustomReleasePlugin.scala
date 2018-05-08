import sbt.Keys._
import sbt._
import sbtrelease._

/**
  * @author Karel Cemus
  */
object CustomReleasePlugin extends AutoPlugin {

  import ReleasePlugin.autoImport._
  import ReleaseStateTransformations._

  object autoImport {

    val playVersion = settingKey[ String ]( "Version of Play framework" )
    val connectorVersion = settingKey[ String ]( "Version redis connector" )
    val specs2Version = settingKey[ String ]( "Version of Specs2 testing framework" )

    val authors = settingKey[ Seq[ String ] ]( "List of authors of the library" )
    val vcsScm = settingKey[ String ]( "URL of the GIT repository" )
  }

  override def trigger = allRequirements

  override def requires = ReleasePlugin

  private def customizedReleaseProcess = {
    Seq[ ReleaseStep ](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      DocumentationUpdate.bumpVersionInDoc,
      DocumentationUpdate.bumpLatestVersionInReadme,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  }

  import autoImport._

  override def projectSettings = Seq[ Setting[ _ ] ](
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false},
    pomExtra := {
      <scm>
        <url>{vcsScm.value}</url>
        <connection>scm:{vcsScm.value}</connection>
      </scm>
      <developers>
        {
          authors.value.map { author =>
            <developer>
              <name>{author}</name>
            </developer>
          }
        }
      </developers>
    },
    // customized release process
    releaseProcess := customizedReleaseProcess
  )
}
