# ADR 0006: Store TTL and SPARQL as Classpath Resources

**Status:** Accepted

**Context:** Domain scripts and queries are reused across services; embedding them in Java source makes reuse hard.

**Decision:** Place `.ttl` and `.sparql` files under `src/main/resources` and load them by classpath path (via `ResourceUtils` or ClassLoader).

**Consequences:**
- Makes query updates independent of Java compilation, enabling hot reload in dev mode.
- Requires conventions for naming and versioning resource paths.
