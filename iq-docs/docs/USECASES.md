# USE CASES — practical examples for IQ

This file lists concrete, discoverable use cases implemented or demonstrated in the repo.

1) LLM-backed APIs (RAG & prompt maps) ✅
- Files: `iq-apis/docs/API_LLM.md`, `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java`.
- What: Expose LLM endpoints that map named prompts, perform RAG from RDF and call provider APIs (OpenAI/Groq/etc.).
- How to test: use `./bin/curl_api` or the Quarkus Dev UI (http://localhost:8080/q/dev/).

2) Agent playbooks & FSMs ✅
- Files: state machine tests in `iq-platform/src/test/java/systems/symbol/fsm`, `iq-platform/src/main/java/systems/symbol/fsm`.
- What: Agents use RDF playbooks, FSMs and scripts to make decisions and trigger actions.

3) Ingest & Lake (knowledge acquisition) ✅
- Files: `iq-platform/src/main/java/systems/symbol/lake/*` and ingest tests.
- What: Ingest HTML/RDF/Tika/text and convert into RDF for the fact graph (used by RAG and searches).

4) Trusted identities & vaults ✅
- Files: `iq-trusted` module, `.iq/vault` examples, `EnvsAsSecrets` and `VFSPasswordVault` usage.
- What: Manage keys, JWTs and repository bootstrapping for multi-realm deployment.

5) Domain demos (tropes, persona, tokenomics) ✅
- `iq-persona` shows persona/UX integration.
- `iq-tokenomic` outlines token usage patterns.

6) Tests & CI patterns ✅
- Unit tests (JUnit5) under each module; integration tests are gated by `-DskipITs` and CI workflows use `-DskipITs`.
- CI workflows in `.github/workflows/` require `git lfs` for large model files when building.

How to add a new use case
- Add RDF (`.ttl`) + SPARQL (`.sparql`) assets under `.iq` or the appropriate module.
- Register any script/prompts in the relevant `IQScriptCatalog`/`ModelScriptCatalog`.
- Add unit tests that mock external LLM calls; add integration tests guarded with `-DskipITs=false` if they need real services.

