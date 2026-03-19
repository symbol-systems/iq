---
id: mcp:arch/iq
type: mcp:ArchitectureDocument
version: "1.0"
realm: iq
status: living
updated: "2026-03-19"
audience: [human, llm, ci]
relates-to: MCP_TODO.md, MCP_CONNECT.md, SPEC.md
---

# IQ Architecture — IQ Capability Map

> **What this document is.**  
> A full-stack capability map: every IQ domain (SPARQL, ontologies, agents, queries, models, graphs, utilities) is located, described, and wired to its IQ tool surface. Read this before implementing any adapter — it's the single source of truth for how the pieces fit together.

---

## How to use this document

**As a human** — start at the layer diagram, then drill into any capability section.  
**As an LLM** — paste the capability section relevant to the tool you're implementing; each section includes the canonical class list, data model, and tool mapping.  
**As CI** — the "Decision log" section records key architectural choices for traceability.

---

## Layer diagram

```
 ┌──────────────────────────────────────────────────────────────────────┐
 │  IQ Clients  (Claude / LLM agents / CLI / external services)│
 └────────────────────────┬─────────────────────────────────────────────┘
  │  IQ protocol  (SSE / HTTP / STDIO)
 ┌────────────────────────▼─────────────────────────────────────────────┐
 │  L1 — Transport   │
 │  MCPServer  (iq-mcp /mcp)   ←  IQ Java SDK 0.17.2 │
 └────────────────────────┬─────────────────────────────────────────────┘
  │
 ┌────────────────────────▼─────────────────────────────────────────────┐
 │  L2 — Connect / Gateway  (MCP_CONNECT.md)│
 │  MCPPipeline  →  AuthGuard → ACLFilter → QuotaGuard   │
 │   → CacheInterceptor → Transformers  │
 │   → ProxyMiddleware → CircuitBreaker │
 │   → AuditWriter → MetricsEmitter │
 └────────────────────────┬─────────────────────────────────────────────┘
  │
 ┌────────────────────────▼─────────────────────────────────────────────┐
 │  L3 — Adapter registry   │
 │  I_MCPService  →  Map<toolName, I_MCPAdapter>│
 │  MCPAdapterBase  (manifest lookup, SHACL validation, policy ASK, │
 │   quota check, doExecute, audit write)   │
 └──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬────────────┘
│  │  │  │  │  │  │  │
   [S2]  [S1]  [S4]  [S5]  [S6]  [S7]  [S8]  [S9]  [S12]
│  │  │  │  │  │  │  │
 ┌──────▼──────▼──────▼──────▼──────▼──────▼──────▼──────▼────────────┐
 │  L4 — Capability domains  (this document)│
 │  │
 │  SPARQL  │  Ontology  │  Agents  │  LLM  │  Graphs  │  Utils│
 └──────────────────────────────────────────────────────────────────────┘
  │
 ┌────────────────────────▼─────────────────────────────────────────────┐
 │  L5 — Storage│
 │  RDF4J repositories (per-realm, in-process or remote)   │
 │  Named graphs: mcp:audit  mcp:policy  mcp:quota  mcp:cache  │
 │   mcp:pipeline  mcp:trace  iq:facts  iq:schema   │
 └──────────────────────────────────────────────────────────────────────┘
```

---

## Capability domains

### 1. SPARQL

**What it is**: the primary query-and-update language over RDF4J repositories. IQ uses SPARQL for data access, governance checks, catalog lookup, and tool execution.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `SPARQLer` | `systems.symbol.rdf4j.sparql` | Execute SELECT / ASK / CONSTRUCT against any `RepositoryConnection` |
| `IQScriptCatalog` | `systems.symbol.rdf4j.sparql` | Registry of named SPARQL scripts loaded from RDF resources |
| `IQScripts` | `systems.symbol.rdf4j.sparql` | Builder / runner for in-graph SPARQL snippets via `rdf:value` |
| `IQSimpleCatalog` | `systems.symbol.rdf4j.sparql` | Lightweight catalog for test and embedded use |
| `ModelScriptCatalog` | `systems.symbol.rdf4j.sparql` | Script catalog backed by a live `Model` (in-memory) |
| `SPARQLMapper` | `systems.symbol.rdf4j.sparql` | Maps `BindingSet` rows to Java objects / RDF models |
| `UsefulSPARQL` | `systems.symbol.rdf4j.util` | Utility SPARQL query fragments (prefixes, common patterns) |

**SPARQL-as-config pattern**: every governance check (policy, quota, trust zone) is a stored SPARQL ASK or UPDATE executed at runtime against a named graph. Scripts are loaded from `.ttl` resources via `IQScriptCatalog`, keyed by `iq:scriptId`.

**IQ tool surface:**

| Tool | SPARQL op | Description |
|------|-----------|-------------|
| `fact.sparql.query` | SELECT / ASK / CONSTRUCT | Ad-hoc query over realm facts |
| `fact.sparql.update` | INSERT / DELETE / LOAD | Guarded write into realm graph |
| `fact.explain` | EXPLAIN (via extensions) | Query plan and cost estimate |
| `fact.walk` | CONSTRUCT recursive | Graph traversal from a seed IRI |
| `fact.describe` | DESCRIBE | Full description of a resource |

**Named graphs used:**

| Graph IRI | Contents |
|-----------|----------|
| `iq:facts` | Domain triples for the realm |
| `iq:schema` | SHACL shapes, OWL axioms, RDFS definitions |
| `mcp:policy` | Role × tool allow-list, trust zone triples |
| `mcp:quota` | Per-principal sliding-window counters |
| `mcp:audit` | `mcp:Event` quads (write-once append) |
| `mcp:pipeline` | Middleware config + circuit-breaker state |
| `mcp:cache` | Cached `I_MCPResult` payloads with TTL |

**SPARQL version**: 1.1 (RDF4J 5.0.2) — including federated `SERVICE`, property paths, named graphs via `GRAPH`, `VALUES`, `BIND`, custom functions (see Utilities section).

---

### 2. Ontologies

**What it is**: IQ is ontology-led — every domain concept has an RDF type, SHACL shape, and IRI. Ontologies drive validation, reasoning, and tool manifests.

**Namespaces in use:**

| Prefix | IRI | Purpose |
|--------|-----|---------|
| `iq:` | `https://symbol.systems/iq/` | Core IQ vocabulary |
| `mcp:` | `urn:mcp:` | IQ governance vocabulary |
| `connect:` | `urn:mcp:pipeline:` | Gateway/middleware vocabulary |
| `owl:` | `http://www.w3.org/2002/07/owl#` | OWL axioms |
| `rdfs:` | via `NS.java` | Labels, comments, subClass |
| `sh:` | `http://www.w3.org/ns/shacl#` | SHACL validation shapes |
| `prov:` | `http://www.w3.org/ns/prov#` | Provenance triples in audit |
| `skos:` | via realm graphs | Concept hierarchies |

**Key vocabulary terms:**

| Term | Type | Meaning |
|------|------|---------|
| `iq:Realm` | `owl:Class` | A bounded domain / tenant |
| `iq:Agent` | `owl:Class` | A stateful, executable actor with a workflow |
| `iq:Workflow` | `owl:Class` | A finite-state machine declaration |
| `iq:to` | `owl:ObjectProperty` | Workflow edge: current state → next state |
| `iq:trusts` | `owl:ObjectProperty` | Trust assertion: issuer iq:trusts realm |
| `iq:hasRole` | `owl:ObjectProperty` | Principal → role assignment |
| `iq:scriptId` | `owl:DatatypeProperty` | Key for SPARQL script in IQScriptCatalog |
| `mcp:ToolManifest` | `owl:Class` | Tool declaration (name, shapes, auth query) |
| `mcp:Event` | `owl:Class` | Audit event |
| `mcp:allows` | `owl:ObjectProperty` | Role → tool permission |
| `mcp:tool` | `owl:DatatypeProperty` | Tool name ***REMOVED*** on an event |
| `sh:NodeShape` | SHACL | Input/output contract per tool |
| `sh:property` | SHACL | Per-property constraint |

**Ontology files** (`.ttl` resources in `src/main/resources/`):

| File | Content |
|------|---------|
| `mcp-vocab.ttl` | Core `mcp:` ontology (classes, properties, shapes) |
| `mcp-connect.ttl` | Middleware instances and `connect:` config |
| `mcp-policy.ttl` | Seed role × tool ACL triples |
| `mcp-quota.ttl` | Default rate-limit declarations |
| `iq-ontology.ttl` | Core `iq:` vocabulary (loaded into `iq:schema` graph) |
| `fsm.ttl` | Workflow templates and state machine vocabulary |

**Validation stack:**

```
Input Model
│
▼
RDF4J ShaclSail  ←  sh:NodeShape from tool manifest (mcp:inputShape)
│
▼
SPARQL ASK  ←  tool manifest mcp:authorizationQuery
│
▼
Adapter doExecute
│
▼
Output Model
│
▼
RDF4J ShaclSail  ←  sh:NodeShape (mcp:outputShape)  [optional pre-send]
```

**SHACL pattern** (per tool in `SPEC.md`):
```turtle
mcp:FactSparqlQueryInput a sh:NodeShape ;
sh:targetNode mcp:input ;
sh:property [
sh:path rdf:value ;
sh:datatype xsd:string ;
sh:minCount 1
] ;
sh:property [
sh:path iq:timeout ;
sh:datatype xsd:integer ;
sh:maxInclusive 300000
] .
```

---

### 3. Agents

**What it is**: IQ agents are stateful actors defined by an RDF workflow graph and executed via a finite-state machine. They have working memory (an RDF model), can invoke LLMs, execute SPARQL, and transition through states.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `I_Agentic` | `systems.symbol.agent` | Top interface: `start`, `stop`, `getStateMachine`, `getThoughts` |
| `AbstractAgent` | `systems.symbol.agent` | Base implementation wiring FSM + memory |
| `IntentAgent` | `systems.symbol.agent` | Handles `ux/intent` transitions (external trigger) |
| `Agentic` | `systems.symbol.agent` | Factory / builder for agents from RDF spec |
| `IQFacade` / `SelfFacade` | `systems.symbol.agent` | Agent's view of IQ capabilities (SPARQL, LLM, secrets) |
| `Facades` | `systems.symbol.agent` | Registry of facades per agent instance |
| `ModelStateMachine` | `systems.symbol.fsm` | FSM backed by an RDF `iq:Workflow` graph |
| `GuardedStateMachine` | `systems.symbol.fsm` | FSM with SPARQL ASK transition guards |
| `AbstractStateMachine` | `systems.symbol.fsm` | Core FSM logic (states, transitions, `next`) |

**Agent lifecycle:**

```
RDF spec (iq:Agent, iq:Workflow)
│
▼
Agentic.build(realm, agentIRI)
│
▼
AbstractAgent  +  ModelStateMachine
││
   IQFacade iq:Workflow graph
   (memory, (states, iq:to edges,
SPARQL,  guard SPARQL ASK)
LLM)
│
▼
IntentAPI  →  actor.trigger / actor.execute (MCP)
```

**Working memory**: each agent holds a `Model` (RDF graph, called "thoughts"). IQ tool `actor.memory` reads it; `actor.memory.set` writes triples into it. Memory is realm-scoped.

**IQ tool surface:**

| Tool | Wraps | Description |
|------|-------|-------------|
| `actor.trigger` | `IntentAgent.trigger()` | Fire a workflow intent (event-driven) |
| `actor.execute` | `AbstractAgent.execute()` | Run a named action and await result |
| `actor.status` | `I_Agentic.getStateMachine()` | Current state, available transitions |
| `actor.start` | `Agentic.build()` + `start()` | Instantiate and start an agent |
| `actor.stop` | `I_Agentic.stop()` | Graceful shutdown |
| `actor.memory` | `I_Agentic.getThoughts()` | Read agent working memory as RDF |
| `actor.memory.set` | SPARQL UPDATE on memory graph | Write triples into agent memory |
| `fleet.list` | `AgentService.list()` | All active agents in realm |
| `fleet.describe` | `I_Agentic` + memory | Describe one agent (state + memory excerpt) |
| `workflow.list` | SPARQL on `iq:Workflow` graph | All declared workflows in realm |
| `workflow.describe` | CONSTRUCT on workflow graph | Full workflow as RDF (states, transitions, guards) |
| `workflow.validate` | ShaclSail on workflow + agent spec | Check workflow is well-formed |

**Workflow RDF pattern** (from `fsm.ttl`):
```turtle
:MyWorkflow a iq:Workflow ;
iq:initial :draft ;
iq:state :draft, :review, :approved, :rejected ;
:draft iq:to :review ;
:review iq:to :approved ;
:review iq:to :rejected .

# Guard (optional):
:draft iq:to :review ;
iq:guard "ASK { <agent> iq:hasRole iq:Reviewer }" .
```

---

### 4. Queries — Stored Query Catalog

**What it is**: IQ stores named SPARQL queries and prompts as RDF resources (`.iq` graph files). The `IQScriptCatalog` loads them at startup; adapters and agents execute them by IRI reference rather than embedding query strings.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `IQScriptCatalog` | `systems.symbol.rdf4j.sparql` | Loads named queries from `RepositoryConnection`; lookup by `iq:scriptId` |
| `IQScripts` | `systems.symbol.rdf4j.sparql` | Fluent builder: `.fromGraph(conn).withId("myQuery").run(mapper)` |
| `ModelScriptCatalog` | `systems.symbol.rdf4j.sparql` | Same but backed by in-memory `Model` (for tests) |
| `IQSimpleCatalog` | `systems.symbol.rdf4j.sparql` | Stateless, no IQ script resolution — bare SPARQL execution |

**Stored query pattern:**
```turtle
# .iq/queries/agents.ttl
<urn:iq:query:listAgents> a iq:Script ;
iq:scriptId "listAgents" ;
rdf:value """
SELECT ?agent ?state WHERE {
GRAPH iq:facts {
?agent a iq:Agent ; iq:state ?state .
}
}
""" .
```

**IQ tool surface:**

| Tool | Description |
|------|-------------|
| `query.list` | List all `iq:Script` resources in the realm |
| `query.get` | Fetch one script body (RDF + `rdf:value` text) |
| `query.run` | Execute a stored query by IRI, binding `$params` at call time |
| `query.create` | Write a new `iq:Script` into the realm graph |
| `query.update` | Replace `rdf:value` of an existing script |
| `query.delete` | Remove a script resource |
| `query.validate` | SPARQL syntax-check a script body without executing |

**Binding injection** — stored queries may reference `$variables` which are substituted from the call's input model before execution:
```sparql
SELECT ?prop ?val WHERE {
<$subjectIRI> ?prop ?val .
}
```
`IQScripts.withBindings(inputModel)` resolves `$subjectIRI` from the model's `iq:binding` triples.

---

### 5. Models — RDF Model layer

**What it is**: the in-process representation of RDF data. IQ uses RDF4J `Model` everywhere — as tool input/output, agent memory, SHACL shapes, and governance configs.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `IQStore` | `systems.symbol.rdf4j.store` | Quarkus-injectable `Repository` with lifecycle |
| `IQConnection` | `systems.symbol.rdf4j.store` | Managed `RepositoryConnection` with auto-close |
| `LiveModel` | `systems.symbol.rdf4j.store` | `Model` backed by a live `RepositoryConnection` slice |
| `SelfModel` | `systems.symbol.rdf4j.store` | Agent's memory model (read/write with RDF4J transactions) |
| `SelfStatement` | `systems.symbol.rdf4j.store` | Single statement mutation helper |
| `MemoryRDFSRepository` | `systems.symbol.rdf4j.store` | In-memory RDFS-inferencing store (for tests / ephemeral realms) |
| `Facts` | `systems.symbol.rdf4j` | Static helpers: `Facts.of(conn)`, `Facts.model(...)` |
| `IRIs` | `systems.symbol.rdf4j` | IRI construction helpers |
| `NS` | `systems.symbol.rdf4j` | Namespace constant registry |
| `RDFLoader` | `systems.symbol.rdf4j.io` | Load Turtle / N-Quads / JSON-LD from path or classpath |
| `RDFDump` | `systems.symbol.rdf4j.io` | Serialise `Model` to string (Turtle, N-Quads, JSON-LD) |
| `Remodels` | `systems.symbol.rdf4j.io` | Copy / merge / filter across models and repositories |
| `NamedMap` | `systems.symbol.model` | Typed `Map<String, T>` backed by RDF (key = IRI ***REMOVED***) |

**Model lifecycle in an IQ call:**

```
JSON-LD body (from IQ client)
│
▼
RDFLoader.parse(body, JSON_LD)  →  Model  (input)
│
▼
SHACL validation  (IQStore.shaclRepo)
│
▼
MCPAdapterBase.doExecute(inputModel)
│
▼
SPARQLer / LLMFactory / Agent
│
▼
output Model
│
▼
RDFDump.asJsonLD(outputModel)  →  IQ response
```

**Serialisation formats supported** (via `RDFDump` / `FileFormats`):

| Format | MIME | Use |
|--------|------|-----|
| JSON-LD | `application/ld+json` | Default IQ wire format |
| Turtle | `text/turtle` | Human-readable; tool `store.export` |
| N-Quads | `application/n-quads` | Streaming bulk transfer |
| N-Triples | `application/n-triples` | Fast parse; CI test fixtures |

---

### 6. Graphs — Named Graph Architecture

**What it is**: RDF4J stores named graphs as `ContextStatement` quads. IQ uses a deliberate named-graph topology — every concern has a canonical graph IRI.


**Graph access patterns:**

| Pattern | SPARQL idiom |
|---------|-------------|
| Read domain facts | `SELECT … WHERE { GRAPH iq:facts { … } }` |
| Check policy | `ASK { GRAPH mcp:policy { <p> iq:hasRole ?r . ?r mcp:allows "tool.name" } }` |
| Append audit event | `INSERT DATA { GRAPH mcp:audit { [] a mcp:Event ; … } }` |
| Update quota counter | `DELETE/INSERT { GRAPH mcp:quota { … } } WHERE { … }` (transactional) |
| Load workflow | `CONSTRUCT { ?s ?p ?o } WHERE { GRAPH iq:workflows { ?s ?p ?o . FILTER(?s = <wf>) } }` |

**Lake module** (`systems.symbol.lake`) — manages repositories beyond the base RDF4J store:

| Class | Role |
|-------|------|
| `Lakes` | CDI bean registry of all realms' `AbstractLake` instances |
| `AbstractLake` | Base lake: `RepositoryConnection`, namespace setup, startup hooks |
| `BootstrapLake` | Lake that loads `.ttl` resources from classpath on first start |
| `BootstrapVFSLake` | Lake that crawls a VFS directory root for `.ttl` files |
| `ContentEntity` | Typed wrapper around a crawled/ingested content item |
| `BootstrapRepository` | Safe-initialise `Repository` from a Turtle seed |

**tool surface for graphs:**

| Tool | Description |
|------|-------------|
| `store.list` | List all `Repository` instances in the realm |
| `store.info` | Statistics for one store (triple count, named graphs, namespaces) |
| `store.export` | Dump a named graph or full store as N-Quads |
| `store.clear` | Remove all triples in a named graph |
| `store.namespaces` | Read or update the namespace prefix table |
| `store.create` | Bootstrap a new in-repo named graph from a Turtle body |
| `store.drop` | Remove a named graph entirely |
| `lake.crawl` | Trigger a VFS crawl to ingest new `.ttl` files |
| `lake.ingest` | Push a Turtle / JSON-LD document into the lake |
| `fact.load` | Parse and load triples from a URL or inline body into `iq:facts` |

---

### 7. LLM Integration

**What it is**: IQ wraps LLM providers (OpenAI, Groq, custom) behind a uniform interface. Conversations are typed RDF exchanges; prompts may embed SPARQL results.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `I_LLM` | `systems.symbol.llm` | Core interface: `invoke(Conversation)→I_LLMessage` |
| `I_LLMConfig` | `systems.symbol.llm` | Named provider config (model, temperature, max tokens) |
| `Conversation` | `systems.symbol.llm` | Ordered list of `I_LLMessage` (typed messages) |
| `TextMessage` | `systems.symbol.llm` | Plain text message |
| `IntentMessage` | `systems.symbol.llm` | Message carrying an `iq:Intent` RDF resource |
| `ImageMessage` | `systems.symbol.llm` | Multi-modal image payload |
| `I_ToolSpec` | `systems.symbol.llm` | Tool spec injected into LLM for function-calling |
| `I_Assist` | `systems.symbol.llm` | Streaming / async assist interface |
| `LLMFactory` | `systems.symbol.llm.gpt` | Produces `I_LLM` instances from named config map |
| `GPTWrapper` | `systems.symbol.llm.gpt` | OpenAI-compatible HTTP client |
| `GPTConfig` | `systems.symbol.llm.gpt` | Config POJO (`endpoint`, `apiKey`, `model`) |

**LLM config pattern** (named maps in `application.properties`):
```properties
iq.llm.providers.default.endpoint=https://api.openai.com/v1
iq.llm.providers.default.model=gpt-4o
iq.llm.providers.groq.endpoint=https://api.groq.com/openai/v1
iq.llm.providers.groq.model=llama3-70b-8192
```

**IQ tool surface:**

| Tool | Wraps | Description |
|------|-------|-------------|
| `llm.list` | `LLMFactory.providers()` | Available named providers |
| `llm.invoke` | `I_LLM.invoke(Conversation)` | One-shot prompt → response |
| `llm.chat` | `Conversation` with history | Multi-turn chat; conversation stored as RDF |
| `llm.embed` | provider embed endpoint | Produce vector embedding for a text ***REMOVED*** |
| `llm.search` | `Recommends` / `SearchMatrix` | Semantic similarity search over realm facts |
| `llm.explain` | `I_LLM` + SPARQL CONSTRUCT | Ask LLM to explain a resource given its RDF description |

**SPARQL ↔ LLM integration pattern** (used in `llm.explain`):
```
fact.describe <resourceIRI>  → description Model
RDFDump.asTurtle(description)→ string context
Conversation.add("System: ...", context)
Conversation.add("User: Explain <resourceIRI>")
I_LLM.invoke(conversation)   → I_LLMessage response
```

---

### 8. Trust & Identity

**What it is**: IQ's trust model is capability-based. Principals hold JWT tokens; roles are RDF-declared; trust zones scope cross-realm access; the vault stores credentials.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `I_TrustZone` | `systems.symbol.trust` | Verify token → `IRI principal`; check realm trust link |
| `I_Authority` | `systems.symbol.trust` | Issue / revoke tokens |
| `I_KeyStore` | `systems.symbol.trust` | Key storage (sign / verify JWTs) |
| `Locksmith` | `systems.symbol.trust` | Coordinate authority + key store |
| `Genesis` | `systems.symbol.trust` | Bootstrap initial trust graph for a realm |
| `VFSKeyStore` | `systems.symbol.trust` | Keys stored in VFS `.iq/vault` |
| `SimpleAuthority` | `systems.symbol.trust` | In-memory authority (dev/test) |
| `TrusteeKeys` | `systems.symbol.trust` | Named key sets |
| `BrokenTrust` | `systems.symbol.trust` | Exception hierarchy for trust failures |

**IQ tool surface:**

| Tool | Description |
|------|-------------|
| `trust.login` | Exchange provider credential for IQ JWT |
| `trust.refresh` | Refresh an expiring JWT |
| `trust.verify` | Inspect a token (claims, roles, trust zone) |
| `trust.roles` | List roles for the current principal |
| `trust.grant` | Admin: assign a role to a principal |
| `trust.revoke` | Admin: remove a role / token |

**Vault / secrets (`systems.symbol.secrets`):**

| Class | Role |
|-------|------|
| `VFSPasswordVault` | Read secrets from `.iq/vault` encrypted store |
| `EnvsAsSecrets` | Overlay: read from env vars first, fall back to vault |
| `SafeSecrets` | Never throw; returns `Optional<String>` |
| `MemoryVault` | In-memory vault for tests |

**IQ tool surface for vault:**

| Tool | Description |
|------|-------------|
| `vault.list` | List secret names (no values) in scope |
| `vault.get` | Retrieve a secret by name (admin only; audit logged) |
| `vault.set` | Write a secret (admin only) |
| `vault.rotate` | Rotate a secret value + re-encrypt |
| `vault.delete` | Remove a secret |

---

### 9. Search & Finder

**What it is**: semantic search and similarity matching over realm facts.

**Key classes:**

| Class | Package | Role |
|-------|---------|------|
| `I_Finder` | `systems.symbol.finder` | Interface: `find(query, realm)→List<IRI>` |
| `AgentMemory` | `systems.symbol.finder` | Search over agent working memory |
| `Recommends` | `systems.symbol.finder` | Collaborative-filter style recommendation |
| `SearchMatrix` | `systems.symbol.finder` | Score matrix for ranked retrieval |

**IQ tool surface:**

| Tool | Description |
|------|-------------|
| `search.find` | Free-text or semantic find over realm |
| `search.similar` | Find resources similar to a seed IRI |
| `search.recommend` | Get recommendations from `SearchMatrix` |
| `search.index` | Trigger re-indexing (after bulk load) |

---

### 10. Custom SPARQL Functions (Utilities)

**What it is**: IQ registers custom SPARQL functions that can be called inside any SPARQL query. They extend the SPARQL function namespace at `iq:fn/`.

| Function | Class | Signature |
|----------|-------|-----------|
| `iq:fn/uuid` | `UUID` | `UUID() → xsd:string` |
| `iq:fn/camelCase` | `CamelCase` | `camelCase(str) → xsd:string` |
| `iq:fn/pascalCase` | `PascalCase` | `pascalCase(str) → xsd:string` |
| `iq:fn/bakeIRI` | `BakeIRI` | `bakeIRI(base, label) → IRI` |
| `iq:fn/namespace` | `Namespace` | `namespace(prefix) → IRI` |
| `iq:fn/alike` | `Alike` | `alike(str1, str2) → xsd:double` (Levenshtein similarity) |
| `iq:fn/levenshtein` | `Levenshtein` | `levenshtein(str1, str2) → xsd:integer` |
| `iq:fn/geoDistance` | `GeoDistance` | `geoDistance(lat1, lon1, lat2, lon2) → xsd:double` |
| `iq:fn/hbs` | `HBS` | `hbs(template, bindings) → xsd:string` (Handlebars template) |
| `iq:fn/custom` | `CustomFunction` | Extension point for custom function registration |

**Registration** — RDF4J `CustomFunction` SPI + `@Singleton` Quarkus bean; functions are auto-discovered via `ServiceLoader`.

**Usage in stored queries:**
```sparql
PREFIX fn: <https://symbol.systems/iq/fn/>
SELECT ?iri WHERE {
BIND(fn:bakeIRI("https://example.org/", ?label) AS ?iri)
}
```

---

### 11. Utilities

General-purpose helpers used across all adapters and capability domains.

| Class | Package | Role |
|-------|---------|------|
| `RDFHelper` | `systems.symbol.rdf4j.util` | `getFirst(model, subj, pred)`, `listOf(model, type)` |
| `RDFPrefixer` | `systems.symbol.rdf4j.util` | Auto-expand CURIEs using realm namespace table |
| `ValueTypeConverter` | `systems.symbol.rdf4j.util` | `Literal → Java`, `Java → Literal` |
| `SupportedScripts` | `systems.symbol.rdf4j.util` | Declares which script types the catalog handles |
| `FakeReturn` | `systems.symbol.rdf4j.util` | Test fixture: predictable `I_MCPResult` stubs |
| `AssetMimeTypes` | `systems.symbol.rdf4j.io` | MIME type registry for RDF serialisations |
| `FileFormats` | `systems.symbol.rdf4j.io` | `RDFFormat` lookup by extension or MIME |
| `Files` | `systems.symbol.rdf4j.io` | VFS-safe file read/write helpers |
| `SPARQLMapper` | `systems.symbol.rdf4j.sparql` | `BindingSet[] → T[]` generic mapper |

---

## Data flow — end to end

```
 IQ client sends: { "tool": "fact.sparql.query", "input": { "@graph": […] } }

 L1  MCPServer.dispatch(request)
   → parse input JSON-LD into Model  (RDFLoader.parse)

 L2  MCPConnectPipeline.invoke(ctx)
   01 AuthGuard  verify JWT → ctx.principal = <principalIRI>
   02 TrustZoneGuard ASK { GRAPH mcp:policy { <p> iq:trusts <realm> } }
   03 ACLFilter  ASK { GRAPH mcp:policy { <p> mcp:allows "fact.sparql.query" } }
   04 QuotaGuard SELECT counter; if exceeded → 429
   06 CacheInterceptor   SHA-256(toolName+inputModel) → cache lookup
   07 InputTransformer   CURIE expand, inject iq:realm

 L3  MCPAdapterBase.invoke("fact.sparql.query", inputModel)
   A  resolveManifestSPARQL on MCPToolRegistry → I_MCPToolManifest
   B  validateInput  ShaclSail.validate(inputModel, manifest.inputShape)
   C  checkPolicyASK (manifest.authorizationQuery)
   D  checkQuota (done in L2 QuotaGuard; adapter may do fine-grained check)
   E  doExecute()FactAdapter: SPARQLer.query(conn, query, timeout)
   F  audit  INSERT into mcp:audit { [] a mcp:Event ; … }

 L2  CacheInterceptorwrite-through: store result for TTL
 OutputTransformer   redact / project / serialise
 AuditWriter append mcp:Event (connect:middlewarePath recorded)
 MetricsEmitter  mcp_calls_total++; span end

 L1  MCPServer.respond   serialize output Model → JSON-LD response
```

---

## Adapter registry — which adapter handles which tool

| Tool prefix | Adapter class | IQ capability |
|-------------|--------------|---------------|
| `fact.*` | `FactAdapter` | `SPARQLer`, `IQScriptCatalog` (S2) |
| `store.*` | `StoreAdapter` | `Lakes`, `BootstrapRepository`, `IQStore` (S3) |
| `realm.*` | `RealmAdapter` | `RealmManager`, `I_Realms`, `I_Realm` (S1) |
| `actor.*` | `AgentAdapter` | `Agentic`, `IntentAgent`, `AgentService` (S4) |
| `fleet.*` | `FleetAdapter` | `Lakes`-level agent registry (S4) |
| `workflow.*` | `WorkflowAdapter` | `ModelStateMachine`, `fsm.ttl` (S12) |
| `query.*` | `QueryAdapter` | `IQScriptCatalog`, stored SPARQL resources (S2) |
| `llm.*` | `LLMAdapter` | `LLMFactory`, `GPTWrapper`, `Conversation` (S5) |
| `trust.*` | `TrustAdapter` | `I_TrustZone`, `Locksmith`, `I_Authority` (S6) |
| `vault.*` | `VaultAdapter` | `VFSPasswordVault`, `EnvsAsSecrets` (S7) |
| `search.*` | `SearchAdapter` | `I_Finder`, `SearchMatrix`, `Recommends` (S8) |
| `lake.*` | `LakeAdapter` | `AbstractLake`, `BootstrapVFSLake`, crawl/ ingest/ (S9) |
| `persona.*` | `PersonaAdapter` | iq-persona module (S10) |
| `kb.*` | `KBAdapter` | `Select.java`, `Construct.java` (S11) |
| `connect.*` | `ConnectAdminAdapter` | Middleware registry / circuit state (MCP_CONNECT) |

---

## Module ownership

| Maven module | Capability domains owned |
|-------------|--------------------------|
| `iq-mcp` | L1 transport, L2 gateway, L3 adapter registry, governance |
| `iq-rdf4j` | SPARQL executor, model layer, custom functions, utilities |
| `iq-platform` | Agents, FSM, LLM, trust, vault, realm, finder, lake abstractions |
| `iq-lake` | Lake implementations, crawl, ingest |
| `iq-mcp` | MCPServer JAX-RS endpoint, Quarkus wiring |
| `iq-trusted` | Production trust zone, VFS key store |
| `iq-persona` | Persona capability |
| `iq-analytics` | KB / analytics adapters |
| `iq-finder` | Search / similarity adapters |
| `iq-onto` | Ontology source files (`.ttl`), domain vocabulary |

---

## Decision log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| D1 | All governance (ACL, quota, audit) stored as RDF named graphs | Single source of truth; queryable via SPARQL; no external config service needed | 2026-03-18 |
| D2 | JSON-LD as default IQ wire format | Preserves RDF semantics over the wire; `@context` maps to `iq:` namespace | 2026-03-18 |
| D3 | Middleware pipeline (IQ Connect) wraps adapter pipeline (MCPAdapterBase) | Separation of concerns: cross-cutting (auth, cache, rate limit) vs. tool-specific (manifest, SHACL, domain calls) | 2026-03-19 |
| D4 | SHACL shapes declared in tool manifests, loaded from RDF | Shapes are part of the ontology; no separate validation framework; shares RDF4J ShaclSail already in the stack | 2026-03-18 |
| D5 | Custom SPARQL functions extend `iq:fn/` namespace | Keeps SPARQL queries readable and avoids procedural escape hatches; functions registered via SPI | 2026-03-19 |
| D6 | Stored queries as `iq:Script` RDF resources (`rdf:value` body) | Queries are first-class knowledge objects: discoverable, versioned, governed, testable via IQ tool `query.run` | 2026-03-19 |
| D7 | Agent memory is a typed RDF `Model` ("thoughts") | Consistent with RDF-first design; memory is legible, queryable, and auditable; no bespoke serialisation | 2026-03-19 |
| D8 | `LLMFactory` uses named-map config; no singleton provider | Supports multi-provider, per-realm provider selection without restart; config resolved from `application.properties` or graph | 2026-03-18 |
| D9 | Circuit-breaker state persisted in `mcp:pipeline` graph | Pod-restartable; state visible to SPARQL; `connect.circuit.status` IQ tool reads it directly | 2026-03-19 |
| D10 | Vault secret interpolation at RDF load time (`{connect:vaultSecret:NAME}`) | Secrets never appear in RDF graphs or audit logs; injected once at startup | 2026-03-19 |

---

## Cross-cutting concerns matrix

Every adapter column must satisfy all rows or explicitly opt out with `connect:bypass`.

|  | Auth | Trust zone | ACL | Quota | SHACL in | SHACL out | Audit | Metrics |
|--|------|-----------|-----|-------|----------|----------|-------|---------|
| `FactAdapter` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `AgentAdapter` | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| `LLMAdapter` | ✓ | ✓ | ✓ | ✓ (budget) | ✓ | — | ✓ | ✓ |
| `TrustAdapter` | ✓ | — | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| `VaultAdapter` | ✓ | ✓ | admin-only | — | ✓ | — | ✓ | ✓ |
| `ConnectAdminAdapter` | ✓ | — | admin-only | — | — | — | ✓ | ✓ |
| `CacheInterceptor` | bypass | bypass | bypass | bypass | — | — | ✓ | ✓ |

---

## Definition of done (architecture level)

The architecture is fully implemented when:

1. Every cell in the adapter table above resolves to a concrete Java class.
2. Every named graph in the topology is initialised at realm bootstrap (`BootstrapLake`).
3. Every IQ tool listed in MCP_TODO.md has a corresponding SHACL `sh:NodeShape` in `mcp-vocab.ttl`.
4. Every SPARQL governance check is a stored `iq:Script` (not an inline string in Java).
5. `mvn test -pl iq-mcp -am` passes with coverage > 80% on all adapter and middleware classes.
6. The full `fact.sparql.query` end-to-end path exercises all 11 middleware + all 6 `MCPAdapterBase` steps (verified by integration test with no mocked IQ layers).
7. Decision log entries D1–D10 each have a unit test or conformance test that would fail if the decision were reversed.
