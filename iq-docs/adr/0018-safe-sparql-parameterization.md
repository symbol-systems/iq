# ADR 0018: Parameterize SPARQL Using Predefined Variables

**Status:** Accepted

**Context:** Building SPARQL queries via string concatenation is error-prone and opens injection risks.

**Decision:** Use SPARQL query templates with predefined variable placeholders (e.g., `?id`) and bind values via repository query APIs rather than concatenating literals.

**Consequences:**
- Queries are safer and easier to cache/locate in logs.
- Requires discipline to keep templates separate from runtime values.
