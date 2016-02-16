import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import nl.bridgeworks.akka.rete._
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReteSpecification(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("ReteSpecification"))

  override def afterAll {
    println("afterAll")
    TestKit.shutdownActorSystem(system)
  }

  "A dummy node" must {
    "pass a fact unchanged" in {
      val probe1 = TestProbe()
      val inferenceRunId = java.util.UUID.randomUUID().toString

      val dummy = system.actorOf(Props(new DummyNodeActor(probe1.ref, Left)))
      dummy ! Assertion(Vector(ConceptOnly("bla")), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "bla")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Left)
    }
  }
}
