# what we model and what we can execute ✨

## what an ontology can represent

- **Entities (things):** People, Organizations, Devices, Products, Documents, Models, Agents, Workflows, Sensors, Events.
- **Types & Class hierarchies:** Domain classes, sub-classes, and domain taxonomies (use SKOS / RDFS / OWL).
- **Relationships:** directed or typed relations between entities (ownership, membership, part-of, uses, dependsOn).
- **Attributes / Literals:** names, timestamps, numeric metrics, URIs that link to resources.
- **States & Processes:** finite states, transitions, lifecycle stages — modeled as Workflow / State resources.
- **Actions & Behaviors:** scriptable actions, tasks, operations that can be triggered (scripts, HTTP calls, LLM prompts).
- **Constraints & Rules:** validation constraints, SHACL shapes, rulesets (SHACL rules, SPARQL rules / updates, custom rule engines).
- **Provenance & Audit:** who/when/how (PROV-O), signatures, evidence, versioning and lineage.
- **Policies & Permissions:** roles, ACLs, access rules and obligations (expressed as triples and ASK checks).
  - Per-type policy templates: use properties like `trust:forType`, `trust:askTemplate` and `trust:defaultAllow` in `.iq` to define per-type ASK-based policies (GQL loads these at wiring time).
- **Temporal, Spatial & Units:** time intervals, geolocation (GeoSPARQL), and typed measures (XSD / QUDT).

---

## which modeled things can be executed and how

- **Workflows / State machines**
  - Model: `iq:Workflow`, `iq:State`, `iq:transition` (e.g., `:s1 iq:to :s2`).
  - Execute: state transitions invoked by events or handlers (Camel routes, webhooks, or scheduled jobs).

- **Scripts & Actions**
  - Model: `iq:Script` with metadata (language, entry-point, author, version, runtime hints).
  - Execute: run via script engines (Groovy, JS, or containerized runtimes) and integrate results back into the RDF store.

- **Queries & Data Transformations**
  - Model: Named SPARQL queries (`iq:sparql` resources) and mappings (`SPARQLMapper`).
  - Execute: SPARQL SELECT/CONSTRUCT/ASK/UPDATE executed via RDF4J; results feed workflows or GraphQL resolvers.

- **Rules & Validation**
  - Model: SHACL shapes or rules as resources; SPARQL rule queries for derived triples.
  - Execute: SHACL validation for schema conformance; SHACL rules / SPARQL UPDATE to materialize inferred facts.

- **Provenance & Auditing Actions**
  - Model: PROV-O activities, agents, generated entities.
  - Execute: record activity traces automatically when scripts or workflows run (emit provenance triples).

- **Natural Language Components & LLM Prompts**
  - Model: Prompt templates and LLM bindings (`ai:` vocabulary). Prompts can be parametric RDF resources.
  - Execute: Template + bindings → call LLM provider → store responses as resources with provenance.

- **GraphQL & API Mappings**
  - Model: GraphQL schema with `@rdf(iri:...)` directives (see `schema.graphqls`).
  - Execute: GraphQL queries map to SPARQL via `SPARQLDataFetcher` and return typed results; can be wrapped with ACL checks.

- **Event-driven triggers**
  - Model: Event types and listeners as resources (e.g., `iq:EventType`, `iq:Listener`).
  - Execute: Use Camel routes to listen for events and invoke SPARQL, scripts, or external services.

---

## Practical & impactful use-cases (concrete examples)

1. **Auditable Compliance & Reporting** ✅
   - Model: policies, obligations, and reporting templates using PROV-O and FIBO-like vocabularies.
   - Execute: Periodic SPARQL queries + SHACL validation produce audit artifacts for regulators.

2. **Supply Chain Provenance** 🌐
   - Model: product lifecycle, batch identifiers, locations, custodians and shipping events.
   - Execute: Trigger provenance capture when physical events arrive (IoT), run queries to detect anomalies and produce recall lists.

3. **Knowledge-driven Automation (Agentic Workflows)** 🤖
   - Model: agents, goals, tasks, workflows and tool bindings (LLM prompts / APIs).
   - Execute: Agents claim tasks, run LLM-assisted steps, record decisions and derived knowledge into the fact graph.

4. **Policy-based Access Control** 🔐
   - Model: ACL triples, roles, resources, permitted actions.
   - Execute: For each GraphQL/SPARQL request, run ASK queries to enforce access; deny or transform responses accordingly.

5. **Research Data Integration & Reproducibility** 🔬
   - Model: datasets, experiments, methods, provenance.
   - Execute: Compose datasets via SPARQL CONSTRUCT and validate with SHACL to produce reproducible bundles.

6. **Digital Twins & Operational Insights** 🏭
   - Model: digital representation of assets, telemetry schemas, thresholds and maintenance windows.
   - Execute: Stream telemetry, infer states (rules), schedule maintenance workflows when thresholds are crossed.

---

## Modeling patterns & recommendations

- Use established vocabularies where possible: **PROV-O** for provenance, **SKOS** for taxonomies, **schema.org** for common entities, **SHACL** for validation.
- Keep executable metadata close to the resource: e.g., a `iq:Script` should carry runtime hints (language, timeout, permissions).
- Prefer IRIs and URNs for identity (use `urn:` for transient or generated contexts in workflows).
- Separate **declarative facts** (data) from **executable artifacts** (scripts, queries, prompts) using distinct classes and properties.
- Model ACLs and obligations explicitly and tie them to GraphQL/REST/SPARQL entry points.

---

## Short examples

Workflow model (Turtle snippet):

```turtle
@prefix : <http://example.org/example#> .
@prefix iq: <http://example.org/iq#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

:invoice-workflow a iq:Workflow ;
iq:initial :draft ;
iq:state :draft , :approved , :paid .

:draft a iq:State ;
iq:to :approved .

:approved a iq:State ;
iq:to :paid .
```

Executable script (Turtle metadata):

```turtle
:send-invoice a iq:Script ;
iq:language "groovy" ;
iq:entryPoint "send.groovy" ;
iq:timeout "PT30S"^^xsd:duration .
```

GraphQL wiring: attach `@rdf(iri: "http://example.org/ontology#Invoice")` to a GraphQL type and use `SPARQLDataFetcher` to resolve.

---

## Operational tips

- Use **try-with-resources** for repository connections and enforce timeouts on expensive queries.
- Add **integration tests** that run in-memory RDF4J repositories and cover the executable artifacts.
- Capture **provenance** for every automated action so outputs are auditable and reproducible.
- Add **limits & quotas** for LLM calls and expensive SPARQL queries to avoid runaway costs.

---

## Suggested next steps (repo improvements)

- Define a small `iq:Action`/`iq:Script` SHACL shape and include CI checks to validate all executable artifacts.
- Add an `acl` enforcement example in `iq-camel`/GraphQL middleware.
- Provide a cookbook page with 3 end-to-end examples: (1) approval workflow with script action, (2) supply-chain provenance capture, (3) GraphQL + ACL query.

---

## References

- PROV-O (Provenance Ontology): https://www.w3.org/TR/prov-o/
- SKOS: https://www.w3.org/TR/skos-reference/
- SHACL: https://www.w3.org/TR/shacl/

---

(If you'd like, I can open a PR that adds the `iq:Script` SHACL shape and a small example folder for one of the suggested use cases.)
