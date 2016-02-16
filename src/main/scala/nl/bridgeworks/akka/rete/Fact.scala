package nl.bridgeworks.akka.rete

sealed trait Fact
case class ConceptOnly(concept: String) extends Fact {
  override def toString = s"Fact($concept)"
}
case class ConceptWithValue(concept: String, value: AnyVal) extends Fact {
  override def toString = s"Fact($concept){$value}"
}

case class Rule(id: String, lhs: Vector[Expr], rhs: Vector[Fact])
case class Assertion(facts: Vector[Fact], inferenceRunId: String)

sealed trait Op
case object Equals extends Op
case object LessThan extends Op
case object GreaterThan extends Op

sealed trait Expr
case class Simple(concept: String) extends Expr
case class ValueOp(concept: String, op: Op, value: AnyVal) extends Expr

sealed trait Side
case object Left extends Side
case object Right extends Side
//in case a beta node is connected to a terminal node
case object NA extends Side