# ADR 0007: Use Structured Logging (SLF4J) over System.out

**Status:** Accepted

**Context:** The runtime runs in containers and logs are ingested by ELK/Log Analytics; raw stdout is hard to parse.

**Decision:** Use SLF4J (Logback/Quarkus logging) with structured key/value fields instead of `System.out/err`.

**Consequences:**
- Enables log-level control and correlation across services.
- Requires devs to use logger instances and avoid ad-hoc printing.
