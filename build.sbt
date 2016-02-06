lazy val root = (project in file(".")).
  settings(
    name := "scala-rete",
    version := "0.1",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    organization := "nl.bridgeworks",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.1"
    )
  )