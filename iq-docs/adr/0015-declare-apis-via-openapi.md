# ADR 0015: Declare Public APIs via OpenAPI Specs

**Status:** Accepted

**Context:** Multiple clients (UI, external apps) consume HTTP endpoints; implicit API contracts lead to drift.

**Decision:** Expose REST APIs via OpenAPI annotations and publish the generated OpenAPI spec as the canonical contract.

**Consequences:**
- Clients can auto-generate code; breaking changes become visible.
- Requires discipline to keep annotations in sync with behavior.
