package nl.bridgeworks.akka.rete

case class Fact(contents: String)
case class Assertion(facts: Vector[Fact], inferenceRunId: String)

sealed trait Side
case object Left extends Side
case object Right extends Side
//in case a beta node is connected to a terminal node
case object NA extends Side