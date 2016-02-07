This is a Scala implementation of the RETE algorithm. Use it to implement production rules systems.

# Why implement RETE in Scala/Akka?
Three main reasons:
  1. Akka's Actor model perfectly suits the RETE network of nodes;
  2. Scala's support for building internal DSLs is by far the best out there right now;
  3. Code runs on the JVM, which is pretty much the standard for large enterprises.

The ambition is to create a lean and mean, highly performant and competitive alternative to bloated production rule systems like Drools and others. It is meant to be the basis for microservices requiring configurable and dynamic business rules.

# Terms:
[RETE algorithm](https://en.wikipedia.org/wiki/Rete_algorithm)

[Forward chaining](https://en.wikipedia.org/wiki/Forward_chaining)

WM - working memory

node - a node from a RETE network maps to an Actor in Scala

fact - something we know of or we can observe, we make decisions based on facts and pre-defined rules that acts upon those facts

# Let's start with an example:
       
### Rules:

1. R1: nasal congestion and viremia -> diagnosis influenza
2. R2: runny nose -> nasal congestion
3. R3: body aches -> achiness
4. R4: headache -> achiness
5. R5: temp greater than 100 -> fever
6. R6: fever and achiness -> viremia

### Input facts:

1. F1: runny nose
2. F2: temp = 101.5
3. F3: headache

### How it works in steps:

1. F1 causes R2 to fire, R2 produces "nasal congestion" (asserts this fact into working memory (WM):
  * at the same time as R2, F2 causes R5 to fire, R5 produces "fever" into WM
  * at the same time as R2 and R5, F3 causes R4 to fire, R4 produces "achiness" into WM
2. so far, all assertions have been passed to RETE and three new facts have been produced:
  * nasal congestion
  * fever 
  * and achiness
3. the last three facts are also provided to RETE and the following happens:
4. facts "fever" and "achiness" trigger rule R6, which asserts "viremia" into WM
5. facts "viremia" and "nasal congestion" (step 1.) trigger rule R1 which then asserts "diagnosis influenza" into WM.

Thee facts (F1, F2 and F3) are given as input to RETE, which then produces four new facts:
"nasal congestion", "fever", "achiness" and "diagnosis viremia".

### Good to know:

A rule has a left-hand side (LHS) and a right-hand side (RHS);

When facts asserted into WM matches the LHS of a rule, RETE executes it's RHS and produces new facts;

In order to support simultaneos and independent clients, a uniquie identifier is required for each batch of facts submitted to RETE initially. We call this an inference run id.

All facts produced by terminal nodes are "fed" back to RETE (see diagram above), which then passes them to all root nodes in it's network. That's how forward chaining works. When no more terminal nodes fire, or the facts produced are already known, the inference run stops.

![forward chaining loop](https://github.com/bridgeworks-nl/scala-rete/blob/master/doc/forward_chaining.png)

# Actor Design
![rete nodes for the first and second rules](https://github.com/bridgeworks-nl/scala-rete/blob/master/doc/rete_nodes.png)

## Root actor
Each rule gets one root actor. It knows of all underlying alpha and dummy actors. The root actor passes on each fact it receives to them.
## Alpha actor
Each pre-condition of a rule gets an alpha actor. For example in rule R1 there are two pre-conditions (nasal congestion and viremia). That means that R1 gets two alpha actors. The alpha actor has a predicate function that checks the corresponding pre-condition. If the predicate returns true, the fact is passed to the underlying beta actor. If the predicate returns false, nothing happens.
##Dummy actor
Passes on a fact it receives without applying any logic. It's introduction simplifies the logic in the alpha actor. It knows about a single beta actor.
## Beta actor
It alwas has a left side and a right side. It receives facts from alpha and dummy actors on it's left and right sides. It knows a single terminal actor.
## Terminal actor
When a terminal actor receives a fact from a beta actor, it executes the RHS of the rule. For example in R1, the terminal actor will produce the "diagnosis influenza" fact.

##Literature
[Formalisation of the Rete Algorithm](https://hal.inria.fr/file/index/docid/280938/filename/rete.formalisation.pdf)
