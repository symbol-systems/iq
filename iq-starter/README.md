# IQ Starter Kit 🚀

A comprehensive, batteries-included package designed for business users, product teams, and architects to explore IQ's enterprise capabilities. **IQ is MCP-first**—use it directly with any LLM (Claude, ChatGPT, local models, etc.).

## What's in the box?

This starter kit comes pre-configured with:

- **MCP-first architecture** — Expose any knowledge graph and workflow as MCP tools for any LLM
- **REST API fallback** — HTTP endpoints for direct queries, agents, and chat (no LLM required)
- **CLI tool** — Interactive command-line for exploration and batch operations
- **Five real-world enterprise use cases** — fully documented with examples
- **20+ connector reference setups** — AWS, Slack, GitHub, databases, and more
- **Ready-to-run agent workflows** — state machines, playbooks, and decision logic
- **Docker Compose stack** — spin everything up in one command
- **Sample knowledge domains** — RDF templates for common scenarios

## Quick navigation

| I want to... | Read this |
|---|---|
| Get running in 5 minutes with MCP | [QUICKSTART.md](docs/QUICKSTART.md) |
| Connect your LLM to IQ | [MCP Guide](docs/MCP.md) |
| Understand what IQ does | [What is IQ?](docs/WHAT_IS_IQ.md) |
| Learn the three runtimes | [Runtimes](docs/RUNTIMES.md) |
| See real use cases | [Use Cases](docs/USECASES.md) |
| Set up connectors | [Connectors Guide](docs/CONNECTORS.md) |
| Configure agents & workflows | [Agent Setup](docs/AGENTS.md) |
| Deploy with Docker | [Docker & Cloud](docs/DEPLOYMENT.md) |
| Test critical endpoints | [Testing Guide](docs/TESTING.md) |
| Troubleshoot issues | [FAQ & Troubleshooting](docs/FAQ.md) |

## System requirements

- **Java 21** (included via Maven wrapper)
- **Docker & Docker Compose** (optional, for the full stack)
- **curl** or Postman (for testing APIs)
- 4GB RAM minimum, 8GB recommended

## Installation & Your First MCP Tool

```bash
# 1. Clone or navigate to IQ
cd /path/to/iq/iq-starter

# 2. Start the MCP server
./bin/start-api

# 3. List available MCP tools
curl http://localhost:8080/mcp/tools | jq .

# 4. Call a tool (example: query your knowledge graph)
curl -X POST http://localhost:8080/mcp/tools/query_knowledge_graph/execute \\
  -H "Content-Type: application/json" \\
  -d '{"sparql": "SELECT DISTINCT ?name WHERE { ?x foaf:name ?name } LIMIT 5"}'
```

That's it. Your enterprise knowledge is now accessible via MCP.

## The three ways to use IQ

### 1. **MCP (Primary)** — Integrate with any LLM
```bash
./bin/start-api
# MCP endpoints available at http://localhost:8080/mcp/*
# Connect via any LLM with MCP support (Claude, ChatGPT, local models, etc.)
```
Best for: giving any LLM direct access to your knowledge graphs and workflows, building AI agents with grounded knowledge.

### 2. **REST API** — For web apps, mobile, existing integrations
```bash
./bin/start-api
# REST endpoints at http://localhost:8080/chat, /sparql, /agents
```
Best for: integrating AI into web UIs, mobile apps, or when you don't want to use an LLM client.

### 3. **Command-Line Interface** — For data teams, analysts, DevOps
```bash
./bin/start-cli
# Interactive mode: explore data, run queries, manage agents
```
Best for: exploring knowledge graphs, ad-hoc queries, batch operations, automation scripts.

## For different roles

**Product Manager** → Start with [USECASES.md](docs/USECASES.md) to understand business value  
**LLM AI Engineer** → Start with [MCP.md](docs/MCP.md) to connect your LLM  
**Data Engineer** → Start with [RUNTIMES.md](docs/RUNTIMES.md) for CLI and REST API options  
**Cloud Architect** → Start with [DEPLOYMENT.md](docs/DEPLOYMENT.md) for cloud setup  
**Developer** → Start with [QUICKSTART.md](docs/QUICKSTART.md) to build your first app  

## Next steps

1. Read [QUICKSTART.md](docs/QUICKSTART.md) (5 min)
2. Pick your integration path above
3. Try the examples in `examples/`
4. Deploy with Docker or cloud

---

**Location:** `/developer/iq/iq-starter/`  
**Status:** Ready to use  
**License:** See [LICENSE.md](docs/LICENSE.md)
