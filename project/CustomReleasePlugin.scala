import com.github.sbt.git.{GitVersioning, SbtGit}
import sbt._
import sbt.Keys._
import sbtrelease._
import xerial.sbt.Sonatype

object CustomReleasePlugin extends AutoPlugin {

  import ReleasePlugin.autoImport._
  import ReleaseStateTransformations._
  import ReleaseUtilities._
  import Sonatype.autoImport._

  object autoImport {
    val playVersion = settingKey[String]("Version of Play framework")
  }

  override def trigger = allRequirements

  override def requires: Plugins = ReleasePlugin && GitVersioning && Sonatype

  private def customizedReleaseProcess: Seq[ReleaseStep] =
    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      DocumentationUpdate.updateDocumentation,
    )

  override def projectSettings: Seq[Setting[?]] = Seq[Setting[?]](
    publishMavenStyle                  := true,
    pomIncludeRepository               := { _ => false },
    // customized release process
    releaseProcess                     := customizedReleaseProcess,
    // release details
    homepage                           := Some(url("https://github.com/KarelCemus/play-redis")),
    licenses                           := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo                            := Some(
      ScmInfo(
        url("https://github.com/KarelCemus/play-redis.git"),
        "scm:git@github.com:KarelCemus/play-redis.git",
      ),
    ),
    developers                         := List(
      Developer(id = "karel.cemus", name = "Karel Cemus", email = "", url = url("https://github.com/KarelCemus/")),
    ),
    // Publish settings
    publishTo                          := sonatypePublishToBundle.value,
    ThisBuild / sonatypeCredentialHost := Sonatype.sonatypeCentralHost,
    // git tags without "v" prefix
    SbtGit.git.gitTagToVersionNumber   := { tag: String =>
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
