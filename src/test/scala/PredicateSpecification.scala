import nl.bridgeworks.akka.rete.{ConceptWithValue, ConceptOnly, Simple, TerminalMain}
import org.scalatest.FlatSpec

class PredicateSpecification extends FlatSpec {
  "A predicate function" should "return true" in {
    assert(TerminalMain.predicate(Simple("Rocky Balboa"))(ConceptOnly("Rocky Balboa")))
    //TODO is AnyVal the right choice?
    assert(TerminalMain.predicate(Simple("Apollo Creed"))(ConceptWithValue("Apollo Creed", 98429)))
  }
}
