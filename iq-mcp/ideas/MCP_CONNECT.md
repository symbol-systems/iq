---
id: mcp:pipeline/gateway
type: mcp:MiddlewareSpec
version: "1.0"
realm: iq
status: in-progress
updated: "2026-03-19"
audience: [human, llm, ci]
relates-to: MCP_TODO.md, SPEC.md
---

# MCP Connect — IQ API Gateway for MCP

> **What this document is.**  
> `MCP Connect` is the middleware layer that sits between any MCP client (Claude, curl, CLI, another service) and the IQ tool adapters defined in `MCP_TODO.md`. It behaves like an API gateway: every call passes through an ordered pipeline of pluggable middleware before reaching the adapter — and again on the way back out.

---

## How to use this document

**As a human** — read the architecture section, then pick a middleware task card and implement it. Each card is fully self-contained.

**As an LLM** — paste any task card and ask: _"Implement this middleware for a Java 21 / Quarkus 3 / RDF4J 5 project. The pipeline is a `List<I_MCPPipeline>` applied in declaration order around `MCPAdapterBase.doExecute()`. Middleware config is stored as RDF in `mcp:pipeline` named graph and loaded at startup by `MCPConnectRegistry`."_

**As CI** — grep `status: open` for pending work. Each task's `creates:` line is the exact file path.

**Namespace reference:**

| Prefix | Expansion |
|--------|-----------|
| `mcp:` | `urn:mcp:` |
| `iq:` | `https://symbol.systems/iq/` |
| `xsd:` | `http://www.w3.org/2001/XMLSchema#` |
| `sh:` | `http://www.w3.org/ns/shacl#` |

---

## Architecture: the Connect pipeline

```
MCP client request
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  MCPServer  (iq-mcp /mcp endpoint)                      │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  MCPConnectPipeline                                    │  │
│  │                                                        │  │
│  │  [1] AuthGuard          ← identity / JWT / mTLS        │  │
│  │  [2] TrustZoneGuard     ← IQ trust zone check          │  │
│  │  [3] ACLFilter          ← role × tool allow-list       │  │
│  │  [4] QuotaGuard         ← per-principal rate limit     │  │
│  │  [5] BudgetGuard        ← per-principal cost limit     │  │
│  │  [6] CacheInterceptor   ← read-only tool cache         │  │
│  │  [7] InputTransformer   ← normalise / enrich input     │  │
│  │  [8] ── adapter.doExecute() ──────────────────────     │  │
│  │  [9] OutputTransformer  ← shape / redact output        │  │
│  │  [10] AuditWriter       ← RDF event to mcp:audit       │  │
│  │  [11] MetricsEmitter    ← Micrometer / OpenTelemetry   │  │
│  │                                                         │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
  MCP client response
```

**Key design rules:**
1. **Order matters** — middleware is applied in the declared sequence; config in `mcp:pipeline` named graph controls order via `mcp:order` literal.
2. **Short-circuit** — any middleware can return a final `I_MCPResult` immediately, skipping all downstream middleware and the adapter.
3. **Immutable request** — middleware receives a copy of the input `Model`; mutation is done by `InputTransformer` producing a new `Model`, not by modifying in place.
4. **RDF config** — every middleware is configured entirely via RDF triples in `mcp:pipeline` named graph. No Java properties files, no separate YAML.
5. **Composable proxies** — `ProxyMiddleware` can forward any (or all) tool calls to a remote MCP server, enabling federation and shadow testing.

---

## Core interface

```java
// iq-mcp/src/main/java/systems/symbol/mcp/connect/I_MCPPipeline.java
public interface I_MCPPipeline {

    /** Unique IRI for this middleware instance (matches mcp: graph). */
    IRI getSelf();

    /** Declaration order — lower numbers run first. */
    int getOrder();

    /**
     * Process the call. Invoke chain.proceed(ctx) to continue; return a result
     * directly to short-circuit. Never return null.
     */
    I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws Exception;
}

// MCPCallContext — mutable envelope passed through the chain
public final class MCPCallContext {
    private final IRI self; // tool IRI / name
    private final Model input;           // original; do not mutate
    private Model transformedInput;      // set by InputTransformer
    private final IRI principal;
    private final IRI realm;
    private final Map<String, Object> attributes; // middleware scratch space
    // ... getters / withers
}

// MCPChain — next step in the pipeline
@FunctionalInterface
public interface I_MCPChain {
    I_MCPResult proceed(MCPCallContext ctx) throws Exception;
}
```

---

## RDF configuration shape

All middleware instances declared in `MCPConnectRegistry` loads them at Quarkus startup.

```turtle
# Example: src/main/resources/mcp.ttl  (loaded into mcp:pipeline at boot)

PREFIX mcp:     <urn:mcp:>

mcp:AuthGuard a mcp:Middleware ;
    mcp:class  <bean:systems.symbol.mcp.connect.AuthGuard>;
    mcp:order  10 ;
    mcp:enabled true ;
    mcp:param  [ mcp:key "jwt.issuer" ; mcp:value "https://symbol.systems" ] .

mcp:ACLFilter a mcp:Middleware ;
    mcp:class  <bean:systems.symbol.mcp.connect.ACLFilter>;
    mcp:order  30 ;
    mcp:enabled true ;
    mcp:policyGraph mcp:policy .

mcp:QuotaGuard a mcp:Middleware ;
    mcp:class   <bean:systems.symbol.mcp.connect.QuotaGuard>;
    mcp:order   40 ;
    mcp:enabled true ;
    mcp:quotaGraph mcp:quota ;
    mcp:windowSeconds 60 .

mcp:CacheInterceptor a mcp:Middleware ;
    mcp:class  <bean:systems.symbol.mcp.connect?.CacheInterceptor" ;
    mcp:order  60 ;
    mcp:enabled true ;
    mcp:ttlSeconds 300 ;
    mcp:maxEntries 1000 ;
    mcp:scope "read-only" .          # only cache risk:read tools
```

---

## Middleware catalogue

### MW-1 — AuthGuard
```
status: open | sprint: 1 | order: 10 | risk: high
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/AuthGuard.java
depends: I_MCPPipeline, I_TrustZone (systems.symbol.trust)
```
**Responsibility**: extract and verify the bearer JWT (or API key) from the MCP request envelope. Populates `ctx.principal`.

**Logic:**
```java
String raw = ctx.getInput()
               .filter(s -> s.getPredicate().equals(MCP.bearerToken))
               .map(s -> s.getObject().stringValue())
               .findFirst()
               .orElseThrow(() -> new UnauthorizedException("No bearer token"));

IRI principal = trustZone.verify(raw); // throws if invalid / expired
return chain.proceed(ctx.withPrincipal(principal));
```

**Short-circuit condition**: any exception from `trustZone.verify()` returns `MCPResultImpl.error("auth.failed", cause)` — never calls `chain.proceed`.

**Config params**: `jwt.issuer` (string), `jwt.audience` (string), `allowApiKey` (boolean, default false).

**Done when**: unit test asserts that a tampered JWT is rejected with `isSuccess=false` and no adapter is invoked.

---

### MW-2 — TrustZoneGuard
```
status: open | sprint: 1 | order: 20 | risk: high
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/TrustZoneGuard.java
depends: I_MCPPipeline, I_TrustZone, I_Realm
```
**Responsibility**: verify that the principal's trust zone covers the requested realm. Prevents cross-realm data leaks.

**SPARQL check:**
```sparql
ASK {
  GRAPH mcp:policy {
    <principalIRI> iq:trusts <realmIRI> .
  }
}
```

**Short-circuit**: returns `error("trust.zone.denied")` if the ASK returns false.

**Done when**: test with two realms; principal trusted in realm A cannot access realm B tools.

---

### MW-3 — ACLFilter
```
status: open | sprint: 1 | order: 30 | risk: high
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/ACLFilter.java
depends: I_MCPPipeline, PolicyChecker (from MCP_TODO S13.4)
config: mcp:policyGraph (default mcp:policy)
```
**Responsibility**: enforce role × tool allow-list. A principal must hold a role that is explicitly permitted to call the requested tool.

**SPARQL check (delegated to PolicyChecker):**
```sparql
ASK {
  GRAPH mcp:policy {
    <principalIRI> iq:hasRole ?role .
    ?role mcp:allows ?t .
    FILTER(?t = "fact.sparql.query")
  }
}
```

**Wildcard support**: `?role mcp:allows mcp:allReadTools` expands to all `risk: read` tools from the tool registry.

**Runtime mutability**: `realm.policy` MCP tool (S1) writes into `mcp:policy`; `ACLFilter` re-evaluates on every call from the live graph — no restart required.

**Done when**: test that `writer` role may call `fact.sparql.update` but not `vault.rotate`; `admin` role may call both.

---

### MW-4 — QuotaGuard
```
status: open | sprint: 1 | order: 40 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/QuotaGuard.java
depends: I_MCPPipeline, RDF4J RepositoryConnection on mcp:quota named graph
config: mcp:windowSeconds (default 60), mcp:limit (from tool manifest mcp:rateLimit)
```
**Responsibility**: sliding-window rate limiter. Per principal × tool, using RDF counters.

**Counter update (atomic SPARQL UPDATE inside transaction):**
```sparql
DELETE { GRAPH mcp:quota { ?s mcp:count ?old } }
INSERT { GRAPH mcp:quota { ?s mcp:count ?new ;
                               mcp:windowStart ?windowStart } }
WHERE {
  BIND(<urn:quota:principal:tool> AS ?s)
  OPTIONAL { GRAPH mcp:quota { ?s mcp:count ?old ; mcp:windowStart ?ws } }
  BIND(IF(BOUND(?ws) && ?now - ?ws < ?windowSec, ?old + 1, 1) AS ?new)
  BIND(IF(BOUND(?ws) && ?now - ?ws < ?windowSec, ?ws, ?now) AS ?windowStart)
}
```

**Short-circuit**: if `?new > limit`, return `error("quota.exceeded", { retryAfterSeconds: windowSec - elapsed })`.

**Per-tool overrides**: read from `mcp:toolOverride` triples on the middleware resource (see RDF config example above).

**Done when**: fire `limit + 1` calls with same principal in `windowSeconds`; assert the last call is rejected with `quota.exceeded`.

---

### MW-5 — BudgetGuard
```
status: open | sprint: 3 | order: 45 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/BudgetGuard.java
depends: CostTracker (MCP_TODO S13.5), mcp:budget named graph
config: mcp:budgetGraph (default mcp:budget), mcp:dailyLimit (xsd:int)
```
**Responsibility**: block calls when a principal has exhausted their daily cost budget (`mcp:cost` units from tool manifest).

**Pre-call check:**
```sparql
ASK {
  GRAPH mcp:budget {
    <principalIRI> mcp:spentToday ?spent ; mcp:dailyLimit ?limit .
    FILTER(?spent + ?toolCost <= ?limit)
  }
}
```

**Post-call deduct** (in `MCPChain` after adapter returns success): run `CostTracker.deduct(principal, toolCost)`.

**`budget.status` tool** (see MCP_TODO): lightweight read of `mcp:spentToday` / `mcp:dailyLimit`.

**Done when**: test that a principal with `dailyLimit=10` is blocked after accumulating 10 cost units.

---

### MW-6 — CacheInterceptor
```
status: open | sprint: 2 | order: 60 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/CacheInterceptor.java
depends: I_MCPPipeline
config: mcp:ttlSeconds (default 300), mcp:maxEntries (default 1000), mcp:scope ("read-only"|"all")
```
**Responsibility**: response cache for idempotent (read-only) tool calls. Cache key = `SHA-256(toolName + canonical(inputModel))`.

**Canonical input serialisation** — use RDF4J `Rio.write(inputModel, NTRIPLES)` sorted alphabetically; hash with SHA-256.

**Cache storage options** (configured via `mcp:backend`):

| Backend | `mcp:backend` value | Dependency |
|---------|------------------------|------------|
| In-process heap (default) | `"heap"` | Caffeine (already in Quarkus) |
| RDF named graph | `"rdf"` | `mcp:cache` named graph via `RepositoryConnection` |
| External Redis | `"redis"` | Quarkus Redis extension |

**Flow:**
```
lookup(cacheKey)
  ├─ HIT  → return cached I_MCPResult (sets mcp:cacheHit=true in result model)
  └─ MISS → chain.proceed(ctx) → store result with TTL → return result
```

**Cache invalidation**: write-tool calls (`risk: write`) matching the same realm trigger `cache.invalidate(realm)`. Exposed via `cache.purge` admin tool (see below).

**Done when**: call `fact.sparql.query` twice with identical input; assert second call returns `mcp:cacheHit true` and adapter is invoked only once.

---

### MW-7 — InputTransformer
```
status: open | sprint: 2 | order: 70 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/InputTransformer.java
depends: I_MCPPipeline
config: mcp:transforms (list of transform IRIs, each a mcp:Transform resource)
```
**Responsibility**: normalise, enrich, or rewrite the input `Model` before it reaches the adapter. Transforms are RDF-declared and applied in order.

**Built-in transforms** (each a `mcp:TransformType`):

| Transform IRI | Effect |
|---------------|--------|
| `mcp:NormaliseIRIs` | Expand CURIEs → full IRIs using realm namespace prefixes |
| `mcp:ResolveBindings` | Replace `iq:binding` shortcuts with full SPARQL VALUES blocks |
| `mcp:InjectRealm` | Add `iq:realm <realmIRI>` triple if absent |
| `mcp:StripSecrets` | Remove any literal matching `mcp:secretPattern` regex |
| `mcp:TemplateExpand` | Render `rdf:value` Mustache templates with `iq:binding` vars |
| `mcp:SchemaDefault` | Inject SHACL `sh:defaultValue` for missing optional properties |

**Custom transforms**: implement `I_MCPInputTransform` and declare in `mcp-connect.ttl`:
```turtle
mcp:MyTransform a mcp:Transform ;
    mcp:type mcp:Custom ;
    mcp:class "com.example.MyTransform" ;
    mcp:order 1 .
```

**Done when**: test that a CURIE `iq:Agent` in input is expanded to `https://symbol.systems/iq/Agent` before reaching the adapter.

---

### MW-8 — OutputTransformer
```
status: open | sprint: 2 | order: 80 | risk: low
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/OutputTransformer.java
depends: I_MCPPipeline
```
**Responsibility**: reshape, filter, or redact the adapter's output `Model` before it reaches the client.

**Built-in transforms:**

| Transform IRI | Effect |
|---------------|--------|
| `mcp:RedactSecrets` | Remove literals matching `mcp:secretPattern` regex — defence in depth |
| `mcp:ProjectFields` | Keep only `mcp:allowedPredicate` triples (allowlist projection) |
| `mcp:AddProvenance` | Inject `prov:wasGeneratedBy`, `prov:generatedAtTime` triples |
| `mcp:SerialiseAs` | Convert payload to requested RDF serialisation (Turtle / N-Quads / JSON-LD) |
| `mcp:PaginateResults` | Slice large result models to `mcp:pageSize` triples; add `mcp:nextPageToken` |

**Done when**: test that a result model containing a literal matching `".*secret.*"` is scrubbed before the response leaves the pipeline.

---

### MW-9 — ProxyMiddleware
```
status: open | sprint: 3 | order: 50 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/ProxyMiddleware.java
depends: I_MCPPipeline, MCP Java SDK client (io.modelcontextprotocol.sdk:mcp:0.17.2)
config: mcp:upstreamUrl, mcp:toolPattern (regex), mcp:mode ("forward"|"shadow"|"fanout")
```
**Responsibility**: forward matching tool calls to a remote MCP server instead of (or in addition to) the local adapter.

**Modes:**

| Mode | Behaviour |
|------|-----------|
| `forward` | Replace local execution; call remote, return its result |
| `shadow` | Call both local and remote concurrently; return local result; log delta |
| `fanout` | Call multiple upstreams; merge result models; return union |

**Config example:**
```turtle
mcp:RemoteIQProxy a mcp:Middleware ;
    mcp:class      "systems.symbol.mcp.connect.ProxyMiddleware" ;
    mcp:order      50 ;
    mcp:enabled    true ;
    mcp:mode       "forward" ;
    mcp:toolPattern "fact\\..*" ;           # forward all fact.* tools
    mcp:upstreamUrl "https://remote.symbol.systems/mcp" ;
    mcp:timeout    10000 ;
    mcp:authHeader [ mcp:key "Authorization" ; mcp:value "{mcp:vaultSecret:REMOTE_TOKEN}" ] .
```

**Vault secret interpolation**: `{mcp:vaultSecret:NAME}` tokens in RDF string literals are resolved from `VFSPasswordVault` at startup — the secret value is never stored in the graph.

**Done when**: unit test with WireMock upstream; verify call to matching tool is forwarded, response is returned with `mcp:proxied true` attribute in result.

---

### MW-10 — CircuitBreaker
```
status: open | sprint: 3 | order: 55 | risk: medium
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/CircuitBreakerMiddleware.java
depends: I_MCPPipeline, Quarkus SmallRye Fault Tolerance (@CircuitBreaker)
config: mcp:failureThreshold (default 5), mcp:successThreshold (default 2), mcp:delaySeconds (default 30)
```
**Responsibility**: wrap adapter or proxy calls with a circuit breaker — open after `failureThreshold` consecutive failures; half-open after `delaySeconds`; close after `successThreshold` consecutive successes.

**States stored in `mcp:pipeline` named graph** (so state survives pod restarts when using a shared RDF4J store):
```turtle
mcp:CircuitBreaker-fact.sparql.query
    mcp:state     "OPEN" ;          # CLOSED | OPEN | HALF_OPEN
    mcp:failCount 5 ;
    mcp:lastFailure "2026-03-19T12:00:00Z"^^xsd:dateTime .
```

**Short-circuit when OPEN**: return `error("circuit.open", { retryAfter: delaySeconds })`.

**Done when**: mock adapter throwing RuntimeException; verify breaker opens after 5 failures; verify it half-opens after delay.

---

### MW-11 — AuditWriter
```
status: open | sprint: 1 | order: 990 | risk: none
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/AuditWriter.java
depends: I_MCPPipeline, mcp:audit named graph, RepositoryConnection
```
**Responsibility**: write one `mcp:Event` quad per call to `mcp:audit` named graph. Always runs last (order 990) regardless of success or failure. Never short-circuits.

**Quad shape** (identical to the governance contract in MCP_TODO):
```turtle
[] a mcp:Event ;
   mcp:tool        "fact.sparql.query" ;
   mcp:realm       <realmIRI> ;
   mcp:principal   <principalIRI> ;
   mcp:timestamp   "…"^^xsd:dateTime ;
   mcp:durationMs  42^^xsd:long ;
   mcp:cost        1^^xsd:int ;
   mcp:success     true^^xsd:boolean ;
   mcp:middlewarePath "AuthGuard→ACLFilter→QuotaGuard→FactAdapter" .
```

`mcp:middlewarePath` records the actual middleware chain traversed — useful for debugging proxied or cached calls.

**Done when**: every test that exercises the pipeline finds one `mcp:Event` in the mock audit graph.

---

### MW-12 — MetricsEmitter
```
status: open | sprint: 4 | order: 995 | risk: none
creates: iq-mcp/src/main/java/systems/symbol/mcp/connect/MetricsEmitter.java
depends: I_MCPPipeline, Quarkus Micrometer extension, OpenTelemetry
```
**Responsibility**: emit structured telemetry. Always runs last (order 995).

**Micrometer counters / timers:**

| Metric | Type | Tags |
|--------|------|------|
| `mcp.calls.total` | Counter | `tool`, `realm`, `result` (success/error) |
| `mcp.call.duration` | Timer | `tool`, `realm` |
| `mcp.cache.hits` | Counter | `tool` |
| `mcp.quota.rejected` | Counter | `tool`, `principal` |
| `mcp.circuit.open` | Gauge | `tool` |

**OpenTelemetry span** wraps each call with `tool.name`, `mcp.principal`, `mcp.realm` attributes.

**Done when**: Quarkus dev mode dev-ui `/q/metrics` shows `mcp_calls_total` counter incrementing per call.

---

---

## Admin tools exposed via MCP

The gateway itself is manageable through MCP tools (registered in `I_MCPService` under `connect.*`).

| Tool | Operation | Role | Notes |
|------|-----------|------|-------|
| `connect.middleware.list` | List active middleware + order + enabled flag | reader | Reads `mcp:pipeline` graph |
| `connect.middleware.enable` | Toggle `mcp:enabled true/false` | admin | Live; no restart |
| `connect.middleware.order` | Change `mcp:order` for a middleware | admin | Triggers pipeline re-sort |
| `connect.acl.allow` | Add `<role> mcp:allows <tool>` triple | admin | Writes to `mcp:policy` graph |
| `connect.acl.deny` | Remove allow triple | admin | |
| `connect.acl.list` | List all role × tool ACL entries | reader | `SELECT` on `mcp:policy` |
| `connect.quota.status` | Current counter for principal × tool | reader | Reads `mcp:quota` graph |
| `connect.quota.reset` | Zero counter for principal × tool | admin | |
| `connect.cache.stats` | Hit/miss counts, entry count, TTL | reader | |
| `connect.cache.purge` | Invalidate cache (all or by realm/tool) | admin | |
| `connect.circuit.status` | Circuit state for each tool | reader | Reads `mcp:pipeline` |
| `connect.circuit.reset` | Force circuit CLOSED | admin | |
| `connect.proxy.list` | Configured upstream proxies | reader | |

---

## Files to create

| File | Module | Phase |
|------|--------|-------|
| `mcp/connect/I_MCPPipeline.java` | `iq-mcp` | S0 |
| `mcp/connect/MCPCallContext.java` | `iq-mcp` | S0 |
| `mcp/connect/MCPChain.java` | `iq-mcp` | S0 |
| `mcp/connect/MCPConnectPipeline.java` | `iq-mcp` | S0 |
| `mcp/connect/MCPConnectRegistry.java` | `iq-mcp` | S0 |
| `resources/mcp-connect.ttl` | `iq-mcp` | S0 |
| `mcp/connect/AuthGuard.java` | `iq-mcp` | MW-1 |
| `mcp/connect/TrustZoneGuard.java` | `iq-mcp` | MW-2 |
| `mcp/connect/ACLFilter.java` | `iq-mcp` | MW-3 |
| `mcp/connect/QuotaGuard.java` | `iq-mcp` | MW-4 |
| `mcp/connect/BudgetGuard.java` | `iq-mcp` | MW-5 |
| `mcp/connect/CacheInterceptor.java` | `iq-mcp` | MW-6 |
| `mcp/connect/InputTransformer.java` | `iq-mcp` | MW-7 |
| `mcp/connect/OutputTransformer.java` | `iq-mcp` | MW-8 |
| `mcp/connect/ProxyMiddleware.java` | `iq-mcp` | MW-9 |
| `mcp/connect/CircuitBreakerMiddleware.java` | `iq-mcp` | MW-10 |
| `mcp/connect/AuditWriter.java` | `iq-mcp` | MW-11 |
| `mcp/connect/MetricsEmitter.java` | `iq-mcp` | MW-12 |
| `mcp/connect/admin/ConnectAdminAdapter.java` | `iq-mcp` | admin tools |

---

## Integration with MCPAdapterBase

`MCPConnectPipeline` wraps `MCPAdapterBase.doExecute()` — the existing governance pipeline in `MCPAdapterBase` (SHACL validation, manifest lookup, etc.) runs **inside** the Connect pipeline, at step 8. This means Connect middleware adds an outer ring of cross-cutting concerns without touching adapter code.

```
MCPConnectPipeline.invoke(toolName, input)
  │
  │  [1..7] pre-middleware
  │
  ├── MCPAdapterBase.invoke(toolName, input)   ← existing governance pipeline
  │       resolveManifest → validateInput → checkPolicy
  │       → checkQuota → doExecute → audit
  │
  [9..11] post-middleware
```

**Override**: an adapter may declare `mcp:bypassMiddleware mcp:CacheInterceptor` in its manifest RDF to skip specific middleware (e.g., `actor.trigger` should never be cached).

```turtle
mcp:ActorTriggerManifest a mcp:ToolManifest ;
    mcp:name "actor.trigger" ;
    mcp:bypass mcp:CacheInterceptor ;
    mcp:bypass mcp:QuotaGuard .    # actor calls are quota-free in this config
```

---

## Ordering guide

| Order range | Purpose |
|-------------|---------|
| 1 – 19 | Identity / auth (must run first to populate `ctx.principal`) |
| 20 – 39 | Trust zone + realm checks |
| 40 – 49 | Access control (ACL, role checks) |
| 50 – 59 | Proxy / federation (may short-circuit before quota check on remote) |
| 60 – 69 | Rate / quota guards |
| 70 – 79 | Budget guards |
| 80 – 89 | Cache (after auth but before adapter) |
| 90 – 99 | Input transformation |
| 100 | Adapter executes |
| 101 – 899 | Output transformation |
| 900 – 989 | Circuit breaker state updates |
| 990 | Audit writer |
| 991 – 999 | Metrics / tracing |

---

## Definition of done (every middleware)

A middleware is `status: done` when ALL of the following are true:

1. **Compiles**: `mvn -DskipTests=false -DskipITs=true package -pl iq-mcp -am` passes.
2. **Unit test — happy path**: pipeline with this middleware in isolation; verify call proceeds to adapter and result is returned.
3. **Unit test — short-circuit**: trigger the blocking condition; verify adapter is NOT invoked and result contains appropriate `mcp:error` literal.
4. **Config loaded from RDF**: `MCPConnectRegistry` can load the middleware from a `mcp-connect.ttl` classpath resource — no hardcoded wiring.
5. **Order respected**: instantiate three middleware out-of-order; verify they run in `mcp:order` sequence.
6. **AuditWriter always runs**: even when upstream middleware short-circuits, `AuditWriter` executes and writes `mcp:Event`.
7. **No secret leakage**: grep test output and audit graph for literal values matching `.*secret.*` / `.*password.*` — must be empty.
