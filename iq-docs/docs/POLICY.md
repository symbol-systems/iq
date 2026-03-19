# Policy templates & per-type ACLs


Example policy (Turtle)

```ttl
@prefix trust: <http://symbol.systems/v0/onto/trust#> .

:PolicyTypeX
    a trust:Policy ;
    trust:forType <http://example.org/TypeX> ;
    trust:askTemplate "ASK WHERE { <{actor}> <http://example.org/altCanQuery> <{type}> }" ;
    trust:defaultAllow false .
```

Placeholders
- `{actor}`: the actor IRI (from `actor` argument or execution context `ctx.actor`).
- `{type}`: the type IRI being queried.
- `{arg.<name>}`: GraphQL argument values available to the resolver (e.g., `{arg.role}`).
- `{ctx.<name>}`: context map values (e.g., JWT claims placed in the GraphQL context).

Advanced
- You can add fallback policies by registering additional `PolicyEngine` instances programmatically (see `PolicyEngineChain`), or by adding multiple policy triples — the engine will pick the first matching policy.
- For complex auth, consider a policy that calls out to an external authorization service by materialising an allow triple and letting ASK use it.

Testing
- Add unit tests using an in-memory RDF4J `SailRepository` and include both the policy and the triples the policy references.

If you'd like, I can add a CLI/import example (`bin/` script) that imports a `policies.ttl` into `.iq` repositories for quick local testing.