# QUICKSTART — 5 minutes to IQ via MCP

IQ is **MCP-first**: it runs as an HTTP server exposing MCP tools, perfect for any LLM (Claude, ChatGPT, local models, etc.).

## Prerequisites

- Java 21+ (Maven will download if needed)
- 4GB free RAM

## Step 1: Start the MCP server (2 minutes)

```bash
cd ./iq-starter
./bin/start
```

You'll see startup logs ending with:
```
2024-03-31 12:34:58 INFO  HTTP server listening on 0.0.0.0:8080
2024-03-31 12:34:59 INFO  MCP endpoints available at /mcp/*
```

The server is ready when you see that message.

> **First run slow?** Normal—Java is JIT-compiling and indexing your knowledge graph. ~30 seconds first time, 5 seconds after.

## Step 2: List available MCP tools (1 minute)

Open a **new terminal** and list your MCP tools:

```bash
curl http://localhost:8080/mcp/tools 2>/dev/null | jq .
```

You'll see:
```json
{
  "tools": [
{
  "name": "query_knowledge_graph",
  "description": "Run SPARQL queries against your knowledge graph",
  "inputSchema": { ... }
},
{
  "name": "execute_workflow",
  "description": "Trigger a state machine with decision logic"
}
  ]
}
```

These tools are now **available to any MCP-compatible LLM**.

## Step 3: Call an MCP tool (1 minute)

Execute the knowledge graph query tool:

```bash
curl -X POST http://localhost:8080/mcp/tools/query_knowledge_graph/execute \
  -H "Content-Type: application/json" \
  -d '{"sparql": "SELECT DISTINCT ?name WHERE { ?x foaf:name ?name } LIMIT 5"}' \
  2>/dev/null | jq .
```

Response:
```json
{
  "results": [
{ "name": "Alice Corp" },
{ "name": "Bob Industries" }
  ]
}
```

## Step 4: Connect your LLM to IQ

**MCP works with any LLM:** Claude, ChatGPT, local Llama, Mistral, or any model with MCP support.

### Option A: Claude Desktop (native MCP)
1. Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or equivalent (Windows/Linux)
2. Add:
```json
{
  "mcpServers": {
"iq": {
  "command": "curl",
  "args": ["http://localhost:8080/mcp/tools"],
  "env": {}
}
  }
}
```
3. Restart Claude, now ask: *"Show me all customers in the knowledge graph"*

Your LLM will automatically use the `query_knowledge_graph` tool.

The same configuration works with any LLM client that supports MCP.

### Option B: HTTP client (any LLM)
Any LLM with HTTP support can call:
```
POST http://localhost:8080/mcp/tools/{tool_name}/execute
Content-Type: application/json

{ "parameter1": "value", ... }
```

## Step 5: Query your knowledge graph directly (REST API fallback)

If you want to query without an LLM:

```bash
./bin/demo-query examples/queries/customers.sparql
```

Output:
```
?name | ?email | ?revenue
------|--------|----------
Alice Corp | alice@acme.com | 1200000
Bob Inc | bob@acme.com | 850000
```

---

## What just happened?

| Component | Purpose |
|---|---|
| **iq-apis (port 8080)** | HTTP server with MCP endpoints + REST API + Web UI |
| **/mcp/tools** | Lists all available MCP tools for your LLM |
| **/mcp/tools/NAME/execute** | Calls a specific tool with parameters |
| **Knowledge graph** | RDF store with your domains, workflows, and configs |

**MCP is the "LLM interface," REST API is the "developer interface."** Use whichever fits your workflow.

---

## Next steps

- **Use IQ with any LLM:** See [MCP.md](MCP.md) for detailed integration patterns
- **Explore use cases:** [Use Cases](USECASES.md) — RAG, workflows, multi-tenancy
- **Connect systems:** [Connectors](CONNECTORS.md) — Slack, GitHub, AWS, databases
- **Deploy to cloud:** [Docker & Cloud](DEPLOYMENT.md)
- **Understand MCP:** [MCP HTTP Guide](MCP.md)

---

## Troubleshooting

**"Port 8080 already in use"**
```bash
./bin/start-api --port 8081
# Then update MCP calls to http://localhost:8081/mcp/tools
```

**"MCP endpoints not found"**
Check IQ started with MCP enabled:
```bash
curl http://localhost:8080/health
```
Should return `{ "status": "running", "mcp": true }`.

**"SPARQL returns empty"**
Your knowledge graph might be empty. Load examples:
```bash
./bin/import-example
```

**"No jq command"**
Install jq (`brew install jq` or `apt-get install jq`) or use Python:
```bash
curl http://localhost:8080/mcp/tools | python3 -m json.tool
```

---

**Ready?** Connect your LLM and start building. See [MCP.md](MCP.md). 🚀
