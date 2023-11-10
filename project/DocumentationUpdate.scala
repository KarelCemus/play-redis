import sbt._
import sbt.Keys._
import sbtrelease._

object DocumentationUpdate {

  import CustomReleasePlugin.autoImport._
  import ReleasePlugin.autoImport._
  import ReleaseKeys._
  import ReleaseUtilities._

  /** version to be released */
  private def next(implicit st: State) = st.get(versions).get._1

  /** directory with documentation */
  private def documentationDirectory(implicit st: State) = st.extracted.get(baseDirectory) / "doc"

  /** all documentation files */
  private def documentationSources(implicit st: State) = {
    documentationDirectory ** (-DirectoryFilter && "*.md")
  }.get

  /** readme file */
  private def readme(implicit st: State) = st.extracted.get(baseDirectory) / "README.md"

  /** groupId used for construction of SBT dependency statement */
  private def groupId(implicit st: State) = st.extracted.get(organization)

  /** artifactId used for construction of SBT dependency statement */
  private def artifactId(implicit st: State) = st.extracted.get(normalizedName)

  /** SBT dependency definition */
  private def sbtDependency(version: String)(implicit st: State) = {
    s""" "$groupId" %% "$artifactId" % "$version" """.trim
  }

  /** Major and minor version of Play framework */
  private def playMinorVersion(implicit st: State) = {
    val version = st.extracted.get(playVersion)
    version.take(version.lastIndexOf("."))
  }

  private object version {
    def placeholder(implicit st: State) = s"<!-- Play $playMinorVersion -->.*<!-- / -->"
    def replacement(version: String)(implicit st: State) = s"<!-- Play $playMinorVersion -->$version<!-- / -->"
  }

  def updateDocumentation: ReleaseStep = ReleaseStep({ implicit st: State =>
    val commitMessage = s"Documentation updated to version $next"
    val program = List[State => State](
      bumpVersionInDoc(_),
      bumpLatestVersionInReadme(_),
      commitVersion(commitMessage)(_)
    ).reduce(_ andThen _)

    program(st)
  })

  private def bumpVersionInDoc(implicit st: State): State = {
    val latest = vcs.latest
    // update versions in documentation
    (readme +: documentationSources).transform {
      _
        .replace(s"blob/$latest", s"blob/$next")
        .replaceAll(sbtDependency(latest), sbtDependency(next))
    }
    // stage the changes
    vcs.stage(readme +: documentationSources: _*)
    st
  }

  /** Update the line with SBT dependency definition */
  private def bumpLatestVersionInReadme(implicit st: State): State = {
    // update the README file
    readme.transform {
      _
        .replaceAll(sbtDependency(vcs.latest), sbtDependency(next))
        .replaceAll(version.placeholder, version.replacement(next))
    }
    // stage the changes
    vcs.stage(readme)
    st
  }

  private def commitVersion(commitMessage: String)(implicit st: State): State = {
    val log = processLogger
    val base = vcs(st).baseDir.getCanonicalFile
    val sign = st.extracted.get(releaseVcsSign)
    val signOff = st.extracted.get(releaseVcsSignOff)

    val status = vcs(st).status.!!.trim

    val newState = if (status.nonEmpty) {
      vcs(st).commit(commitMessage, sign, signOff) ! log
      st
    } else {
      // nothing to commit
      st
    }
    newState
  }
}
