import com.github.sbt.git.{GitVersioning, SbtGit}
import sbt._
import sbt.Keys._
import sbtrelease._

object CustomReleasePlugin extends AutoPlugin {

  import ReleasePlugin.autoImport._
  import ReleaseStateTransformations._
  import ReleaseUtilities._

  object autoImport {
    val playVersion = settingKey[String]("Version of Play framework")
  }

  override def trigger = allRequirements

  override def requires: Plugins = ReleasePlugin && GitVersioning

  private def customizedReleaseProcess: Seq[ReleaseStep] =
    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      DocumentationUpdate.updateDocumentation,
    )

  override def projectSettings: Seq[Setting[?]] = Seq[Setting[?]](
    publishMavenStyle                := true,
    pomIncludeRepository             := { _ => false },
    // customized release process
    releaseProcess                   := customizedReleaseProcess,
    // sbt-release 1.5+ expects a snapshot version unless the bump is not Next, we start from the latest tag
    releaseVersionBump               := Version.Bump.Minor,
    // release details
    homepage                         := Some(url("https://github.com/KarelCemus/play-redis")),
    licenses                         := Seq(License.Apache2),
    scmInfo                          := Some(
      ScmInfo(
        url("https://github.com/KarelCemus/play-redis.git"),
        "scm:git@github.com:KarelCemus/play-redis.git",
      ),
    ),
    developers                       := List(
      Developer(id = "karel.cemus", name = "Karel Cemus", email = "", url = url("https://github.com/KarelCemus/")),
    ),
    // Publish settings
    // Central Portal: releases are staged locally and uploaded as a bundle by sonaRelease
    publishTo                        := {
      if (version.value.endsWith("-SNAPSHOT")) Some("central-snapshots" at "https://central.sonatype.com/repository/maven-snapshots/")
      else localStaging.value
    },
    // git tags without "v" prefix
    SbtGit.git.gitTagToVersionNumber := { (tag: String) =>
      if (tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
  )

  private lazy val inquireVersions: ReleaseStep = { implicit st: State =>
    val extracted = Project.extract(st)

    val useDefs = false
    val currentV = vcs.latest
    val nextVersion = st.extracted.runTask(releaseVersion, st)._2(currentV)
    val bump = Version.Bump.Minor

    val suggestedReleaseV: String = Version(nextVersion).map(_.bump(bump).unapply).getOrElse(versionFormatError(currentV))

    st.log.info("Press enter to use the default value")

    // flatten the Option[Option[String]] as the get returns an Option, and the value inside is an Option
    val releaseV = readVersion(suggestedReleaseV, "Release version [%s] : ", useDefs, st.get(ReleaseKeys.commandLineReleaseVersion).flatten)

    st.put(ReleaseKeys.versions, (releaseV, releaseV))
  }

}
