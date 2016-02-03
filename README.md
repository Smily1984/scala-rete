This is a Scala implementation of the RETE algorithm. Use it to implement production rules systems.

# Why implement RETE in Scala?
Two main reasons:
1. Scala's Actor model perfectly suits the RETE network of nodes;
2. Scala's support for building internal DSLs is by far the best out there right now;

# Terms:
[RETE algorithm](https://en.wikipedia.org/wiki/Rete_algorithm)
[Forward chaning](https://en.wikipedia.org/wiki/Forward_chaining)
WM - working memory

# An Example of Forward Chaining:

Rules:

1. R1: nasal congestion and viremia -> diagnosis influenza
2. R2: runny nose -> nasal congestion
3. R3: body aches -> achiness
4. R4: headache -> achiness
5. R5: temp greater than 100 -> fever
6. R6: fever and achiness -> viremia

Input facts:

1. F1: runny nose
2. F2: temp = 101.5
3. F3: headache

How it works in steps:

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

Bottom line is:

Thee facts (F1, F2 and F3) are given as input to RETE, which then produces four new facts:
"nasal congestion", "fever", "achiness" and "diagnosis viremia".

Good to know:

An inference run finishes when no more new facts get produced as a result of one or more rules firing (i.e. when R6 fires and asserts the fact into WM, no more rules fire);
A rule has a left-hand side (LHS) and a right-hand side (RHS);
When facts asserted into WM match the LHS of a rule, RETE executes it's RHS and produces new facts;
