# ADR 0010: Unit Tests Use In-Memory RDF4J Repository

**Status:** Accepted

**Context:** Running tests against persistent RDF stores is slow and brittle.

**Decision:** Use `SailRepository` with `MemoryStore` in unit tests; reserve external stores for integration tests.

**Consequences:**
- Fast, deterministic tests; no external dependencies required.
- Integration tests must still validate against realistic storage backend (e.g., native store).
