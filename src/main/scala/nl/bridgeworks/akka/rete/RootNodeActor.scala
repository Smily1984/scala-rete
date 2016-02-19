package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class RootNodeActor(underlyingNodes: List[ActorRef]) extends Actor with ReteNodeActor {
  def receive = {
    case a: Assertion => fire(a, underlyingNodes)
    case _ => println(s"Root: confused.")
  }
}
