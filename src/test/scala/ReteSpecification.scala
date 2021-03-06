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

  val inferenceRunId = java.util.UUID.randomUUID().toString

  "A dummy node" must {
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
      cs ! Assertion(Vector(ConceptOnly("Paris"), ConceptOnly("city")), inferenceRunId)
    }
  }

  "A beta node" must {
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
      val d = delta(List[(Fact, String)](), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId))

      assert(d.facts.size == 1)
      assert(d.inferenceRunId == inferenceRunId)
      assert(d.facts.head.concept == "Amsterdam")
    }

    "filter out and skip a fact that's already known" in {
      val d = delta(List((ConceptOnly("Amsterdam"), inferenceRunId)), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId))

      assert(d.facts.isEmpty)
    }

    "add the fact if it's another inference run" in {
      val d = delta(List((ConceptOnly("Amsterdam"), inferenceRunId)), Assertion(Vector(ConceptOnly("Amsterdam")), inferenceRunId + "-another"))

      assert(d.facts.size == 1)
      assert(d.inferenceRunId == inferenceRunId + "-another")
      assert(d.facts.head.concept == "Amsterdam")
    }

    "do nothing with an empty assertion" in {
      val d = ensureSafety(Assertion(Vector(ConceptOnly("")), inferenceRunId))

      assert(d.facts.isEmpty)
    }
  }

  "A terminal node" must {
    "produce a new fact when it gets an assertion from a node above" in {
      val a = Vector(ConceptOnly("Belmopan"))
      val p = Vector(ConceptOnly("capital of Belize"))

      //hook up the terminal node to the probe instead of the conflict set node
      val terminal = system.actorOf(Props(new TerminalNodeActor(p, probe1.ref)))
      terminal ! (Assertion(a, inferenceRunId), Right)
      val msg = probe1.receiveOne(1 second).asInstanceOf[Assertion]

      assert(msg.facts.head.asInstanceOf[ConceptOnly].concept == "capital of Belize")
      assert(msg.inferenceRunId == inferenceRunId)
    }
  }

  "A root node" must {
    "pass a fact to all underlying nodes" in {
      val a = Vector(ConceptOnly("Belmopan"))

      val root = system.actorOf(Props(new RootNodeActor(List(probe1.ref, probe1.ref, probe1.ref))))
      root ! Assertion(a, inferenceRunId)

      val msg = probe1.receiveOne(1 second).asInstanceOf[Assertion]
      assert(msg.facts.head.asInstanceOf[ConceptOnly].concept == "Belmopan")
      assert(msg.inferenceRunId == inferenceRunId)

      val msg2 = probe1.receiveOne(1 second).asInstanceOf[Assertion]
      assert(msg == msg2)

      val msg3 = probe1.receiveOne(1 second).asInstanceOf[Assertion]
      assert(msg == msg3)

      probe1.expectNoMsg
    }
  }
}