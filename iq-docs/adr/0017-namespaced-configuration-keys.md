# ADR 0017: Namespaced Configuration Key Conventions

**Status:** Accepted

**Context:** Many modules and connectors share config names (e.g., `url`, `timeout`), leading to accidental collisions.

**Decision:** Use module-prefixed keys (e.g., `llm.openai.apiKey`, `rdf.store.url`) and enforce via documentation and examples.

**Consequences:**
- Reduces accidental misconfiguration when multiple modules are active.
- Requires discipline when adding new config options to follow the naming scheme.
