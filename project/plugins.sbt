resolvers += Resolver.url( "scoverage-bintray", url( "https://dl.bintray.com/sksamuel/sbt-plugins/" ) )( Resolver.ivyStylePatterns )

// library release
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

// PGP signature
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

// checks for updates
addSbtPlugin( "com.timushev.sbt" % "sbt-updates" % "0.6.1" )

// code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")

// uploads the coverage results into the coveralls.io
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.1")

// code linter
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
