package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class AlphaNodeActor(predicate: (Fact) => Boolean) extends Actor with ReteNodeActor {

  //TODO optimize the network maybe to re-use alpha nodes and send facts to multiple underlying beta nodes
  private var betas = List[ActorRef]()

  def receive = {
    //execute the predicate function and send to the beta node
    case a:Assertion =>
      //the alpha node always receives a single fact in an assertion
      //it is always on the right above a beta node
      if (predicate(a.facts.head)) fire(a, Right, betas)
    case ("add child", a:ActorRef) => betas = a :: betas
    case _ => println("Alpha: confused.")
  }
}
