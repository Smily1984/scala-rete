package nl.bridgeworks.akka.rete

import akka.actor._

class TerminalNodeActor(factsFromRHS: Vector[Fact], csNode: ActorRef) extends Actor with ReteNodeActor with ActorLogging {
  def receive = {
    //a rule fires by asserting all facts from its RHS into RETE
    case (a:Assertion, side:Side) =>
      fire(Assertion(factsFromRHS, a.inferenceRunId), csNode)
    case _ => log.warning("Terminal: confused.")
  }
}

class DeadListener extends Actor with ActorLogging {
  def receive = {
    case DeadLetter(msg, from, to) => log.warning(s"Dead: $msg from $from to $to")
    case _ => log.warning("Dead: confused.")
  }
}
