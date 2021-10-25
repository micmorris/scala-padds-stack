import sbtrelease.Version.Bump.Bugfix
import sbtrelease.{Version, versionFormatError}
import scalapb.compiler.Version.scalapbVersion

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Xlint:-unused,_" //Unused imports and unused params
)

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

Compile / PB.targets := Seq(
  scalapb
    .gen(flatPackage = true, javaConversions = false) -> (Compile / sourceManaged).value
)

//TODO: Credentials
//credentials += Credentials(Path.userHome / ".ivy2" / "credentials")
//TODO: Publish Destination
//publishTo := Some(
//  "My Nexus Repo" at "https://todo"
//)

releaseVersionBump := Bugfix
releaseCommitMessage := s"Setting version to ${(ThisBuild / version).value} [ci skip])"

releaseVersion := { ver =>
  Version(ver)
    .map(_.bump(releaseVersionBump.value).withoutQualifier.string)
    .getOrElse(versionFormatError(ver))
}

releaseNextVersion := { ver =>
  Version(ver).map(_.string).getOrElse(versionFormatError(ver))
}

lazy val scalacticVersion = "3.2.9"

lazy val root = (project in file(".")).settings(
  inThisBuild(List(organization := "micmorris", scalaVersion := "2.12.15")),
  name := "scala-padds-stack",
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.11.1",
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2" % "protobuf",
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2",
    "org.scalactic" %% "scalactic" % scalacticVersion,
    "org.scalatest" %% "scalatest" % scalacticVersion % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test
  )
)
