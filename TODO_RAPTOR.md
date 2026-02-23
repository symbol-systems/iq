# TODO — Actionable tasks for Camel + RDF4J + GraphQL 🔧

## Medium (P1)
4. Improve Content-Type and RDF serialization negotiation 🌐
   - `RDF4JProcessor.processConstruct()` and `GraphQueryProcessor` should pick RDF format based on Accept header (JSON-LD, Turtle, RDF/XML) and set `Content-Type` accordingly.
   - Add tests for serialization to JSON-LD and Turtle.

5. Add GraphQL ACL enforcement middleware ⚖️
   - Implement a `AuthorizationDataFetcher` wrapper that runs an ASK or SPARQL check against ACL triples before delegating to `SPARQLDataFetcher`.
   - Tie checks into GraphQL wiring so specific queries/fields are protected (use `@acl(role:...)` directive if present in schema).

6. Add query/argument validation and guard against SPARQL injection
   - Ensure all GraphQL arguments are bound via prepared query bindings (you have helpers in `SPARQLMapper.setBindings`) and validate expected types (IRIs vs ***REMOVED***s).

7. Add pagination & sorting to GraphQL->SPARQL
   - Support `first`, `offset`, `orderBy` arguments, translating them into `LIMIT`/`OFFSET`/`ORDER BY` clauses.

---

## Low (P2)
8. Add instrumentation & timeouts
   - Expose `max-execution-time` at GraphQL layer and Camel endpoint level and enforce it.
   - Collect simple metrics (counts, timing) for expensive queries.

9. Improve `IRIDirective` behavior
   - Validate directive presence and attach resolved IRI as metadata to the field/object to avoid repeated directive parsing at fetch time.

10. Update dependencies & document compatibility
- Review `camel.version` in `iq-trusted/pom.xml` and update to a stable release or add compatibility notes.

11. Tests & Documentation
- Add integration tests to `iq-camel` and `iq-rdf4j-graphql` that run in-memory RDF4J Repositories, start a minimal Camel context and execute GraphQL queries.
- Update `iq-camel/README.md` and `iq-rdf4j-graphql/README.md` with a quickstart (example route, example GraphQL query, expected result).

---

