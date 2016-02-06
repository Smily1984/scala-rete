package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

class TerminalNodeActor(factsFromRHS: Vector[Fact], csNode: ActorRef) extends Actor with ReteNodeActor {
  def receive = {
    //a rule fires by asserting all facts from its RHS into RETE
    //TODO check if facts are already known, currently fires always
    case a: Assertion => fire(Assertion(factsFromRHS, a.inferenceRunId), Vector(csNode))
    case _ => println("Terminal: confused.")
  }
}

object TerminalMain {
  def main(args: Array[String]) {
    val system = ActorSystem("Shakespeare")

    val cs = system.actorOf(Props(new ConflictSetActor(Vector.empty[ActorRef])), "cs")
    val t = system.actorOf(Props(new TerminalNodeActor(Vector(Fact("achiness")), cs)), "terminal")
    val b = system.actorOf(Props(new BetaNodeActor(t, NA)), "beta")
    val d = system.actorOf(Props(new DummyNodeActor(b, Left)), "dummy")
    val a = system.actorOf(Props(new AlphaNodeActor(b, Right)), "alpha")
    val root = system.actorOf(Props(new RootNodeActor("R4", Vector(a, d))), "root")

    root ! Assertion(Vector(Fact("headache")), java.util.UUID.randomUUID.toString)

    system.terminate
  }
}