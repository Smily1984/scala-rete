package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class ConflictSetActor(rootNodes: Vector[ActorRef]) extends Actor {
  def receive = {
    case ConflictSet(facts) => println(s"CS: got $facts in a set.")
    case _ => println("CS: confused.")
  }
}