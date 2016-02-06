package nl.bridgeworks.akka.rete

case class Fact(contents: String)
case class ConflictSet(facts: Vector[Fact])

sealed trait Side
case object Left extends Side
case object Right extends Side