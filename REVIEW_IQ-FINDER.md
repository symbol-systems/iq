# REVIEW_IQ-FINDER.md — Review & Migration Plan for `iq-finder`

**Status**: Module exists at `iq-finder/` but is currently *disabled* in the parent `pom.xml`. The module shows dependencies on LangChain4j and embedding libraries but contains minimal code and empty `ingest` packages. Restoration is feasible; the module appears to be early-stage or in-progress (indexing/search feature set).

---

## Quick Findings (2 sentences)
- `iq-finder` depends on `langchain4j` and embedding packages (useful for vector search/semantic retrieval), but implementation folders are empty or minimal. This suggests the project was started, partially scaffolded, and then disabled pending architecture decisions or dependency upgrades.

---

## Why it may have been disabled
- Incomplete implementation: core ingestion/indexing code (`ingest/`) is empty.
- Dependency drift: LangChain and embedding packages evolve quickly and may have caused integration or licensing concerns.
- Unclear product fit: team may have deferred semantic search until embedding strategy solidified.

---

## Impact if left disabled
- No in-repo semantic search implementation (limits local retrieval/semantic search features).  
- Potential duplication risk if external projects implement the same feature differently.

---

## Remediation Options (Mutually Exclusive)
1. **Restore & Implement** (Recommended if semantic search is in scope): finish implementation (ingest → embeddings → index → query) and add tests.  
2. **Extract to a Separate Service**: move to a standalone repo with clear API to decouple lifecycle.  
3. **Archive**: if the team chooses another approach, archive the module and document replacements.

---

## Recommended Action: Restore & Implement — Step-by-step
1. Repro: `mvn -pl iq-finder -am clean install` and capture build/test failures.  
2. Define scope: choose a vector store (Milvus, FAISS, or hosted) and pick LangChain bindings (langchain4j modules needed).  
3. Implement a small end-to-end PoC: ingest a sample TTL, compute embeddings, store in index, run a query.  
4. Add tests: unit tests for embedding conversion and an integration test that validates at least one query result.  
5. Add configuration docs (e.g., env vars for embedding provider and vector store).  
6. Add CI job and restore module in parent POM.  
7. Add an example to `.iq/` that demonstrates search/ingest flow.

---

## Acceptance Criteria
- `mvn -pl iq-finder -am clean install` succeeds and CI includes the module.  
- PoC test proves embeddings + search end-to-end with an in-memory or dev vector store.  
- README documents how to run the finder and configure embedding/vector providers.

---

## Timeline & Effort Estimate
- Investigation & repro: 1–2 hours.  
- Small PoC & tests: 6–10 hours.  
- Docs + CI + cleanup: 2–4 hours.  
- Total: 9–16 hours.

---

## Notes
- If a separate managed vector service is chosen, document required credentials and provisioning steps in `DEPLOYMENT.md`.
- Consider adding feature flag or Quarkus profile so finder can be optional at runtime.

---

## Quick Checklist (PR Template)
- [ ] Run `mvn -pl iq-finder -am clean install` locally.  
- [ ] Add PoC/integration test for embeddings + search.  
- [ ] Document configuration and usage in README.  
- [ ] Add module to CI and remove comment in the parent POM, or move to separate repo if chosen.

