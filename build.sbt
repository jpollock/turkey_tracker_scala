organization := "com.example"


// set the main class for 'sbt run'
mainClass in (Compile, run) := Some("com.example.Main")

scalaVersion := "2.13.6"

val AkkaStreamKafkaVersion = "2.1.1"
val AkkaVersion = "2.6.15"
val JacksonVersion = "2.11.4"

enablePlugins(AkkaserverlessPlugin, JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters" // for Jackson
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.7" % Test,
  "com.typesafe.akka"     %% "akka-stream-kafka"     % AkkaStreamKafkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion
)
