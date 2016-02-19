package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class ConflictSetActor extends Actor with ReteNodeActor {
  private var rootNodes = List[ActorRef]()

  def receive = {
    case a:Assertion =>
      println(s"CS: got an assertion in a set.")
      fire(a, rootNodes)
    case ("add child", a:ActorRef) =>
      println("CS: adding rule " + a)
      rootNodes = a :: rootNodes
    case _ => println("CS: confused.")
  }
}