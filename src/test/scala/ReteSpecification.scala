import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import nl.bridgeworks.akka.rete._
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReteSpecification(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("ReteSpecification"))

  override def afterAll = TestKit.shutdownActorSystem(system)

  val inferenceRunId = java.util.UUID.randomUUID().toString
  val probe1 = TestProbe()

  "A dummy node" must {
    "pass a fact unchanged" in {
      val dummy = system.actorOf(Props(new DummyNodeActor(probe1.ref, Left)))
      dummy ! Assertion(Vector(ConceptOnly("bla")), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "bla")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Left)
    }
  }

  "An alpha node" must {
    "pass a fact if a simple predicate is true" in {
      val predicate = TerminalMain.predicate(Simple("Barcelona"))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(predicate, probe1.ref, Left)))
      alpha ! Assertion(Vector(ConceptOnly("Barcelona")), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "Barcelona")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Left)
    }

    "pass a fact if a predicate with an expression is true" in {
      val predicate = TerminalMain.predicate(ValueOp("temperature", LessThan, 100))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(predicate, probe1.ref, Right)))
      alpha ! Assertion(Vector(ConceptWithValue("temperature", 90)), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptWithValue].concept == "temperature")
      assert(msg._1.facts.head.asInstanceOf[ConceptWithValue].value == 90)
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Right)
    }

    "not pass a fact if a predicate is false" in {
      val predicate = TerminalMain.predicate(Simple("John"))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(predicate, probe1.ref, Right)))
      alpha ! Assertion(Vector(ConceptOnly("Marry")), inferenceRunId)
      probe1.expectNoMsg()
    }
  }
}
