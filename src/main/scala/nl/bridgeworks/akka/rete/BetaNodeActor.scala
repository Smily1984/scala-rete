package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class BetaNodeActor(underlyingNode: ActorRef) extends Actor {
  def receive = {
    case (Fact(contents), side: Side) => if (checkWM(contents, side)) underlyingNode ! Fact(contents)
    case _ => println("Beta: confused.")
  }

  //TODO implement the check in WM
  def checkWM(contents: String, side: Side): Boolean = {
    println(s"Beta: checks WM for $contents on the $side")
    true
  }
}
