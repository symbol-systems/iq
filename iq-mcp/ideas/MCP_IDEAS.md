---
id: mcp:plan/iq-mcp-todo
type: mcp:DevelopmentPlan
version: "2.0"
realm: iq
status: in-progress
updated: "2026-03-18"
audience: [human, llm, ci]
---

# IQ → MCP: Full-Stack Access Plan

> **What this document is.**  
> Each section is a self-contained work unit: it names the IQ components being wrapped, the files to create, the exact Java/SPARQL patterns to follow, and the acceptance criteria — enough for a human or an LLM agent to pick up any task and implement it without reading anything else first.

---

## How to use this document

**As a human** — read the phase overview table, pick a phase, and follow the task cards top-to-bottom.

**As an LLM** — paste any task card into context and ask: _"Implement this task. The project is a Java 21 / Quarkus 3 / RDF4J 5 Maven multi-module build. Follow patterns in `MCPAdapterBase` and existing `I_MCP*` interfaces."_

**As CI / automation** — tasks have `status:` fields. Grep `status: open` to find pending work; `status: done` for completed.

**Namespace reference** used throughout:

| Prefix | Expansion |
|--------|-----------|
| `mcp:` | `urn:mcp:` (this plan's namespace) |
| `iq:` | `https://symbol.systems/iq/` |
| `rdf:` | `http://www.w3.org/1999/02/22-rdf-syntax-ns#` |
| `sh:` | `http://www.w3.org/ns/shacl#` |
| `xsd:` | `http://www.w3.org/2001/XMLSchema#` |

---

## Subsystem map

Every IQ subsystem that MCP will expose, with its canonical Java package and tool prefix.

---

| ID | Subsystem | Java package | Tool prefix | Sprint |
|----|-----------|-------------|-------------|--------|
| S0 | Foundation / transport | `systems.symbol.mcp` | *(internal)* | 1 |
| S1 | Realms | `systems.symbol.realm` | `realm.*` | 2 |
| S2 | RDF / SPARQL facts | `systems.symbol.rdf4j` | `fact.*` | 1 |
| S3 | RDF4J stores | `systems.symbol.lake` | `store.*` | 2 |
| S4 | Agents & fleet | `systems.symbol.agent`, `.fleet` | `actor.*`, `fleet.*` | 3 |
| S5 | LLM providers | `systems.symbol.llm` | `llm.*` | 3 |
| S6 | Trust & identity | `systems.symbol.trust` | `trust.*` | 4 |
| S7 | Vault / secrets | `systems.symbol.secrets` | `vault.*` | 4 |
| S8 | Search & finder | `systems.symbol.finder` | `search.*` | 5 |
| S9 | Lake & ingest | `systems.symbol.lake` | `lake.*` | 5 |
| S10 | Personas | `iq-persona` module | `persona.*` | 6 |
| S11 | Analytics / KB | `systems.symbol.controller.kb` | `kb.*`, `analytics.*` | 6 |
| S12 | Workflows / FSM | `systems.symbol.fsm` | `workflow.*` | 7 |
| S13 | Governance (cross-cutting) | `MCPAdapterBase` | *(every tool)* | 1+7 |

---

## Tool registry (all 71 tools)

A single place to see every planned MCP tool, its read/write risk level, and required role.

| Tool | Risk | Min role | Sprint | Status |
|------|------|----------|--------|--------|
| `fact.sparql.query` | read | reader | 1 | open |
| `fact.sparql.update` | write | writer | 1 | open |
| `fact.describe` | read | reader | 1 | open |
| `fact.walk` | read | reader | 1 | open |
| `fact.explain` | read | reader | 1 | open |
| `fact.load` | write | writer | 2 | open |
| `realm.list` | read | reader | 2 | open |
| `realm.status` | read | reader | 2 | open |
| `realm.schema` | read | reader | 2 | open |
| `realm.export` | read | reader | 2 | open |
| `realm.import` | write | admin | 2 | open |
| `realm.policy` | read/write | admin | 2 | open |
| `realm.create` | write | admin | 2 | open |
| `store.list` | read | reader | 2 | open |
| `store.info` | read | reader | 2 | open |
| `store.create` | write | admin | 2 | open |
| `store.drop` | write | admin | 2 | open |
| `store.export` | read | reader | 2 | open |
| `store.clear` | write | admin | 2 | open |
| `store.namespaces` | read/write | writer | 2 | open |
| `fleet.list` | read | reader | 3 | open |
| `fleet.describe` | read | reader | 3 | open |
| `actor.trigger` | write | writer | 3 | open |
| `actor.execute` | write | writer | 3 | open |
| `actor.status` | read | reader | 3 | open |
| `actor.start` | write | writer | 3 | open |
| `actor.stop` | write | writer | 3 | open |
| `actor.memory` | read | reader | 3 | open |
| `actor.memory.set` | write | writer | 3 | open |
| `llm.list` | read | reader | 3 | open |
| `llm.invoke` | write | writer | 3 | open |
| `llm.chat` | write | writer | 3 | open |
| `llm.search` | read | reader | 3 | open |
| `llm.explain` | read | reader | 3 | open |
| `llm.status` | read | reader | 3 | open |
| `trust.login` | write | public | 4 | open |
| `trust.refresh` | write | reader | 4 | open |
| `trust.verify` | read | reader | 4 | open |
| `trust.revoke` | write | admin | 4 | open |
| `trust.roles` | read | reader | 4 | open |
| `trust.policies` | read | reader | 4 | open |
| `vault.list` | read | admin | 4 | open |
| `vault.exists` | read | admin | 4 | open |
| `vault.status` | read | admin | 4 | open |
| `vault.rotate` | write | admin | 4 | open |
| `search.text` | read | reader | 5 | open |
| `search.semantic` | read | reader | 5 | open |
| `search.sparql` | read | reader | 5 | open |
| `search.geo` | read | reader | 5 | open |
| `search.suggest` | read | reader | 5 | open |
| `lake.list` | read | reader | 5 | open |
| `lake.status` | read | reader | 5 | open |
| `lake.snapshot` | write | writer | 5 | open |
| `lake.restore` | write | admin | 5 | open |
| `lake.ingest` | write | writer | 5 | open |
| `lake.crawl` | write | writer | 5 | open |
| `lake.diff` | read | reader | 5 | open |
| `persona.list` | read | reader | 6 | open |
| `persona.describe` | read | reader | 6 | open |
| `persona.activate` | write | writer | 6 | open |
| `persona.create` | write | writer | 6 | open |
| `kb.query` | read | reader | 6 | open |
| `kb.search` | read | reader | 6 | open |
| `analytics.graph` | read | reader | 6 | open |
| `analytics.summary` | read | reader | 6 | open |
| `analytics.triples` | read | reader | 6 | open |
| `workflow.list` | read | reader | 7 | open |
| `workflow.describe` | read | reader | 7 | open |
| `workflow.validate` | read | reader | 7 | open |
| `workflow.history` | read | reader | 7 | open |
| `workflow.visualize` | read | reader | 7 | open |
| `audit.query` | read | admin | 7 | open |
| `budget.status` | read | reader | 7 | open |

---

## Governance contract (every tool inherits this)

Every adapter extends `MCPAdapterBase`. The base enforces this pipeline unconditionally:

```
invoke(toolName, inputModel)
  │
  ├─ 1. resolveManifest(toolName)        ← from MCPToolRegistry (RDF graph)
  ├─ 2. validateInput(inputModel, shape) ← RDF4J ShaclSail
  ├─ 3. checkPolicy(role, toolIRI, realm)← SPARQL ASK on mcp:policy graph
  ├─ 4. checkQuota(toolIRI, principal)   ← SPARQL ASK on mcp:quota graph
  ├─ 5. execute(...)                     ← adapter-specific delegation
  └─ 6. audit(event)                     ← INSERT into mcp:audit named graph
```

**Audit triple shape** (all fields mandatory):
```turtle
[] a mcp:Event ;
   mcp:tool      "fact.sparql.query" ;
   mcp:realm     <realmIRI> ;
   mcp:principal <principalIRI> ;
   mcp:timestamp "2026-03-18T12:00:00Z"^^xsd:dateTime ;
   mcp:durationMs 42^^xsd:long ;
   mcp:cost      1^^xsd:int ;
   mcp:success   true^^xsd:boolean .
```

---

## S0 — Foundation & transport

> **Why first.** Everything else depends on the server wire-up and the base adapter. Implement once; all later phases inherit for free.

### Task MCP-0.1 — MCP server endpoint
```
status: open | sprint: 1 | risk: none
creates: iq-run-apis/src/main/java/systems/symbol/controller/MCPServer.java
depends: I_MCPService (exists)
```
Add a Quarkus JAX-RS resource at `POST /mcp` and `GET /mcp/sse` that:
- Deserialises the MCP JSON-RPC envelope (MCP SDK 0.17.2 `McpMessage`).
- Routes `tools/call` → `I_MCPService.invokeTool(name, model)`.
- Routes `tools/list` → `I_MCPService.listAllTools()`.
- Guards behind `mcp.enabled=true` config property (default `false`).
- Supports SSE streaming for long-running tool calls.

**Pattern** (Quarkus + SDK server wiring):
```java
@Path("/mcp")
@ApplicationScoped
public class MCPServer {
    @Inject I_MCPService service;
    @POST @Consumes("application/json") @Produces("application/json")
    public Response handle(McpMessage msg) { ... }
    @GET @Path("/sse") @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context SseEventSink sink) { ... }
}
```

**Done when:** `curl -X POST /mcp -d '{"method":"tools/list"}'` returns a JSON array of tool names in dev mode.

---

### Task MCP-0.2 — MCPAdapterBase (governance pipeline)
```
status: open | sprint: 1 | risk: none
creates: iq-mcp/src/main/java/systems/symbol/mcp/MCPAdapterBase.java
depends: I_MCPAdapter, I_MCPResult, I_MCPToolManifest (all exist)
wraps: RDF4J ShaclSail, SPARQLExecutor (iq-rdf4j)
```
Abstract class implementing the 6-step governance pipeline above. All five checkers (`resolveManifest`, `validateInput`, `checkPolicy`, `checkQuota`, `audit`) must be final methods — adapters override only `doExecute(toolName, inputModel)`.

**Done when:** A no-op subclass of `MCPAdapterBase` passes `MCPAdapterBaseTest` verifying that a missing policy blocks invocation and that an audit triple is written to the in-memory store.

---

### Task MCP-0.3 — MCPToolRegistry (RDF-first manifest catalog)
```
status: open | sprint: 1 | risk: none
creates: iq-mcp/src/main/java/systems/symbol/mcp/MCPToolRegistry.java
         iq-mcp/src/main/resources/mcp-schema.ttl
         iq-mcp/src/main/resources/mcp-shapes.ttl
depends: RDF4J MemoryStore
```
- Loads `mcp-schema.ttl` (ontology: `mcp:ToolManifest`, `mcp:rateLimit`, `mcp:cost`, `mcp:inputShape`, `mcp:authQuery`) at startup.
- Each adapter calls `registry.register(manifestIRI, model)` — no hardcoded lists.
- `MCPToolRegistry.lookup(toolName)` executes:
  ```sparql
  SELECT ?iri ?desc ?shape ?auth ?limit WHERE {
    ?iri a mcp:ToolManifest ;
         mcp:name ?name ;
         rdfs:comment ?desc ;
         mcp:inputShape ?shape ;
         mcp:authQuery ?auth ;
         mcp:rateLimit ?limit .
    FILTER(?name = "fact.sparql.query")
  }
  ```

**Done when:** `registry.listAll()` returns all registered manifests; unknown tool name returns `Optional.empty()`.

---

### Task MCP-0.4 — MCPServiceImpl + MCPResultImpl
```
status: open | sprint: 1 | risk: none
creates: iq-mcp/src/main/java/systems/symbol/mcp/MCPServiceImpl.java
         iq-mcp/src/main/java/systems/symbol/mcp/MCPResultImpl.java
         iq-mcp/src/main/java/systems/symbol/mcp/MCPToolManifestImpl.java
depends: I_MCPService, I_MCPResult, I_MCPToolManifest (interfaces exist)
```
- `MCPServiceImpl`: `@ApplicationScoped` Quarkus bean; maintains `Map<String, I_MCPAdapter>` keyed by tool name; routes `invokeTool` to the correct adapter.
- `MCPResultImpl`: Java `record` — immutable, no setters.
- `MCPToolManifestImpl`: builder pattern, validates name matches `[a-z]+\.[a-z.]+` pattern.

**Done when:** `MCPServiceImpl` compiles, `invokeTool("unknown", model)` returns `isSuccess=false` with descriptive error.

---

## S1 — Realms

> **IQ interfaces**: `I_Realm`, `I_Realms` · **Implementations**: `Realm`, `Realms`, `RealmManager`, `SafeRepositoryManager`  
> **Package**: `systems.symbol.realm`

### Task MCP-1.1 — RealmAdapter skeleton
```
status: open | sprint: 2 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/RealmAdapter.java
depends: MCP-0.2 (MCPAdapterBase), I_Realms
```
Inject `I_Realms`; register tools: `realm.list`, `realm.status`, `realm.schema`, `realm.export`, `realm.import`, `realm.policy`, `realm.create`.

### Task MCP-1.2 — realm.list
```
status: open | sprint: 2 | risk: none
```
**SPARQL** (executed against meta-graph):
```sparql
SELECT ?realm ?label ?status WHERE {
  ?realm a iq:Realm ; rdfs:label ?label .
  OPTIONAL { ?realm mcp:status ?status }
} ORDER BY ?label
```
Returns JSON-LD array. No write; `reader` role sufficient.

### Task MCP-1.3 — realm.status
```
status: open | sprint: 2 | risk: none
```
Calls `SafeRepositoryManager.isConnected(realmIRI)` → writes `mcp:status "connected"/"disconnected"` to response model. Also returns `mcp:tripleCount` (cheap `SELECT (COUNT(*) AS ?n) WHERE { ?s ?p ?o }`).

### Task MCP-1.4 — realm.schema
```
status: open | sprint: 2 | risk: none
```
```sparql
CONSTRUCT { ?s ?p ?o } WHERE {
  GRAPH <urn:iq:schema> { ?s ?p ?o }
}
```
Returns Turtle by default; respects `Accept` header.

### Task MCP-1.5 — realm.export / realm.import
```
status: open | sprint: 2 | risk: medium (import is destructive)
```
- **export**: stream named graphs as Turtle using RDF4J `Rio.write(…)`.
- **import**: accept base64-encoded Turtle or JSON-LD in `rdf:value`; parse with `Rio.parse(…)`; `INSERT DATA INTO <graphIRI>`; requires `admin` role; writes delta count to audit.

### Task MCP-1.6 — realm.policy / realm.create
```
status: open | sprint: 2 | risk: high (admin only)
```
- **policy read**: `DESCRIBE <policyIRI>` on `mcp:policy` named graph.
- **policy write**: `INSERT DATA { GRAPH mcp:policy { <role> mcp:allows <tool> } }`.
- **create**: `RealmManager.create(iri, label)` → bootstrap `.iq` skeleton; requires `admin` role; cannot reuse existing IRI.

---

## S2 — RDF / SPARQL facts (FactAdapter)

> **Highest value adapter — implement first after foundation.**  
> **IQ classes**: `RepositoryConnection` (RDF4J), `SPARQLExecutor`, stored queries in `.iq` graphs  
> **Package**: `systems.symbol.rdf4j`

### Task MCP-2.1 — FactAdapter skeleton
```
status: open | sprint: 1 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/FactAdapter.java
depends: MCP-0.2, RepositoryConnection per realm
```
Pool one `RepositoryConnection` per realm; release in `finally`. Register: `fact.sparql.query`, `fact.sparql.update`, `fact.describe`, `fact.walk`, `fact.explain`, `fact.load`.

### Task MCP-2.2 — fact.sparql.query
```
status: open | sprint: 1 | risk: none
input shape: sh:property [ sh:path rdf:value ; sh:datatype xsd:string ; sh:minCount 1 ]
             sh:property [ sh:path iq:timeout ; sh:datatype xsd:integer ]
             sh:property [ sh:path iq:graphIRI ; sh:datatype xsd:anyURI ]
output: JSON-LD bindings for SELECT; Turtle for CONSTRUCT/DESCRIBE; xsd:boolean for ASK
```
Detect query form from first keyword; use `TupleQuery` / `GraphQuery` / `BooleanQuery` accordingly. Apply `iq:timeout` via `query.setMaxExecutionTime(ms/1000)`.

### Task MCP-2.3 — fact.sparql.update
```
status: open | sprint: 1 | risk: medium
input: rdf:value (UPDATE string), iq:dryRun (xsd:boolean, default false)
```
If `iq:dryRun=true`: execute inside a transaction, capture delta (added/removed counts via `RepositoryResult` before/after), rollback — return delta without persisting. Otherwise commit. Log delta to `mcp:audit` as `mcp:added` / `mcp:removed` literals.

### Task MCP-2.4 — fact.describe
```
status: open | sprint: 1 | risk: none
input: iq:subject (IRI), iq:maxDepth (xsd:int, default 1)
```
Recursive `DESCRIBE <IRI>` up to `maxDepth` hops, deduplicating visited IRIs. Cycle guard: maintain `Set<IRI> visited`. Return merged `Model`.

### Task MCP-2.5 — fact.walk
```
status: open | sprint: 2 | risk: none
input: iq:root (IRI), iq:direction ("out"|"in"|"both"), iq:maxHops (xsd:int, default 3)
```
BFS from root IRI. For each hop:
```sparql
SELECT ?next WHERE {
  GRAPH ?g { <current> ?p ?next }
  FILTER(isIRI(?next))
}
```
Return subgraph as Turtle with `mcp:hopCount` literal in result.

### Task MCP-2.6 — fact.explain / fact.load
```
status: open | sprint: 2 | risk: low / medium
```
- **explain**: `TupleQuery.explain(Explanation.Level.Timed)` → return plan as `text/plain` in `rdf:value`.
- **load**: dereference URL with `Rio.parse(url.openStream(), …)`; `add(model, graphIRI)` via `RepositoryConnection`; requires `writer` role. Reject non-RDF MIME types.

---

## S3 — RDF4J stores

> **IQ classes**: `BootstrapRepository`, `SafeRepositoryManager`, `Lakes`, `AbstractLake`  
> **Package**: `systems.symbol.lake`

### Task MCP-3.1 — StoreAdapter
```
status: open | sprint: 2 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/StoreAdapter.java
depends: MCP-0.2, SafeRepositoryManager, Lakes
```

### Tasks MCP-3.2 → 3.8 — store tools

| Task | Tool | Key call | Role |
|------|------|----------|------|
| 3.2 | `store.list` | `Realms.all()` → `repo.getRepositoryInfo()` | reader |
| 3.3 | `store.info` | `SELECT (COUNT(*) AS ?n) WHERE {?s ?p ?o}` + namespace list | reader |
| 3.4 | `store.create` | `BootstrapRepository.create(id, config)` → persist in `.iq/repositories/` | admin |
| 3.5 | `store.drop` | `SafeRepositoryManager.removeRepository(id)` — confirm flag required | admin |
| 3.6 | `store.export` | `Rio.write(conn.getStatements(null,null,null,true), writer, NQUADS)` streamed | reader |
| 3.7 | `store.clear` | `conn.clear(graphIRI?)` — if no graph IRI, clears all; double confirmation | admin |
| 3.8 | `store.namespaces` | `conn.getNamespaces()` (GET) / `conn.setNamespace(p,iri)` (SET) | writer |

---

## S4 — Agents & fleet

> **IQ classes**: `I_Agent`, `I_Fleet`, `AgentBuilder`, `AgentService`, `I_StateMachine`, `AgentAction`  
> **Package**: `systems.symbol.agent`, `systems.symbol.fleet`  
> **REST ref**: `IntentAPI` at `POST ux/intent/{realm}`

### Task MCP-4.1 — ActorAdapter
```
status: open | sprint: 3 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/ActorAdapter.java
depends: MCP-0.2, AgentService, I_Fleet
```

### Tasks MCP-4.2 → 4.10 — fleet & actor tools

| Task | Tool | Key call / SPARQL | Notes |
|------|------|-------------------|-------|
| 4.2 | `fleet.list` | `SELECT ?a ?label WHERE { ?a a iq:Agent ; rdfs:label ?label }` | read |
| 4.3 | `fleet.describe` | `DESCRIBE <agentIRI>` + follow `iq:workflow` | read |
| 4.4 | `actor.trigger` | `AgentService.next(new AgentAction(actor, intent, bindings))` | fires FSM transition |
| 4.5 | `actor.execute` | `AgentService.execute(action, bindings)` | generic action |
| 4.6 | `actor.status` | `agent.getStateMachine().getState()` + serialize `thoughts` | read; cheap |
| 4.7 | `actor.start` | `agent.start()` — propagate `StateException` as `mcp:error` | write |
| 4.8 | `actor.stop` | `agent.stop()` | write |
| 4.9 | `actor.memory` | `agent.getThoughts()` → Turtle / JSON-LD via `Rio.write(…)` | read |
| 4.10 | `actor.memory.set` | `conn.add(inputModel, agentMemoryGraph)` | write; writer role |

**Integration test**: build minimal FSM agent (2 states, 1 transition); call `actor.trigger`; assert `actor.status` returns new state. Use in-memory RDF4J store + `AgentBuilder`.

---

## S5 — LLM providers

> **IQ classes**: `I_LLM`, `I_LLMConfig`, `LLMFactory`, `GPTWrapper`, `Conversation`, `I_LLMessage`  
> **Package**: `systems.symbol.llm`

### Task MCP-5.1 — LlmAdapter
```
status: open | sprint: 3 | risk: low (no real keys in unit tests)
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/LlmAdapter.java
depends: MCP-0.2, LLMFactory
```
Inject `LLMFactory`; always mock `I_LLM` in unit tests — never call real endpoints.

### Tasks MCP-5.2 → 5.7 — llm tools

| Task | Tool | Key call | Notes |
|------|------|----------|-------|
| 5.2 | `llm.list` | `LLMFactory.getProviders()` → map of name→model | read; no auth needed |
| 5.3 | `llm.invoke` | `llm.ask(prompt, context)` — `rdf:value` = prompt; `iq:context` = Turtle model | records `iq:tokens` in audit |
| 5.4 | `llm.chat` | `Conversation` keyed by `mcp:sessionId` (in-memory TTL map, 30 min) | stateful |
| 5.5 | `llm.search` | embed query → vector NN search; fallback: `FILTER(contains(lcase(?label), lcase(?q)))` | read |
| 5.6 | `llm.explain` | `fact.describe` → Turtle string → prepend to prompt → `llm.invoke` | composed tool |
| 5.7 | `llm.status` | HTTP HEAD to provider endpoint; return `mcp:latencyMs`, `mcp:available` | admin |

---

## S6 — Trust & identity

> **IQ classes**: `I_TrustZone`, `I_Authority`, `I_KeyStore`, `TokenAPI`, `Locksmith`, `Genesis`  
> **Package**: `systems.symbol.trust`  
> **Rule**: `trust.*` tools NEVER emit JWT secrets in audit log — log only `mcp:principal` IRI and `mcp:provider`.

### Task MCP-6.1 — TrustAdapter
```
status: open | sprint: 4 | risk: high (identity boundary)
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/TrustAdapter.java
depends: MCP-0.2, TokenAPI, I_Authority
```

### Tasks MCP-6.2 → 6.7 — trust tools

| Task | Tool | Key call | Role |
|------|------|----------|------|
| 6.2 | `trust.login` | `TokenAPI.login(realm, provider, bindings)` → JWT + `mcp:roles` | public |
| 6.3 | `trust.refresh` | `TokenAPI.refresh(realm)` — validate expiry first | reader |
| 6.4 | `trust.verify` | `I_TrustZone.verify(jwt)` → claims as RDF model | reader |
| 6.5 | `trust.revoke` | `INSERT DATA { GRAPH mcp:revoked { <tokenIRI> mcp:revokedAt ?now } }` | admin |
| 6.6 | `trust.roles` | `SELECT ?r WHERE { <principal> iq:hasRole ?r }` | reader |
| 6.7 | `trust.policies` | `CONSTRUCT { ?s ?p ?o } WHERE { GRAPH mcp:policy { ?s ?p ?o } }` | reader |

---

## S7 — Vault / secrets

> **IQ classes**: `VFSPasswordVault`, `EnvsAsSecrets`, `I_KeyStore`, `VFSKeyStore`  
> **Package**: `systems.symbol.secrets`  
> **Hard invariants** — enforced in `VaultAdapter`, not overridable by subclasses:
> - Secret *values* are never written to response model, audit log, or any named graph.
> - Every call requires `trust.admin` role — no exceptions.

### Task MCP-7.1 — VaultAdapter
```
status: open | sprint: 4 | risk: critical
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/VaultAdapter.java
depends: MCP-0.2, VFSPasswordVault, EnvsAsSecrets
```

### Tasks MCP-7.2 → 7.5 — vault tools

| Task | Tool | Returns | Key call |
|------|------|---------|----------|
| 7.2 | `vault.list` | List of secret *names* (strings) only | `I_KeyStore.keys()` |
| 7.3 | `vault.exists` | `mcp:exists true/false` | `I_KeyStore.has(name)` |
| 7.4 | `vault.status` | `mcp:healthy`, `mcp:backend` ("vfs"/"env") | file-exists + `EnvsAsSecrets.isAvailable()` |
| 7.5 | `vault.rotate` | `mcp:rotated true`, new name only | `Locksmith.generate(name)` → write; record in audit |

---

## S8 — Search & finder

> **IQ classes**: `I_ModelFinder`, `iq-finder` module  
> **Package**: `systems.symbol.finder`

### Task MCP-8.1 — SearchAdapter
```
status: open | sprint: 5 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/SearchAdapter.java
depends: MCP-0.2, I_ModelFinder
```

| Task | Tool | Strategy |
|------|------|----------|
| 8.2 | `search.text` | RDF4J Lucene full-text index; fallback: `FILTER(contains(?label, ?q))` |
| 8.3 | `search.semantic` | Embed query → cosine NN; fallback if no vector index |
| 8.4 | `search.sparql` | Load SPARQL script by IRI from `.iq` catalog; apply user `iq:binding` map |
| 8.5 | `search.geo` | Filter by `iq:lat`/`iq:lon` bounding box; requires GeoLite2 DB in `iq-agentic/db/` |
| 8.6 | `search.suggest` | `SELECT DISTINCT ?label WHERE { ?s rdfs:label ?label . FILTER(strstarts(lcase(?label), lcase(?q))) } LIMIT 10` |

---

## S9 — Lake & ingest

> **IQ classes**: `AbstractLake`, `BootstrapLake`, `Lakes`, `BootstrapVFSLake`, `crawl/`, `ingest/`  
> **Package**: `systems.symbol.lake`

### Task MCP-9.1 — LakeAdapter
```
status: open | sprint: 5 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/adapter/LakeAdapter.java
depends: MCP-0.2, Lakes, AbstractLake
```

| Task | Tool | Key call |
|------|------|----------|
| 9.2 | `lake.list` | `Lakes.all()` → IRI + label per lake |
| 9.3 | `lake.status` | Triple count + `dcterms:modified` from named graph metadata |
| 9.4 | `lake.snapshot` | `COPY GRAPH <lake:IRI> TO GRAPH <lake:IRI:snap:timestamp>` |
| 9.5 | `lake.restore` | `MOVE GRAPH <snap> TO GRAPH <lake>` — requires `iq:confirm true` input |
| 9.6 | `lake.ingest` | URL → `Rio.parse(url.openStream(), …)` → `AbstractLake.ingest(model, graph)` |
| 9.7 | `lake.crawl` | Insert `mcp:CrawlJob` resource into graph; scheduler picks up on next tick |
| 9.8 | `lake.diff` | `CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <a> { ?s ?p ?o } MINUS { GRAPH <b> { ?s ?p ?o } } }` |

---

## S10 — Personas

> **IQ module**: `iq-persona`  
> **Note**: inspect `iq-persona/src` before implementing — resolve actual API surface first.

| Task | Tool | SPARQL / call |
|------|------|--------------|
| MCP-10.2 | `persona.list` | `SELECT ?p ?name ?role WHERE { ?p a iq:Persona ; rdfs:label ?name ; iq:role ?role }` |
| MCP-10.3 | `persona.describe` | `DESCRIBE <personaIRI>` |
| MCP-10.4 | `persona.activate` | `INSERT DATA { GRAPH mcp:session { <sessionIRI> mcp:activePersona <personaIRI> } }` — affects `llm.invoke` context |
| MCP-10.5 | `persona.create` | `INSERT DATA { GRAPH <realmIRI> { <newIRI> a iq:Persona ; rdfs:label ?name ; … } }` |

---

## S11 — Analytics & knowledge base

> **IQ classes**: `Select.java`, `Construct.java` in `systems.symbol.controller.kb`  
> **Modules**: `iq-analytics`, `iq-finder`

| Task | Tool | Delegate |
|------|------|----------|
| MCP-11.2 | `kb.query` | Resolve SPARQL script IRI from catalog → `Select.run(bindings)` |
| MCP-11.3 | `kb.search` | `iq-finder` search service → ranked result model |
| MCP-11.4 | `analytics.graph` | Serve pre-computed analytics named graph as Turtle |
| MCP-11.5 | `analytics.summary` | `analytics.graph` result → Turtle string → `llm.invoke` prompt |
| MCP-11.6 | `analytics.triples` | `SELECT ?g (COUNT(*) AS ?n) WHERE { GRAPH ?g { ?s ?p ?o } } GROUP BY ?g ORDER BY DESC(?n)` |

---

## S12 — Workflows & FSM

> **IQ classes**: `I_StateMachine`, `iq-abstract/fsm/`, `iq-platform/fsm/`  
> **RDF resource**: `iq-run-apis/src/main/resources/assets/fsm.ttl`

| Task | Tool | SPARQL / logic |
|------|------|----------------|
| MCP-12.2 | `workflow.list` | `SELECT ?w ?label WHERE { ?w a iq:Workflow ; rdfs:label ?label }` |
| MCP-12.3 | `workflow.describe` | `CONSTRUCT { ?s ?p ?o } WHERE { ?s iq:to|iq:state|iq:initial ?o . FILTER(?wf = <workflowIRI>) }` |
| MCP-12.4 | `workflow.validate` | `ASK { <fromState> iq:to <toState> }` — returns `xsd:boolean` |
| MCP-12.5 | `workflow.history` | `SELECT ?event ?ts ?from ?to WHERE { GRAPH mcp:audit { ?event mcp:actor <agentIRI> ; mcp:timestamp ?ts ; mcp:from ?from ; mcp:to ?to } } ORDER BY ?ts` |
| MCP-12.6 | `workflow.visualize` | CONSTRUCT states + transitions → map to Mermaid `stateDiagram-v2` text; return `text/plain` |

---

## S13 — Governance (cross-cutting, implemented in MCPAdapterBase)

These are not separate adapters — they are capabilities baked into the base class used by every adapter.

### MCP-13.1 — Audit graph
```
status: open | sprint: 1 (baked into MCPAdapterBase)
named graph: mcp:audit
```
- Write one `mcp:Event` quad per tool call (shape in "Governance contract" section above).
- Auto-prune: Quarkus `@Scheduled(every="24h")` deletes events older than `mcp:auditRetentionDays` (default 90).
- `audit.query` tool: thin wrapper over `fact.sparql.query` scoped to `GRAPH mcp:audit`.

### MCP-13.2 — Quota / rate limiting
```
status: open | sprint: 1
named graph: mcp:quota
```
Policy check:
```sparql
ASK {
  GRAPH mcp:quota {
    <toolIRI> mcp:requestCount ?n ; mcp:limit ?limit .
    FILTER(?n < ?limit)
  }
}
```
Counter reset via `@Scheduled(every="1m")` — SPARQL UPDATE zeroes `mcp:requestCount` for all tools.

### MCP-13.3 — SHACL validation
```
status: open | sprint: 1
file: iq-mcp/src/main/resources/mcp-shapes.ttl
```
- Each tool input has a shape in `mcp-shapes.ttl`.
- `MCPAdapterBase.validateInput(Model)` uses RDF4J `ShaclSail` (wrap `MemoryStore` with `ShaclSail`).
- Violations return `mcp:ValidationReport` (never a raw exception to the client).

### MCP-13.4 — ACL as RDF
```
status: open | sprint: 1
named graph: mcp:policy
```
ACL triple form: `<roleIRI> mcp:allows <toolNameLiteral>`.  
`PolicyChecker.ask(role, toolName, realm)`:
```sparql
ASK {
  GRAPH mcp:policy { <roleIRI> mcp:allows ?t . FILTER(?t = "fact.sparql.query") }
}
```
`realm.policy` tool (S1) lets admins update ACLs at runtime — no redeploy.

### MCP-13.5 — Cost tracking
```
status: open | sprint: 7
named graph: mcp:budget
```
Each tool manifest declares `mcp:cost` (integer quota units). `CostTracker` accumulates per-principal, per-day spend:
```sparql
INSERT {
  GRAPH mcp:budget {
    <principal> mcp:spentToday ?newTotal ; mcp:lastUpdated ?now .
  }
} WHERE {
  GRAPH mcp:budget { <principal> mcp:spentToday ?old }
  BIND(?old + ?cost AS ?newTotal)
}
```
`budget.status` tool returns remaining quota; blocks `invokeTool` when exhausted.

---

## Files to create (complete list)

| File | Module | Phase |
|------|--------|-------|
| `controller/MCPServer.java` | `iq-run-apis` | S0 |
| `mcp/MCPAdapterBase.java` | `iq-mcp` | S0 |
| `mcp/MCPServiceImpl.java` | `iq-mcp` | S0 |
| `mcp/MCPResultImpl.java` | `iq-mcp` | S0 |
| `mcp/MCPToolManifestImpl.java` | `iq-mcp` | S0 |
| `mcp/MCPToolRegistry.java` | `iq-mcp` | S0 |
| `resources/mcp-schema.ttl` | `iq-mcp` | S0 |
| `resources/mcp-shapes.ttl` | `iq-mcp` | S0 |
| `mcp/adapter/RealmAdapter.java` | `iq-mcp` | S1 |
| `mcp/adapter/FactAdapter.java` | `iq-mcp` | S2 |
| `mcp/adapter/StoreAdapter.java` | `iq-mcp` | S3 |
| `mcp/adapter/ActorAdapter.java` | `iq-mcp` | S4 |
| `mcp/adapter/LlmAdapter.java` | `iq-mcp` | S5 |
| `mcp/adapter/TrustAdapter.java` | `iq-mcp` | S6 |
| `mcp/adapter/VaultAdapter.java` | `iq-mcp` | S7 |
| `mcp/adapter/SearchAdapter.java` | `iq-mcp` | S8 |
| `mcp/adapter/LakeAdapter.java` | `iq-mcp` | S9 |
| `mcp/adapter/PersonaAdapter.java` | `iq-mcp` | S10 |
| `mcp/adapter/KBAdapter.java` | `iq-mcp` | S11 |
| `mcp/adapter/WorkflowAdapter.java` | `iq-mcp` | S12 |
| `mcp/governance/QuotaChecker.java` | `iq-mcp` | S13 |
| `mcp/governance/PolicyChecker.java` | `iq-mcp` | S13 |
| `mcp/governance/CostTracker.java` | `iq-mcp` | S13 |
| `mcp/MCPAdapterBaseTest.java` | `iq-mcp` test | S0 |
| `mcp/adapter/FactAdapterIT.java` | `iq-mcp` test | S2 |
| `mcp/adapter/ActorAdapterIT.java` | `iq-mcp` test | S4 |

---

## Definition of done (every task)

A task is `status: done` when ALL of the following are true:

1. **Compiles**: `mvn -DskipTests=false -DskipITs=true package -pl iq-mcp -am` passes.
2. **Unit test**: at least one test per tool using in-memory RDF4J store — no real network calls.
3. **Manifest registered**: `MCPToolRegistry.lookup("tool.name")` returns non-empty.
4. **SHACL shape exists**: shape for this tool's input defined in `mcp-shapes.ttl`.
5. **Audit verified**: test asserts an `mcp:Event` triple exists in the mock audit graph after invoke.
6. **Policy enforced**: test asserts that a call without the required role returns `isSuccess=false`.
7. **SPEC.md updated**: formal input/output schema block added for the tool.
8. **README tool table updated**: tool row added with risk level and min role.
