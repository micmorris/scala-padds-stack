import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
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

// Disable multiple tests at once
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

// Compile ScalaPB and pull external_protobuf content
Compile / PB.targets := Seq(
  scalapb
    .gen(flatPackage = true, javaConversions = false) -> (Compile / sourceManaged).value
)

// Compile Guardrail
Compile / guardrailTasks := List(
  ScalaServer(
    baseDirectory.value / "src/main/resources/generated-openapi/openapi.yaml",
    pkg = "com.padds.example.guardrail"
  )
)


//TODO: Credentials
//credentials += Credentials(Path.userHome / ".ivy2" / "credentials")
//TODO: Publish Destination
//publishTo := Some(
//  "My Nexus Repo" at "https://todo"
//)

// Docker / Building and Packaging
Compile / mainClass := Some("com.padds.example.server.PaddsServer")

dockerBaseImage := "openjdk:8-alpine"
dockerUpdateLatest := true
//TODO: docker push registry
dockerRepository := Some("micmorris.padds.example")

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

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  ReleaseStep(releaseStepTask(Docker / publish)), // : ReleaseStep that creates a docker image and pushes it to a remote registry
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

// Dependencies
lazy val akkaHttpVersion = "10.2.6"
lazy val akkaVersion = "2.6.16"
lazy val prometheusVer = "0.12.0"
lazy val pureConfigVer = "0.17.0"
lazy val scalacticVersion = "3.2.9"
lazy val catsVersion = "2.6.1"
lazy val circeVersion = "0.14.1"

lazy val root = (project in file(".")).settings(
  inThisBuild(List(organization := "micmorris", scalaVersion := "2.12.15")),
  name := "scala-padds-stack",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.6" % Runtime,
    "io.prometheus" % "simpleclient" % prometheusVer,
    "io.prometheus" % "simpleclient_common" % prometheusVer,
    "fr.davit" %% "akka-http-metrics-prometheus" % "1.6.0",
    "org.scalactic" %% "scalactic" % scalacticVersion,
    "com.github.pureconfig" %% "pureconfig-core" % pureConfigVer,
    "com.github.pureconfig" %% "pureconfig-generic" % pureConfigVer,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.11.1",
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2" % "protobuf",
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2",
    "org.scalactic" %% "scalactic" % scalacticVersion,
    "org.scalatest" %% "scalatest" % scalacticVersion % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test
  )
)
