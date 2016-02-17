package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class AlphaNodeActor(predicate: (Fact) => Boolean, betaNode: ActorRef, onSide: Side) extends Actor with ReteNodeActor {
  def receive = {
    //execute the predicate function and send to the beta node
    case a:Assertion =>
      //the alpha node always receives a single fact in an assertion
      if (predicate(a.facts.head)) fire(a, onSide, betaNode)
    case _ => println("Alpha: confused.")
  }
}
