# RUNTIMES — How to use IQ (three entry points)

IQ can be used in three different ways depending on your needs. Pick the one that fits your use case.

## Overview

| Runtime | Best for | Start with |
|---|---|---|
| **REST API** | Building apps, web UIs, mobile integration | `./bin/start-api` |
| **CLI** | Data exploration, analytics, operations | `./bin/start-cli` |
| **MCP** | Giving LLMs access to enterprise data | `./bin/start-mcp` |

---

## 1. REST API

### What it is

A Quarkus-based HTTP server that exposes IQ's capabilities as REST endpoints. Use it to integrate IQ into your web app, mobile app, or third-party system.

### When to use

- Building a web UI
- Integrating with an existing SaaS or mobile app
- Creating a chatbot
- Exposing AI capabilities to external partners

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
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "realm": "acme-corp",
    "message": "What is the status of order #12345?"
  }'

Response:
{
  "response": "Order #12345 is in shipped status. Left warehouse on Mar 28.",
  "model": "gpt-3.5-turbo",
  "tokens_used": 87,
  "grounded_facts": [
    "orders/12345",
    "shipping/fedex-12345"
  ]
}
```

#### Agent API
Trigger a workflow (multi-step decision-making).

```bash
curl -X POST http://localhost:8080/agent/trigger \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "realm": "acme-corp",
    "intent": "approve-purchase-order",
    "object_id": "po-98765",
    "requester": "alice@acme.com"
  }'

Response:
{
  "status": "approved",
  "state_transition": "pending → approved",
  "actions_triggered": [
    "send_email_to_procurement",
    "bill_account_12345",
    "create_jira_ticket_ACME-5432"
  ],
  "decision_trace": [
    "✓ Requester Alice has purchase authority (role: manager)",
    "✓ Budget available: $45,000/month, spent: $12,500",
    "✓ Vendor in approved list",
    "→ DECISION: approve"
  ]
}
```

#### OpenAI-Compatible Endpoint
Drop-in replacement for OpenAI's chat completions API (but grounded in your knowledge).

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-YOUR_TOKEN" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {"role": "user", "content": "What is our customer retention rate?"}
    ]
  }'

Response:
{
  "id": "chatcmpl-123abc",
  "object": "chat.completion",
  "created": 1711875296,
  "model": "gpt-3.5-turbo",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Our customer retention rate for 2024 is 94.2% 
        (from analytics data), up from 91.8% in 2023."
      }
    }
  ],
  "usage": {"prompt_tokens": 15, "completion_tokens": 28, "total_tokens": 43}
}
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
      }
      LIMIT 10'
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
# Disables dev UI, enables security headers, etc.
```

Or build and run a container:
```bash
docker build -t iq-api:latest .
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e IQ_AUTH_SECRET=... \
  iq-api:latest
```

---

## 2. Command-Line Interface (CLI)

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
iq> get-type :Customer

# Run SPARQL
iq> query examples/queries/top-customers.sparql
iq> query --file my-analysis.sparql --output csv

# Agents & workflows
iq> agent list
iq> agent trace :approve-purchase-order po-98765
iq> agent run approve-purchase-order po-98765 requester=alice@acme.com

# Import/export
iq> import-file customers.ttl
iq> export-realm my-project customers.ttl
iq> export-query results.sparql results.csv

# Connectors
iq> connector list
iq> connector read aws s3://my-bucket --into-realm my-project
iq> connector write slack-message "#general" "Order approved!"
```

### Batch mode (programmatic)

Instead of interactive, run a script:

```bash
iq-cli < commands.txt
```

File contents:
```
use-realm acme-corp
query analytics.sparql --output results.csv
agent run daily-sync
export-realm result.ttl
```

### Configuration

Environment variables:
```bash
export IQ_HOME=/path/to/.iq
export IQ_REALM=acme-corp
./bin/start-cli
```

Or in `.iq/cli-config.properties`:
```properties
default.realm=acme-corp
default.output.format=table
output.directory=./results/
```

---

## 3. Model Context Protocol (MCP)

### What it is

A protocol that lets Claude, ChatGPT, Llama, and other LLMs call IQ as a tool. The LLM can ask "What's our revenue?" and IQ answers with actual data.

### When to use

- Giving Claude direct access to your enterprise data
- Building AI assistants that need ground truth
- Reducing hallucinations in LLM responses
- Integrating IQ with existing AI workflows

### Quick start

```bash
./bin/start-mcp
# Starts the MCP server on stdio (for local Claude Desktop) 
# or TCP (for remote connections)
```

### Configuration

In your Claude Desktop config (`~/.config/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "iq": {
      "command": "/path/to/iq-starter/bin/start-mcp",
      "env": {
        "IQ_REALM": "acme-corp",
        "OPENAI_API_KEY": "sk-..."
      }
    }
  }
}
```

### What Claude can do

```
User: Tell me about our biggest customer

Claude: [calls IQ tool] "get_customer_by_revenue"
IQ: Acme Corp, $2.3M revenue, 87 active users, 3 support tickets open

Claude: "Your biggest customer is Acme Corp with $2.3M in annual 
revenue. They have 87 active users and currently 3 open support 
tickets, including one critical issue with single sign-on."
```

### Available MCP tools (from IQ)

- `search_knowledge` — Full-text search across your facts
- `query_sparql` — Run a SPARQL query
- `get_entity` — Fetch details about a specific customer/order/etc
- `trigger_agent` — Run a workflow
- `list_connectors` — See what's connected
- `read_connector` — Fetch fresh data from an external system

---

## Comparison table

| Feature | REST API | CLI | MCP |
|---|---|---|---|
| **Best for** | Web apps, integrations | Data analysis, ops | LLM integration |
| **Startup time** | 5-10 seconds | 5-10 seconds | 2-3 seconds |
| **Scalability** | High (stateless servers) | Medium (single process) | High (stateless) |
| **Development** | Postman, curl, SDK | Bash, Python scripts | Any LLM client |
| **Team** | Developers | Data engineers, ops | AI engineers |
| **Cost** | Cloud compute | Local CPU | Cloud compute |

---

## Advanced: Running all three together

For a production setup, you might run:
- REST API on port 8080 (for your apps)
- CLI workers in cron jobs (for scheduled tasks)
- MCP on a TCP port (for your AI assistants)

All three share the same knowledge graph under `.iq/repositories/`.

```bash
# Terminal 1: REST API
./bin/start-api

# Terminal 2: MCP server
./bin/start-mcp --port 5555

# Terminal 3: Scheduled CLI tasks
while true; do
  ./bin/start-cli < cron-tasks.txt
  sleep 3600
done
```

---

## Troubleshooting

**REST API won't start**
- Check port 8080 isn't used: `lsof -i :8080`
- Check `OPENAI_API_KEY` is set if using LLM features

**CLI hangs**
- Try `Ctrl+C` and restart
- Check `.iq/repositories/default/` exists

**MCP not connecting to Claude**
- Verify config in `~/.config/Claude/claude_desktop_config.json`
- Check IQ server is running
- Restart Claude app

---

## Next steps

- [USECASES.md](USECASES.md) — See real examples using these runtimes
- [AGENTS.md](AGENTS.md) — Learn to build multi-step workflows
- [DOCKER.md](DOCKER.md) — Deploy to cloud
- [FAQ.md](FAQ.md) — Troubleshooting

