# SEMANTICS — IQ

IQ values **explicitness** (structured RDF, named prompts, catalogs) and **operability** (small modular services, clear secrets handling). 

When in doubt, we prefer an explicit TTL + SPARQL artifact and a short unit test that documents expected behaviour.

If you want, I can add a short example demonstrating the recommended `TTL + SPARQL + test + catalog` pattern and a PR checklist entry that enforces it.

## Core ideas

- RDF-first: domain logic, policies and playbooks are represented as RDF/Turtle (`*.ttl`) and executed/queried through SPARQL (`*.sparql`).
- fact graph: a declarative knowledge graph that stores facts, processes, intents and agent state.
- Namespaces: key prefixes used across the repo include `iq:` (code), `ai:` (AI axioms), and `my:` (per-agent namespaces).
- Trust zones: artifacts and knowledge are classified into five zones—Code, AI, Agents, Curated, Community—affecting provenance and governance.

Patterns to follow
- Use SPARQL to `CONSTRUCT`, `SELECT`, `INSERT` and `DELETE` to manipulate knowledge. Example SPARQL files: `.iq/lake/` and many `.sparql` resources under modules.
- Keep behavioral logic as scripts and models (look for `.groovy`, `.sparql`, and `.ttl` under `iq-run-apis/.iq` and `iq-*/src/main/resources`).
- Use `IQScriptCatalog` and `ModelScriptCatalog` to register and manage scripts and model queries — changes here often accompany feature work.

Secrets & repositories
- Secrets are provided by `EnvsAsSecrets`, and Vault storage uses VFSPasswordVault (`.iq/vault` in repo is example data—do not commit real keys).
- RDF repositories are visible under `.iq/repositories` for local dev and testing; tests often use in-memory stores.

Files worth reading
- `iq-run-apis/.iq/*` (example lake, prompts, repos)
- `iq-platform/src/main/java/systems/symbol/rdf4j/**` and `iq-rdf4j` module
- `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` (LLM configuration semantics)

Tip: When adding new domain behaviour, add or update a `.ttl` + `.sparql` pair and register it in the relevant catalog so tests and runtime discover it.
