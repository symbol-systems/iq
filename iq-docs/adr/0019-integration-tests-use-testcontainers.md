# ADR 0019: Use Testcontainers for Integration Tests# ADR 0019: Use Testcontainers for Integration Tests











- Tests run slower; keep the suite focused and optional for local runs.- CI becomes consistent across runners; no external dependencies required.**Consequences:****Decision:** Use Testcontainers to spin up required services during integration test execution (`-DskipITs=false`).**Context:** Integration tests require services like RDF stores or databases; relying on system-local instances makes CI flaky.**Status:** Accepted
**Status:** Accepted

**Context:** Integration tests require services (e.g., RDF store, databases) that are hard to reproduce consistently across environments.

**Decision:** Use Testcontainers to spin up ephemeral service containers in CI for integration tests, while keeping unit tests lightweight.

**Consequences:**
- Provides consistent integration environments and avoids shared test infrastructure.
- Adds dependency on Docker being available in CI agents.
