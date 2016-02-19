package nl.bridgeworks.akka.rete

import akka.actor.{ActorRef, Actor}

class BetaNodeActor extends Actor with ReteNodeActor {
  //keep fact and the corresponding inference run id
  var wmLeft = List[String]()
  var wmRight = List[String]()

  //TODO how to initialize as a single actor, same question in the alpha node
  var underlyingNode = List[ActorRef]()

  def receive = {
    case (a: Assertion, onSide: Side) if a.facts.length == 1 =>
      //the beta node always receives a single fact in an assertion
      if (wmUpdated(onSide, withInferenceRunId = a.inferenceRunId))
        //the beta is always on the left above an underlying node
        fire(a, Left, underlyingNode)
    case ("add child", a:ActorRef, side:Side) => underlyingNode = a :: underlyingNode
    //TODO remove later once proven to work
    case a: Assertion if a.facts.length > 1  => println("Beta: multiple facts.")
    case _ => println("Beta: confused.")
  }

  def wmUpdated(onSide: Side, withInferenceRunId: String): Boolean = {
    onSide match {
      case Left =>
        wmRight.find(item => item == withInferenceRunId) match {
          case None =>
            wmLeft = update(wmLeft, withInferenceRunId)
            false
          case Some(_) =>
            true
        }
      case Right =>
        wmLeft.find(item => item == withInferenceRunId) match {
          case None =>
            wmRight = update(wmRight, withInferenceRunId)
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