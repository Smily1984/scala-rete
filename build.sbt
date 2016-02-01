name := "RETE in Scala"

version := "0.1.0"

scalaVersion := "2.11.7"

organization := "nl.bridgeworks"

libraryDependencies ++= {
	Seq(
		"org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
    "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
	)
}

scalacOptions ++= Seq("-unchecked", "-deprecation")
