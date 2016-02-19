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
    val cs = system.actorOf(Props(new ConflictSetActor()))
    val rootNodes = rules map (rule => buildRuleNetwork(rule, cs, system))
    rootNodes foreach {node => cs ! ("add child", node)}
    cs
  }

  def buildRuleNetwork(rule: Rule, csNode: ActorRef, system: ActorSystem): ActorRef = {
    //spin a terminal that will handle all facts from the RHS of the rule
    val terminal = spinTerminal(rule, csNode, system)
    //spin a single dummy node
    val dummy = system.actorOf(Props(new DummyNodeActor(Left)))
    //spin an alpha node for each LHS of the rule
    val alphas = spinAlpha(rule, system)
    spinBeta(alphas, terminal, dummy, system)
    spinRoot(dummy :: alphas, system)
  }

  def spinAlpha(rule: Rule, system: ActorSystem): List[ActorRef] = {
    val alphas:Vector[ActorRef] = rule.lhs map {e =>
      val p = predicate(e)_
      //an alpha node is always on the right of an underlying beta node, see README.md
      system.actorOf(Props(new AlphaNodeActor(p, Right)))
    }

    alphas.toList
  }

  def spinBeta(alphas: List[ActorRef], terminal: ActorRef, parentOnLeft: ActorRef, system: ActorSystem): Unit = {
    //there's always the same amount of alpha nodes as beta nodes
    //TODO optimize in the future and re-use an alpha node when more rules have the same expression in their LHS
    alphas match {
      //TODO here nothing really happens, how to implement "return"
      case Nil => println("done creating beta nodes.")
      case a :: tail => {

        val b = tail match {
          //when dealing the last alpha node, side doesn't matter because the underlying node will be a terminal node
          case Nil => {
            val b = system.actorOf(Props(new BetaNodeActor(NA)))
            println(s"created beta node: $b", b)
            b ! ("add child", terminal, NA)
            b
          }
          //beta node is always on the left above another underlying beta node
          case _ => {
            val b = system.actorOf(Props(new BetaNodeActor(Left)))
            println(s"created beta node: $b", b)
            b
          }
        }

        //notify the nodes (d+a or b+a) above that an underlying beta node was created
        println(s"notifying parent on the left: $parentOnLeft")
        parentOnLeft ! ("add child", b, Left)
        println(s"notifying parent on the right: $a")
        a ! ("add child", b)

        spinBeta(tail, terminal, b, system)
      }
    }
  }

  def spinTerminal(rule: Rule, csNode: ActorRef, system: ActorSystem): ActorRef = {
    system.actorOf(Props(new TerminalNodeActor(rule.rhs, csNode)), "t-" + rule.id)
  }

  def spinRoot(alphas: List[ActorRef], system: ActorSystem): ActorRef = {
    system.actorOf(Props(new RootNodeActor(alphas)))
  }
}
