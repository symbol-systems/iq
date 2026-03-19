# ADR 0002: RDF-First Domain Modeling

**Status:** Accepted

**Context:** Domain concepts are used by multiple subsystems (API, reasoning, UI). Maintaining separate model layers causes drift.

**Decision:** Treat RDF/Turtle as the canonical domain model and generate runtime objects from it instead of hand-coding POJOs first.

**Consequences:**
- Changes in domain are done in `.ttl` and automatically reflected in SPARQL and runtime behavior.
- Requires tooling to validate and load RDF at build/runtime.
