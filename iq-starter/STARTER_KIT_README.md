# iq-starter — comprehensive kitchen sink package

IQ is a knowledge execution platform for stateful AI workflows. This directory contains everything you need to:

- **Start using IQ** in 5 minutes
- **Understand how it works** with real examples
- **Deploy to production** with Docker/Kubernetes
- **Connect enterprise systems** via 20+ connectors
- **Build intelligent workflows** with state machines

## What's IQ?

**TL;DR:** A platform that combines knowledge graphs (structured data), AI reasoning (LLMs), and connectors to external systems into executable workflows.

```
Your data (AWS, Slack, GitHub, Databases)
   ↓ (ingest)
Knowledge graph (facts about your business)
   ↓ (reason)
AI decision engine (based on rules + LLM)
   ↓ (execute)
Actions (send email, post Slack, update database)
```

See [WHAT_IS_IQ.md](docs/WHAT_IS_IQ.md) for a deep dive.

## 🚀 Quick start (5 minutes)

```bash
# 1. Start the API server
./bin/start-api

# 2. In another terminal: Ask a question
./bin/demo-chat "Tell me about IQ"

# 3. View the response 
# → Grounded answer from knowledge graph + LLM
```

That's it. You're running IQ. See [QUICKSTART.md](docs/QUICKSTART.md) for details.

## 📚 Documentation (read in order)

| Document | For | Read time |
|---|---|---|
| [WHAT_IS_IQ.md](docs/WHAT_IS_IQ.md) | Non-technical overview | 5 min |
| [QUICKSTART.md](docs/QUICKSTART.md) | Get running in 5 minutes | 5 min |
| [RUNTIMES.md](docs/RUNTIMES.md) | How to use (REST API, CLI, MCP) | 10 min |
| [USECASES.md](docs/USECASES.md) | Real business scenarios | 15 min |
| [CONNECTORS.md](docs/CONNECTORS.md) | Integrate external systems | 10 min |
| [AGENTS.md](docs/AGENTS.md) | Build intelligent workflows | 15 min |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Technical deep dive | 20 min |
| [DOCKER.md](docker/DEPLOYMENT.md) | Deploy to cloud | 10 min |
| [FAQ.md](docs/FAQ.md) | Troubleshooting | as needed |
| [GLOSSARY.md](docs/GLOSSARY.md) | Terms & definitions | as needed |

**Start here:** [WHAT_IS_IQ.md](docs/WHAT_IS_IQ.md) for context, then [QUICKSTART.md](docs/QUICKSTART.md) to get running.

## 🎯 Quick paths by role

**I'm a Product Manager**
→ Read [USECASES.md](docs/USECASES.md)  
→ Pick your use case  
→ Share with your team

**I'm a Data Engineer**
→ Read [CONNECTORS.md](docs/CONNECTORS.md)  
→ Configure connectors for your systems  
→ Query across systems with SPARQL

**I'm a Cloud Architect**
→ Read [DOCKER.md](docker/DEPLOYMENT.md)  
→ Deploy to your cloud (AWS/Azure/GCP)  
→ Scale horizontally

**I'm a Developer**
→ Read [AGENTS.md](docs/AGENTS.md)  
→ Build workflows in RDF  
→ Use REST API to integrate

**I'm an Analyst**
→ Read [RUNTIMES.md](docs/RUNTIMES.md#2-command-line-interface-cli)  
→ Use CLI to explore knowledge  
→ Run SPARQL queries

## 📦 What's included

```
iq-starter/
├── README.md                  ← You are here
├── INDEX.md                   ← Table of contents
├── docs/
│   ├── WHAT_IS_IQ.md          ← Concepts
│   ├── QUICKSTART.md          ← 5-minute setup
│   ├── RUNTIMES.md            ← API, CLI, MCP
│   ├── USECASES.md            ← Real scenarios
│   ├── CONNECTORS.md          ← Integrations
│   ├── AGENTS.md              ← Workflows
│   ├── ARCHITECTURE.md        ← How it works
│   ├── FAQ.md                 ← Troubleshooting
│   └── GLOSSARY.md            ← Terms
├── bin/
│   ├── start-api              ← Launch REST server
│   ├── start-cli              ← Launch CLI
│   ├── start-mcp              ← Launch LLM integration
│   ├── demo-chat              ← Try chat API
│   ├── demo-query             ← Try SPARQL
│   └── import-example         ← Load sample data
├── examples/
│   ├── domains/               ← RDF knowledge (customers, orders)
│   ├── queries/               ← SPARQL query examples
│   ├── connectors/            ← Connector configs
│   ├── agents/                ← Workflow definitions
│   └── README.md              ← Guide to examples
├── docker/
│   ├── docker-compose.yml     ← Full stack
│   ├── .env.example           ← Configuration template
│   └── DEPLOYMENT.md          ← Cloud deployment
└── .iq-templates/             ← Realm configs (optional)
```

## 🔧 System requirements

- **Java 21+** (Maven wrapper included, no separate install)
- **Docker & Docker Compose** (optional, for full-stack mode)
- **curl** (for testing APIs)
- **4GB RAM minimum** (8GB recommended)

## 🌟 Key features

✅ **Knowledge Graph** — Unified, queryable facts about your business  
✅ **Stateful Workflows** — Track request state through approval processes  
✅ **20+ Connectors** — AWS, Slack, GitHub, Jira, Salesforce, databases, etc.  
✅ **LLM Grounding** — Prevent AI hallucinations with real data  
✅ **Multi-Tenant** — Isolate data and secrets per customer/team  
✅ **Audit Logs** — See who did what, when, and why  
✅ **SPARQL Queries** — Query across all connected systems in one query  

## 🚦 Get started now

```bash
# 1. Read the overview
cat docs/WHAT_IS_IQ.md

# 2. Get running in 5 minutes
./bin/start-api
# (in another terminal)
./bin/demo-chat "Tell me about IQ"

# 3. Explore use cases
cat docs/USECASES.md

# 4. Deploy (when ready)
docker-compose -f docker/docker-compose.yml up -d
```

## 🔗 Useful links

- **Repository:** https://github.com/symbol-systems/iq
- **Full docs:** ../iq-docs/docs/
- **License:** ../LICENSE
- **Support:** https://github.com/symbol-systems/iq/issues

## ❓ Common questions

**Q: Do I need a credit card to start?**  
A: No. If you don't use OpenAI, it's free. If you do, you need an OpenAI API key.

**Q: How long does it take to setup?**  
A: 5 minutes to get running, 1-2 weeks to integrate your systems.

**Q: Can I run this without an AI model?**  
A: Yes. You can use IQ purely for knowledge graphs and SPARQL queries.

**Q: What happens to my data?**  
A: It stays in your knowledge graph. Only what you explicitly send to an LLM goes to that LLM.

See [FAQ.md](docs/FAQ.md) for more.

## 📊 Enterprise use cases

1. **Knowledge Ingestion & RAG** — Ingest docs → searchable knowledge → grounded answers
2. **Agent Workflows** — Multi-step approval processes with state tracking
3. **Multi-Connector Orchestration** — One query across your entire tech stack
4. **Trusted Identity** — Multi-tenant isolation, JWT auth, secrets vaults
5. **LLM Grounding** — Prevent hallucinations with real data

See [USECASES.md](docs/USECASES.md) for detailed examples of each.

---

**Ready?** Start with [WHAT_IS_IQ.md](docs/WHAT_IS_IQ.md) or [QUICKSTART.md](docs/QUICKSTART.md) 🚀
