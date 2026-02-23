# Copilot instructions for IQ (systems.symbol)

## Quick orientation
- Big picture: IQ is a multi-module Java (Maven) project that turns RDF graphs into an executable knowledge/playbook. Key runtime is Quarkus (`iq-run-apis`) and core libraries live in `iq-platform`, `iq-rdf4j` and `iq-trusted`.
- Language & platforms: Java 21, Quarkus 3.x, RDF4J for RDF/SPARQL, JUnit5 for tests.

## Where to start (files to read)
- Top-level: `README.md` and `BUILD.md` (build commands, Java version, Quarkus tips).
- Runtime & APIs: `iq-run-apis/README.md` + `iq-run-apis/docs/API_LLM.md` (LLM endpoints and examples).
- LLM integration: `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` and tests under `iq-platform/src/test/java/systems/symbol/llm/gpt/`.
- RDF/SPARQL patterns: look in `iq-*/src/main/resources/**.ttl` and `.sparql` files and classes under `systems.symbol.rdf4j` or `iq-rdf4j`.
- Secrets & local store: `.iq/` (example vault, repositories). See `EnvsAsSecrets` / `VFSPasswordVault` uses in `iq-run-apis` / `iq-trusted`.

## Common developer workflows (commands)
- Full build: `./mvnw clean install` (or `mvn clean install`). Use the wrapper when available.
- Dev mode (live coding): `./mvnw compile quarkus:dev -pl iq-run-apis -am` (also `./bin/iq`). Dev UI is at `http://localhost:8080/q/dev/`.
- Compile only (fast): `./bin/compile-apis` or `mvn compile -pl iq-run-apis -am`.
- Build container image: `./bin/build-image` or `./mvnw -Dquarkus.container-image.build=true -DskipTests install`.
- Run unit tests: `mvn test` (JUnit5). Integration tests are skipped by default with property `-DskipITs=true`.
- Run integration tests: `mvn -DskipITs=false verify -pl <module>` (set the property or unset it in CI when needed).

## Conventions & patterns to follow
- Multi-module builds: prefer `-pl <module> -am` to compile only affected modules.
- RDF-first design: domain logic is expressed as RDF/TTL + SPARQL. When adding features, check for corresponding `.ttl` or `.sparql` artifacts and update `IQScriptCatalog`/`ModelScriptCatalog` where appropriate.
- LLM configs are named maps (providers → named maps); changes to LLM behaviour usually touch `LLMFactory`, `GPTWrapper`, and mapped prompts in `.iq` or `/docs`.
- Secrets: runtime reads secrets from environment variables and `.iq/vault` (VFSPasswordVault). Never commit real credentials — use env vars or local `.iq` copies for dev.
- Tests that require real LLM keys or external services are often commented or in integration tests — prefer stubbing or mocking in unit tests.

## Integration points & external dependencies
- LLM providers (OpenAI / Groq / custom) — see `iq-run-apis/docs/API_LLM.md` and `LLMFactory`.
- RDF storage: RDF4J repositories under `.iq/repositories` or in-memory stores used in tests.
- CI: GitHub Actions workflows (`.github/workflows/jars.yaml`, `docker.yaml`) use JDK 21 and `git lfs` (model files). Follow the workflow conventions (build with `-DskipITs` in CI).

## Testing and debugging tips
- Use Quarkus dev mode and the Dev UI to call endpoints quickly during development.
- Logs: Quarkus logs go to console; many subsystems (LLMFactory, VFSPasswordVault) log helpful messages on startup showing secrets/LLM map resolution — scan those messages when debugging provider configuration.
- Unit vs integration: keep expensive LLM or external calls out of unit tests; create small integration test suites that run only when `-DskipITs=false`.

## PR/Agent behaviour guidance (when you're an automated code author)
- Make small, focused changes and run `mvn -DskipTests=false -DskipITs=true package` locally before opening a PR.
- If code touches LLM flows, add tests that mock the provider; if adding integration tests, mark them so CI runs them separately and document required env secrets in the PR description (don’t include secrets).
- When modifying RDF models or scripts (`.ttl`, `.sparql`, `.groovy`), include a short README or comment explaining the intent and verify expected SPARQL queries still work.
- Avoid editing `.iq/vault` checked-in examples unless you are adding sanitized sample data; do not add credentials.

## Where to add more documentation
- Small changes: update the module `README.md` (e.g., `iq-run-apis/README.md`, `iq-platform/README.md`).
- API changes: add or update `iq-run-apis/docs/` (for LLM APIs, endpoints and examples).

If anything here is unclear or you'd like me to expand a section (e.g., a specific module's architecture, test knobs, or example PR checklist), say which area and I'll iterate. ✅
