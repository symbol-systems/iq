# ADR 0001: Use Quarkus Dev Mode for Local Iteration

**Status:** Accepted

**Context:** Fast feedback loops are critical when working with RDF/SPARQL schemas and LLM prompts.

**Decision:** Use `mvnw -pl iq-apis -am quarkus:dev` as the default local dev workflow.

**Consequences:**
- Hot-reload supports changes to Java, resources, and most config without restart.
- Avoids custom watch scripts or slow full rebuilds.
