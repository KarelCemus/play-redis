import scala.sys.process.ProcessLogger

import sbt._
import sbtrelease._

/**
  * @author Karel Cemus
  */
object Utilities {

  import ReleasePlugin.autoImport._

  implicit class StateExtraction( val st: State ) extends AnyVal {
    def extracted = Project.extract( st )
  }

  def vcs( implicit st: State ): Vcs = {
    Project.extract( st ).get( releaseVcs ).getOrElse {
      sys.error( "Aborting release. Working directory is not a repository of a recognized VCS." )
    }
  }

  private def processLogger( implicit st: State ): ProcessLogger = new ProcessLogger {
    override def err( s: => String ): Unit = st.log.info( s )
    override def out( s: => String ): Unit = st.log.info( s )
    override def buffer[ T ]( f: => T ): T = st.log.buffer( f )
  }

  implicit class ExecutableVcs( val vcs: Vcs ) extends AnyVal {
    def stage( files: File* )( implicit st: State ): Unit = vcs.add( files.getAbsolutePaths: _* ) !! processLogger
  }

  /**
    * Helper class implementing a transform operation over a file.
    * The file is opened, transformed, and saved.
    */
  implicit class FilesUpdater( val files: Seq[ File ] ) extends AnyVal {

    def transform( transform: String => String ): Unit = {
      files.foreach {
        file => IO.write( file, transform( IO.read( file ) ) )
      }
    }

    def getAbsolutePaths: Seq[ String ] = files.map( _.getAbsolutePath )
  }

  implicit def file2updater( file: File ): FilesUpdater = new FilesUpdater( Seq( file ) )
}
