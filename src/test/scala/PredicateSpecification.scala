import nl.bridgeworks.akka.rete._
import org.scalatest.FlatSpec

class PredicateSpecification extends FlatSpec {
  "A predicate function" should "return true" in {
    assert(predicate(Simple("Rocky Balboa"))(ConceptOnly("Rocky Balboa")))
    //TODO is AnyVal the right choice?
    assert(predicate(Simple("Apollo Creed"))(ConceptWithValue("Apollo Creed", 98429)))
    //checks the workings of an expression
    assert(predicate(ValueOp("temperature", GreaterThan, 100))(ConceptWithValue("temperature", 150)))
    assert(predicate(ValueOp("temperature", Equals, 100))(ConceptWithValue("temperature", 100)))
    assert(predicate(ValueOp("temperature", LessThan, 100))(ConceptWithValue("temperature", 90)))
  }

  it should "return false" in {
    assert(!predicate(Simple("Philadelphia"))(ConceptOnly("New York")))
    //can't evaluate the expression if the concept has no associated value
    assert(!predicate(ValueOp("temperature", GreaterThan, 100))(ConceptOnly("temperature")))
    //checks the workings of an expression
    assert(!predicate(ValueOp("speed", GreaterThan, 100))(ConceptWithValue("speed", 90)))
    assert(!predicate(ValueOp("speed", Equals, 100))(ConceptWithValue("speed", 105)))
    assert(!predicate(ValueOp("speed", LessThan, 100))(ConceptWithValue("speed", 120)))
    //comparing two different concepts doesn't make sense
    assert(!predicate(ValueOp("speed", GreaterThan, 100))(ConceptWithValue("pressure", 150)))
    assert(!predicate(ValueOp("speed", GreaterThan, 100))(ConceptOnly("weight")))
  }
}
