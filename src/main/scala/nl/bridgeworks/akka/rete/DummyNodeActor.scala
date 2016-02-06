package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class DummyNodeActor(betaNode: ActorRef, side: Side) extends Actor with ReteNodeActor {
  def receive = {
    case a: Assertion => fire(a, side, Vector(betaNode))
    case _ => println("Dummy node: confused.")
  }
}