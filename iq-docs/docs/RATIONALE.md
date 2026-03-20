# design rationale for IQ

IQ is a platform for building, running and governing neuro-symbolic agents and playbooks. The project focuses on representing knowledge as RDF graphs (the "fact graph") and making those graphs executable via scripts, SPARQL and lightweight runtime glue.

## Key motivations
- **Explainability & provenance**: RDF triples and named IRIs provide transparent provenance and easier auditing than opaque state blobs.
- **Composability**: Domain behaviour is data-first (TTL + SPARQL + scripts), enabling reuse across agents and deployments.
- **Trust & Governance**: Trust zones and explicit vaults (`.iq/vault`, `EnvsAsSecrets`) enable multi-realm security models and separation of concerns.
- **Practical dev experience**: Quarkus provides fast dev loop and a small, well-integrated runtime for APIs and local testing.

## Core design decisions (with why)
- **RDF-first (TTL + SPARQL)** — chosen for a clear, formal representation of knowledge that supports queries, constructs and reasoning. It trades some developer-friendliness for precision and explicitness.
- **Scripts as data** — prompts, Groovy scripts and SPARQL live alongside TTL assets; this keeps behaviour versionable and discoverable (see `IQScriptCatalog`/`ModelScriptCatalog`).
- **LLM integration via named maps** — many use-cases need multiple providers/configs; `LLMFactory` standardises config resolution and secret lookup, keeping provider code small and testable.
- **Local examples** — a local area to store sample vaults, repos and prompts that mirror production concerns without storing secrets.
- **Modular Maven multi-module layout** — keeps responsibilities separated (runtime APIs vs platform libs vs RDF tooling), enabling partial builds with `-pl <module> -am`.

## Trade-offs & constraints
- RDF and SPARQL are powerful but have a learning curve — prefer providing small examples (`.ttl` + `.sparql`) when adding functionality.
- Quarkus favors rapid iteration and small deployments but introduces framework-specific idioms (Dev Mode, extensions) contributors should follow.
- Integration tests that touch live LLMs or external services are powerful but fragile and costly; keep them gated (`-DskipITs`) and document required secrets.

## Practical guidance for contributors
- Add **data + query**: when adding behaviour, provide a small `.ttl` and a `.sparql` file and register them in the appropriate catalog (if applicable).
- Tests: mock LLMs in unit tests and reserve real-provider tests for integration tests with clear documentation and gating by `-DskipITs=false`.
- Secrets: never commit real credentials. Use `EnvsAsSecrets` or local `.iq/vault` sanitized examples for development.
- Small PRs: prefer narrow, focused PRs that change a module and include tests and docs (`IQ.md`, `SEMANTICS.md` or the module README).

## Files to read for context
- `README.md` and `BUILD.md` — high-level and build commands
- `iq-apis/README.md` and `iq-apis/docs/API_LLM.md` — runtime and LLM examples
- `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` — LLM config resolution
- `IQScriptCatalog` / `ModelScriptCatalog` classes — where scripts and queries are discovered
- `.iq/` — sample vaults, repositories and prompt maps

