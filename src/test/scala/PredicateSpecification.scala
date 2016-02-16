import nl.bridgeworks.akka.rete.{ConceptOnly, Simple, TerminalMain}
import org.scalatest.FlatSpec

class PredicateSpecification extends FlatSpec {
  "A predicate function" should "return true" in {
    assert(TerminalMain.predicate(Simple("Rocky Balboa"))(ConceptOnly("Rocky Balboa")))
  }
}
