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
  }

  override def trigger = allRequirements

  override def requires = ReleasePlugin

  override def projectSettings = Seq[ Setting[ _ ] ](
    releaseProcess := Seq[ ReleaseStep ](
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
  )
}
