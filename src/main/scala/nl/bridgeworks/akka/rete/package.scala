package nl.bridgeworks.akka

import akka.actor.{Props, ActorRef, ActorSystem}

package object rete {
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
        case _ =>
          println("lessThan: confused " + v1.getClass.toString)
          false
      }
    else false
  }

  def greaterThan(v1: AnyVal, v2: AnyVal): Boolean = {
    if(v1.getClass == v1.getClass)
      v1 match {
        case i:Int => v1.asInstanceOf[Int] > v2.asInstanceOf[Int]
        case i:Double => v1.asInstanceOf[Double] > v2.asInstanceOf[Double]
        case _ =>
          println("lessThan: confused " + v1.getClass.toString)
          false
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
