import sbt._
import sbt.Keys._
import sbtrelease._

/**
  * @author Karel Cemus
  */
object DocumentationUpdate {

  import CustomReleasePlugin.autoImport._
  import ReleasePlugin.autoImport._
  import ReleaseKeys._
  import Utilities._

  /** version to be released */
  private def next( implicit st: State ) = st.get( versions ).get._1

  /** latest version extracted from git tag on the current branch */
  private def latest( implicit st: State ) = vcs.cmd( "describe", "--abbrev=0", "--tags" ).!!.trim

  /** directory with documentation */
  private def documentationDirectory( implicit st: State ) = st.extracted.get( baseDirectory ) / "doc"

  /** all documentation files */
  private def documentationSources( implicit st: State ) = {
    documentationDirectory ** ( -DirectoryFilter && "*.md" )
  }.get

  /** readme file */
  private def readme( implicit st: State ) = st.extracted.get( baseDirectory ) / "README.md"

  /** groupId used for construction of SBT dependency statement */
  private def groupId( implicit st: State ) = st.extracted.get( organization )

  /** artifactId used for construction of SBT dependency statement */
  private def artifactId( implicit st: State ) = st.extracted.get( normalizedName )

  /** SBT dependency definition */
  private def sbtDependency( version: String )( implicit st: State ) = {
    s""" "$groupId" %% "$artifactId" % "$version" """.trim
  }

  /** Major and minor version of Play framework */
  private def playMinorVersion( implicit st: State ) = {
    val version = st.extracted.get( playVersion )
    version.take( version.lastIndexOf( "." ) )
  }

  private object version {
    def placeholder( implicit st: State ) = s"<!-- Play $playMinorVersion -->.*<!-- / -->"
    def replacement( version: String )( implicit st: State ) = s"<!-- Play $playMinorVersion -->$version<!-- / -->"
  }

  def bumpVersionInDoc: ReleaseStep = ReleaseStep( { implicit st: State =>
    // update versions in documentation
    ( readme +: documentationSources ).transform {
      _.replace( s"blob/$latest", s"blob/$next" )
    }
    // stage the changes
    vcs.stage( readme +: documentationSources: _* )

    st
  } )

  /** Update the line with SBT dependency definition */
  def bumpLatestVersionInReadme: ReleaseStep = ReleaseStep( { implicit st: State =>
    // update the README file
    readme.transform { _
      .replaceAll( sbtDependency( latest ), sbtDependency( next ) )
      .replaceAll( version.placeholder, version.replacement( next ) )
    }
    // stage the changes
    vcs.stage( readme )

    st
  } )
}
