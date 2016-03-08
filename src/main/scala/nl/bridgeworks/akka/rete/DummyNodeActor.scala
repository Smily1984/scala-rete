package nl.bridgeworks.akka.rete

import akka.actor.{ActorLogging, ActorRef, Actor}

//TODO how to set the side from a message, not initialize upfront?
class DummyNodeActor extends Actor with ReteNodeActor with ActorLogging {
  //TODO how to init with a single actor ref?
  private var beta = List[ActorRef]()

  def receive = {
    case a:Assertion =>
      fire(a, Left, beta)
    case ("add child", a:ActorRef, s:Side) =>
      beta = a :: beta
    case _ => log.warning("Dummy: confused.")
  }
}
