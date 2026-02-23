# iq-mcp: IQ Model Context Protocol Integration

Bridges **IQ's RDF-first agentic workflows** with the **Model Context Protocol (MCP)**, enabling LLMs and external systems to interact with IQ's knowledge graphs, agents, and trust zones via standardized tool invocations.

## Overview

MCP (Model Context Protocol 0.17.2) is a standardized protocol for connecting LLMs to data sources, APIs, and tools. The `iq-mcp` module provides:

1. **High-level contracts** (`I_MCPAdapter`, `I_MCPToolManifest`, `I_MCPResult`, `I_MCPService`) that follow IQ's I_-prefixed interface patterns.
2. **Tool adapters** that map MCP tool calls to IQ's RDF-first execution model:
   - `FactAdapter`: SPARQL queries (`sparql.query`, `sparql.update`)
   - `ActorAdapter`: Agent execution (`actor.trigger`, `actor.execute`)
   - `TrustAdapter`: Token issuance (`trust.login`)
   - `LlmAdapter`: LLM invocation with context (`llm.invoke`)
   - `RealmAdapter`: Realm inspection and export (`realm.export`)
3. **MCPServer** implementation: Quarkus REST endpoint publishing MCP protocol and routing tool calls to adapters.

## Key Interfaces

### `I_MCPAdapter`
High-level contract for MCP adapters. Defines:
- `getSelf()`: adapter identity (IRI)
- `getRealm()`: associated realm (I_Realm)
- `invoke(toolName, inputModel)`: executes a named tool
- `listTools()`: enumerates available tools
- `getTool(toolName)`: retrieves tool manifest

```java
I_MCPResult result = adapter.invoke("sparql.query", sparqlModel);
```

### `I_MCPToolManifest`
Tool identity and governance. Includes:
- `getName()`: tool name (e.g., "sparql.query")
- `getDescription()`: human-readable description
- `getInputShape()`: SHACL shape for input parameters
- `getOutputShape()`: SHACL shape for output format
- `getAuthorizationQuery()`: SPARQL ASK for fine-grained access control
- `getRateLimit()`, `getCost()`: quotas and resource tracking

### `I_MCPResult`
Execution outcome. Captures:
- `isSuccess()`: success/failure status
- `getPayload()`: result as RDF Model
- `getError()`: error message if failed
- `getAudit()`: RDF audit trail (who, when, cost)
- `getCost()`, `getDurationMillis()`: resource consumption

### `I_MCPService`
Coordinator for multiple adapters. Manages:
- adapter registration/unregistration
- global tool registry
- tool discovery and invocation routing
- cross-realm authorization

## IQ → MCP Tool Mapping

See [MCP.md#iq--mcp-tools](../MCP.md#iq--mcp-tools) for detailed API documentation.

| IQ Component | MCP Tools | REST API |
|---|---|---|
| **RDF4J Repository** | `sparql.query`, `sparql.update` | POST /sparql |
| **Agent Workflows** | `actor.trigger`, `actor.execute`, `actor.status` | POST /ux/intent/{realm} |
| **Trust/Identity** | `trust.login`, `trust.refresh` | POST /trust/token/{realm}/{provider} |
| **LLM Invocation** | `llm.invoke`, `llm.search` | POST /ux/llm/{realm} |
| **Realm Management** | `realm.export`, `realm.import` | GET/POST /ux/realm/{realm} |

## Quick Start

### 1. Deploy MCP Server

```bash
# Build iq-mcp module
mvn clean install -pl iq-mcp -am

# Start iq-run-apis with MCP server enabled
./mvnw compile quarkus:dev -pl iq-run-apis -am -Dmcp.enabled=true
```

### 2. Register Adapters

```java
I_MCPService server = MCPServerFactory.createServer(realm);
server.registerAdapter(new FactAdapter(realm));
server.registerAdapter(new ActorAdapter(realm));
server.registerAdapter(new TrustAdapter(realm));
```

### 3. Invoke Tools

**Via REST API:**
```bash
curl -X POST http://localhost:8080/mcp/tools/sparql.query \
  -H "Content-Type: application/rdf+xml" \
  -d @sparql-input.rdf
```

**Programmatically:**
```java
Model input = createSparqlQueryModel("SELECT ?x WHERE {...}");
I_MCPResult result = server.invokeTool("sparql.query", input);

if (result.isSuccess()) {
    Model facts = result.getPayload();
    // Process RDF facts
} else {
    System.err.println(result.getError().get());
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│          LLM (Claude, GPT-4, etc.)                       │
│          with MCP Protocol                              │
└────────────┬────────────────────────────────────────────┘
             │ (MCP tool calls: { tool, input })
             │
┌────────────▼────────────────────────────────────────────┐
│          MCPServer (Quarkus REST endpoint)              │
│          - Tool discovery (MCP list_tools)              │
│          - Tool execution (MCP call_tool)               │
└────────────┬────────────────────────────────────────────┘
             │
      ┌──────┴──────┬─────────────┬──────────────┐
      │             │             │              │
      ▼             ▼             ▼              ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│   Fact   │ │  Actor   │ │  Trust   │ │   LLM    │
│ Adapter  │ │ Adapter  │ │ Adapter  │ │ Adapter  │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │             │
     ▼            ▼            ▼             ▼
  RDF4J       I_Agent       TokenAPI      LLMFactory
  (SPARQL)  (State Mach.)   (Identity)   (OpenAI/Groq)
     │            │            │             │
     └────────────┴────────────┴─────────────┘
            │
            ▼
     I_Realm (fact graph + secrets)
```

## Adapter Pattern

Each adapter implements `I_MCPAdapter` and exposes a set of tools:

```java
public class FactAdapter implements I_MCPAdapter {
    
    private final I_Realm realm;

    public FactAdapter(I_Realm realm) {
        this.realm = realm;
    }

    @Override
    public IRI getSelf() {
        return vf.createIRI(IQ.NS, "FactAdapter");
    }

    @Override
    public I_Realm getRealm() {
        return realm;
    }

    @Override
    public I_MCPResult invoke(String toolName, Model inputModel) throws Exception {
        return switch (toolName) {
            case "sparql.query" -> executeSparqlQuery(inputModel);
            case "sparql.update" -> executeSparqlUpdate(inputModel);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    @Override
    public Collection<I_MCPToolManifest> listTools() {
        return List.of(
            new SparqlQueryManifest(),
            new SparqlUpdateManifest()
        );
    }
}
```

## Tool Governance

Each tool's manifest includes:
- **SHACL shapes**: Validate input parameters and output format
- **Authorization queries**: SPARQL ASK checks (e.g., "Does caller have iq:canExecute?")
- **Rate limits**: Prevent abuse (e.g., "1000 queries/min per actor")
- **Cost tracking**: LLM tokens, compute units, storage quota

Example SHACL shape for `sparql.query`:
```ttl
:SparqlQueryShape a sh:NodeShape ;
  sh:targetNode :SparqlQueryInput ;
  sh:property [
    sh:path rdf:value ;
    sh:datatype xsd:string ;
    sh:description "SPARQL SELECT query" ;
    sh:minCount 1
  ] .
```

## Testing

```bash
# Run unit tests (mocked adapters)
mvn test -pl iq-mcp

# Run integration tests (real RDF repositories, LLM calls if API keys set)
mvn -DskipITs=false verify -pl iq-mcp
```

## Building & Packaging

```bash
# Compile iq-mcp module
mvn compile -pl iq-mcp -am

# Build JAR (included in iq-run-apis)
mvn package -pl iq-run-apis -am

# Build OCI image with MCP support
mvn -Dquarkus.container-image.build=true install -pl iq-run-apis -am
```

## Dependencies

- `io.modelcontextprotocol.sdk:mcp` (0.17.2): Official MCP Java SDK (Reactive Streams)
- `iq-abstract`, `iq-platform`, `iq-rdf4j`: Core IQ libraries
- `org.eclipse.rdf4j:rdf4j-runtime` (5.0.2): RDF storage and SPARQL
- `org.slf4j:slf4j-api`: Logging
- `junit-jupiter`: Testing

## Next Steps

- [ ] Implement core adapters: FactAdapter, ActorAdapter, TrustAdapter, LlmAdapter
- [ ] Create SPARQL-based tool manifests (SHACL shapes)
- [ ] Add audit trail middleware (logging tool calls to RDF)
- [ ] Integrate with iq-run-apis REST endpoints
- [ ] Add JSON ↔ RDF translation layer (for non-RDF LLM clients)
- [ ] Performance benchmarking (tool throughput, latency)

## References

- [MCP.md](../MCP.md): MCP protocol documentation and IQ tool mapping
- [INTERACES.md](../INTERACES.md): Contract inventory (52 I_-prefixed interfaces)
- [iq-run-apis/docs/API_LLM.md](../iq-run-apis/docs/API_LLM.md): LLM API examples
- [Official MCP Documentation](https://modelcontextprotocol.io/)
