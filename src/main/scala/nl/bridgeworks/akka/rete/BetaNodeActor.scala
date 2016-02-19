package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class BetaNodeActor extends Actor with ReteNodeActor {
  //keep fact and the corresponding inference run id
  var wmLeft = List[(Fact, String)]()
  var wmRight = List[(Fact, String)]()

  //TODO how to initialize as a single actor, same question in the alpha node
  var underlyingNode = List[ActorRef]()

  def receive = {
    case (a: Assertion, from: Side) if a.facts.length == 1 =>
      //the beta node always receives a single fact in an assertion
      if (checkWM(a.facts.head, from, a.inferenceRunId))
        //the beta is always on the left above an underlying node
        fire(a, Left, underlyingNode)
    case ("add child", a:ActorRef, side:Side) => underlyingNode = a :: underlyingNode
    //TODO remove later once proven to work
    case a: Assertion if a.facts.length > 1  => println("Beta: multiple facts.")
    case _ => println("Beta: confused.")
  }

  def checkWM(fact: Fact, side: Side, inferenceRunId: String): Boolean = {
    side match {
      case Left =>
        wmRight.find(item => item._2 == inferenceRunId) match {
          case None =>
            wmLeft = addToWM(wmLeft, fact, inferenceRunId)
            false
          case Some(_) =>
            true
        }
      case Right =>
        wmLeft.find(item => item._2 == inferenceRunId) match {
          case None =>
            wmRight = addToWM(wmRight, fact, inferenceRunId)
            false
          case Some(_) =>
            true
        }
      case _ =>
        println("Beta: (wm) confused.")
        false
    }
  }
}
