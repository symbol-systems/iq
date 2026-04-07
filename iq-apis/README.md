# iq-apis — IQ API Server

`iq-apis` is the runtime entry point for IQ. It exposes the LLM, chat, agent, and avatar endpoints as a secure REST API, backed by a knowledge graph and a fleet of stateful AI agents.

When you start `iq-apis`, IQ boots all configured realms, initialises their agents, and begins listening for requests. Each realm is independently secured with JWT tokens and maintains its own knowledge state.

## What it provides

- **Chat API** — send messages to a realm's AI avatar and receive grounded, context-aware replies
- **Agent API** — trigger intent-driven agent transitions backed by real domain knowledge
- **OpenAI-compatible endpoint** — drop-in replacement for OpenAI chat completions, pointed at your own LLMs
- **Realm management** — multi-tenant knowledge graph isolation with per-realm secrets and state machines
- **Live dev UI** — inspect routes, test endpoints, and view CDI beans at `http://localhost:8080/q/dev/`

## Starting the server

```bash
./bin/iq
```

The server starts in development mode with live reloading. Save a file and IQ recompiles and restarts on the fly — no manual restarts needed.

## Sending a chat message

```bash
./bin/curl_chat
```

## Calling the agent endpoint

```bash
./bin/curl_agent
```

## Using the OpenAI-compatible endpoint

```bash
./bin/curl_api
```

## Building for production

```bash
./bin/build-image
```

This builds a container image using the Docker builder. The resulting image runs the full IQ API server and is ready for deployment anywhere containers run.

## Compiling without starting

```bash
./bin/compile-apis
```

Useful for verifying the build before a deploy without launching the server.

## Configuration

IQ reads realm configuration from `.iq/` in the working directory. LLM provider mappings, prompt templates, and connector settings all live there. Set `IQ` to identify the running instance.

## Requirements

- Java 21
- Maven (wrapper included — no separate install needed)
