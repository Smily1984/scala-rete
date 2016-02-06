package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class RootNodeActor(ruleId: String, underlyingNodes: Vector[ActorRef]) extends Actor with ReteNodeActor {
  def receive = {
    case a: Assertion => fire(a, underlyingNodes)
    case _ => println(s"Root for $ruleId: confused.")
  }
}
