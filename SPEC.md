# IQ Specifications, Abstractions, and Idioms

This document describes key architecture concepts and conventions used in the IQ project.

## 1. Core principles

- Knowledge-first: IQ represents domain data as graphs (RDF) and operates using declarative patterns.
- Connector-driven: each integration is a connector module (iq-connect/*), with uniform lifecycle and status semantics.
- Policy and content as code: behavior is authored using scripts and RDF templates, not hardcoded by business logic.

## 2. Components

- `iq-platform`: core libraries and shared services.
- `iq-connect`: connectors for services (AWS, Azure, GitHub, Slack, etc.).
- `iq-apis`: REST API runtime layer over platform services.
- `iq-mcp`: MCP runtime layer over platform services.
- `iq-cli`, `iq-cli-pro`, `iq-cli-server`: command line tools for user interaction and orchestration.

## 3. Runtime model

- `Knowledge graph`: central state management in RDF repositories.
- `Agents`: result of combining rules, connectors, and tasks.
- `Playbooks`: declarative instruction collections executed by the engine.

## 4. Gesture conventions

- `refresh()`: connector method to sync external state into the graph.
- `status`: each connector reports status node in RDF, used for audit and health checks.
- `ttl` and `sparql` assets: configuration and logic usually stored as Turtle and SPARQL files.

## 5. Extension patterns

- Add new connector by copying a template module under `iq-connect/` and implementing `Connector` contract.
- Reuse shared helpers from `iq-utils` and `iq-aspects` modules.
- Keep API surface stable with `iq-apis` endpoint bindings.

## 6. Non-functional expectations

- Platform supports local and containerized execution.
- Auth and secrets are configured through environment variables and vault conventions (`.iq/vault`).
- Testing: unit tests in `src/test`, integration flagged with `-DskipITs=false`.

