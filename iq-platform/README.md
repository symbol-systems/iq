# iq-platform — Core Agent Engine

`iq-platform` is the heart of IQ. It provides the agent runtime, realm lifecycle management, LLM provider wiring, state machine execution, and the RDF repository layer that everything else builds on.

This is where intents become actions. A realm loads, its knowledge graph is read, agents are instantiated, and the finite state machine that governs each agent begins running. LLM decisions, SPARQL queries, and script executions all flow through this layer.

## What it provides

- **RealmManager** — creates, caches, and manages isolated knowledge realms, each with its own repository and secrets
- **Agent lifecycle** — boots agents from realm data, wires LLM decision-making, and registers them with the thread manager
- **ModelStateMachine** — an RDF-backed finite state machine that tracks where each agent is in its workflow
- **LLMFactory** — resolves and initialises LLM providers (OpenAI, Groq, custom endpoints) from realm configuration
- **IQScriptCatalog / ModelScriptCatalog** — loads and executes named SPARQL scripts that drive domain behaviour
- **RDF repository configuration** — manages native, memory, and inferencing stores using the RDF4J config vocabulary
- **JWT token generation** — mints realm-scoped bearer tokens for secure API access

## Role in the system

`iq-platform` is a library module — it does not run standalone. `iq-apis` depends on it to boot the full server. `iq-agentic` builds on it to construct agents and avatars.

When adding new platform-level behaviour — a new kind of realm, a new state machine rule, a new repository type — this is where it lives.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-platform -am compile`
