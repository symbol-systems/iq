# IQ Starter Kit 🚀

A comprehensive, batteries-included package designed for business users, product teams, and architects to explore IQ's enterprise capabilities. **No coding required—just configuration and examples.**

## What's in the box?

This starter kit comes pre-configured with:

- **Three runtime entry points** — REST API, command-line interface, and LLM integration
- **Five real-world enterprise use cases** — fully documented with examples
- **20+ connector reference setups** — AWS, Slack, GitHub, databases, and more
- **Ready-to-run agent workflows** — state machines, playbooks, and decision logic
- **Docker Compose stack** — spin everything up in one command
- **Sample knowledge domains** — RDF templates for common scenarios

## Quick navigation

| I want to... | Read this |
|---|---|
| Get running in 5 minutes | [QUICKSTART.md](docs/QUICKSTART.md) |
| Understand what IQ does | [What is IQ?](docs/WHAT_IS_IQ.md) |
| Learn the three runtimes | [Runtimes](docs/RUNTIMES.md) |
| See real use cases | [Use Cases](docs/USECASES.md) |
| Set up connectors | [Connectors Guide](docs/CONNECTORS.md) |
| Configure agents & workflows | [Agent Setup](docs/AGENTS.md) |
| Deploy with Docker | [Docker & Cloud](docs/DOCKER.md) |
| Troubleshoot issues | [FAQ & Troubleshooting](docs/FAQ.md) |

## System requirements

- **Java 21** (included via Maven wrapper)
- **Docker & Docker Compose** (optional, for the full stack)
- **curl** or Postman (for testing APIs)
- 4GB RAM minimum, 8GB recommended

## Installation

```bash
# 1. Clone or navigate to the IQ repository
cd /path/to/iq

# 2. Start the default runtime (embedded in the starter kit)
cd iq-starter
./bin/start-api

# 3. Open your browser to http://localhost:8080/q/dev/
```

That's it. You're running IQ.

## The three ways to use IQ

### 1. **REST API** (for web apps, mobile, integrations)
```bash
./bin/start-api
# Then call http://localhost:8080/
```
Best for: integrating AI into existing systems, building web UIs, mobile apps.

### 2. **Command-Line Interface** (for data teams, analysts, DevOps)
```bash
./bin/start-cli
# Interactive mode: explore data, run queries, manage agents
```
Best for: exploring knowledge graphs, ad-hoc queries, batch operations.

### 3. **Model Context Protocol** (for LLM tool integration)
```bash
./bin/start-mcp
# Use with Claude, ChatGPT, or local LLMs
```
Best for: giving AI systems direct access to your enterprise data and tools.

## Use cases by role

**Product Manager** → [Feature Flagging & Progressive Release](examples/agents/feature-flags/)  
**Data Engineer** → [ETL & Lake Ingestion](examples/domains/data-lake/)  
**Cloud Architect** → [Multi-Cloud Infrastructure](examples/connectors/aws-azure-gcp/)  
**Customer Success** → [Support Automation & Routing](examples/agents/support-bot/)  
**Finance** → [Budget Tracking & Anomaly Detection](docs/USECASES.md)

## Documentation structure

```
iq-starter/
├── README.md                    ← You are here
├── docs/
│   ├── QUICKSTART.md            ← 5-minute setup
│   ├── WHAT_IS_IQ.md            ← Concept overview
│   ├── RUNTIMES.md              ← API, CLI, MCP explained
│   ├── USECASES.md              ← Real business scenarios
│   ├── CONNECTORS.md            ← 20+ integrations
│   ├── AGENTS.md                ← Workflows & state machines
│   ├── DOCKER.md                ← Container deployment
│   ├── FAQ.md                   ← Troubleshooting
│   └── GLOSSARY.md              ← Terms & concepts
├── bin/
│   ├── start-api                ← Launch REST server
│   ├── start-cli                ← Launch CLI
│   ├── start-mcp                ← Launch LLM integration
│   ├── demo-chat                ← Try chat API
│   ├── demo-agent               ← Try agent API
│   ├── import-example            ← Load sample knowledge
│   └── setup-connectors         ← Configure integrations
├── examples/
│   ├── domains/                 ← RDF knowledge templates
│   ├── queries/                 ← SPARQL examples
│   ├── connectors/              ← Connector configs
│   └── agents/                  ← Workflow playbooks
├── docker/
│   ├── docker-compose.yml       ← Full stack
│   ├── .env.example             ← Configuration template
│   └── Dockerfile.starter       ← Optimized image
└── .iq-templates/               ← Realm configs
```

## First use: the "hello" example

Every IQ instance starts with a simple chat-based greeting. This initializes your knowledge graph and verifies the system is working.

```bash
cd iq-starter
./bin/demo-chat "What is IQ?"
```

Expected response: "IQ is a runtime for stateful AI workflows..." (from your built-in knowledge).

## Five enterprise use cases

Each comes with a complete example, including RDF knowledge, SPARQL queries, and connector setups.

| Use Case | What it does | See |
|---|---|---|
| **Knowledge Ingestion & RAG** | Ingest docs, PDFs, databases → turn into searchable knowledge | [examples/domains/rag/](examples/domains/rag/) |
| **Agent Workflows** | Multi-step tasks: approve/reject, route between systems, track state | [examples/agents/workflows/](examples/agents/workflows/) |
| **Multi-Connector Orchestration** | Query AWS + GitHub + Slack + Snowflake in one SPARQL query | [examples/connectors/orchestration/](examples/connectors/orchestration/) |
| **Trusted Identity & Secrets** | Multi-realm isolation, JWT tokens, credential vaults | [docs/AGENTS.md](docs/AGENTS.md) |
| **LLM-Grounded Decision Making** | Use your own data to ground LLM decisions, avoid hallucinations | [examples/agents/decision-engine/](examples/agents/decision-engine/) |

## Next steps

1. **[QUICKSTART.md](docs/QUICKSTART.md)** — Get running in 5 minutes
2. **[WHAT_IS_IQ.md](docs/WHAT_IS_IQ.md)** — Answer "what is IQ and why should I care?"
3. **[USECASES.md](docs/USECASES.md)** — See which scenario matches your team's needs
4. **[DOCKER.md](docs/DOCKER.md)** — Deploy to your cloud or on-premise infrastructure

## Support & community

- **GitHub Issues** — [symbol-systems/iq/issues](https://github.com/symbol-systems/iq/issues)
- **Documentation** — [iq-docs/](../iq-docs/docs/)
- **Build from source** — `./mvnw clean install -DskipTests`

## License

This starter kit is part of the IQ platform. See [LICENSE](../LICENSE) for details.

---

**Ready?** Start with [QUICKSTART.md](docs/QUICKSTART.md) 🚀
