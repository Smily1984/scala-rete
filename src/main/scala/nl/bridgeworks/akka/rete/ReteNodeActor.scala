package nl.bridgeworks.akka.rete

import akka.actor.ActorRef

trait ReteNodeActor {
  //send all facts to all nodes, but split them into separate assertions
  def fire(a: Assertion, n: Vector[ActorRef]) = {
    n foreach (node => a.facts foreach (fact => node ! Assertion(Vector(fact), a.inferenceRunId)))
  }

  def fire(a: Assertion, from: Side, n: Vector[ActorRef]) = {
    n foreach (node => a.facts foreach (fact => node ! (Assertion(Vector(fact), a.inferenceRunId), from)))
  }
}
