package nl.bridgeworks.akka.rete

import akka.actor._

class TerminalNodeActor(factsFromRHS: Vector[Fact], csNode: ActorRef) extends Actor with ReteNodeActor {
  def receive = {
    //a rule fires by asserting all facts from its RHS into RETE
    case (a:Assertion, side:Side) =>
      fire(Assertion(factsFromRHS, a.inferenceRunId), csNode)
    case _ => println("Terminal: confused.")
  }
}

class DeadListener extends Actor {
  def receive = {
    case DeadLetter(msg, from, to) => println(s"Dead: $msg from $from to $to")
    case _ => println("Dead: confused.")
  }
}

object TerminalMain {
  def main(args: Array[String]) {
    val system = ActorSystem("Shakespeare")
    val listener = system.actorOf(Props(new DeadListener))
    system.eventStream.subscribe(listener, classOf[DeadLetter])

    val r1 = Rule("1", Vector[Expr](Simple("nasal congestion"), Simple("viremia")), Vector[Fact](ConceptOnly("diagnosis influenza")))
    val r2 = Rule("2", Vector[Expr](Simple("runny nose")), Vector[Fact](ConceptOnly("nasal congestion")))
    val r3 = Rule("3", Vector[Expr](Simple("body aches")), Vector[Fact](ConceptOnly("achiness")))
    val r4 = Rule("4", Vector[Expr](Simple("headache")), Vector[Fact](ConceptOnly("achiness")))
    val r5 = Rule("5", Vector[Expr](ValueOp("temp", GreaterThan, 100)), Vector[Fact](ConceptOnly("fever")))
    val r6 = Rule("6", Vector[Expr](Simple("fever"), Simple("achiness")), Vector[Fact](ConceptOnly("viremia")))

    val rules = Vector[Rule](r1, r2, r3, r4, r5, r6)

    val cs = buildReteNetwork(rules, system)
    cs ! Assertion(Vector(ConceptOnly("runny nose"), ConceptWithValue("temp", 101), ConceptOnly("headache")), java.util.UUID.randomUUID.toString)

    Thread.sleep(2000)

    cs ! "print"

    system.terminate
  }
}