resolvers += Resolver.url( "scoverage-bintray", url( "https://dl.bintray.com/sksamuel/sbt-plugins/" ) )( Resolver.ivyStylePatterns )

// library release
addSbtPlugin( "com.github.gseitz" % "sbt-release" % "1.0.6" )

// PGP signature
addSbtPlugin( "com.jsuereth" % "sbt-pgp" % "1.1.0" )

// checks for updates
addSbtPlugin( "com.timushev.sbt" % "sbt-updates" % "0.3.1" )

// code coverage
addSbtPlugin( "org.scoverage" % "sbt-scoverage" % "1.5.1" )

// uploads the coverage results into the coveralls.io
addSbtPlugin( "org.scoverage" % "sbt-coveralls" % "1.2.2" )

// lists project dependencies
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
