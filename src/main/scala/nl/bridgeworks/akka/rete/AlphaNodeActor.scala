package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class AlphaNodeActor(betaNode: ActorRef, side: Side) extends Actor {
  def receive = {
    //execute the predicate function and send to the beta node
    case Fact(contents) => if (predicate(contents)) betaNode ! (Fact(contents), side)
    case _ => println("Alpha: confused.")
  }

  def predicate(factToMatch: String):Boolean = {
    factToMatch == "headache"
  }
}
