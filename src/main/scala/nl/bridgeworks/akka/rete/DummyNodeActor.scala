package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class DummyNodeActor(betaNode: ActorRef, side: Side) extends Actor {
  def receive = {
    case Fact(contents) => betaNode ! (Fact(contents), side)
    case _ => println("Dummy node: confused.")
  }
}