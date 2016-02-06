package nl.bridgeworks.akka.rete

import akka.actor.{Props, ActorSystem, Actor}

class TerminalNodeActor extends Actor {
  def receive = {
    case Fact(contents) => println(s"Terminal node: got this $contents Fact")
    case _ => println("Terminal node: confused.")
  }
}

object TerminalMain {
  def main(args: Array[String]) {
    val system = ActorSystem("Shakespeare")
    val t = system.actorOf(Props[TerminalNodeActor], "terminal")
    val d = system.actorOf(Props(new DummyNodeActor(t)), "dummy")

    d ! Fact("hello first fact")

    system.terminate
  }
}