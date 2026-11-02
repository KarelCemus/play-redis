// checks for updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.1")

// code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")

// library release
addSbtPlugin("com.github.sbt" % "sbt-git"     % "2.2.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp"     % "2.3.2")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.5.0")

// linters
addSbtPlugin("org.typelevel"   % "sbt-tpolecat"    % "0.5.7")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.6.3")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"    % "2.6.2")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"    % "0.14.9")
