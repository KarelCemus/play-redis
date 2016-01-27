credentials ++= (
  for {
    user <- Option(System.getenv() get "SONATYPE_USERNAME")
    pass <- Option(System.getenv() get "SONATYPE_PASSWORD")
  } yield Seq(
      Credentials( "Sonatype Nexus Repository Manager",  "oss.sonatype.org",  user,  pass ),
      Credentials(          "Nexus Repository Manager",  "oss.sonatype.org",  user,  pass )
  )
).getOrElse( Seq() )
