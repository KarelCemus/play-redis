resolvers += Resolver.url( "scoverage-bintray", url( "https://dl.bintray.com/sksamuel/sbt-plugins/" ) )( Resolver.ivyStylePatterns )

// library release
addSbtPlugin( "com.github.gseitz" % "sbt-release" % "1.0.11" )

// PGP signature
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

// checks for updates
addSbtPlugin( "com.timushev.sbt" % "sbt-updates" % "0.4.0" )

// code coverage
addSbtPlugin( "org.scoverage" % "sbt-scoverage" % "1.6.0" )

// uploads the coverage results into the coveralls.io
addSbtPlugin( "org.scoverage" % "sbt-coveralls" % "1.2.2" )

// lists project dependencies
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")

// code linter
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")
