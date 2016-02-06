package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class DummyNodeActor(terminalNode: ActorRef) extends Actor {
  def receive = {
    case Fact(message) => terminalNode ! Fact(message)
    case _ => println("Dummy node: confused.")
  }
}