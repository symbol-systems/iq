# ADR 0009: Use Quarkus REST Client for External APIs

**Status:** Accepted

**Context:** Manual HTTP clients lead to duplicated code and inconsistent timeouts/retries.

**Decision:** Use Quarkus REST Client (`@RegisterRestClient`) for calls to external services (LLM providers, connectors).

**Consequences:**
- Centralizes config (timeouts, base URLs, auth) and enables CDI injection.
- Adds a compile-time dependency on Quarkus REST Client and requires interface-based client definitions.
