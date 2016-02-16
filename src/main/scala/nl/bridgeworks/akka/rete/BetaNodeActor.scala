package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class BetaNodeActor(underlyingNode: ActorRef, onSide: Side) extends Actor with ReteNodeActor {
  def receive = {
    case (a: Assertion, from: Side) if a.facts.length == 1 =>
      //the beta node always receives a single fact in an assertion
      if (checkWM(a.facts.head, from)) onSide match {
        case NA =>
          //pass the fact to a terminal node
          fire(a, Vector(underlyingNode))
        case _ =>
          //pass the fact to another beta node
          fire(a, onSide, Vector(underlyingNode))
      }
    //TODO remove later once proven to work
    case a: Assertion if a.facts.length > 1  => println("Beta: multiple facts.")
    case _ => println("Beta: confused.")
  }

  //TODO implement the check in WM
  def checkWM(fact: Fact, side: Side): Boolean = {
    println(s"Beta: checks WM for $fact on the $side")
    true
  }
}
