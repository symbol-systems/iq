# IQ — Knowledge Execution for Cognitive AI

IQ is a runtime platform that turns structured knowledge and AI into executable, stateful workflows. It connects your data, your tools, and your reasoning in one coherent system — without rewriting integration logic every time something changes.

Built for teams that want AI to actually *do things*, not just respond to prompts.

## What IQ does

IQ reads your domain knowledge, applies declarative rules and LLM reasoning, and executes intent-driven workflows across connected systems. It maintains state, tracks transitions, and calls out to external services at exactly the right moment — so you don't have to orchestrate all of that by hand.

- Stateful AI agents that remember where they are in a process
- Pluggable connectors for cloud platforms, databases, APIs, and messaging systems
- LLM decision-making backed by real structured data, not just context windows
- JWT-secured multi-realm architecture — isolate tenants, roles, or projects cleanly

## Modules at a glance

| Module | What it does |
|---|---|
| `iq-apis` | The Quarkus REST server — your AI API endpoint |
| `iq-platform` | Core agent engine, realm management, LLM wiring |
| `iq-agentic` | Agent builder, avatar, and decision-making toolkit |
| `iq-cli` | Community command-line interface |
| `iq-cli-pro` | Extended CLI with advanced orchestration |
| `iq-mcp` | Model Context Protocol server for LLM tool integration |
| `iq-connect/*` | 20+ connectors: AWS, GitHub, Slack, JDBC, Kafka, and more |
| `iq-lake` | Document and data lake ingestion pipeline |
| `iq-onnx` | Local ONNX model inference, embeddings, and reranking |
| `iq-trusted` | Auth, JWT, secrets, and trust primitives |
| `iq-finder` | Semantic search and corpus indexing |
| `iq-intents` | Intent definitions and RDF repository configuration |
| `iq-camel` | Apache Camel routing and RDF stream processing |
| `iq-rdf4j-graphql` | GraphQL access layer over your knowledge graph |
| `iq-rdf4j-graphs` | Graph transforms and JGraphT bridge |
| `iq-tokenomic` | Token cost tracking and budget enforcement |
| `iq-aspects` | Shared utilities — identity, dates, environment helpers |
| `iq-abstract` | Core interfaces and contracts used across all modules |
| `iq-lab` | Experimental features, persona engine, Discord integration |
| `iq-onto` | Ontology management and vocabulary tooling |
| `iq-tropes` | Narrative pattern library for agent personas |
| `iq-skeleton` | Starter template for new connectors |

## Getting started

Requires Java 21 and Maven. Clone the repository and run:

```bash
./bin/iq
```

This starts the full IQ API server in development mode with live reloading. The dev UI is available at `http://localhost:8080/q/dev/`.

### Send your first chat request

```bash
./bin/curl_chat
```

### Run the agent endpoint

```bash
./bin/curl_agent
```

### Start the CLI

```bash
./bin/iq-cli
```

### Build a container image

```bash
./bin/build-image
```

### Cut a release

```bash
./bin/release "v1.0.0"
```

## Architecture

IQ is knowledge-first. Domain behaviour is expressed as declarative policies and queries — not hardcoded business logic. Each realm (tenant, project, or agent group) has its own isolated knowledge graph, secrets store, and state machine. Connectors plug into this cleanly and uniformly.

The platform runs on Quarkus for fast startup, low memory footprint, and container-native deployment.

## Repository

[github.com/symbol-systems/iq](https://github.com/symbol-systems/iq) — contributions and issues welcome.
