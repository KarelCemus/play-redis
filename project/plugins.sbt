resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

// checks for updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

// code coverage and uploader of the coverage results into the coveralls.io
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.12")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.11")

// library release
addSbtPlugin("com.github.sbt" % "sbt-git"      % "2.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.10.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.2.1")
addSbtPlugin("com.github.sbt" % "sbt-release"  % "1.1.0")

// linters
addSbtPlugin("org.typelevel"   % "sbt-tpolecat"    % "0.5.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.3.0")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"    % "2.5.2")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"    % "0.12.1")
