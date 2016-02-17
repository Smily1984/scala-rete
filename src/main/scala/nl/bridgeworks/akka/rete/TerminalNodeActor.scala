package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

class TerminalNodeActor(factsFromRHS: Vector[Fact], csNode: ActorRef) extends Actor with ReteNodeActor {
  def receive = {
    //a rule fires by asserting all facts from its RHS into RETE
    //TODO check if facts are already known, currently fires always
    case a: Assertion => println(s"Terminal: $a") //fire(Assertion(factsFromRHS, a.inferenceRunId), csNode)
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

    val rules = Vector[Rule](r4)

    val cs = buildReteNetwork(rules, system)
    cs ! Assertion(Vector(ConceptOnly("headache")), java.util.UUID.randomUUID.toString)

    system.terminate
  }

  def predicate(expr: Expr)(fact: Fact): Boolean = {
    expr match {
      case s:Simple => s.concept == fact.concept
      //doesn't make sense to compare when there isn't a value associated with the concept
      case v:ValueOp if fact.isInstanceOf[ConceptOnly] => false
      //execute the operation only if the concept matches and there is a value associated with it
      case v:ValueOp if fact.isInstanceOf[ConceptWithValue] && v.concept == fact.concept => executeExpression(v.op, v.value, fact.asInstanceOf[ConceptWithValue])
      case _ => false
    }
  }

  def executeExpression(op: Op, withValue: AnyVal, on: ConceptWithValue): Boolean = {
    op match {
      case Equals => on.value == withValue
      case LessThan => lessThan(on.value, withValue)
      case GreaterThan => greaterThan(on.value, withValue)
    }
  }

  def lessThan(v1: AnyVal, v2: AnyVal): Boolean = {
    if(v1.getClass == v1.getClass)
      v1 match {
        case i:Int => v1.asInstanceOf[Int] < v2.asInstanceOf[Int]
        case i:Double => v1.asInstanceOf[Double] < v2.asInstanceOf[Double]
        case _ => {
          println("lessThan: confused " + v1.getClass.toString)
          false
        }
      }
    else false
  }

  def greaterThan(v1: AnyVal, v2: AnyVal): Boolean = {
    if(v1.getClass == v1.getClass)
      v1 match {
        case i:Int => v1.asInstanceOf[Int] > v2.asInstanceOf[Int]
        case i:Double => v1.asInstanceOf[Double] > v2.asInstanceOf[Double]
        case _ => {
          println("lessThan: confused " + v1.getClass.toString)
          false
        }
      }
    else false
  }

  def buildReteNetwork(rules: Vector[Rule], system: ActorSystem): ActorRef = {
    val cs = system.actorOf(Props(new ConflictSetActor()), "cs")
    val rootNodes = rules map (rule => buildRuleNetwork(rule, system, cs))
    println("root: " + rootNodes)
    rootNodes foreach {node => cs ! ("rule added", node)}
    cs
  }

  def buildRuleNetwork(rule: Rule, system: ActorSystem, csNode: ActorRef): ActorRef = {
    val t = system.actorOf(Props(new TerminalNodeActor(rule.rhs, csNode)), "t-" + rule.id)
    val b = system.actorOf(Props(new BetaNodeActor(t, NA)), "beta-" + rule.id)
    val d = system.actorOf(Props(new DummyNodeActor(b, Left)), "dummy-" + rule.id)
    //TODO create an alpha node for each rule LHS, supports a single LHS for now
    val a = system.actorOf(Props(new AlphaNodeActor(predicate(rule.lhs.head), b, Right)), "alpha-" + rule.id)
    system.actorOf(Props(new RootNodeActor(rule.id, List(a, d))), "root-" + rule.id)
  }
}