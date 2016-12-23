lazy val root = (project in file(".")).
  settings(
    name := "scala-rete",
    version := "0.1",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    organization := "nl.bridgeworks",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.2",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % "2.4.2" % "test",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      "org.scalaz" %% "scalaz-core" % "7.2.4"
    )
  )