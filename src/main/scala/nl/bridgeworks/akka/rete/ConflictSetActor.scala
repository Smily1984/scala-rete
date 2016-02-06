package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class ConflictSetActor(rootNodes: Vector[ActorRef]) extends Actor with ReteNodeActor {
  def receive = {
    case Assertion(facts, runId) => {
      println(s"CS: got $facts in a set.")
      //fire(rootNodes, Assertion(facts, runId))
    }
    case _ => println("CS: confused.")
  }
}