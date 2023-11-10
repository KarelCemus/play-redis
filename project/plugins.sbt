resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

// checks for updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.1")

// code coverage and uploader of the coverage results into the coveralls.io
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.1")

// code linter
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

// library release
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
