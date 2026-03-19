# ADR 0008: Prefer Immutable Domain Objects

**Status:** Accepted

**Context:** Concurrent access to domain objects and caching can lead to subtle state bugs.

**Decision:** Model domain value objects as immutable (final fields, no setters) and use builders for construction when needed.

**Consequences:**
- Reduces accidental shared mutable state; simplifies reasoning.
- May require more boilerplate or helper factories for complex objects.
