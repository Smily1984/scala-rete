import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import nl.bridgeworks.akka.rete._
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReteSpecification(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("RETE"))

  override def afterAll = TestKit.shutdownActorSystem(system)

  val probe1 = TestProbe()

  "A dummy node" must {
    val inferenceRunId = java.util.UUID.randomUUID().toString

    "pass a fact unchanged" in {
      val dummy = system.actorOf(Props(new DummyNodeActor()))
      //make sure the dummy knows of an underlying beta node (this is simulated by the probe)
      dummy ! ("add child", probe1.ref, NA)
      dummy ! Assertion(Vector(ConceptOnly("bla")), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "bla")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Left)
    }
  }

  "An alpha node" must {
    "pass a fact if a simple predicate is true" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString

      val p = predicate(Simple("Barcelona"))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(p)))
      alpha ! ("add child", probe1.ref)
      alpha ! Assertion(Vector(ConceptOnly("Barcelona")), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "Barcelona")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Right)
    }

    "pass a fact if a predicate with an expression is true" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString

      val p = predicate(ValueOp("temperature", LessThan, 100))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(p)))
      alpha ! ("add child", probe1.ref)
      alpha ! Assertion(Vector(ConceptWithValue("temperature", 90)), inferenceRunId)
      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptWithValue].concept == "temperature")
      assert(msg._1.facts.head.asInstanceOf[ConceptWithValue].value == 90)
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Right)
    }

    "not pass a fact if a predicate is false" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString

      val p = predicate(Simple("John"))_
      val alpha = system.actorOf(Props(new AlphaNodeActor(p)))
      alpha ! ("add child", probe1.ref)
      alpha ! Assertion(Vector(ConceptOnly("Marry")), inferenceRunId)

      probe1.expectNoMsg()
    }
  }

  "A RETE network with one rule" must {
    val rule = Rule("single", Vector(Simple("Belmopan")), Vector(ConceptOnly("capital of Belize")))
    val cs = buildReteNetwork(Vector(rule), system)

    "initialize without errors" in {
      assert(cs.isInstanceOf[ActorRef])
    }

    //TODO how to check if the actor logs INFO messages?
    "handle an assertion without errors" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString

      cs ! Assertion(Vector(ConceptOnly("Belmopan")), inferenceRunId)
    }
  }

  "A RETE network with a rule that has multiple LHS parts" must {
    val rule = Rule("one", Vector(Simple("Paris"), Simple("city")), Vector(ConceptOnly("capital of France")))
    val cs = buildReteNetwork(Vector(rule), system)

    "initialize without errors" in {
      assert(cs.isInstanceOf[ActorRef])
    }

    "handle an assertion without errors" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString

      cs ! Assertion(Vector(ConceptOnly("Paris"), ConceptOnly("city")), inferenceRunId)
    }
  }

  "A beta node" must {
    val inferenceRunId = java.util.UUID.randomUUID().toString

    "pass a fact further down if the same fact arrives on both sides" in {
      val b = system.actorOf(Props(new BetaNodeActor()))
      b ! ("add child", probe1.ref, NA)

      b ! (Assertion(Vector(ConceptOnly("London")), inferenceRunId), Left)
      b ! (Assertion(Vector(ConceptOnly("London")), inferenceRunId), Right)

      val msg = probe1.receiveOne(1 second).asInstanceOf[(Assertion, Side)]

      assert(msg._1.facts.head.asInstanceOf[ConceptOnly].concept == "London")
      assert(msg._1.inferenceRunId == inferenceRunId)
      assert(msg._2 == Left)
    }

    "not pass a fact if the fact arrives twice on the same side" in {
      val b = system.actorOf(Props(new BetaNodeActor()))
      b ! ("add child", probe1.ref, NA)

      b ! (Assertion(Vector(ConceptOnly("London")), inferenceRunId), Left)
      b ! (Assertion(Vector(ConceptOnly("London")), inferenceRunId), Left)

      probe1.expectNoMsg()
    }
  }

  //TODO property based test for this logic?
  "WM" must {
    val inferenceRunId = java.util.UUID.randomUUID().toString

    "add an unknown fact" in {
      val wm = update(List[String](), inferenceRunId)
      assert(wm.size == 1)
    }

    "skip a known fact" in {
      val wm = update(List[String](), inferenceRunId)
      val wm2 = update(wm, inferenceRunId)

      assert(wm.size == 1)
      assert(wm2.size == 1)
    }

    "add the same fact, because it's from another inference run" in {
      val wm = update(List[String](), inferenceRunId)
      val wm2 = update(wm, inferenceRunId + "-2")

      assert(wm2.size == 2)
    }
  }

  "Conflict resolution" must {
    "add new fact" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString
      val d = delta(List[(Fact, String)](), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId))

      assert(d.facts.size == 1)
      assert(d.inferenceRunId == inferenceRunId)
      assert(d.facts.head.concept == "Amsterdam")
    }

    "filter out and skip a fact that's already known" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString
      val d = delta(List((ConceptOnly("Amsterdam"), inferenceRunId)), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId))

      assert(d.facts.isEmpty)
    }

    "add the fact if it's another inference run" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString
      val d = delta(List((ConceptOnly("Amsterdam"), inferenceRunId)), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId + "-another"))

      assert(d.facts.size == 1)
      assert(d.inferenceRunId == inferenceRunId + "-another")
      assert(d.facts.head.concept == "Amsterdam")
    }

    "do nothing with an empty assertion" in {
      val inferenceRunId = java.util.UUID.randomUUID().toString
      val d = ensureSafety(Assertion(Vector(ConceptOnly("")), inferenceRunId))

      assert(d.facts.isEmpty)
    }
  }
}