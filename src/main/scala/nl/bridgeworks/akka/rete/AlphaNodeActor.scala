package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class AlphaNodeActor(betaNode: ActorRef, onSide: Side) extends Actor with ReteNodeActor {
  def receive = {
    //execute the predicate function and send to the beta node
    case a: Assertion =>
      if (predicate(a.facts.head.contents)) fire(a, onSide, Vector(betaNode))
    case _ => println("Alpha: confused.")
  }

  def predicate(factToMatch: String):Boolean = {
    factToMatch == "headache"
  }
}
