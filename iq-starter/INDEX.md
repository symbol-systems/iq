# IQ Starter Kit — Table of Contents

## 📚 Documentation (read in order)

1. **[README.md](README.md)** — Overview and architecture
2. **[WHAT_IS_IQ.md](WHAT_IS_IQ.md)** — Concepts and "why IQ?"
3. **[QUICKSTART.md](QUICKSTART.md)** — 5-minute setup
4. **[RUNTIMES.md](RUNTIMES.md)** — How to use (REST API, CLI, MCP)
5. **[USECASES.md](USECASES.md)** — Real business scenarios
6. **[CONNECTORS.md](CONNECTORS.md)** — Integrate with external systems
7. **[AGENTS.md](AGENTS.md)** — Build intelligent workflows
8. **[DOCKER.md](DOCKER.md)** — Deploy to cloud
9. **[FAQ.md](FAQ.md)** — Troubleshooting
10. **[GLOSSARY.md](GLOSSARY.md)** — Terms and definitions

## 🚀 Quick paths by role

**Product Manager** → [USECASES.md](USECASES.md#quick-comparison)  
**Data Engineer** → [CONNECTORS.md](CONNECTORS.md)  
**Cloud Architect** → [DOCKER.md](DOCKER.md)  
**Analyst** → [RUNTIMES.md](RUNTIMES.md#2-command-line-interface-cli)  
**Developer** → [AGENTS.md](AGENTS.md)

## 📦 What's included

| Folder | Contents |
|---|---|
| `docs/` | Complete user documentation |
| `bin/` | Helper scripts (start-api, demo-chat, etc.) |
| `examples/` | Sample RDF, SPARQL, workflows, configs |
| `docker/` | Docker Compose, deployment config |
| `.iq-templates/` | Realm templates and configuration |

## ⚡ Start here

```bash
# 1. Read this file
cat README.md

# 2. Understand what IQ is
cat docs/WHAT_IS_IQ.md

# 3. Get running in 5 minutes
bash docs/QUICKSTART.md

# 4. Pick your use case
cat docs/USECASES.md
```

## 🎯 Common tasks

| Task | Command | Docs |
|---|---|---|
| Start API server | `./bin/start-api` | [QUICKSTART.md](QUICKSTART.md) |
| Send chat message | `./bin/demo-chat "your question"` | [RUNTIMES.md](RUNTIMES.md#chat-api) |
| Run a SPARQL query | `./bin/demo-query examples/queries/00-hello.sparql` | [RUNTIMES.md](RUNTIMES.md#sparql-query-endpoint) |
| Connect Slack | See `examples/connectors/slack-config.ttl` | [CONNECTORS.md](CONNECTORS.md#example-1-slack-connector) |
| Deploy to Kubernetes | See `docker/k8s/` | [DOCKER.md](DOCKER.md#kubernetes) |
| Debug an agent | `iq> agent trace <id>` | [AGENTS.md](AGENTS.md#monitoring-agents) |

## 🔗 Links

- **GitHub:** https://github.com/symbol-systems/iq
- **Documentation:** ../iq-docs/docs/
- **License:** ../LICENSE

---

**Ready?** Start with [README.md](README.md) 🚀
