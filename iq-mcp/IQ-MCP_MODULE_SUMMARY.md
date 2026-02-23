# iq-mcp Module Creation Summary

## Overview

Successfully scaffolded the **iq-mcp** Maven module for IQ's Model Context Protocol (MCP) integration following project idioms and conventions.

## Module Structure

```
iq-mcp/
├── pom.xml  # Maven configuration
├── README.md# Module documentation
├── SPEC.md  # MCP tool specifications
└── src/
├── main/
│   ├── java/
│   │   └── systems/
│   │   └── symbol/
│   │   └── mcp/
│   │   ├── I_MCPAdapter.java   # High-level adapter contract
│   │   ├── I_MCPResult.java# Execution result contract
│   │   ├── I_MCPService.java# Server/coordinator contract
│   │   └── I_MCPToolManifest.java  # Tool definition contract
│   └── resources/   # (empty - config as RDF)
└── test/
└── java/
└── systems/
└── symbol/
└── mcp/
└── I_MCPAdapterTest.java # Smoke tests
```

## Files Created

### 1. **pom.xml** — Maven Configuration
- **Parent**: iq-parent (0.91.5)
- **Key Dependencies**:
  - `io.modelcontextprotocol.sdk:mcp:0.17.2` (MCP Java SDK)
  - `iq-abstract`, `iq-platform`, `iq-rdf4j` (IQ core libraries)
  - RDF4J 5.0.2, SLF4J, JUnit5
- **Properties**: Java 21, skipITs=true
- **Build**: Standard Maven compiler, surefire, jar plugins

### 2. **I_MCPAdapter.java** — High-Level Adapter Contract (5 methods)
```java
public interface I_MCPAdapter {
IRI getSelf();  // Adapter identity
I_Realm getRealm(); // Associated realm
I_MCPResult invoke(String toolName, Model input) 
throws SecretsException, Exception;// Execute tool
Collection<I_MCPToolManifest> listTools(); // Enumerate tools
I_MCPToolManifest getTool(String toolName);// Get tool manifest
}
```

**Pattern**: Bridges MCP string-based tool calls to IQ's RDF-first execution model.

### 3. **I_MCPToolManifest.java** — Tool Definition (7 methods)
Captures tool identity and governance:
- `getName()`, `getDescription()`, `getCategory()` — metadata
- `getInputShape()`, `getOutputShape()` — SHACL constraints
- `getAuthorizationQuery()` — SPARQL ASK for fine-grained access control
- `getRateLimit()`, `getCost()` — quotas and resource tracking

### 4. **I_MCPResult.java** — Execution Outcome (6 methods)
Encapsulates tool invocation results:
- `isSuccess()`, `getError()`, `getCause()` — status and diagnostics
- `getPayload()` — RDF Model result
- `getAudit()` — RDF audit trail (who, when, cost, signature)
- `getCost()`, `getDurationMillis()` — resource consumption tracking

### 5. **I_MCPService.java** — Coordinator (6 methods)
Manages multiple adapters and routes tool calls:
- `registerAdapter()`, `unregisterAdapter()` — adapter lifecycle
- `getAdapters()`, `getAdapterForTool()` — adapter discovery
- `listAllTools()`, `getTool()` — global tool registry
- `invokeTool()` — route and execute tools

### 6. **README.md** — Module Documentation
- Overview of MCP integration
- Quick-start deployment instructions
- Architecture diagram (LLM → MCP → Adapters → IQ components)
- Adapter pattern explanation
- Tool governance model (SHACL, authorization, rate limits)
- Tool mapping table (RDF components → MCP tools)
- Testing and packaging instructions

### 7. **SPEC.md** — MCP Tool Specifications (Formal)
Comprehensive tool contract definitions with:

#### Fact Adapter Tools (RDF/SPARQL)
- `fact.sparql-query`: SELECT/ASK/CONSTRUCT with SHACL input/output schemas
- `fact.sparql-update`: INSERT/DELETE operations with dry-run support
- `fact.describe`: Resource description with depth control

#### Actor Adapter Tools (Agentic Workflows)
- `actor.trigger`: State machine transitions via intent
- `actor.execute`: Action execution with context
- `actor.status`: Query agent state (cheap read-only)

#### Trust Adapter Tools (Identity)
- `trust.login`: JWT token issuance (OAuth, GitHub, Google, custom)
- `trust.refresh`: Token renewal

#### LLM Adapter Tools (Knowledge + Generation)
- `llm.invoke`: LLM completion with RDF context injection
- `llm.search`: Semantic search via embeddings

#### Realm Adapter Tools (Governance)
- `realm.export`: Full graph + config export (archive format)

**Each tool includes**:
- SHACL input/output schemas
- SPARQL ASK authorization queries
- Rate limits and cost models
- Example curl commands
- Implementation pointers (Java adapter class)

**Common audit trail**: RDF audit log format (prov:, iq: predicates, HMAC-SHA256 signatures)

### 8. **I_MCPAdapterTest.java** — Unit Tests (Smoke tests)
Verifies interface compilation and method counts:
- Load all 4 core interfaces
- Verify method counts (5, 6, 7, 6 respectively)

---

## Build Results

**Module Status**: ✅ **BUILD SUCCESS**

```
[INFO] IQ MCP ............................................. SUCCESS [ 13.349 s ]
[INFO] BUILD SUCCESS
```

**Artifacts Generated**:
- `/media/me/SD/iq/iq-mcp/target/iq-mcp-0.91.5.jar` (main library)
- `/media/me/SD/iq/iq-mcp/target/iq-mcp-0.91.5-tests.jar` (test classes)

**Root pom.xml**: Updated with `<module>iq-mcp</module>` registration

---

## Project Idioms Followed

1. **I_-Prefixed Interfaces**: All 4 core contracts follow IQ's naming convention
2. **RDF-First Design**: Tool manifests and policies stored as RDF/SHACL
3. **Realm-Based Identity**: Tools scoped to realms, executor identity tracked
4. **State Machine Pattern**: Actor tools leverage I_StateMachine contract
5. **Adapter Pattern**: Each tool category (fact, actor, trust, llm) is an adapter
6. **Governance-as-RDF**: Authorization via SPARQL ASK, audit trails as RDF
7. **Secrets Management**: Integration with I_Realm's vault (VFSPasswordVault, EnvsAsSecrets)
8. **Cost Tracking**: Every tool invocation tracked for quota + billing

---

## Integration Points

### With iq-run-apis
- MCPServer implementation will expose MCP protocol as Quarkus REST endpoints
- Expected endpoints: `POST /mcp/tools/{toolName}`, `GET /mcp/tools`

### With iq-platform
- LlmAdapter wraps I_LLM (GPTWrapper, LLMFactory)
- Tool contexts use I_Facade bindings

### With iq-rdf4j
- FactAdapter executes SPARQL on I_Realm repositories
- SHACL validation via RDF4J's built-in SHACL engine

### With Identity & Trust
- TrustAdapter delegates to TokenAPI (iq-run-apis)
- Authorization checks use existing iq:canQuery, iq:canExecute predicates

---

## Pending Adapter Implementations

The following adapters are **stubbed** in SPEC.md and ready for implementation:

1. **FactAdapter** — SPARQL query/update execution
2. **ActorAdapter** — Agent state machine transitions
3. **TrustAdapter** — JWT token issuance and refresh
4. **LlmAdapter** — LLM invocation with context injection
5. **RealmAdapter** — Realm inspection and export
6. **MCPRealmService** — Quarkus REST endpoint

Each adapter class will:
- Implement `I_MCPAdapter`
- Create tool manifests (implement `I_MCPToolManifest`)
- Execute tools and return `I_MCPResult` with audit trails
- Be registered with `MCPServer` at startup

---

## Testing Strategy

### Unit Tests (src/test/)
- Interface smoke tests (method counts, contracts)
- Mock-based adapter tests (no external dependencies)

### Integration Tests (-DskipITs=false)
- Real RDF repositories (`.iq/repositories/test`)
- LLM API calls (if `LLM_KEY` env var set; otherwise skipped)
- Agent state machine execution

---

## Next Steps

1. **Implement FactAdapter** — Wrap iq-rdf4j's SPARQL execution
2. **Implement ActorAdapter** — Wrap AgentService and state transitions
3. **Implement TrustAdapter** — Integrate TokenAPI (iq-run-apis/src/main/java/.../api/trust/)
4. **Implement LlmAdapter** — Wrap iq-platform's LLMFactory
5. **Create MCPRealmService** — Quarkus REST resource routing tool calls
6. **Add JSON ↔ RDF Serialization** — For non-RDF MCP clients
7. **Performance Benchmarks** — Tool throughput, latency, memory
8. **CI Integration** — GitHub Actions workflow for iq-mcp tests

---

## References

- **Module README**: [iq-mcp/README.md](iq-mcp/README.md)
- **Tool Specifications**: [iq-mcp/SPEC.md](iq-mcp/SPEC.md)
- **MCP Documentation**: [MCP.md](MCP.md)
- **Interface Inventory**: [INTERACES.md](INTERACES.md)
- **API Examples**: [iq-run-apis/docs/API_LLM.md](iq-run-apis/docs/API_LLM.md)

---

## Summary

The **iq-mcp** module has been successfully scaffolded with:
- ✅ 4 high-level interface contracts (I_MCPAdapter, I_MCPResult, I_MCPToolManifest, I_MCPService)
- ✅ Complete Maven build configuration with MCP SDK 0.17.2 dependency
- ✅ Comprehensive documentation (README.md + SPEC.md) with tool definitions, schemas, and examples
- ✅ Successful compilation and packaging (JAR artifact generated)
- ✅ Baseline test coverage for interface verification

The module is ready for adapter implementations that will bridge IQ's RDF-first architecture with the MCP protocol, enabling LLMs to query graphs, execute agents, and manage identity via standardized tool invocations.
