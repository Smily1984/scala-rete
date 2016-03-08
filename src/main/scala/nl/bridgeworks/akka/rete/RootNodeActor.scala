package nl.bridgeworks.akka.rete

import akka.actor.{ActorLogging, ActorRef, Actor}

class RootNodeActor(underlyingNodes: List[ActorRef]) extends Actor with ReteNodeActor with ActorLogging {
  def receive = {
    case a: Assertion => fire(a, underlyingNodes)
    case _ => log.warning(s"Root: confused.")
  }
}
