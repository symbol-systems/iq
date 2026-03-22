# iq-mcp — Model Context Protocol Server

`iq-mcp` exposes IQ's knowledge graph, agent triggers, and SPARQL capabilities through the Model Context Protocol (MCP). This makes IQ's data and reasoning directly available to any LLM client that speaks MCP — including Claude, Cursor, and compatible tooling.

Think of it as a bridge: your LLM asks a question, MCP routes it to IQ, IQ queries real structured knowledge and returns a grounded answer.

## What it exposes via MCP

- **SPARQL query tool** — run SELECT queries against the IQ knowledge graph from inside your LLM conversation
- **SPARQL update tool** — write new facts back into the knowledge graph
- **RDF describe tool** — retrieve a full description of any resource by IRI
- **Actor trigger tool** — fire agent transitions from an LLM tool call
- **Server management tool** — inspect connected realms and server status

## Starting the MCP server

```bash
./bin/iq-mcp
```

This compiles and launches the MCP server. Point your MCP-capable LLM client at the resulting stdio or socket endpoint.

## Security

The built-in auth middleware extracts the `sub` claim from a bearer JWT for principal identification. This is suitable for local development and trusted environments. For production use, replace it with a full JWT validator that verifies signatures and issuers.

## Requirements

- Java 21
- Maven (wrapper included)
- A running IQ knowledge graph (local or remote)

## Part of the IQ family

`iq-mcp` is a companion to `iq-apis`. You can run both simultaneously — `iq-apis` serves your HTTP REST clients while `iq-mcp` serves your LLM tool clients.
