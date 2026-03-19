# iq-kernel — Specification & Architecture

> **Status:** Draft — March 2026  
> **Scope:** Refactoring to isolate common runtime components shared by REST APIs, MCP servers, CLI and event hubs.

---

## 1. Motivation

The IQ codebase has grown organically around four runtime surfaces:

| Runtime surface | Module   | Bootstrap class |
|-----------------|------------------|-------------------------|
| REST API| `iq-run-apis`| `RealmPlatform` (Quarkus `@Singleton`) |
| MCP server  | `iq-mcp` | `MCPServerBuilder` + Quarkus CDI   |
| CLI | `iq-cli` | `CLIContext` + Picocli  |
| Event hub   | `iq-camel`   | `IQRouters` (Apache Camel)  |

Each surface reimplements the same concerns:

- **Realm/workspace boot** — `RealmPlatform.onStart`, `CLIContext.init`, and the Camel route builder all open a `RealmManager`, wire a `RepositoryConnection`, load secrets, and start a `ThreadManager`.
- **Middleware pipeline** — `iq-mcp` has a proper `MCPConnectPipeline` / `I_MCPPipeline` chain; `iq-run-apis` uses ad-hoc `RealmAPI.authenticate()` + `GuardedAPI`; the CLI has none; the Camel module has `IQRoutePolicy`.
- **Command / request model** — `AbstractCLICommand` (CLI), `RealmAPI` (REST), `I_MCPTool` (MCP), `AbstractCamelRDFProcessor` (Camel) all represent "a unit of work dispatched on a live realm". They share no common type.
- **Error model** — `CLIException`, `OopsException`, `APIException`, `PlatformException`, `StateException`, `BudgetException`, `MCPException` are parallel exception hierarchies with no common root.
- **Output / result model** — REST has `I_Response` / `OopsResponse` / `RDFResponse`; MCP has `MCPResult` / `I_MCPResult`; CLI prints to `System.out`; Camel exchanges carry results in message bodies. No shared `KernelResult`.

`iq-kernel` extracts a **runtime-agnostic kernel layer** that all four surfaces depend on, eliminating this duplication without introducing Quarkus, Picocli, MCP SDK, or Camel dependencies.

---

## 2. Current Module Dependency Graph

```
iq-abstract  (pure interfaces, no deps)
└── iq-rdf4j (RDF4J wrappers)
└── iq-platform   (domain services: realm, lake, LLM, trust, secrets, FSM, agent, intent)
├── iq-run-apis  (Quarkus REST runtime)
├── iq-mcp   (MCP server)
├── iq-cli   (Picocli CLI)
└── iq-agentic   (fleet / budget / avatars)
└── iq-camel  (event routing)
```

**Problems with this graph:**

1. `RealmPlatform` (pure Java) lives in `iq-run-apis` but its `RealmManager` + `ThreadManager` bootstrapping is needed by every surface. The CLI re-creates equivalent logic in `CLIContext`.
2. `AgentService` (pure Java, no Quarkus import) lives in `iq-run-apis`; it belongs lower.
3. `Workspace` (referenced by `CLIContext`) lives in `iq-platform` but contains no RDF4J — it could live in `iq-kernel`.
4. `iq-platform` carries a heavy dependency set (Tika, Groovy, Nashorn, AWS SDK, Bouncy Castle, OkHttp) that CLI/MCP don't fully need.

---

## 3. Target Module Dependency Graph

```
iq-abstract  (unchanged — pure interfaces, zero deps)
└── iq-kernel(NEW — runtime-agnostic kernel: realm boot, pipeline, command, result, events)
└── iq-rdf4j  (unchanged)
└── iq-platform   (trimmed — domain services only)
├── iq-run-apis   (Quarkus REST)
├── iq-mcp(MCP server)
├── iq-cli(Picocli CLI)
├── iq-agentic(fleet)
└── iq-camel  (event routing)
```

`iq-kernel` sits between `iq-abstract` and `iq-rdf4j`/`iq-platform`. It carries **no framework annotations** and no heavy runtime dependencies. Each surface module then owns only what is truly framework-specific.

---

## 4. iq-kernel Responsibilities

### 4.1 Kernel Lifecycle

```
systems.symbol.kernel
  I_Kernel  — start / stop / getSelf / getContext
  KernelContext — live state (realm, secrets, threads ...)
  KernelBuilder — fluent boot API used by each surface
  KernelException   — root unchecked exception
```

`KernelBuilder` replaces the scattered boot logic currently in:
- `RealmPlatform.onStart()` (iq-run-apis)
- `CLIContext.init()` (iq-cli)
- Quarkus CDI producers in iq-mcp

```java
// intended usage (all surfaces)
KernelContext ctx = KernelBuilder.create()
.withSelf(I_Self.self())
.withSecrets(new EnvsAsSecrets())
.withRealm(realmHome)
.build();
```

### 4.2 Pipeline / Middleware

**Correction from initial draft** — the original spec proposed moving `MCPConnectPipeline`, `I_MCPPipeline`, `MCPChain`, and their `*Middleware` implementations into `iq-kernel`. This is wrong for two reasons:

1. **Type coupling.** `I_MCPPipeline` and all five `*Middleware` implementations are typed on `MCPCallContext` and `MCPException`. Moving them to `iq-kernel` either forces those MCP SDK–dependent types into the kernel, or requires a painful generic wrapping layer that adds complexity without value.

2. **Category error for `IQRoutePolicy`.** `IQRoutePolicy` implements `org.apache.camel.spi.RoutePolicy`, which is a *route lifecycle* interface (init / start / stop / suspend / resume + `onExchangeBegin`). It is not a chain-of-responsibility pipeline — it is a Camel-specific ACL enforcement hook wired to exchange events. Conflating it with a pipeline abstraction is incorrect.

**What belongs in iq-kernel — structural pattern + shared context only:**

```
systems.symbol.kernel.pipeline
  I_Middleware<CTX>   — process(ctx, I_Chain<CTX>): void  (or short-circuit)
  I_Chain<CTX>— proceed(ctx)  (functional interface — the continuation)
  I_Pipeline<CTX> — build(List<I_Middleware<CTX>>): I_Chain<CTX>
  KernelCallContext   — shared call envelope (see below)
```

`KernelCallContext` carries the cross-cutting fields that are meaningful for every surface:

```java
// systems.symbol.kernel.pipeline.KernelCallContext
public class KernelCallContext {
private final String  traceId;  // UUID per request
private final Instant startTime;
private IRI   realm;// set by auth middleware
private Stringprincipal;// set by auth middleware
private boolean   authorised;
private final Map<String,Object> attributes; // extensible bag
}
```

**What stays in each surface module:**

| Surface concern | Type | Stays in |
|---|---|---|
| MCP pipeline runner | `MCPConnectPipeline` | `iq-mcp` |
| MCP middleware SPI | `I_MCPPipeline`, `MCPChain` | `iq-mcp` |
| MCP call envelope | `MCPCallContext` | `iq-mcp` (extends `KernelCallContext`) |
| MCP middleware impls | `AuthGuard/ACLFilter/QuotaGuard/AuditWriter/SparqlSafety` | `iq-mcp` |
| REST auth | `RealmAPI.authenticate()`, `GuardedAPI` | `iq-run-apis` |
| Camel route ACL | `IQRoutePolicy` | `iq-camel` (not a pipeline) |

The relationship from `iq-mcp`'s perspective:

```java
// MCPCallContext wraps the shared kernel context
public final class MCPCallContext extends KernelCallContext {
private final String toolName;  // MCP-specific
private final Map<String,Object> rawInput; // immutable MCP input
// ... mcp.* attribute keys
}

// MCPConnectPipeline uses I_Pipeline<MCPCallContext> from kernel
public class MCPConnectPipeline implements I_Pipeline<MCPCallContext> { ... }

// I_MCPPipeline delegates to I_Middleware<MCPCallContext>
public interface I_MCPPipeline extends I_Middleware<MCPCallContext> { ... }
```

This means `iq-mcp`'s existing five middlewares require no code movement — they simply confirm to the structural contract already present. The kernel test suite can exercise `I_Pipeline` using a lightweight `TestCallContext extends KernelCallContext` without touching any MCP type.

**`IQRoutePolicy` refactoring path** (separate from pipeline): introduce `I_AccessPolicy` in `iq-kernel` as a single-method interface (`boolean allows(KernelCallContext)`) and have `IQRoutePolicy.onExchangeBegin` delegate to it. This resolves the duplication without misclassifying the Camel hook as a pipeline stage.

### 4.3 Command / Request Model

A transport-agnostic work unit, analogous to CLI's `AbstractCLICommand`, REST's `RealmAPI` handler, or MCP's `I_MCPTool`.

```
systems.symbol.kernel.command
  I_KernelCommand — execute(KernelRequest): KernelResult
  KernelRequest   — subject (IRI), params (Bindings), caller (IRI), realm
  KernelResult<T> — ok(value) | error(KernelException)
  AbstractKernelCommand   — wires KernelContext; implements I_KernelCommand
```

Each surface adapter bridges to the kernel command:

| Surface  | Adapter bridge|
|--------------|---------------------------------------|
| REST API | `RealmAPI` delegates to `KernelCommand` via `AgentService` |
| MCP  | `I_MCPTool.execute()` → `KernelCommand.execute()` |
| CLI  | `AbstractCLICommand.call()` → `KernelCommand.execute()` |
| Event hub| Camel processor → `KernelCommand.execute()` |

### 4.4 Event Hub Abstraction

Decouples event producers/consumers from Camel implementation. Allows the same event semantics to be tested without a running Camel context, and used via pub/sub in non-Camel deployments (e.g. Vert.x in Quarkus).

```
systems.symbol.kernel.event
  I_EventHub  — publish(KernelEvent), subscribe(IRI topic, I_EventSink)
  I_EventSource   — produces KernelEvents
  I_EventSink — consumes KernelEvents
  KernelEvent — topic (IRI), payload (Model | Bindings | String), timestamp
  SimpleEventHub  — in-memory broadcast (testing / CLI)
```

The Camel module provides `CamelEventHub implements I_EventHub` by wrapping `ProducerTemplate`. `iq-run-apis` provides a `VertxEventHub` using Quarkus event bus. Both satisfy the same interface; tests use `SimpleEventHub`.

### 4.5 Error Model

Single exception hierarchy replaces six parallel ones:

```
KernelException (RuntimeException)
  ├── KernelBootException   — replaces PlatformException, CLIException (boot phase)
  ├── KernelAuthException   — replaces OopsException(UNAUTHORIZED/FORBIDDEN)
  ├── KernelCommandException— replaces StateException, APIException (execution phase)
  ├── KernelSecretException — replaces SecretsException
  └── KernelBudgetException — replaces BudgetException
```

Each surface maps its own exception type to a KernelException at the boundary.

### 4.6 Workspace

Move `Workspace` from `iq-platform` into `iq-kernel`. `Workspace` manages the `.iq/` directory tree, the VFS-backed repository selection, and `I_Self` resolution — concerns needed by both CLI and API before any framework starts up.

```
systems.symbol.kernel.workspace
  Workspace   — resolves $IQ_HOME, opens repository, provides I_Self
  WorkspaceProbe  — reads build.properties / MANIFEST.MF for version
```

---

## 5. Package Layout

```
iq-kernel/
└── src/main/java/systems/symbol/kernel/
├── I_Kernel.java
├── KernelContext.java
├── KernelBuilder.java
├── KernelException.java   — root
├── KernelBootException.java
├── KernelAuthException.java
├── KernelCommandException.java
├── KernelSecretException.java
├── KernelBudgetException.java
├── command/
│   ├── I_KernelCommand.java
│   ├── AbstractKernelCommand.java
│   ├── KernelRequest.java
│   └── KernelResult.java
├── pipeline/
│   ├── I_Middleware.java— process(CTX, I_Chain<CTX>)
│   ├── I_Chain.java — proceed(CTX)  [functional interface]
│   ├── I_Pipeline.java  — build(List<I_Middleware<CTX>>): I_Chain<CTX>
│   ├── I_AccessPolicy.java  — allows(KernelCallContext): boolean
│   └── KernelCallContext.java   — shared envelope (traceId, principal, realm, authorised)
│   NOTE: *Middleware implementations stay in iq-mcp (typed on MCPCallContext)
├── event/
│   ├── I_EventHub.java
│   ├── I_EventSource.java
│   ├── I_EventSink.java
│   ├── KernelEvent.java
│   └── SimpleEventHub.java
└── workspace/
├── Workspace.java  — moved from iq-platform
└── WorkspaceProbe.java
```

---

## 6. Refactoring Plan

### Phase 1 — Extract (no breaking changes)

1. Create `iq-kernel` Maven module (parent = `iq-parent`, depends only on `iq-abstract`, `slf4j-api`, `rdf4j-model-api`).
2. Move `Workspace` from `iq-platform` to `iq-kernel`; add a deprecated alias in `iq-platform`.
3. Introduce `KernelException` hierarchy; leave existing exceptions as subclasses for now (type alias pattern).
4. Add `I_Kernel`, `KernelContext`, `KernelBuilder` with no-op implementations; wire into `CLIContext` as a first consumer.
5. Add `I_Middleware<CTX>`, `I_Chain<CTX>`, `I_Pipeline<CTX>`, `KernelCallContext`, `I_AccessPolicy`; have `MCPCallContext` extend `KernelCallContext`; have `MCPConnectPipeline` implement `I_Pipeline<MCPCallContext>`. No middleware implementations move — they stay in `iq-mcp`.
6. Add `I_EventHub`, `KernelEvent`, `SimpleEventHub`; wire `IQRouters` in `iq-camel` to implement `I_EventHub`.

### Phase 2 — Consolidate

7. Move `AgentService` from `iq-run-apis` into `iq-platform` (it has no Quarkus dependency).
8. Extract Quarkus-specific lifecycle from `RealmPlatform` into a thin `@Singleton` shim; move the pure Java boot logic to `KernelBuilder`.
9. Migrate `AbstractCLICommand` to extend `AbstractKernelCommand`; remove `CLIException` in favour of `KernelCommandException`.
10. Confirm `MCPConnectPipeline` implements `I_Pipeline<MCPCallContext>`; have `I_MCPPipeline` extend `I_Middleware<MCPCallContext>` — no code movement, just interface alignment. Introduce `I_AccessPolicy` in kernel and wire `IQRoutePolicy.onExchangeBegin` to delegate to it, separating the Camel route lifecycle hook from the authorization logic.
11. Add `KernelRequest` / `KernelResult` as the shared contract; surface adapters (REST, MCP, CLI, Camel) wrap to/from transport-specific types at their boundaries.

### Phase 3 — Tighten iq-platform

12. Remove heavy ingest dependencies (Tika, Groovy, AWS S3 SDK) from the iq-platform POM; place them in a new `iq-platform-extras` module (or keep as optional/provided). This makes the base dependency usable by lightweight surfaces without pulling in 100 MB of transitives.
13. Declare final module separation: each surface module may only import `iq-kernel`, `iq-platform`, and its own framework (Quarkus / Picocli / MCP SDK / Camel). Cross-surface imports are forbidden.

---

## 7. Maven POM Sketch

```xml
<artifactId>iq-kernel</artifactId>
<name>IQ Kernel</name>
<description>Runtime-agnostic kernel: lifecycle, pipeline, command, event hub, workspace.</description>

<dependencies>
  <!-- interfaces only -->
  <dependency>
<groupId>systems.symbol</groupId>
<artifactId>iq-abstract</artifactId>
<version>${project.version}</version>
  </dependency>

  <!-- RDF model types (no repository, no SPARQL) -->
  <dependency>
<groupId>org.eclipse.rdf4j</groupId>
<artifactId>rdf4j-model-api</artifactId>
<version>${rdf4j.version}</version>
  </dependency>

  <!-- VFS for Workspace -->
  <dependency>
<groupId>org.apache.commons</groupId>
<artifactId>commons-vfs2</artifactId>
  </dependency>

  <!-- Logging API only (no impl) -->
  <dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-api</artifactId>
  </dependency>
</dependencies>
```

No Quarkus, no Picocli, no MCP SDK, no Camel, no Groovy, no Tika, no AWS.

---

## 8. Key Design Rules

1. **iq-kernel has no framework annotations.** No `@Singleton`, `@ApplicationScoped`, `@Command`, `@Tool`. Those live exclusively in the surface module.
2. **All surfaces depend on iq-kernel, not on each other.** CLI, REST, MCP, Camel import kernel types; they never import from each other.
3. **KernelContext is immutable after boot.** It is created once by `KernelBuilder`, passed through the command chain; it is never modified by a controller or command handler.
4. **One error hierarchy.** `KernelException` and its subtypes are the only checked-equivalent exceptions crossing module boundaries. Each surface catches them and converts to its own error format (HTTP status, MCP error object, exit code, Camel exchange failure).
5. **I_EventHub is always in-memory unless a surface provides an implementation.** Tests and CLI sessions use `SimpleEventHub`. Quarkus and Camel provide richer implementations via their own modules.
6. **RDF-first config.** `KernelBuilder` reads `.iq/` TTL files for realm config (mirrors the existing `RDFConfigFactory` / `Poke` pattern). No new YAML or properties formats.

---

## 9. What iq-kernel is NOT

- Not a new application server. `RealmPlatform` and Quarkus stay in `iq-run-apis`.
- Not a replacement for `iq-platform`. Domain services (LLM, lake ingestors, trust/PKI, FSM) stay in `iq-platform`.
- Not a framework shim. No CDI, no JAX-RS, no WebSocket here.
- Not a dependency aggregator. `iq-kernel` must have a lightweight POM footprint so that `iq-cli` ceases to transitively pull Tika, AWS SDK, Groovy, and all Quarkus extensions.

---

## 10. Open Questions

| # | Question | Options |
|---|----------|---------|
| 1 | Should `iq-rdf4j` depend on `iq-kernel` or remain independent? | Option A: `iq-kernel` depends on `rdf4j-model-api` only; `iq-rdf4j` remains separate and both feed into `iq-platform`. Option B: merge `iq-kernel` and `iq-rdf4j` into a single low-level module. **Preferred: Option A** — keeps RDF store implementation concerns separate from kernel lifecycle. |
| 2 | Where does `Workspace.java` currently live (class not found in `iq-platform` tree at time of writing)? | Find canonical location; if it is in `iq-rdf4j` or `iq-trusted`, move to `iq-kernel.workspace`. |
| 3 | `iq-platform` dependency on `quarkus-tika` (in POM) — is it used at runtime or only test-time? | Audit and move to `iq-platform-extras` or keep with `<optional>true</optional>`. |
| 4 | Should `SimpleEventHub` support ordered delivery guarantees for the test surface? | Use a `LinkedBlockingQueue` internally; single-threaded dispatch by default. Opt in to async with thread pool at surface wiring time. |
