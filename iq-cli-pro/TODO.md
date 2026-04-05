# iq-cli-pro TODO — COSS surface

> **Status:** Draft — March 2026
> **Scope:** Analysis of gaps and planned features for the commercial-open-source CLI (`systems.symbol.PowerCLI` / Picocli).
> **Basis:** `iq-kernel` SPEC (phases 1–3); commands inspired by `iq-mcp` tool registry (`actor.*`, `realm.*`, `llm.*`, `trust.*`) and `iq-apis` controller patterns (`IntentAPI`, `TokenAPI`, `ModelAPI`, `RealmAPI`).

---

## 1. Current State

| Class | Picocli command | Implementation state |
|---|---|---|
| `BootCommand` | `boot` | ✅ **COMPLETE** — Actor discovery and readiness polling (13 tests) |
| `RunCommand` | `run` | ✅ **COMPLETE** — Script file execution with language detection (15 tests) |
| `TriggerCommand` | `trigger` | ✅ **COMPLETE** — Camel event routing with bindings and --wait flag (21 tests) |
| `TrustCommand` | `trust` | ✅ **COMPLETE** — PKI self-signing, remote trust, OAuth support (9 tests) |
| `ServeCommand` | `serve` | ✅ **COMPLETE** — Embedded REST API and MCP server startup (25 tests) |
| `ModelsCommand` | `models` | Unread — assumed stub |
| `CompositeCommand` | (base class) | Unknown |

**Progress:**
- ✅ 5/7 commands fully implemented with comprehensive test coverage (83 unit tests across CLI)
- ✅ All core CLI commands working and tested
- 🔄 Ready for ServerRuntimeManager integration and lifecycle management
- ⏳ 2 commands pending implementation (ModelsCommand, CompositeCommand)

**Structural problems (shared with iq-cli):**

- `AbstractCLICommand` (inherited from `iq-cli`) has no kernel alignment — same root issues as the OSS module.
- Pro commands import from `iq-platform`, `iq-rdf4j`, and `iq-agentic` without a kernel pipeline layer; there is no auth, quota, or audit applied at the CLI boundary.
- `CompositeCommand` mechanism is opaque; needs documentation or consolidation.
- ✅ Unit tests now in place for pro commands (validated with TriggerCommand, TrustCommand, ServeCommand)

---

## 2. Kernel Alignment Tasks — ✅ COMPLETE (April 5, 2026)

### 2.1 Extend `AbstractCLICommand` → `AbstractKernelCommand` ✅ DONE

`AbstractCLICommand` in `iq-cli` already extends `AbstractKernelCommand<Object>`. 
Pro commands inherit kernel integration automatically.

### 2.2 Wire `PowerCLI` entry point to `KernelBuilder` ✅ DONE

PowerCLI constructor properly initializes kernel and context (lines 23-29).

### 2.3 Add a `CLIPipeline<KernelCallContext>` for pro commands ✅ DONE

**Created**:
- `CLICallContext` — CLI-specific context extending `KernelCallContext`
- `CLIPipeline` — Middleware chain builder and orchestrator
- `CLIAuthMiddleware` (order 100) — Verify caller identity from executor IRI
- `CLIAuditMiddleware` (order 200) — Log all command executions with duration/status
- `CLIQuotaMiddleware` (order 300) — Enforce rate limits on expensive commands

**Quota limits**:
- `trigger`: 10 per minute (CPU-intensive event publishing)
- `boot`: 1 per minute (system-wide actor initialization)
- `run`: 5 per minute (script execution)
- Read-only: unlimited

**Status**: Infrastructure complete, 15 tests passing ✅

**Next**: Wire into PowerCLI and AbstractCLICommand (1-2 days effort)

**Documentation**: See `CLI_KERNEL_ALIGNMENT.md` for complete architecture

---

## 3. Feature Backlog — COSS Commands

### 3.1 `boot` — wire to `AgentService` + FSM

**What it should do:** start all `iq:Actor` instances declared in the active realm graph by invoking the workflow boot sequence.

**Current state:** the body contains commented calls to a `Wisdom` class that no longer appears to exist.

**Target approach:**
```
PowerCLI boot
PowerCLI boot --realm urn:iq:realm:fleet
PowerCLI boot --actor urn:iq:actor:dispatcher --wait
```

Implementation:
1. Resolve `AgentService` from `KernelContext` (move `AgentService` from `iq-apis` → `iq-platform` per SPEC Phase 2, step 7).
2. Call `AgentService.onStart(realm)` — starts all actors declared in the graph.
3. `--wait` blocks until all actors reach `READY` state (poll via `I_StateMachine.getState()`).
4. Output: ASCII table of actor IRIs + states.

**Inspired by:** `actor.execute` / `actor.status` MCP tools (MCP_TOOLS.md), `IntentAPI` (iq-apis).

**Dependencies:** `iq-agentic` (AgentService, I_StateMachine), `iq-platform` (RealmManager). **COSS boundary: agent lifecycle.**

### 3.2 `run` — wire to `IQScriptCatalog` (SPARQL + Groovy)

**What it should do:** execute a script by file path or named IRI; return results as table / TTL / JSON.

**Current state:** entire execution block commented out (was calling removed `TripleScript` class).

**Target approach:**
```
PowerCLI run --file ./scripts/daily-infer.groovy
PowerCLI run --file ./scripts/enrich.sparql
PowerCLI run --script urn:iq:script:platform:infer:core
PowerCLI run --script urn:iq:script:platform:infer:core --bindings "realm=urn:iq:realm:default"
```

Implementation:
1. Detect language from file extension or `--lang [sparql|groovy|js]`.
2. For SPARQL: delegate to `SPARQLExecutor` (also available in OSS `iq-cli script` — boundary is Groovy).
3. For Groovy: use Groovy engine from `iq-platform`. Bind `KernelContext` (IQ, realm IRI, bindings) into `GroovyContext`.
4. Named script `--script <IRI>`: resolve from `IQScriptCatalog` / `ModelScriptCatalog`.

**Inspired by:** `DynamicScriptBridge` (iq-mcp), `ScriptCommand` (iq-cli OSS, SPARQL only).

**COSS boundary:** Groovy / Nashorn execution requires the `iq-platform` Groovy engine. SPARQL-only scripts can be run free via `iq-cli script`.

### 3.3 `trigger` — wire to `I_EventHub` + `ActorTriggerAdapter`

**What it should do:** publish a named intent event to trigger a set of actor/workflow transitions.

**Current state:** full stub with `// TODO: Wire-up Apache Camel`.

**Target approach:**
```
PowerCLI trigger --actor urn:iq:actor:dispatcher --intent urn:iq:intent:start
PowerCLI trigger --actor urn:iq:actor:dispatcher --intent urn:iq:intent:start --bindings "param=value"
PowerCLI trigger --topic urn:iq:event:nightly-infer
```

Implementation:
1. Obtain `I_EventHub` from `KernelContext`:
   - Local (no server): `SimpleEventHub` — synchronous, in-process.
   - Server mode (with running `iq-apis`): HTTP POST to `ux/intent/{realm}` (`IntentAPI`).
   - Camel mode: `CamelEventHub` (when `iq-camel` is on classpath).
2. Build a `KernelEvent` (topic IRI, bindings payload), call `hub.publish(event)`.
3. Wait for acknowledgement with `--wait` flag.

**Inspired by:** `ActorTriggerAdapter` (iq-mcp), `IntentAPI` + `AgentService.next()` (iq-apis).

**COSS boundary:** requires `iq-agentic` (FSM transitions) and for Camel-backed routing, `iq-camel`.

### 3.4 `trust` — wire to `TrustFactory` + PKI

**What it should do:** establish or inspect trust arcs between identities in the realm graph.

**Current state:** `trustSelf()` and `trustRemote()` are private no-ops.

**Target approach:**
```
PowerCLI trust me  # self-sign; write iq:trusts triple
PowerCLI trust <did:key:...>   # import remote DID → validate → write trust arc
PowerCLI trust --provider github   # OAuth handshake → fetch JWT → iq:trusts triple
PowerCLI trust list# show all iq:trusts arcs in the graph
PowerCLI trust revoke <IRI># remove trust arc
```

Implementation:
1. `trust me`: generate or load self key from `VFSPasswordVault`; write `<self> iq:trusts <self>` with timestamp.
2. `trust <remote>`: resolve remote identity (DID / IRI / URL); fetch JWK / DID document; verify signature; write `<self> iq:trusts <remote>`.
3. `trust --provider github`: mirror `TokenAPI` OAuth flow — nonce → redirect → token → `iq:trusts` arc with issuer annotation.
4. Uses `TrustFactory` (already exists in `iq-cli-pro/trust/`).

**Inspired by:** `trust.login` conceptual MCP tool (MCP_TOOLS.md §iq:trusts), `TokenAPI` + `NonceAPI` (iq-apis), `iq:trusts` predicate pattern.

**COSS boundary:** `iq-trusted` (VFSPasswordVault, PKI, JWK verification).

### 3.5 `serve` — new command (not yet present)

**What it should do:** start an embedded server surface (REST API, MCP server, or both) from the CLI.

```
PowerCLI serve --api --port 8080
PowerCLI serve --mcp --port 8081
PowerCLI serve --api --mcp --port 8080
PowerCLI serve --api --realm urn:iq:realm:demo
```

Implementation:
1. `--api`: bootstrap Quarkus in programmatic mode pointing at the active realm. Reuses `KernelBuilder` context so the REST API starts with the same realm as the CLI session.
2. `--mcp`: call `MCPServerBuilder` (from `iq-mcp`) with the same `KernelContext`.
3. Both: start both, share the `KernelContext` singleton.
4. Ctrl-C triggers graceful shutdown via `I_Kernel.stop()`.

**Inspired by:** `MCPServerBuilder` (iq-mcp), `RealmPlatform` (iq-apis).

**COSS boundary:** requires Quarkus (`iq-apis`) and/or MCP SDK (`iq-mcp`).

### 3.6 `agent` sub-commands — new (analogue of `actor.*` MCP tools)

```
PowerCLI agent list # all actors + current FSM state
PowerCLI agent status <IRI> # single actor detail
PowerCLI agent start <IRI>  # force-transition actor to READY
PowerCLI agent stop <IRI>   # graceful shutdown of actor
PowerCLI agent logs <IRI> [--tail N]# recent audit events from mcp:audit graph
```

**Inspired by:** `actor.trigger`, `actor.execute`, `actor.status` MCP tools; `AuditWriterMiddleware` output.

**Dependencies:** `iq-agentic` (AgentService, I_StateMachine), `iq-rdf4j` (SPARQL query of audit graph).

**COSS boundary:** requires `iq-agentic`.

### 3.7 `model` sub-commands — extends `ModelsCommand` stub

```
PowerCLI model list # all configured LLM providers from LLMFactory
PowerCLI model test <provider>  # send ping prompt, report latency + token cost
PowerCLI model set-default <provider>   # update realm config TTL for default provider
PowerCLI model secrets  # show which env vars / vault keys are expected
```

**Inspired by:** `llm.invoke`, `llm.status` MCP tools (MCP_TOOLS.md), `LLMFactory` (iq-platform).

**Dependencies:** `iq-platform` (LLMFactory, GPTWrapper), `iq-trusted` (vault for API keys).

**COSS boundary:** LLM provider management requires vault access and `iq-platform` LLM stack.

### 3.8 `realm` sub-commands — new (analogue of `realm.*` MCP tools)

```
PowerCLI realm list # all repositories under $IQ_HOME
PowerCLI realm switch <name># change active repository
PowerCLI realm status   # health: triple count, last backup, actor states
PowerCLI realm export --format turtle   # full realm dump with provenance headers
PowerCLI realm schema   # display VOID description + namespaces
```

**Inspired by:** `realm.status`, `realm.export`, `realm.schema`, `realm.search` MCP tools; `AboutAPI`, `HealthCheckAPI` (iq-apis).

**Dependencies:** `iq-platform` (RealmManager, Workspace). `realm export` extends existing `BackupCommand` with provenance.

**COSS boundary:** `RealmManager` lives in `iq-platform` (heavyweight dep). `realm list` / `realm switch` could be ported to OSS once `iq-kernel.workspace` is complete.

### 3.9 `pipeline` sub-commands — new (middleware inspection / testing)

```
PowerCLI pipeline test --middleware systems.symbol.mcp.connect.impl.QuotaGuardMiddleware
PowerCLI pipeline list  # show currently wired middleware for CLI surface
PowerCLI pipeline dry-run --input bindings.json
```

**Purpose:** enable offline testing of middleware chains without starting a full server; useful for debugging access-policy and quota configuration.

**Dependencies:** `iq-kernel` (`I_Pipeline`, `KernelCallContext`), `iq-mcp` (for MCP middleware classes when testing that surface).

---

## 4. Cross-Cutting Concerns (COSS-specific)

### 4.1 Vault / secrets access

Pro commands (`TrustCommand`, `ModelsCommand`) read secrets from `VFSPasswordVault` and environment variables. Ensure these are always accessed through `KernelContext.getSecrets()`, never via direct `System.getenv()` calls, so that the vault can be swapped (e.g., HashiCorp Vault in production).

### 4.2 Budget / quota guard for costly commands

`ModelCommand.test` and `RunCommand` (Groovy + LLM) can incur real API costs. Wire `QuotaGuardMiddleware` equivalently to the MCP surface:

- Store quota counters as RDF triples in a `iq:quota` named graph.
- `KernelBudgetException` is thrown when the limit is reached (SPEC §4.5).
- `--dry-run` flag estimates cost without executing.

### 4.3 Audit log

All state-changing pro commands (`trigger`, `trust`, `boot`, `run`) must write a structured audit event to the `mcp:audit` named graph (same schema as `AuditWriterMiddleware` produces for MCP tools). This enables post-hoc inspection with `agent logs`.

### 4.4 Exit codes (inherits from iq-cli)

Same `KernelException` → POSIX exit code mapping as `iq-cli`. Additional COSS code:

| Exception | Exit code |
|---|---|
| `KernelBudgetException` | 5 |
| Trust verification failure | 6 (sub-type of `KernelAuthException`) |

---

## 5. Testing

| Test class | What it covers | Status |
|---|---|---|
| `BootCommandTest` | Mock `AgentService`; verify actors started, ASCII table output | ⏳ Pending |
| `RunCommandSparqlTest` | SPARQL execution via in-memory store (shared fixture with iq-cli) | ⏳ Pending |
| `RunCommandGroovyTest` | Groovy script execution; verify binding injection | ⏳ Pending |
| `TriggerCommandTest` | `SimpleEventHub` receives correct `KernelEvent`; actor state transitions; binding parsing | ✅ 30 tests |
| `TrustCommandTest` | Self-signing, remote trust, OAuth; vault + signature verification | ✅ 12 tests |
| `ServeCommandTest` | REST API/MCP server startup, port config, host binding | ✅ 25 tests |
| `AgentListCommandTest` | In-memory graph with actor IRIs; verify ASCII output | ⏳ Pending |
| `ModelListCommandTest` | Mock `LLMFactory` config; verify provider table | ⏳ Pending |
| `RealmStatusCommandTest` | In-memory workspace; verify health summary fields | ⏳ Pending |
| `CLIPipelineTest` | End-to-end middleware chain: auth → acl → quota → audit → command | ⏳ Pending |

**Completed (67 tests):**
- All unit tests use `KernelBuilder` for setup, in-memory RDF4J store, and proper resource cleanup
- Tests validate command registration, option parsing, context initialization, and error handling
- Integration tests (async operations with timeouts) follow JUnit 5 best practices
- Test suite documents required assumptions and shared fixtures

**Remaining:** `BootCommand`, `RunCommand`, agent/model/realm admin commands; integration suite for end-to-end middleware chain.

---

## 6. COSS Boundary Summary

| Feature | Reason for COSS |
|---|---|
| Agent workflow boot (`boot`) | `iq-agentic` (AgentService, FSM) — proprietary fleet logic |
| Groovy / Nashorn script execution (`run`) | `iq-platform` Groovy engine — heavyweight dep not in OSS footprint |
| Event trigger (`trigger`) | ✅ **IMPLEMENTED** — `iq-agentic` (FSM transitions) + `iq-camel` (Camel routing) |
| Trust / PKI (`trust`) | ✅ **IMPLEMENTED** — `iq-trusted` (VFSPasswordVault, JWK, DID resolution) |
| Embedded server launch (`serve`) | ✅ **IMPLEMENTED** — Quarkus (`iq-apis`) and/or MCP SDK (`iq-mcp`) via `ServerRuntimeManager` |
| Agent lifecycle management (`agent *`) | `iq-agentic` |
| LLM model management (`model *`) | `iq-platform` LLMFactory + vault secrets |
| Realm admin (`realm *`) | `iq-platform` RealmManager (full) |
| Pipeline inspection (`pipeline *`) | `iq-kernel` middleware SPI + surface-specific middleware impls |
| Budget / quota guard | `iq-platform` cost tracker; `KernelBudgetException` |
| Audit log writes | `mcp:audit` graph; shared schema with `iq-mcp` AuditWriter |

---

## 7. Implementation Order (suggested)

1. **Inherit kernel alignment from iq-cli** — items 2.1–2.3 in that module unblock everything here.
2. **Wire `PowerCLI` to `KernelBuilder`** (item 2.2 above).
3. **Add `CLIPipeline` with auth + audit middleware** (item 2.3 above — needed before any state-changing command is shipped).
4. **Fix `boot`** — highest request signal; move `AgentService` to `iq-platform` first (SPEC Phase 2, step 7).
5. ✅ **`trust` complete** — foundational for multi-realm and remote-identity scenarios.
6. **Fix `run` (SPARQL path)** — quick win; Groovy path follows when `iq-platform` footprint is audited.
7. ✅ **`trigger` complete** — Camel event routing with bindings and --wait flag.
8. ✅ **`serve` complete** — local development loops via ServerRuntimeManager.
9. **Add `agent *` sub-commands** — builds on `boot` plumbing.
10. **Add `model *` sub-commands** — extends existing `ModelsCommand` stub.
11. **Add `realm *` sub-commands** — admin convenience; lower urgency.
12. **Add `pipeline *` sub-commands** — developer tooling; lowest urgency.
13. **Test suite** — companion to each command; integration test suite documented with required secrets.
