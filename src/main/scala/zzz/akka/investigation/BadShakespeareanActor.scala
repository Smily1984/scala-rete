package zzz.akka.investigation

import akka.actor.{Props, ActorSystem, Actor}

class BadShakespeareanActor extends Actor {
  def receive = {
    case "good evening" => println("Him: Forsooth")
    case "you're terrible" => println("Him: Yup")
  }
}

object BadShakespeareanMain {
  val system = ActorSystem("Shakespeare")
  val actor = system.actorOf(Props[BadShakespeareanActor], "Shake")

  def send(msg: String): Unit = {
    println(s"Me: $msg")
    actor ! msg
    Thread.sleep(100)
  }

  def main(args: Array[String]): Unit = {
    send("good evening")
    send("you're terrible")
    system.terminate()
  }
}
