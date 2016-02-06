package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class RootNodeActor(ruleId: String, underlyingNodes: Vector[ActorRef]) extends Actor {
  def receive = {
    case ConflictSet(facts) => facts foreach (fact => multicast(underlyingNodes, fact))
    case _ => println(s"Root for $ruleId: confused.")
  }

  //TODO replace this with an AKKA URI expression?
  def multicast(nodes: Vector[ActorRef], fact: Fact): Unit = {
    nodes foreach (_ ! fact)
  }
}
