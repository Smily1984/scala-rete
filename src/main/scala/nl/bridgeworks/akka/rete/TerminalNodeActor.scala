package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

class TerminalNodeActor(factsFromRHS: Vector[Fact], csNode: ActorRef) extends Actor {
  def receive = {
    case Fact(contents) => fire(contents)
    case _ => println("Terminal: confused.")
  }

  //a rule fires by asserting all facts from its RHS into RETE
  def fire(contents: String) = {
    //TODO check if facts are already known, currently fires always
    csNode ! ConflictSet(factsFromRHS)
  }
}

object TerminalMain {
  def main(args: Array[String]) {
    val system = ActorSystem("Shakespeare")

    val cs = system.actorOf(Props(new ConflictSetActor(Vector.empty[ActorRef])), "cs")
    val t = system.actorOf(Props(new TerminalNodeActor(Vector(Fact("achiness")), cs)), "terminal")
    val b = system.actorOf(Props(new BetaNodeActor(t)), "beta")
    val d = system.actorOf(Props(new DummyNodeActor(b, Left)), "dummy")
    val a = system.actorOf(Props(new AlphaNodeActor(b, Right)), "alpha")
    val root = system.actorOf(Props(new RootNodeActor("R4", Vector(a, d))), "root")

    root ! ConflictSet(Vector(Fact("headache")))

    system.terminate
  }
}