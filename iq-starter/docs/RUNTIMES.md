# RUNTIMES — How to use IQ (three entry points)

IQ is **MCP-first**: built to integrate with any LLM (Claude, ChatGPT, Llama, Mistral, etc.) as a native tool. But you can also use it as a REST API or CLI depending on your needs.

## Overview

| Runtime | Best for | Start with |
|---|---|---|
| **MCP** | Any LLM access to enterprise data (PRIMARY) | `./bin/start-api` |
| **REST API** | Web apps, mobile, direct API calls | `./bin/start-api` |
| **CLI** | Data exploration, analytics, operations | `./bin/start-cli` |

All three start from the same `./bin/start-api` server; they're just different interfaces to the same knowledge graph.

---

## 1. Model Context Protocol (MCP) — PRIMARY USE CASE

### What it is

A protocol that lets any LLM (Claude, ChatGPT, Llama, Mistral, or any model with MCP support) call IQ as a tool. The LLM can ask "What's our revenue?" and IQ answers with actual data from your knowledge graph, without hallucinating.

MCP endpoints are HTTP-based, exposed by the same `iq-api` server that powers REST and CLI. No separate process needed.

### When to use

- Giving any LLM direct access to your enterprise data (PRIMARY use case)
- Building AI assistants that need ground truth
- Reducing hallucinations in LLM responses
- Integrating IQ with existing AI workflows

### Quick start

```bash
./bin/start-api
# MCP endpoints now listening on http://localhost:8080/mcp/*
```

Then test MCP:
```bash
curl http://localhost:8080/mcp/tools | jq .
```

Connect your LLM (edit `~/.config/Claude/claude_desktop_config.json` for Claude, or use appropriate config for your LLM):
```json
{
  "mcpServers": {
"iq-knowledge": {
  "command": "curl",
  "args": ["--json", "-X", "POST", "http://localhost:8080/mcp/tools/query_knowledge_graph/execute"]
}
  }
}
```

### What your LLM can do

```
User: "Show me the top 5 customers by revenue"

Your LLM → calls MCP tool query_knowledge_graph
IQ: Executes SPARQL, returns actual data from knowledge graph
Your LLM: "Your top 5 customers are... [with real numbers]"
```

No hallucinations. Real data. Every time.

### Available MCP tools (HTTP endpoints)

All available at `http://localhost:8080/mcp/tools`:

- **query_knowledge_graph** — Run SPARQL queries against your knowledge graph
- **execute_workflow** — Trigger state machines and workflows
- **list_connectors** — Get status of all configured connectors (AWS, Slack, GitHub, etc.)
- **read_connector** — Fetch fresh data from an external system
- **get_entity** — Fetch details about a specific customer, order, etc.
- **search_knowledge** — Full-text search across your facts

### Learn more

Full MCP guide: [MCP.md](MCP.md)

---

## 2. REST API (For web apps, mobile, direct integration)

### What it is

A Quarkus-based HTTP server that exposes IQ's capabilities as REST endpoints. Use it to integrate IQ into your web app, mobile app, or third-party system.

### When to use

- Building a web UI or dashboard
- Integrating with an existing SaaS or mobile app
- Creating a chatbot without LLM integration
- Exposing AI capabilities to external partners
- When you don't want to use an LLM client

### Quick start

```bash
./bin/start-api
# Server starts on http://localhost:8080
```

### Endpoints (main APIs)

#### Chat API
Send a message, get a grounded answer.

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
"realm": "acme-corp",
"message": "What is the status of order #12345?"
  }'
```

#### Agent API
Trigger a workflow (multi-step decision-making).

```bash
curl -X POST http://localhost:8080/agent/trigger \
  -H "Content-Type: application/json" \
  -d '{
"realm": "acme-corp",
"intent": "approve-purchase-order",
"object_id": "po-98765",
"requester": "alice@acme.com"
  }'
```

#### SPARQL Query Endpoint
Direct access to the knowledge graph (for power users).

```bash
curl -X POST http://localhost:8080/sparql \
  -H "Content-Type: application/sparql-query" \
  -d 'SELECT ?customer ?revenue WHERE {
?customer a :Customer ;
  :revenue ?revenue .
FILTER (?revenue > 1000000)
  }'
```

### Configuration

Edit `.iq/config.properties`:
```properties
quarkus.http.port=8080
iq.realm.default=acme-corp
iq.llm.provider=openai
iq.llm.model=gpt-3.5-turbo
iq.auth.jwt.secret=your-secret-key
```

### Running in production

```bash
./bin/start-api --mode=production
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-... iq-api:latest
```

---

## 3. Command-Line Interface (CLI)

### What it is

An interactive shell for exploring knowledge graphs, running queries, and managing agents. Great for data engineers, operations, and exploratory analysis.

### When to use

- Exploring what knowledge you have
- Running ad-hoc SPARQL queries
- Testing agents before deploying
- Batch ingestion or export
- Troubleshooting knowledge gaps

### Quick start

```bash
./bin/start-cli

iq> help
iq> list-realms
iq> use-realm acme-corp
iq> search "customers in California"
```

### Common commands

```bash
# Explore realms
iq> list-realms
iq> use-realm my-project

# Explore knowledge
iq> list-facts --limit 10
iq> describe :Order/12345

# Run SPARQL
iq> query examples/queries/top-customers.sparql
iq> query --file my-analysis.sparql --output csv

# Agents
iq> agent list
iq> agent run approve-purchase-order po-98765 requester=alice@acme.com

# Import/export
iq> import-file customers.ttl
iq> export-realm my-project result.ttl
```

### Batch mode

```bash
iq-cli < commands.txt
```

### Configuration

```bash
export IQ_HOME=/path/to/.iq
export IQ_REALM=acme-corp
./bin/start-cli
```

---

## Comparison table

| Feature | MCP | REST API | CLI |
|---|---|---|---|
| **Best for** | LLM integration | Web apps, mobile | Data analysis |
| **Startup time** | 5-10 sec | 5-10 sec | 5-10 sec |
| **Scalability** | High | High | Medium |
| **No LLM needed** | Yes (standalone) | ✓ Yes | ✓ Yes |
| **User type** | AI engineers | Developers | Data engineers |

---

## Advanced: Running all three together

All three are available simultaneously from one `./bin/start-api` process:

```bash
./bin/start-api
# Provides:
# - MCP endpoints at POST http://localhost:8080/mcp/tools/{name}/execute
# - REST API at POST http://localhost:8080/chat, /agent/trigger, /sparql
# - CLI accessible via ./bin/start-cli
```

All three share the same knowledge graph.

---

## Troubleshooting

**Port 8080 already in use**
- `./bin/start-api --port 8081`

**MCP endpoints not reachable**
- `curl http://localhost:8080/mcp/status`

**Your LLM can't connect to MCP**
- Check your LLM client's MCP configuration
- Verify IQ server is running
- Restart your LLM application

---

## Next steps

- **Try MCP:** [QUICKSTART.md](QUICKSTART.md) — 5 minutes to first MCP tool
- **Learn MCP:** [MCP.md](MCP.md) — Full guide with patterns
- **See use cases:** [USECASES.md](USECASES.md) — Real examples
- **Build workflows:** [AGENTS.md](AGENTS.md) — State machines and decisions
- **Deploy:** [DEPLOYMENT.md](DEPLOYMENT.md) — Docker & cloud
