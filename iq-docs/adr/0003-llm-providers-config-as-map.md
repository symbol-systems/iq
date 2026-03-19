# ADR 0003: LLM Providers Configured as Named Maps

**Status:** Accepted

**Context:** Multiple LLM providers are supported; hardcoding one provider per build makes switching difficult.

**Decision:** Define LLM providers as a map in configuration (`application.properties` or `.yaml`), keyed by provider name, and resolve at runtime by name.

**Consequences:**
- Enables runtime switching and multi-provider support without code changes.
- Requires clear naming and validation of provider configurations.
