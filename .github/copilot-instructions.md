# Copilot instructions for IQ (systems.symbol)

## Big picture architecture
- Multi-module Maven project, root `pom.xml` controls versions.
- Key components:
  - `iq-apis`: Quarkus REST/LLM runtime entrypoint (LLM endpoints in `src/main/java/systems/symbol/`).
  - `iq-platform`: core business logic (LLM provider wrappers, RDF/SPARQL orchestration, `LLMFactory`, `GPTWrapper`).
  - `iq-rdf4j`, `iq-rdf4-graphs`, `iq-trusted`: RDF repository + model drivers and graph transforms.
  - `iq-connect/*`: connector library family (AWS, Azure, Slack, JDBC, etc.); each submodule has own `src/main/java` and `README`.
- Data flow: `.ttl`/`.sparql` scripts drive domain behavior; `IQScriptCatalog` / `ModelScriptCatalog` loads them.

## Critical workflows (must-copy commands)
- Full build: `./mvnw clean install` from repo root (wrapper preferred). Optionally `mvn -DskipTests=true -DskipITs=true clean install` for faster local edits.
- Module build: `./mvnw -pl iq-apis -am compile` or `-pl iq-connect/iq-connect-aws -am test`.
- Dev run: `./mvnw compile quarkus:dev -pl iq-apis -am`; dev UI at `http://localhost:8080/q/dev/`.
- CLI run: `./bin/iq` (uses built artifacts + runtime config in `.iq/`).
- Tests:
  - unit: `mvn test` (JUnit5)
  - integration: `mvn -DskipITs=false verify -pl <module>`
  - skip IT: `-DskipITs=true` (default in CI/workspace)
- Container image: `./bin/build-image` or `./mvnw -Dquarkus.container-image.build=true -DskipTests install`.

## Project-specific conventions
- Always prefer `-pl <module> -am` for incremental compile/test.
- RDF-first logic: new features usually add `.ttl` / `.sparql` plus Java hook in `iq-platform`/`iq-trusted`.
- LLM configuration: text prompts, instructions and provider maps are in `.iq/` and `iq-apis/docs/API_LLM.md`.
- No hardcoded secrets: use env vars and `.iq/vault` with `VFSPasswordVault`/`EnvsAsSecrets`.
- Module tests mostly isolated; integration tests access real endpoints and are controlled by `skipITs` flag.

## Integration & external points
- LLM providers: openai/groq/custom via `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java`.
- RDF store: in-memory for tests, persistent config via `.iq/repositories`; drivers in `iq-rdf4j`.
- Connectors: `iq-connect/*` modules provide pluggable service adapters (AWS, Slack, Kafka, etc.).
- CI workflow: `.github/workflows/jars.yaml`, `docker.yaml`, with JDK 21 + git-lfs; “release” tasks in `bin/release`.

## Debug & troubleshooting quick hits
- Quarkus logs are most informative; run `./mvnw -pl iq-apis -am quarkus:dev` and inspect startup logs for LLM map resolution.
- For failing RDF/SPARQL, examine `src/main/resources/**/*.sparql`, and check `IQScriptCatalog` in `iq-platform`.
- If modules compile but tests fail, clear caches: `./mvnw -pl <mod> -am clean test`.

## PR/agent behavior
- Keep changes small and focused; run `mvn -DskipTests=false -DskipITs=true package` locally.
- Document `.ttl/.sparql` behavior and update related script catalogs when adding model rules.
- For LLM integration changes, mock provider behavior in `iq-platform` tests (avoid live API keys in unit tests).
- Do not commit production secrets to `.iq/vault`; change is valid only with sanitized sample data.
- For working/temporary files use `./tmp/` not `/tmp/`
- When fixing, don't remove/stub/placeholder functionality - fix it properly to be production-ready
