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
    val dummy = system.actorOf(Props(new DummyNodeActor()))
    //spin an alpha node for each LHS of the rule
    val alphas = spinAlpha(rule, system)
    spinBeta(alphas, terminal, dummy, system)
    spinRoot(dummy :: alphas, system)
  }

  def spinAlpha(rule: Rule, system: ActorSystem): List[ActorRef] = {
    val alphas:Vector[ActorRef] = rule.lhs map {e =>
      val p = predicate(e)_
      system.actorOf(Props(new AlphaNodeActor(p)))
    }

    alphas.toList
  }

  def spinBeta(alphas: List[ActorRef], terminal: ActorRef, parentOnLeft: ActorRef, system: ActorSystem): Unit = {
    //there's always the same amount of alpha nodes as beta nodes
    //TODO optimize in the future and re-use an alpha node when more rules have the same expression in their LHS
    alphas match {
      //TODO here nothing really happens, how to implement "return"
      case Nil =>
      case a :: tail =>
        //TODO further simplify this piece
        val b = tail match {
          case Nil =>
            val b = system.actorOf(Props(new BetaNodeActor()))
            //this is the last beta node to be created (the one above the terminal node), so link with beta and terminal
            b ! ("add child", terminal, NA)
            b
          case _ =>
            val b = system.actorOf(Props(new BetaNodeActor()))
            b
        }

        //notify the nodes (d+a or b+a) above that an underlying beta node was created
        parentOnLeft ! ("add child", b, Left)
        a ! ("add child", b)

        spinBeta(tail, terminal, b, system)
    }
  }

  def spinTerminal(rule: Rule, csNode: ActorRef, system: ActorSystem): ActorRef = {
    system.actorOf(Props(new TerminalNodeActor(rule.rhs, csNode)), "t-" + rule.id)
  }

  def spinRoot(underlyingNodes: List[ActorRef], system: ActorSystem): ActorRef = {
    system.actorOf(Props(new RootNodeActor(underlyingNodes)))
  }

  def update(workingMemory:List[String], withInferenceRunId:String): List[String] = {
    workingMemory.find(item => item == withInferenceRunId) match {
      case None =>
        withInferenceRunId :: workingMemory
      case Some(_) =>
        workingMemory
    }
  }

  def delta(workingMemory:List[(Fact, String)], withAssertion:Assertion): Assertion = {
    //extract the facts already known for this particular inference run
    val knownFactsForThisInferenceRun:List[Fact] = for (f <- workingMemory; if f._2 == withAssertion.inferenceRunId) yield f._1
    val newFacts:Set[Fact] = withAssertion.facts.toSet.diff(knownFactsForThisInferenceRun.toSet)
    Assertion(newFacts.toVector, withAssertion.inferenceRunId)
  }

  def ensureSafety(forAssertion:Assertion): Assertion = {
    //concepts like "" doesn't make sense
    Assertion(forAssertion.facts.filter(f => f.concept.length > 0), forAssertion.inferenceRunId)
    //TODO distinct facts in the assertion
  }
}