package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class BetaNodeActor(underlyingNode: ActorRef, onSide: Side) extends Actor with ReteNodeActor {
  def receive = {
    case (a: Assertion, from: Side) if a.facts.length == 1 =>
      if (checkWM(a.facts.head.contents, from)) onSide match {
        case NA => fire(a, Vector(underlyingNode))
        case _ => fire(a, onSide, Vector(underlyingNode))
      }
    //TODO remove later once proven to work
    case a: Assertion if a.facts.length > 1  => println("Beta: multiple facts.")
    case _ => println("Beta: confused.")
  }

  //TODO implement the check in WM
  def checkWM(contents: String, side: Side): Boolean = {
    println(s"Beta: checks WM for $contents on the $side")
    true
  }
}
