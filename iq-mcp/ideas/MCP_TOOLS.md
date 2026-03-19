# MCP for IQ

**IQ is a Knowledge + Action Fabric**: We bridge LLM reasoning capabilities with IQ's neuro-symbolic fact graphs, SPARQL query engine, and agentic workflows. 

LLM becomes a **reasoning agent that operates over declarative, auditable RDF graphs** — not just a code executor.


| Tool | Delegates To | Rate Limit | Audited |
|------|--------------|-----------|---------|
| sparql.query | FactAdapter | 100/min | ✓ |
| sparql.update | FactAdapter | 10/min | ✓ |
| rdf.describe | FactAdapter | 50/min | ✓ |
| rdf.walk | FactAdapter | 50/min | ✓ |
| llm.invoke | LlmAdapter | 10/min | ✓ |
| llm.status | LlmAdapter | 10/min | ✓ |
| actor.trigger | AgentAdapter | 20/min | ✓ |
| actor.execute | AgentAdapter | 20/min | ✓ |
| actor.status | AgentAdapter | 20/min | ✓ |
| realm.export | FactAdapter | 5/min | ✓ |
| realm.search | SearchAdapter | 15/min | ✓ |
| realm.status | RealmAdapter | 15/min | ✓ |
| realm.schema | FactAdapter | 15/min | ✓ |

---

## 
1. RDF-as-contract: Tools, roles, constraints, ACLs, and tool manifests are inferred from existing RDF resources in the fact graph — discoverable by SPARQL. 💡
2. Query-first interface: Encourage clients to read/understand (SPARQL) before acting (tools). Prefer `SELECT`/`CONSTRUCT` over pushing writes. 🔎
4. Governance-as-RDF + SHACL: Use SHACL shapes to validate inputs and SPARQL `ASK` checks for access guards; store policies in the realm graph so they are auditable and editable. 🛡️
5. Adapters wrap IQ components (SPARQLExecutor, ScriptRunner, LLMFactory, WorkflowExecutor, Vault) applying a standard governance wrapper (trust checks, quotas, auditing). ♻️

---

## Runtime Adapter Pattern (Idiomatic + Abstract)
- `MCPAdapterBase` (generic): accepts (role, realm, toolIri, inputModel)
  - 1) Resolve tool manifest (IQScripts / ModelScriptCatalog)
  - 2) Validate inputs using SHACL shapes stored in graph
  - 3) Enforce trust zone via ASK queries (policy -> true/false)
  - 4) Rate-limit and cost-check using RDF counters and `CostTracker` (stored in graph)
  - 5) Execute: delegate to SPARQLExecutor | ScriptRunner | WorkflowExecutor | LLMAdapter
  - 6) Audit: write structured event to `mcp:audit` graph (no secrets)

---

## Tool types & execution modes

## Governance modeled as RDF (why this is idiomatic)
- Policies and quotas already appear as model-first. Representing governance as RDF keeps a single source-of-truth in the fact graph, simplifies auditing, and enables SPARQL-based analysis and tests.

---

## MCP for Java (summary) ✅

**Official SDK & docs**
- The official Java SDK is maintained at `https://github.com/modelcontextprotocol/java-sdk` and the reference documentation lives at `https://modelcontextprotocol.io/sdk/java/mcp-overview`. A Spring integration (`mcp-spring` / Spring AI MCP) provides client and server starters for Spring Boot projects.

**Quickstart (Maven)**
```xml
<dependency>
  <groupId>io.modelcontextprotocol.sdk</groupId>
  <artifactId>mcp</artifactId>
  <version>0.17.2</version> 
</dependency>
```
<!-- use the latest version from Maven Central -->

**Key points**
- API model: **Reactive Streams** (Project Reactor) for async/streaming; synchronous facades provided for blocking use cases.
- JSON: zero-dependency `mcp-json` abstraction; default Jackson modules (`mcp-jackson2` / `mcp-jackson3`).
- Transports: JDK `HttpClient` (client) and Servlet/Jakarta (server) are built-in. Spring WebClient, WebFlux, WebMVC modules are available. SDK also supports STDIO and streaming transports (SSE / streamable HTTP).
- Security: **pluggable authorization hooks** (integrate Spring Security, MicroProfile JWT or custom solutions at transport layer).
- BOM and modules: `mcp-bom`, `mcp-core`, `mcp-json`, `mcp-jackson2/3`, `mcp-spring` (pick modules needed).

**Integrating with IQ (Quarkus / Java 21)**
- Client usage: consume MCP servers using the SDK client (JDK HttpClient transport) directly from IQ modules.
- Server usage in Quarkus: the Java SDK server APIs are transport-agnostic; prefer implementing a Servlet or Vert.x transport adapter for Quarkus (or reuse `mcp`'s Servlet core). Reuse the SDK's JSON binding and authorization hooks, and choose the synchronous or reactive facade to match Quarkus execution model.
- Tip: use `mcp-bom` to pin dependencies and `mcp-jackson3` if the project uses Jackson 3.

**References & resources**
- GitHub: `https://github.com/modelcontextprotocol/java-sdk` 🔗
- Docs: `https://modelcontextprotocol.io/sdk/java/mcp-overview` 🔗

---

## IQ → MCP tools: mapping key RDF terms to runtime tools 🔧

### iq:trusts (trust registration / token issuance)
- RDF role: `iq:trusts` links an *issuer* (service/URL) to a realm or secret resource (e.g., `<https://github.com> iq:trusts my:MY_GITHUB_SECRET`).
- Runtime/API: implemented by `systems.symbol.controller.trust.TokenAPI` (POST `trust/token/{realm}/{provider}` + `trust/refresh/{realm}`). The runtime resolves an agent flow (AgentBuilder → agent.start()) and records `issuer TRUSTS self` when trust is established (`trusting(...)`).
- MCP tool mapping: conceptual tool `trust.login` (alias: `trust.token.login`)
  - Inputs: `realm` (string), `provider` (string), provider params in bindings (code/state)
  - Outputs: `access_token` (JWT), `roles` (array), `trusted` (boolean)
  - Checks: policy/ASK checks, add auditing to `mcp:audit`, enforce trust zone constraints before returning tokens.
- See: `.iq/lake/.../trust/github/index.ttl` (trust flow example), `TokenAPI.java` (implementation) ✅

---

### iq:to (workflow transitions)
- RDF role: `iq:to` models edges in a finite-state `iq:Workflow` (e.g., `:draft iq:to :approved`).
- Runtime/API: transitions are executed via agent state machines. `systems.symbol.controller.ux.IntentAPI` (POST `ux/intent/{realm}`) accepts `AgentAction` (actor, intent, bindings) and calls `AgentService.next(intent)` → `I_StateMachine.transition(...)`.
- MCP tool mapping: maps directly to `actor.execute` / `actor.trigger` tool(s)
  - Inputs: `actor` (IRI string), `intent` (target state IRI), optional `bindings` (input model)
  - Outputs: resulting `state`, possible `transitions`, updated working memory (RDF model)
  - Checks: JWT-based auth, SHACL validation on inputs, `ASK` governance checks before state change.
- See: `iq-run-apis/src/main/resources/assets/fsm.ttl`, `IntentAPI.java`, `AgentService.java` ✅

---

### rdf:value (payload carrier for queries, prompts, messages)
- RDF role: `rdf:value` is used as the canonical ***REMOVED*** container for message content, SPARQL text, or prompt templates (e.g., `:Message rdf:value "..."` or `.iq` resources containing SPARQL via `rdf:value`).
- Runtime/API: FactAdapter / IQScriptCatalog / ScriptRunner read `rdf:value` when executing queries or prompts. Stored queries and prompts (in `.iq` graphs) are canonical tool inputs.
- MCP tool mapping: used as the input field for `sparql.query`, `sparql.update`, and `llm.invoke` tools
  - Pattern: a tool manifest or input model may reference a resource IRI; the adapter loads `rdf:value` as the content to execute (SPARQL text or prompt template), resolving bindings from the call context.
- See: `iq-run-apis/target/test-classes/assets/index.ttl` (messages) and `.iq` backup files where `rdf:value` contains SPARQL snippets ✅

---

### iq:Agent (agent resources & lifecycle)
- RDF role: `iq:Agent` marks resources with an associated `iq:workflow` and executable behaviour. An agent exposes working memory (`thoughts`) and a state machine.
- Runtime/API: Agents are instantiated with `AgentBuilder`, expose lifecycle APIs (`start`, `stop`) and provide an interface to the state machine (`getStateMachine()`). `AgentAction` + `IntentAPI` is the primary external invocation surface.
- MCP tool mapping: maps to `actor.trigger`, `actor.execute`, `actor.status` tools
  - `actor.trigger` / `actor.execute`: invoke an agent intent (see `ux/intent`), request transition or run an action.
  - `actor.status`: query agent current state and transitions, and optionally `rdf.describe` the agent's working memory.
  - Security: calls must be authorized (JWT), audited, and may be constrained by realm-based `ASK` policies.
- See: `iq-platform/src/main/java/systems/symbol/agent/*`, `IntentAPI.java`, `AgentService.java` ✅

---

## Examples (quick call patterns)
- Trust login (provider flow)
  - POST /trust/token/{realm}/{provider}
  - Body: provider params (code/state, callback bindings)
  - Response: { "access_token": "<jwt>" }

- Agent intent (workflow transition)
  - POST /ux/intent/{realm}
  - Body: { "actor": "urn:..", "intent": "urn:..", "state": { ... } }
  - Response: { "actor": "...", "intent": "<new-state>", "next": [ ... ], "state": { ... } }

---

### Implementation pointers & next steps
- Recommend adding small MCP tool manifests (RDF) for:
  - `trust.token.login` (bind provider flow → aud claim generation)
  - `actor.execute` (input schema + SHACL shape for AgentAction)
  - `sparql.query` (resource input schema that reads `rdf:value`)
- Add tests that exercise the MCP mapping: e.g., a conformance test that calls `ux/intent` via the MCP Java SDK client and asserts audit + provenance triples.

---
- Spring AI MCP docs: `https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html` 🔗
- Maven Central listing: `https://central.sonatype.com/artifact/io.modelcontextprotocol.sdk/mcp` 🔗

---
