## IQ: An operating environment for symbolic cognition

IQ enforces operational governance for fleets of neuro-symbolic cognitive AI.

An AI in this context refers to a well-governed entity capable of making autonomous decisions
and executing tasks based on its internal state.

IQ converts RDF graphs, templates into actionable playbooks and patterns.

### Declare an example AI domain

@base https://example.com/v0/
ai: a iq:AI.

# This AI domain trusts the IQ runtime.

ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.

my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake. 

iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Basic needs

ai: iq:needs iq:.
