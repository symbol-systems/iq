# iq-agentic — Agent Builder and Decision Toolkit

`iq-agentic` provides the building blocks for creating, running, and composing AI agents within IQ. It bridges the knowledge graph, the LLM provider, and the intent execution pipeline into a single coherent agent abstraction.

An agent in IQ is not just a prompt wrapper. It has a state machine, a set of intents it can execute, a knowledge graph it reasons over, and a budget it operates within. `iq-agentic` wires all of that together.

## What it provides

- **AgentBuilder** — fluent builder for constructing agents from a realm connection, bindings, and secrets. Handles LLM wiring, SPARQL capability injection, and scripting setup in a few method calls
- **Avatar** — a full conversational agent that combines LLM decision-making with stateful intent execution and chat history
- **ExecutiveAgent** — the primary agent implementation, combining intent execution with delegation and state tracking
- **LLMDecision** — connects an agent to an LLM for transition decisions, grounded by a prompt constructed from real knowledge
- **ChainOfCommand** — composes multiple decision-makers into a ranked pipeline
- **Budget / Treasury** — tracks token spend and enforces cost limits per agent run
- **Remodel** — an intent that rewrites an agent's knowledge based on SPARQL script output, keeping the agent's model current
- **JSR233** — scripting intent that executes named scripts against the agent's state

## Role in the system

`iq-agentic` is consumed by `iq-apis` and `iq-trusted`. It does not run standalone. When building a new kind of agent behaviour, this is where the construction logic lives.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-agentic -am compile`
