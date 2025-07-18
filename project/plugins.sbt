// checks for updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

// code coverage and uploader of the coverage results into the coveralls.io
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.15")

// library release
addSbtPlugin("com.github.sbt" % "sbt-git"      % "2.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-release"  % "1.4.0")

// linters
addSbtPlugin("org.typelevel"   % "sbt-tpolecat"    % "0.5.2")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.4.0")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"    % "2.5.5")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"    % "0.14.3")
