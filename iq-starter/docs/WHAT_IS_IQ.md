# What is IQ?

IQ is an **enterprise knowledge graph platform** designed for modern AI applications. It orchestrates data, workflows, and decision logic across your entire technology stack—seamlessly bridging LLMs, databases, APIs, and business processes.

## Core capabilities

### 1. **Knowledge Graph (RDF/SPARQL)**
- Model your domain once, query from anywhere
- Semantic relationships beyond SQL
- Real-time reasoning and inference
- SPARQL querying with optional rules and constraints

### 2. **MCP-First Integration**
- Expose any knowledge graph operation as an MCP tool for Claude, ChatGPT, or local LLMs
- No wrapper code needed—IQ auto-exposes tools
- Multi-turn reasoning with persistent context

### 3. **Workflow Orchestration**
- State machines with decision logic
- Agent playbooks and multi-step workflows
- Real-time event handling (Kafka, webhooks, message queues)
- Conditional routing and complex orchestration

### 4. **Connector Library**
- 20+ pre-built integrations: AWS, Azure, Slack, GitHub, databases, data warehouses
- REST API + SDK support
- Secure credential management

### 5. **REST API + CLI**
- Full HTTP API for direct access
- CLI tools for batch operations and local exploration
- Web UI for visualization and debugging

---

## Three runtimes

IQ can run anywhere:

| Runtime | Use Case | Startup |
|---------|----------|---------|
| **iq-apis** | Production MCP server, REST API, Web UI | ~30 sec via Quarkus |
| **iq-cli** | Batch queries, local automation, scripts | <1 sec, no server |
| **iq-agentic** | Complex multi-step agent workflows | Handles long-running tasks |

---

## Common use cases

### **Retrieval-Augmented Generation (RAG)**
Query your knowledge graph from Claude / ChatGPT as an MCP tool. The LLM asks questions, IQ returns semantic answers.

```
User: "Who are our top 5 customers by revenue?"
↓
Claude uses MCP tool: query_knowledge_graph()
↓
IQ executes SPARQL, returns structured data
↓
Claude formats and presents answer
```

### **Workflow Automation**
State machines + connectors. Trigger actions in Slack, AWS, Salesforce from decision logic.

```
RDF rule: IF (customer.risk > 0.8) THEN alert(legal-team)
↓
Workflow executor monitors graph changes
↓
Sends message to Slack via iq-connect-slack
```

### **Multi-Tenant SaaS**
Isolate knowledge graphs per tenant, use connectors for tenant-specific data sources.

```
iq-apis → [Tenant A KG] [Tenant B KG] [Tenant C KG]
```

### **Decision Support**
Complex decision logic in SPARQL/RDF instead of buried in Python code.

```
SPARQL query with 50+ rules
↓ Executed in milliseconds
↓ Returns ranked recommendations
```

---

## Architecture overview

```
┌─────────────────────────────────────────┐
│Your LLM (Claude, ChatGPT)   │
└────────────────┬────────────────────────┘
 │ MCP Protocol (HTTP)
┌────────────────▼────────────────────────┐
│IQ MCP Server (iq-apis)  │
├──────────────────────────────────────────┤
│ • MCP Tool Endpoint/mcp/tools│
│ • REST API /api/*│
│ • Web Dashboard/ui   │
├──────────────────────────────────────────┤
│ Knowledge Graph Executor│
│ • SPARQL Query Engine   │
│ • RDF Reasoning Engine  │
│ • Workflow Orchestrator │
├──────────────────────────────────────────┤
│ Connector Layer │
│ • AWS, Azure, Slack, GitHub, DB...  │
│ • Credential Manager & VaultSecret  │
├──────────────────────────────────────────┤
│ Storage Backend │
│ • In-Memory RDF Store (dev/test)│
│ • RDF4J Persistent Store (production)   │
│ • Custom SPARQL Endpoint (enterprise)   │
└──────────────────────────────────────────┘
```

---

## Why IQ?

### Compared to traditional RAG
- **IQ:** Semantic relationships, reasoning, workflow integration
- **Traditional RAG:** Vector similarity + keyword search

### Compared to rule engines
- **IQ:** Interoperable (MCP, REST, CLI), built for LLMs
- **Rule Engines:** Often require proprietary languages and tight coupling

### Compared to workflow platforms
- **IQ:** Knowledge-driven (your logic is queryable, inspectable data)
- **Platforms:** Process-driven (workflows are black boxes)

---

## Getting started

1. **5-minute quickstart:** [QUICKSTART.md](QUICKSTART.md)
2. **Connect your LLM:** [MCP.md](MCP.md)
3. **Build a use case:** [USECASES.md](USECASES.md)
4. **Deploy to cloud:** [DEPLOYMENT.md](DEPLOYMENT.md)

---

**Questions?** See [FAQ.md](FAQ.md) or check the examples in `examples/` directory.
