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

    val r1 = Rule("1", Vector[Expr](Simple("nasal congestion"), Simple("viremia")), Vector[Fact](ConceptOnly("diagnosis influenza")))
    val r2 = Rule("2", Vector[Expr](Simple("runny nose")), Vector[Fact](ConceptOnly("nasal congestion")))
    val r3 = Rule("3", Vector[Expr](Simple("body aches")), Vector[Fact](ConceptOnly("achiness")))
    val r4 = Rule("4", Vector[Expr](Simple("headache")), Vector[Fact](ConceptOnly("achiness")))
    val r5 = Rule("5", Vector[Expr](ValueOp("temp", GreaterThan, 100)), Vector[Fact](ConceptOnly("fever")))
    val r6 = Rule("6", Vector[Expr](Simple("fever"), Simple("achiness")), Vector[Fact](ConceptOnly("viremia")))

    val rules = Vector[Rule](r1, r2, r3, r4, r5, r6)

    val cs = buildReteNetwork(rules, system)
    cs ! Assertion(Vector(ConceptOnly("headache")), java.util.UUID.randomUUID.toString)

    system.terminate
  }

  def predicate(expr: Expr)(fact: Fact): Boolean = {
    expr match {
      case s:Simple => s.concept == fact
      case v:ValueOp => false
      case _ => true
    }
  }

  def executeOp(fact: Fact, op: Op, value: AnyVal): Boolean = ???

  def buildReteNetwork(rules: Vector[Rule], system: ActorSystem): ActorRef = {
    val cs = system.actorOf(Props(new ConflictSetActor(Vector.empty[ActorRef])), "cs")
    rules map (rule => buildRuleNetwork(rule, system, cs))

    //TODO send all new root nodes to the cs node
    cs
  }

  def buildRuleNetwork(rule: Rule, system: ActorSystem, csNode: ActorRef): ActorRef = {
    val t = system.actorOf(Props(new TerminalNodeActor(rule.rhs, csNode)), "t-" + rule.id)
    val b = system.actorOf(Props(new BetaNodeActor(t, NA)), "beta-" + rule.id)
    val d = system.actorOf(Props(new DummyNodeActor(b, Left)), "dummy-" + rule.id)
    //TODO synthesize the predicate function based on the rule LHS
    //TODO create an alpha node for each rule LHS
    val a = system.actorOf(Props(new AlphaNodeActor(predicate(rule.lhs.head), b, Right)), "alpha-" + rule.id)
    system.actorOf(Props(new RootNodeActor(rule.id, Vector(a, d))), "root-" + rule.id)
  }
}