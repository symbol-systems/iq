# IQ — project overview

- An operating environment for neuro-symbolic cognitive AI: it turns RDF graphs into executable playbooks and agent behaviour.

Major components
- `iq-apis` — Quarkus runtime exposing REST APIs (LLM endpoints, Agent APIs, Model APIs).
- `iq-platform` — core libraries: RDF helpers, state machines, ingestion, LLM glue (`LLMFactory`, `GPTWrapper`).
- `iq-rdf4j` / `iq-trusted` — RDF storage, repository helpers, security and vault integration.
- `iq-agentic` / `iq-persona` — agent frameworks and persona tooling.

How they fit together
- Repositories (.iq or external RDF stores) hold the fact graph.
- Runtimes (Quarkus apps in `iq-apis`) load scripts and catalogs at startup, exposing APIs that query/modify the graph and call LLMs.
- LLM configs are named maps (provider → named map) defined in `.iq` or application config; runtime resolves secrets via `EnvsAsSecrets`.

Quick commands
- Dev (live code): `./mvnw compile quarkus:dev -pl iq-apis -am` (or `./bin/iq`)
- Compile only: `./bin/compile-apis`
- Full build: `./mvnw clean install`
- Build container image: `./bin/build-image` or `./mvnw -Dquarkus.container-image.build=true -DskipTests install`

Where to read next
- `README.md`, `BUILD.md` (build/test notes)
- `iq-apis/README.md` and `iq-apis/docs/API_LLM.md` (runtime and LLM API examples)
- `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` (LLM integration)

Developer caveats
- Do not commit real secrets; use environment variables or add sanitized samples to `.iq`.
- Keep LLM integration tests isolated (they often require live keys and are in integration tests).
