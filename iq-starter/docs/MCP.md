# MCP (Model Context Protocol) — Using IQ with Claude and LLMs

IQ is **MCP-first**: it exposes your knowledge graphs, workflows, and connectors as MCP tools that Claude, ChatGPT, and other LLMs can call directly.

## What is MCP?

[Model Context Protocol](https://modelcontextprotocol.io/) is a standard that lets LLMs access external tools and data sources. IQ implements MCP over HTTP, meaning:

- Your LLM can query your knowledge graph
- Your LLM can trigger workflows and agents
- Your LLM can inspect connector status
- Everything stays within your control (no API calls to third parties)

## Quick start: Claude + IQ (5 minutes)

### 1. Start IQ's MCP server

```bash
cd /path/to/iq/iq-starter
./bin/start-api
```

IQ now listens on `http://localhost:8080` and exposes MCP tools.

### 2. List available tools

```bash
curl http://localhost:8080/mcp/tools | jq .
```

Output (example):
```json
{
  "tools": [
    {
      "name": "query_knowledge_graph",
      "description": "Run SPARQL queries against the knowledge graph",
      "inputSchema": {
        "type": "object",
        "properties": {
          "sparql": { "type": "string", "description": "SPARQL query" }
        },
        "required": ["sparql"]
      }
    },
    {
      "name": "execute_workflow",
      "description": "Execute a state machine workflow with context and decisions",
      "inputSchema": { ... }
    },
    {
      "name": "list_connectors",
      "description": "Get status and configuration of all connectors"
    }
  ]
}
```

### 3. Call a tool via HTTP (for testing)

```bash
curl -X POST http://localhost:8080/mcp/tools/query_knowledge_graph/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sparql": "SELECT ?name ?email WHERE { ?x foaf:name ?name ; foaf:mbox ?email } LIMIT 5"
  }' | jq .
```

Response:
```json
{
  "results": [
    { "name": "Alice Corp", "email": "alice@acme.com" },
    { "name": "Bob Industries", "email": "bob@acme.com" }
  ]
}
```

### 4. Connect Claude Desktop (native MCP)

Edit `~/.claude/claude_desktop_config.json` (or `/Users/[user]/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "iq-local": {
      "command": "curl",
      "args": ["--json", "-X", "POST"],
      "env": {
        "IQ_HOST": "http://localhost:8080"
      }
    }
  }
}
```

Or simpler: use a local proxy script.

### 5. Try it in Claude

In Claude, type:
```
Show me the top 10 customers by revenue. Use the knowledge graph to find them.
```

Claude automatically calls the `query_knowledge_graph` MCP tool and gets your actual data.

---

## Available MCP Tools

### `query_knowledge_graph`
Run SPARQL queries without an LLM. Gets real data from your RDF store.

**Input:**
```json
{
  "sparql": "SELECT ?x WHERE { ?x rdf:type foaf:Person }"
}
```

**Output:**
```json
{
  "results": [ { "x": "http://example.com/alice" }, ... ]
}
```

### `execute_workflow`
Trigger a state machine, pass context, and get decision results.

**Input:**
```json
{
  "workflow_name": "customer_approval",
  "context": {
    "customer_id": "12345",
    "amount": 50000
  },
  "max_steps": 10
}
```

**Output:**
```json
{
  "final_state": "approved",
  "trace": [
    { "state": "pending", "decision": "check_credit_score" },
    { "state": "approved", "actions": ["send_email", "update_crm"] }
  ]
}
```

### `list_connectors`
See which connectors are configured and their status.

**Input:** (none)

**Output:**
```json
{
  "connectors": [
    {
      "name": "slack",
      "status": "connected",
      "config": { "channels": ["#alerts", "#logs"] }
    },
    {
      "name": "github",
      "status": "connected",
      "config": { "repos": ["org/repo1", "org/repo2"] }
    }
  ]
}
```

---

## Integration patterns

### Pattern 1: Knowledge-grounded responses
Claude asks IQ for facts, then answers based on real data.

```
User: "Who are our top customers?"
↓
Claude → MCP → IQ: query_knowledge_graph("SELECT ?customer ?revenue ...")
↓
IQ returns: [{ customer: "Acme Inc", revenue: "2.1M" }, ...]
↓
Claude: "Your top customer is Acme Inc with $2.1M in annual revenue."
```

### Pattern 2: Workflow execution via Claude
Claude triggers complex workflows through IQ.

```
User: "Approve the customer onboarding for account 12345"
↓
Claude → MCP → IQ: execute_workflow("customer_onboarding", { account_id: "12345" })
↓
IQ: Runs state machine, checks credit, sends emails, updates CRM
↓
Claude: "Onboarding approved. Customer email and welcome package sent."
```

### Pattern 3: Connector status checks
Claude monitors your infrastructure via IQ connectors.

```
User: "What's the status of all our integrations?"
↓
Claude → MCP → IQ: list_connectors()
↓
IQ returns: [{ name: "slack", status: "connected" }, { name: "aws", status: "error" }, ...]
↓
Claude: "Slack and GitHub are connected. AWS connector has an authentication error."
```

---

## Setting up MCP tools for specific use cases

### Adding a custom SPARQL query as an MCP tool

1. Create a SPARQL file in `examples/queries/`:
```sparql
# examples/queries/customers-by-revenue.sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

SELECT ?name ?revenue
WHERE {
  ?customer rdf:type foaf:Organization ;
            foaf:name ?name ;
            :revenue ?revenue .
}
ORDER BY DESC(?revenue)
LIMIT 10
```

2. Register it in `examples/.iq/queries.ttl`:
```turtle
:customer_revenue_report a iq:SavedQuery ;
  rdfs:label "Top 10 customers by revenue" ;
  iq:sparql_file "examples/queries/customers-by-revenue.sparql" ;
  iq:description "Returns organizations sorted by annual revenue" .
```

3. Now Claude can call: `query_knowledge_graph("SELECT from :customer_revenue_report")`

### Adding a workflow as an MCP tool

See [AGENTS.md](AGENTS.md) for creating workflows. Once defined in `.iq/workflows/`, they automatically become MCP tools.

---

## Troubleshooting MCP

### "Tools list is empty"
Check that IQ started with MCP enabled:
```bash
curl http://localhost:8080/mcp/status
# Should return: { "mcp": true, "ready": true }
```

### "Claude can't find the MCP server"
Check your `claude_desktop_config.json` path:
- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux:** `~/.config/Claude/claude_desktop_config.json`

Restart Claude after editing.

### "MCP calls are slow"
First call queries the knowledge graph (normal, ~1-2 sec). Cached queries are faster. For production, use persistent database (PostgreSQL). See [DEPLOYMENT.md](DEPLOYMENT.md).

### "Tool execution failed"
Check the error response:
```bash
curl -X POST http://localhost:8080/mcp/tools/query_knowledge_graph/execute \
  -H "Content-Type: application/json" \
  -d '{"sparql": "INVALID SPARQL"}' | jq .
# Shows parsing error
```

Fix your SPARQL or workflow definition and try again.

---

## Advanced: Hosting MCP on the cloud

IQ's MCP endpoints are standard HTTP POST, so you can:

1. **Deploy on AWS:** Use ECS Fargate, expose port 8080
2. **Deploy on Azure:** Use Container Instances or App Service
3. **Use tunneling:** `ngrok http 8080` to expose local IQ to Claude
4. **Internal network:** Keep IQ private within your VPC, use VPN for Claude

See [DEPLOYMENT.md](DEPLOYMENT.md) for cloud setup.

---

## API reference

### GET `/mcp/tools`
List all available MCP tools.

**Response:**
```json
{
  "tools": [
    {
      "name": "tool_name",
      "description": "What this tool does",
      "inputSchema": { ... }
    }
  ]
}
```

### POST `/mcp/tools/{tool_name}/execute`
Call a specific MCP tool.

**Request:**
```json
{
  "param1": "value1",
  "param2": "value2"
}
```

**Response:**
```json
{
  "result": "output",
  "metadata": { ... }
}
```

### GET `/mcp/status`
Check if MCP is healthy and ready.

**Response:**
```json
{
  "mcp": true,
  "ready": true,
  "tools_count": 5,
  "knowledge_graph_size": 2847
}
```

---

## Next steps

- **Try examples:** See [QUICKSTART.md](QUICKSTART.md) for MCP tool calls
- **Build workflows:** [AGENTS.md](AGENTS.md) shows how to create MCP-callable workflows
- **Deploy to cloud:** [DEPLOYMENT.md](DEPLOYMENT.md) for production MCP setup
- **Connect to Claude:** Use the native MCP integration shown above
