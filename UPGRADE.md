# IQ Upgrade Roadmap

## Part I — Architecture Review

### 1.1 Module Map (current)

```
iq-parent (pom, v0.91.5)
├── iq-abstract          — Shared interfaces (I_Self, I_Agent, I_StateMachine, I_Realm, …)
├── iq-kernel            — KernelBuilder / KernelContext / pipeline / event bus / workspace
├── iq-aspects           — Utilities: string, bean, POJO converters, XSD, Levenshtein
├── iq-rdf4j             — RDF4J repository wrappers, SPARQL execution, Facts, IRIs
├── iq-intents           — Intent/FSM runtime (declarative transitions via SPARQL + TTL)
├── iq-platform          — LLMFactory, GPTWrapper, AgentBuilder, research, realm, lake, VFS
├── iq-secrets/
│   ├── iq-secrets-core  — VFSPasswordVault, EnvsAsSecrets, MemoryVault (IMPLEMENTED)
│   ├── iq-secrets-aws   — AwsSecretsManagerProvider       (STUB — UnsupportedOperation)
│   ├── iq-secrets-azure — AzureKeyVaultSecretsProvider    (STUB — UnsupportedOperation)
│   ├── iq-secrets-gcp   — GcpSecretManagerProvider        (STUB — UnsupportedOperation)
│   └── iq-secrets-hashicorp — HashicorpVaultSecretsProvider (STUB — UnsupportedOperation)
├── iq-lake              — Data lake ingestion (RDF, RSS, Tika, VFS, file clone)
├── iq-finder            — Fact / semantic search
├── iq-agentic           — ExecutiveAgent, ExecutiveFleet, budget, ChainOfCommand
├── iq-apis              — Quarkus REST: IntentAPI, TokenAPI, ModelAPI, RealmAPI, HealthCheckAPI
├── iq-rdf4j-graphs      — JGraph / model graph adapter
├── iq-rdf4j-graphql     — GraphQL → SPARQL bridge
├── iq-rdf4j-camel       — Apache Camel RDF route planner
├── iq-camel             — Route policies and Camel integration
├── iq-mcp               — MCP server, tool registry, middleware pipeline, dynamic bridge
├── iq-trusted           — Trust namespace constants, TrustedPlatform, SPIFFE/PKI vocab
├── iq-connect/
│   └── iq-connect-core  — AbstractConnector, I_Connector, I_SyncState, registry (in root pom)
│   └── iq-connect-aws   — AWS EC2/S3/IAM/CloudTrail/Pricing scanners (not in root pom)
│   └── iq-connect-github — GitHub org/repo/user/team scanners (not in root pom)
│   └── 20+ more connectors — skeleton or template only, not in root pom
├── iq-cli               — OSS Picocli CLI (init, import, export, backup, list, sparql, about)
├── iq-cli-pro           — COSS Picocli CLI (boot, run, trigger, trust, models) — mostly stubs
└── iq-cli-server        — Runtime lifecycle CLI (ServerRuntimeManager) — skeleton only
```

### 1.2 Data-flow summary

```
External Systems ──► iq-connect/* ──► RDF4J Repository (quad store)
                                             │
User / LLM ──► iq-apis (REST) ──► AgentService ──► iq-platform (FSM + LLM)
               iq-mcp (MCP)  ──► MCPToolRegistry ──► ActorTriggerAdapter
               iq-cli / pro  ──► CLIContext ──► (direct repository access)
                                             │
                              iq-trusted (PKI/JWT/SPIFFE arcs)
                              iq-secrets  (vault / env / cloud backends)
```

**Critical structural gap:** `iq-cli` and `iq-cli-pro` bypass the kernel pipeline entirely — no auth, no audit, no quota. Every command writes directly to the repository.

---

## Part II — Gap Inventory

Every item below is confirmed by code inspection. Severity ratings: 🔴 Blocker · 🟠 High · 🟡 Medium · 🟢 Low.

### 2.1 Secrets backends — all cloud providers are stubs

| File | Gap | Severity |
|---|---|---|
| `iq-secrets-aws/AwsSecretsManagerProvider.java` | All 3 methods throw `UnsupportedOperationException` | 🔴 |
| `iq-secrets-azure/AzureKeyVaultSecretsProvider.java` | All 3 methods throw `UnsupportedOperationException` | 🔴 |
| `iq-secrets-gcp/GcpSecretManagerProvider.java` | All 3 methods throw `UnsupportedOperationException` | 🔴 |
| `iq-secrets-hashicorp/HashicorpVaultSecretsProvider.java` | All 3 methods throw `UnsupportedOperationException` | 🔴 |
| `iq-secrets` | No `IQ_SECRETS_BACKEND` env-driven provider selection | 🔴 |
| `iq-secrets` | No secret rotation, namespace policy, or RBAC enforcement | 🟠 |

**Impact:** Any enterprise, SaaS, or cloud deployment that cannot use plain files or env vars has zero working secrets backend.

### 2.2 MCP security — development stubs only

| File | Gap | Severity |
|---|---|---|
| `iq-mcp/AuthGuardMiddleware.java` | JWT parsed but **signature not verified** — dev stub explicitly | 🔴 |
| `iq-mcp/MCPServerBuilder.build()` | Returns `null` — SDK transport integration deferred | 🔴 |
| `iq-mcp/IQRoutePolicy.java` | `// TODO: Check ACL` — Camel route has no access-control check | 🔴 |
| `iq-mcp/SparqlSafetyMiddleware` | Not wired to production pipeline | 🟠 |

### 2.3 Runtime lifecycle — ServerRuntimeManager entirely unimplemented

| File | Gap | Severity |
|---|---|---|
| `iq-platform/runtime/ServerRuntimeManager.java` | All 6 default methods throw `UnsupportedOperationException`: `start`, `stop`, `reboot`, `health`, `debug`, `dump` | 🔴 |
| `iq-cli-server` | No concrete implementation of `ServerRuntimeManager` exists | 🔴 |
| `iq-apis/RuntimeAPI` | REST lifecycle endpoints delegating to `ServerRuntimeManager` not wired | 🟠 |

### 2.4 iq-cli — broken and missing commands

| Command | State | Severity |
|---|---|---|
| `infer` | Entire body commented out | 🔴 |
| `agent` | Explicit stub comment: "integrate with IntentAPI or iq-mcp for real runs" | 🔴 |
| `recover` | Body unknown / unverified — no test coverage | 🟠 |
| `script` | Status unknown — no test coverage | 🟠 |
| `render` | Status unknown — no test coverage | 🟠 |
| `CLIContext.init()` | Duplicates `RealmPlatform.onStart()` — diverges from kernel | 🟠 |
| `AbstractCLICommand` | Not connected to `AbstractKernelCommand` — no pipeline, no auth | 🔴 |
| `CLIException` | Parallel exception hierarchy alongside `KernelException`, `MCPException` | 🟡 |
| `iq-cli/src/test/` | **Zero test files** | 🔴 |

### 2.5 iq-cli-pro — commands are shells with commented-out or no logic

| Command | State | Severity |
|---|---|---|
| `boot` | References deleted `Wisdom` class; all agent logic commented out | 🔴 |
| `run` | References deleted `TripleScript` class; execution block commented out | 🔴 |
| `trigger` | `// TODO: Wire-up Apache Camel` | 🔴 |
| `trust` | `trustSelf()` and `trustRemote()` are private no-ops | 🔴 |
| `models` | State unknown; assumed stub | 🟠 |
| `iq-cli-pro/src/test/` | **Zero test files** | 🔴 |

### 2.6 Authentication & authorisation gaps in iq-apis

| File / Location | Gap | Severity |
|---|---|---|
| `TokenAPI.java:104-105` | `// TODO: authenticate (subject is a user)` · `// TODO: authorize (subject known to audience)` | 🔴 |
| `TokenAPI.java:243` | `throw new UnsupportedOperationException("oops.entitled")` in entitlement check path | 🔴 |
| `TrustedPlatform.java:71` | `//TODO: I_Self.trust(name) && name.length()>` — trust validation never executed | 🔴 |
| `MY_IQ_AI.java:44-45` | `ai.XXX()` and `ai.XXXX()` — guard calls are placeholder identifiers | 🟠 |

### 2.7 Budget & cost control — approval logic commented out

| File | Gap | Severity |
|---|---|---|
| `iq-agentic/budget/Budget.java` | `fund()` method has approval check commented out (`if (!approved(...)) throw`) | 🟠 |
| `iq-agentic` | `I_Treasury` interface has no implementation beyond `Funded` wrapper | 🟠 |
| LLM calls | No per-call token accounting, no quota enforcement on `GPTWrapper` | 🟠 |

### 2.8 iq-connect — connectors not wired into root build

| Connector | State | In root pom.xml |
|---|---|---|
| `iq-connect-core` | Implemented — `AbstractConnector`, `I_Connector`, `I_SyncState` | ✅ |
| `iq-connect-aws` | Implemented (multiple scanners) | ❌ |
| `iq-connect-github` | Implemented (multiple scanners) | ❌ |
| `iq-connect-template` | Template only | ❌ |
| `iq-connect-azure`, `gcp`, `slack`, `kafka`, `jdbc`, `k8s`, `jira`, `confluence`, `salesforce`, `snowflake`, `databricks`, `datadog`, `digitalocean`, `docker`, `redis`, `parquet`, `graphql`, `openapi`, `stripe`, `scan-cve`, `sparql`, `google-apps`, `office-365` | Skeleton or missing | ❌ |
| Connector ontology (`iq-onto`) | Module exists but empty / not built | ❌ |
| `IConnector` FSM lifecycle | Design exists in `iq-connect/todo/` but not implemented | 🟠 |

### 2.9 LLM layer — hard-coded providers, no streaming, no caching

| Gap | Severity |
|---|---|
| `LLMFactory` supports OpenAI, Groq, Anthropic, Gemini, Bedrock, Ollama via TTL config | 🟠 |
| No per-provider retry/circuit-breaker | 🟠 |
| Provider model names hard-coded as strings (no model registry in RDF) | 🟡 |


remove `langchain4j` as dependency - wire-up ONNX directly | 🟡 |

### 2.10 Dependency hygiene — pre-release versions in production BOM

| Dependency | Version | Issue |
|---|---|---|
| `apache-camel` | `4.0.0-RC2` | Release Candidate — not production-stable |
| `groovy` | `5.0.0-alpha-8` | Alpha — API unstable |
| `slf4j` | `1.7.32` | Very old; SLF4J 2.x released and widely adopted |
| `theokanning-openai` | `0.18.2` | Old unofficial client; superseded |
| `quarkus` | `3.17.5` | Current LTS — acceptable |

### 2.11 Abandoned / dead code

review for resurrection ... only if adds value.

| File | Status |
|---|---|
| `iq-platform/agent/LazyAgent.old` | Abandoned — not compiled |
| `iq-platform/agent/ScriptAgent.java.old` | Abandoned |
| `iq-agentic/decide/IntentDecision.old` | Abandoned |
| `iq-agentic/decide/AgentDecision.old` | Abandoned |
| `iq-agentic/self/SelfIntent.old` | Abandoned |
| `iq-agentic/prompt/PromptChain.old` · `ChoicePrompt.old` · `SimplePrompt.old` | Abandoned |
| `iq-rdf4j/util/TripleFinder.old` · `VariableCollector.java.old` | Abandoned |
| `iq-rdf4j-camel/self/Execute.java.old` | Abandoned |
| `iq-platform/finder/FactFinder.java.old` + 2 more | Abandoned |
| `iq-platform/llm/JSONMessage.old` | Abandoned |
| `<!-- <module>iq-persona</module> -->` | Commented-out module in root pom |

### 2.12 Observability & compliance gaps

| Gap | Severity |
|---|---|
| No distributed tracing (OpenTelemetry / Jaeger) | 🟠 |
| No structured metrics endpoint (Micrometer / Prometheus) beyond Quarkus default | 🟡 |
| `AuditWriterMiddleware` (MCP) exists but no central audit sink or retention policy | 🟠 |
| Secret reads not logged — no SOC2-compatible audit trail | 🔴 |
| SHACL validation available in RDF4J but not surfaced as API or CLI command | 🟡 |
| No multi-tenant isolation enforcement at repository level | 🟠 |

### 2.13 Test coverage gaps

| Module | Test files | Critical gap |
|---|---|---|
| `iq-cli` | 0 | No test for any command |
| `iq-cli-pro` | 0 | No test for any command |
| `iq-cli-server` | 0 | No test for any command |
| `iq-secrets-aws/azure/gcp/hashicorp` | 0 | No test for any provider |
| `iq-connect` (non-core) | 0 | No connector integration tests |
| `iq-agentic` | ~4 files | Fleet and budget not covered |
| `iq-mcp` | Sparse | AuthGuard not tested against real JWT; `MCPServerBuilder.build()` returns null |

---

## Part III — Upgrade Roadmap

Upgrades are sequenced by dependency; each block can be sprint-sized (1–2 weeks). Version tags are advisory.

---

### U-1 · v0.92 · Kernel Alignment & CLI Foundation

**Goal:** Make `iq-cli` and `iq-cli-pro` production-grade by unifying them on the kernel pipeline.

#### U-1.1 Wire `CLIContext` to `KernelBuilder`

Replace duplicated workspace boot logic:

```java
// CLIContext.java — after
KernelContext ctx = KernelBuilder.create()
    .withSelf(I_Self.self())
    .withSecrets(SecretsProviderFactory.resolve())  // reads IQ_SECRETS_BACKEND
    .withRealm(home)
    .build();
```

`Workspace` + `WorkspaceProbe` stay in `iq-kernel.workspace`. `CLIContext` becomes a thin Picocli holder for `KernelContext`.

**Files:** `CLIContext.java`, `systems.symbol.CLI` (main).

#### U-1.2 Extend `AbstractCLICommand` → `AbstractKernelCommand`

```java
public abstract class AbstractCLICommand
        extends AbstractKernelCommand
        implements Callable<Object> {

    @Override
    public final Object call() throws Exception {
        KernelRequest req = buildRequest();
        KernelResult<?> result = execute(req);
        return result.isOk() ? result.value() : handleError(result.error());
    }

    protected abstract KernelRequest buildRequest();
}
```

All command classes inherit auth, quota, and audit through the kernel pipeline — zero per-command duplication.

**Files:** `AbstractCLICommand.java`, all `*Command.java` in `iq-cli` and `iq-cli-pro`.

#### U-1.3 Collapse exception hierarchies

Deprecate `CLIException extends Exception` in favour of `KernelCommandException extends KernelException`. Retain deprecated alias for one release cycle.

**Files:** `CLIException.java`, all catch sites in `iq-cli` and `iq-cli-pro`.

#### U-1.4 Implement `ServerRuntimeManager`

Create `QuarkusRuntimeManager implements ServerRuntimeManager` in `iq-platform`:

| Method | Implementation |
|---|---|
| `start(type)` | Fork `quarkus:dev` or target JAR via `ProcessBuilder`; write PID to `.iq/runtime/{type}.pid` |
| `stop(type)` | Read PID; send SIGTERM; wait; escalate to SIGKILL |
| `reboot(type)` | `stop` + `start` with configurable wait |
| `health(type)` | HTTP GET `{baseUrl}/q/health` → map to `RuntimeStatus` |
| `debug(type, enable)` | HTTP POST `{baseUrl}/q/dev/toggle-debug` |
| `dump(type, path)` | HTTP GET `{baseUrl}/q/dev/heap-dump` → write to path |

Add `ServerRuntimeManagerFactory.register(type, manager)` for pluggable runtime types (`api`, `mcp`, `custom`).

**Files:** New `QuarkusRuntimeManager.java`, `ServerRuntimeManagerFactory.java` in `iq-platform`.

#### U-1.5 Fix broken `iq-cli` commands

| Command | Action |
|---|---|
| `infer` | Uncomment body; wire to `SPARQLExecutor` via `IQScriptCatalog` |
| `recover` | Implement using `ImportExport` infrastructure from `BackupCommand` + `ImportCommand`; add `--force` confirmation |
| `script` | Implement SPARQL dispatch) |
| `render` | Implement CONSTRUCT + RDF4J `Rio` serialise; expose `--format` option |
| `agent` | Wire to MCP (server mode) or `ExecutiveFleet` (local mode) |

#### U-1.6 Add `I_AccessPolicy` guard to CLI pipeline

Single middleware before any command executes:

```java
public class WorkspaceInitGuard implements I_Middleware<KernelCallContext> {
    @Override
    public I_MCPResult handle(KernelCallContext ctx, MCPChain chain) {
        if (!ctx.getKernel().isInitialized())
            throw new KernelBootException("kernel.not.initialised");
        return chain.next(ctx);
    }
}
```

Replaces duplicated `if (!context.isInitialized())` checks across every command class.

#### U-1.7 Test harness for CLI

Add `TestCLIExecutor` in `iq-cli/src/test/` with:
- In-process Picocli dispatch (no subprocess).
- In-memory `RepositoryConnection` (RDF4J `MemoryStore`).
- Assertions on stdout capture and repository state.
- Minimum 1 test per command; minimum 1 negative (unauthorised / missing workspace) test.

**Acceptance criteria:** `mvn test -pl iq-cli,iq-cli-pro,iq-cli-server` passes with zero failures and >70% line coverage on all command classes.

---

### U-2 · v0.92 · Cloud Secrets Backends

**Goal:** Implement all four cloud secrets providers so deployments on AWS, Azure, GCP, and HashiCorp Vault work out of the box.

#### U-2.1 Environment-driven provider selection

Add `SecretsProviderFactory` to `iq-secrets-core`:

```java
public class SecretsProviderFactory {
    public static I_SecretsStore resolve() {
        String backend = System.getenv("IQ_SECRETS_BACKEND");  // local|aws|azure|gcp|hashicorp
        return switch (backend != null ? backend : "local") {
            case "aws"       -> new AwsSecretsManagerProvider();
            case "azure"     -> new AzureKeyVaultSecretsProvider();
            case "gcp"       -> new GcpSecretManagerProvider();
            case "hashicorp" -> new HashicorpVaultSecretsProvider();
            default          -> new VFSPasswordVault();
        };
    }
}
```

#### U-2.2 AWS Secrets Manager

Use AWS SDK v2 (`software.amazon.awssdk:secretsmanager`). Credential chain: instance profile → env → `.aws/credentials`.

```java
public I_Secrets getSecrets(IRI agent) throws SecretsException {
    SecretsManagerClient client = SecretsManagerClient.create();
    GetSecretValueResponse r = client.getSecretValue(
        GetSecretValueRequest.builder()
            .secretId(SecretNaming.toAwsId(agent))
            .build());
    return SimpleSecrets.parse(r.secretString());
}
```

Secret name convention: `iq/{realmLocalName}/{agentLocalName}`.

#### U-2.3 Azure Key Vault

Use `azure-security-keyvault-secrets` with `DefaultAzureCredential` (supports managed identity, env, interactive).

#### U-2.4 GCP Secret Manager

Use `google-cloud-secretmanager`. Secret name: `projects/{project}/secrets/iq-{realmLocalName}-{agentLocalName}/versions/latest`.

#### U-2.5 HashiCorp Vault

Use `io.github.jopenlibs:vault-java-driver` (or `spring-vault` subset). Honour `VAULT_ADDR`, `VAULT_TOKEN`, `VAULT_NAMESPACE`. Path convention: `secret/data/iq/{realm}/{agent}`.

#### U-2.6 Secret audit logging

Wrap every secrets read through `SecretAuditInterceptor`:

```java
// Logs: timestamp, agentIRI, secretName, backend, caller — never the secret value
log.info("secret.read: agent={} key={} backend={}", agent, key, backend);
```

Write to `iq:SecretAuditEvent` named graph for compliance queries.

#### U-2.7 Rotation support

Add `I_SecretsStore.rotate(IRI agent, String key)` default method. Implement for AWS (via `rotate-secret`) and HashiCorp (via `lease renew`). Document expected cadence in `.iq/vault/POLICY.md`.

**Acceptance criteria:** Integration test suite (gated by `IQ_TEST_SECRETS_BACKEND`) passes for each backend. Unit tests use `MemoryVault` (existing).

---

### U-3 · v0.93 · MCP Production Security

**Goal:** Replace development stubs in the MCP layer with cryptographically verified, policy-enforced middleware.

#### U-3.1 JWT signature verification in `AuthGuardMiddleware`

Replace best-effort extraction with `nimbus-jose-jwt` (already a Quarkus transitive):

```java
// Config-driven: jwtSecret, jwksUri, or oidcDiscoveryUrl
JWSVerifier verifier = buildVerifier(config);   // HMAC or RSA/ECDSA
JWTClaimsSet claims  = SignedJWT.parse(tokenString).verify(verifier);
String subject       = claims.getSubject();
Instant expiry       = claims.getExpirationTime().toInstant();
if (Instant.now().isAfter(expiry))
    throw new MCPException("mcp.auth.token.expired");
ctx.setPrincipal(Values.iri(subject));
```

Support: HMAC-SHA256 (shared secret), RSA/ECDSA (JWKS endpoint), OIDC discovery.

#### U-3.2 SPARQL-backed ACL enforcement

`AclAskSparqlMiddleware` executes a SPARQL ASK configured in the realm graph:

```sparql
ASK { <{principal}> iq:trusts <{toolIri}>. <{toolIri}> iq:trusts <{principal}> }
```

Tool IRIs are declared in the realm's `.ttl`; administrators grant/revoke access by editing RDF — no code changes needed.

#### U-3.3 Rate limiting / quota middleware

`QuotaGuardMiddleware` reads per-principal limits from the realm graph:

```sparql
SELECT ?limit ?window WHERE {
  <{principal}> iq:rateLimit [
    iq:calls ?limit ; iq:windowSeconds ?window
  ]
}
```

Uses a sliding-window counter stored in a dedicated named graph (`iq:quota`). Exceeding limit returns `MCPException("mcp.quota.exceeded")`.

#### U-3.4 Wire `MCPServerBuilder.build()` to MCP SDK transport

Replace `return null` with actual SDK wiring (MCP SDK Java 1.1.0 / Quarkus CDI producer):

```java
// Quarkus CDI producer
@Produces @ApplicationScoped
public McpSyncServer mcpServer(MCPServerBuilder builder) {
    McpServerFeatures.SyncToolSpecification toolSpec = buildToolSpec(builder.getTools());
    return McpSyncServer.using(QuarkusSseServerTransport.builder()
        .sseEndpoint("/mcp/sse")
        .postEndpoint("/mcp/message")
        .build())
        .serverInfo(builder.getServerName(), builder.getServerVersion())
        .tools(toolSpec)
        .build();
}
```

#### U-3.5 ACL check in `IQRoutePolicy` (iq-camel)

Implement the `// TODO: Check ACL` in `IQRoutePolicy`:

```java
boolean allowed = sparql.ask(
    "ASK { <" + principal + "> iq:mayRoute <" + routeId + "> }", connection);
if (!allowed) exchange.setException(new SecurityException("route.acl.denied"));
```

**Acceptance criteria:** MCP integration tests with expired JWT, revoked ACL, and quota-exceeded cases all return correct error codes. `AuthGuardMiddleware` has a unit test with a real HMAC-signed JWT.

---

### U-4 · v0.93 · iq-cli-pro Feature Completion

**Goal:** All five COSS commands become functional end-to-end.

#### U-4.1 `boot` — wire to `AgentService`

Move `AgentService` from `iq-apis` → `iq-platform` (PLAN_v92 §2). `BootCommand` calls:

```java
AgentService svc = ctx.getService(AgentService.class);
svc.onStart(realmIri);
if (waitFlag) svc.awaitReady(timeoutMs);
Display.table(svc.actorStates());
```

Drop references to deleted `Wisdom` class. Accept `--actor <IRI>` for single-actor boot.

#### U-4.2 `run` — wire to `IQScriptCatalog`

Replace `TripleScript` references with `IQScriptCatalog` + `SPARQLExecutor` (SPARQL path) and Groovy engine (Groovy path):

```java
Script script = catalog.resolve(scriptRef, language);
ScriptResult result = executor.run(script, bindings, connection);
Display.result(result, outputFormat);
```

Language detected from extension (`.sparql`, `.groovy`, `.js`). Groovy gated on `IQ_EDITION=pro`.

#### U-4.3 `trigger` — wire to `I_EventHub`

```java
I_EventHub hub = ctx.getEventHub();   // SimpleEventHub (local) or CamelEventHub (if iq-camel present)
KernelEvent event = KernelEvent.of(actorIri, intentIri, bindings);
hub.publish(event);
if (waitFlag) hub.awaitAck(event.getId(), timeoutMs);
```

No Camel dependency required for local mode; Camel auto-detected via `ServiceLoader`.

#### U-4.4 `trust` — wire to `TrustFactory` + `VFSPasswordVault`

| Sub-command | Implementation |
|---|---|
| `trust me` | Generate/load self key from vault; write `<self> iq:trusts <self>` + timestamp |
| `trust <IRI>` | Resolve remote DID/IRI; fetch JWK/DID document via HTTP; verify signature; write trust arc |
| `trust --provider github` | OAuth handshake: nonce → token endpoint → JWT → `iq:trusts` arc, issuer annotated |
| `trust list` | SPARQL SELECT over `iq:trusts` predicate; display as table |
| `trust revoke <IRI>` | SPARQL DELETE trust arc; log revocation event |

#### U-4.5 `models` — implement `ModelsCommand`

```
PowerCLI model list                  → list configured LLM providers from LLMFactory + realm graph
PowerCLI model test <provider>       → send ping prompt; report latency + token count + cost estimate
PowerCLI model set-default <provider>→ update realm TTL; reload LLMFactory config
PowerCLI model secrets               → show expected env vars / vault keys per provider
```

#### U-4.6 New `agent` sub-commands (parity with MCP actor.* tools)

```
PowerCLI agent list                  → all actor IRIs + FSM state
PowerCLI agent status <IRI>          → single actor detail + recent audit events
PowerCLI agent start <IRI>           → trigger FSM → READY transition
PowerCLI agent stop <IRI>            → graceful shutdown
PowerCLI agent logs <IRI> [--tail N]  → audit graph SPARQL SELECT
```

#### U-4.7 New `serve` command

```
PowerCLI serve --api --port 8080     → embedded Quarkus (iq-apis) on KernelContext
PowerCLI serve --mcp --port 8081     → MCPServerBuilder + SSE transport
PowerCLI serve --api --mcp           → both, shared KernelContext
```

---

### U-5 · v0.94 · Connector Ecosystem

**Goal:** Promote the connector library from design to production-ready, add missing connectors to the build, implement the lifecycle FSM.

#### U-5.1 Connector SPI in `iq-connect-core`

Implement the designed abstractions from `iq-connect/todo/02-abstractions.md`:

```java
public interface IConnectorKernel {
    void start();
    void stop();
    void refresh();
    void reconnect();
    ConnectorStatus status();
    IRI getGraphIri();
}

public interface IConnectorSession {
    RepositoryConnection getConnection();
    I_Secrets getSecrets();
}
```

Add `I_SyncCheckpoint` implementation with SPARQL-backed persistence (checkpoint triple in connector graph).

#### U-5.2 FSM-driven connector lifecycle

Each connector transitions: `STOPPED → STARTING → RUNNING → (PAUSED | ERROR) → STOPPED`.

Transitions are declared in a shared `connector.ttl` template and evaluated by the intent engine. Backoff and retry policies declared in RDF:

```turtle
<urn:iq:connector:aws> iq:retryPolicy [
    iq:maxRetries 5 ;
    iq:backoffMs  2000 ;
    iq:backoffMultiplier 2.0
] .
```

#### U-5.3 Add iq-connector to root pom.xml and each connectors to the iq-connector/pom.xml 

Priority order for inclusion:

| Priority | Connector | Rationale |
|---|---|---|
| 1 | `iq-connect-aws` | Multiple scanners already implemented; just needs pom wiring |
| 2 | `iq-connect-github` | Already implemented; needs pom wiring |
| 3 | `iq-connect-slack` | Event-driven use cases; high demand |
| 4 | `iq-connect-kafka` | Streaming data pipelines |
| 5 | `iq-connect-jdbc` | Generic SQL → RDF; broadest enterprise reach |
| 6 | `iq-connect-k8s` | Infra-aware agents |
| 7 | `iq-connect-jira` | Issue tracking, workflow integration |
| 8 | `iq-connect-azure` | Cloud parity with AWS |

Each added connector requires: integration test (gated by `skipITs`), a `README.md` with config reference, and a secret key manifest.

#### U-5.4 Connector ontology (`iq-onto`)

Define `connector.ttl` ontology:

```turtle
iq:Connector a owl:Class .
iq:ConnectorStatus a owl:Class .
iq:syncedAt a owl:DatatypeProperty ; rdfs:domain iq:Connector .
iq:checkpointToken a owl:DatatypeProperty .
iq:retryPolicy a owl:ObjectProperty .
```

Publish to `iq-onto` module and include in root pom.

#### U-5.5 Connector registry API endpoint

Add `GET /connectors` → list all registered connectors + status. `POST /connectors/{id}/refresh` → trigger sync. Both backed by `InMemoryConnectorRegistry` (current) with persistence path to RDF graph.

---

### U-6 · v0.94 · LLM Layer Modernisation

**Goal:** Multi-provider routing, streaming, caching, cost tracking — upgrade from GPT-only to a unified model layer.

#### U-6.1 Expand provider support

Most providers support common OpenAPI spec, we don't need custom code.

Add providers to `LLMFactory`:

| Provider | Model examples |
|---|---|
| Generic | `llm-gpt-generic` via OpenAI chat API |

Remove `langchain4j`.  Remove `theokanning-openai`.

We don't want external dependencies since all simple REST APIs.

#### U-6.2 Model registry in RDF

Provider configuration stored per-realm:

```turtle
<urn:iq:llm:gpt4o> a iq:LLM ;
    iq:url    "https://api.openai.com/v1/chat/completions" ;
    iq:model  "gpt-4o" ;
    iq:secret "OPENAI_API_KEY" ;
    iq:config [
        llm:contextLength 128000 ;
        llm:costPerMTokenIn  "2.50"^^xsd:decimal ;
        llm:costPerMTokenOut "10.00"^^xsd:decimal .
    ]
```

`LLMFactory.configure()` resolves from this graph instead of hard-coded strings.

#### U-6.3 Streaming support

Add `I_LLM.stream(conversation)` returning `Flow.Publisher<String>` (Java 9 reactive streams). Wire to REST endpoint via Quarkus SSE (`@RestStreamElementType(MediaType.TEXT_PLAIN)`).

#### U-6.4 Per-call cost tracking

After each LLM call, write a `iq:LLMResponseEvent` triple to the audit graph:

```turtle
[] a iq:LLMResponseEvent ;
    iq:actor      <{agentIri}> ;
    iq:provider   <{providerIri}> ;
    llm:tokensIn   42 ;
    llm:tokensOut  180 ;
    llm:costUSD    "0.0032"^^xsd:decimal ;
    llm:timestamp  "2026-03-20T12:00:00Z"^^xsd:dateTime .
```

Budget enforcement in `Budget.fund()` can then query this graph before approving new calls.

#### U-6.5 Retry and circuit breaker

Wrap `GPTWrapper.chat()` in a Resilience4J `CircuitBreaker` + `Retry`:

```java
CircuitBreaker cb = CircuitBreaker.ofDefaults("llm." + config.getName());
Retry retry = Retry.of("llm." + config.getName(),
    RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofSeconds(2)).build());
return Decorators.ofSupplier(() -> doChat(conversation))
    .withCircuitBreaker(cb)
    .withRetry(retry)
    .get();
```

#### U-6.6 Upgrade pre-release dependencies

| Dependency | From | To |
|---|---|---|
| `apache-camel` | `4.0.0-RC2` | `4.8.x` (current LTS) |
| `groovy` | `5.0.0-alpha-8` | `4.0.x` (stable) |
| `slf4j` | `1.7.32` | `2.0.x` |

---

### U-7 · v0.95 · Observability, Compliance & Multi-tenancy

**Goal:** Production-grade observability, audit compliance, SHACL data quality, and full multi-tenant isolation.

#### U-7.1 OpenTelemetry tracing

Add Quarkus `quarkus-opentelemetry` extension. 

We need a re-usable, generic and flexible approach to tracing (use I_Trace contract to abstract) - Annotate:

- Every command/intent/action by agents.
- Every `I_LLM.chat()` call → span with model, token count, latency.
- Every connector `refresh()` → span with connector type, record count.
- Every MCP tool invocation → span with tool IRI, principal.
- Every SPARQL query over 100ms → span with query fingerprint.

Export to OTLP endpoint (configurable via `MY_OPENTELEMETRY_ENDPOINT`).


#### U-7.2 Prometheus metrics

Add `quarkus-micrometer-registry-prometheus`. Expose:

We need a re-usable, generic and flexible approach to metrics (use I_Metrics contract to abstract) - Annotate:

```
.metrics("urn:iq:llm:calls_total{provider, model, status}
.metrics("urn:iq:llm:tokens_total{provider, model, direction}
.metrics("urn:iq:connector:sync_duration_seconds{connector}
.metrics("urn:iq:mcp:tool_calls_total{tool, principal, status}
.metrics("urn:iq:rdf:triples_total{realm}
```

#### U-7.3 Centralised audit trail

Implement `AuditService` in `iq-platform`:

- Writes all audit events (tool calls, secret reads, FSM transitions, login attempts) to `iq:AuditLog` named graph.
- Exposes `GET /audit?from=&to=&principal=&event=` query endpoint.
- Configurable sink: RDF graph (default) + optional Elastic/Splunk HTTP sink via `IQ_AUDIT_SINK`.
- Enforces that audit events are immutable (no `DELETE` allowed on audit graph).

#### U-7.4 SHACL data quality

Add `iq validate` CLI command and `POST /validate` REST endpoint:

- Loads shapes from `--shapes <file>` or the repo itself.
- Runs RDF4J SHACL validator.
- Returns violations as SPARQL result set or ASCII table.
- Pre-commit hook in `iq-cli`: warn on violations before `backup` or `export`.

#### U-7.5 Multi-tenant repository isolation

Each realm gets a dedicated `RepositoryConnection` scope. 

Add `TenantIsolationFilter` (Quarkus `ContainerRequestFilter`) that:

1. Extracts realm IRI from JWT claim `iq:realm`.
2. Passes realm IRI to `RealmPlatform.getRealm(iri)`.
3. Rejects cross-realm references in SPARQL queries (federated SERVICE calls require explicit allowlist).

#### U-7.6 RBAC on REST API

Add `@RolesAllowed` annotations to `iq-apis` controllers using Quarkus security:

| Endpoint | Minimum role |
|---|---|
| `POST /ux/intent/{realm}` | `iq:operator` |
| `GET /kb/select` | `iq:reader` |
| `POST /kb/select` (update) | `iq:editor` |
| `GET /runtime/dump` | `iq:admin` |
| `POST /runtime/debug` | `iq:admin` |

Roles declared in realm graph; resolved by `TrustTokenFilter` via JWT claims.

#### U-7.7 Remove dead code and `.old` files

Automated cleanup:

```bash
find . -name "*.old" -not -path "*/target/*" -delete
```

Remove commented-out `iq-persona` module reference from root `pom.xml` or promote to a real module. Remove `ai.XXX()` / `ai.XXXX()` placeholder calls from `MY_IQ_AI.java`.

---

## Part IV — Summary Priority Matrix

| ID | Upgrade | Severity | Effort | Version |
|---|---|---|---|---|
| U-1 | Kernel alignment + CLI foundation | 🔴 Blocker | 2 weeks | 0.92 |
| U-2 | Cloud secrets backends | 🔴 Blocker | 1.5 weeks | 0.92 |
| U-3 | MCP production security | 🔴 Blocker | 1.5 weeks | 0.93 |
| U-4 | iq-cli-pro feature completion | 🔴 Blocker | 2 weeks | 0.93 |
| U-5 | Connector ecosystem | 🟠 High | 3 weeks | 0.94 |
| U-6 | LLM layer modernisation | 🟠 High | 2 weeks | 0.94 |
| U-7 | Observability, compliance, multi-tenancy | 🟠 High | 2 weeks | 0.95 |

**Total estimated scope:** ~14 productive engineering weeks for a single engineer; ~5–6 weeks with a small team (3 engineers).

---

## Part V — Conventions and Non-Negotiables

The following must hold across all upgrades:

1. **No hardcoded secrets.** All credentials via `SecretsProviderFactory`; env var fallback only in CI.
2. **RDF-first configuration.** Every behavioural policy (LLM provider, ACL, retry, quota) declared in `.ttl`; Java reads, not decides. No use of new namespaces. No hard-coded IRIs. Everything configurable. Smart defaults.
3. **Pipeline-first invocation.** Every external entry point (REST, MCP, CLI) passes through the kernel pipeline — auth → ACL → quota → audit — before reaching business logic.
4. **No pre-release versions in released artifacts.** Camel RC, Groovy alpha must be upgraded before any `0.92` tag.
5. **Test before merge.** Every new class requires at least one unit test. Every command requires a `TestCLIExecutor` scenario. Integration tests gated by `skipITs=false`.
6. **Idiomatic interfaces.** New public APIs extend existing `I_*` interfaces from `iq-abstract` or `iq-kernel`; do not introduce a fourth parallel exception hierarchy. Consolidate into `iq-abstract` or `iq-kernel` - MECE.
7. **Audit everything.** Secret reads, LLM calls, FSM transitions, and trust changes must each produce an audit event. Never log secret values.

