# iq-abstract — Core Interfaces and Contracts

`iq-abstract` defines the foundational interfaces, exceptions, and shared types that every other IQ module depends on. It is the lingua franca of the platform — the set of contracts that allows agents, connectors, realms, and tools to interoperate without coupling to each other's implementations.

## What it defines

- **Agent interfaces** — `I_Agent`, `I_Intent`, `I_Intents`, `I_Delegate` and the contracts for stateful execution
- **State machine interfaces** — `I_StateMachine`, `StateException`, and the transition model
- **Finder and search interfaces** — `I_Search`, `I_Corpus`, `I_Find`, `I_Found`, `I_Indexer` — the contracts for semantic retrieval
- **Platform primitives** — `I_Self` (identity), `I_StartStop` (lifecycle), `IQ_NS` (IQ vocabulary IRIs)
- **Realm contracts** — `I_Realm`, `I_Realms`, `PlatformException`
- **Shared constants** — `COMMONS` (namespace URIs), `RDF` (annotation type), `Formats` (date and string formatters)

## Role in the system

Every module depends on `iq-abstract`. It has no dependencies of its own beyond RDF4J model types. This keeps the contract layer lean and stable while implementations evolve freely.

If you are building a new connector, a new agent behaviour, or an integration with an external tool, the interfaces you need to satisfy are defined here.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-abstract compile`
