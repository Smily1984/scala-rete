package nl.bridgeworks.akka.rete

import akka.actor.{DeadLetter, Props, ActorSystem}

object ConsoleApp {
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

    val r7 = Rule("7", Vector[Expr](Simple("Single with few financial burdens")), Vector[Fact](ConceptOnly("5 points")))
    val r8 = Rule("8", Vector[Expr](Simple("A couple with children")), Vector[Fact](ConceptOnly("3 points")))
    val r9 = Rule("9", Vector[Expr](Simple("Young family with a home")), Vector[Fact](ConceptOnly("1 points")))
    val r10 = Rule("10", Vector[Expr](Simple("Mature family")), Vector[Fact](ConceptOnly("5 points")))
    val r11 = Rule("11", Vector[Expr](Simple("Preparing for retirement")), Vector[Fact](ConceptOnly("3 points")))
    val r12 = Rule("12", Vector[Expr](Simple("Retired")), Vector[Fact](ConceptOnly("1 points")))

    val rules = Vector[Rule](r1, r2, r3, r4, r5, r6)

    val cs = buildReteNetwork(rules, system)
    cs ! Assertion(Vector(ConceptOnly("runny nose"), ConceptWithValue("temp", 101), ConceptOnly("headache")), java.util.UUID.randomUUID.toString)

    Thread.sleep(50)

    cs ! "print"

    system.terminate
  }
}
