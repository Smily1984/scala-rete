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

  //TODO implement the check in WM
  def checkWM(fact: Fact, side: Side, inferenceRunId: String): Boolean = {
    println(s"Beta $self: checks WM for $fact on the $side")
    side match {
      case Left => {
        wmRight.find(item => (item._1 == fact && item._2 == inferenceRunId)) match {
          case None =>
            println("from left not found in right, adding.")
            wmLeft = addToWM(wmLeft, fact, inferenceRunId)
            false
          case Some(_) =>
            wmLeft = addToWM(wmLeft, fact, inferenceRunId)
            println("from left found in right!")
            true
        }
      }
      case Right => {
        wmLeft.find(item => (item._1 == fact && item._2 == inferenceRunId)) match {
          case None =>
            println("from right not found in left, adding.")
            wmRight = addToWM(wmRight, fact, inferenceRunId)
            false
          case Some(_) =>
            wmRight = addToWM(wmRight, fact, inferenceRunId)
            println("from right found in left!")
            true
        }
      }
      case _ => {
        println("Beta: (wm) confused.")
        false
      }
    }
  }
}
