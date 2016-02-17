package nl.bridgeworks.akka.rete

import akka.actor.ActorRef

trait ReteNodeActor {
  def fire(a: Assertion, n: ActorRef):Unit = {
    fire(a, List(n))
  }

  def fire(a: Assertion, from: Side, n: ActorRef):Unit = {
    fire(a, from, List(n))
  }

  //send all facts to all nodes, but split them into separate assertions
  def fire(a: Assertion, n: List[ActorRef]):Unit = {
    n foreach (node => a.facts foreach (fact => node ! Assertion(Vector(fact), a.inferenceRunId)))
  }

  def fire(a: Assertion, from: Side, n: List[ActorRef]):Unit = {
    n foreach (node => a.facts foreach (fact => node ! (Assertion(Vector(fact), a.inferenceRunId), from)))
  }
}
