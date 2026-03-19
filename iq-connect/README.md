# iq-connect

A collection of **connectors** that integrate IQ with external systems and cloud APIs.

Each connector is a small, self-contained adapter that:

- Maintains its own **kernel** and **quad store** (graph) representing the connector state
- Supports **read-only**, **write-only**, or **read-write** modes
- Can be used in **federated** RDF/SPARQL queries and workflows
- Is designed to be **minimal**, **reactive**, and **DRY** (shared core abstractions + small per-connector surface)

## Project structure

- `iq-connect-core/` – Core abstractions, interfaces, utilities, and the shared kernel model.
- `iq-connect-*/` – Individual connectors for specific systems (AWS, Azure, Jira, GitHub, etc.).

## Goals

1. Provide a consistent, clean abstraction surface for building connectors.
2. Keep each connector small and focused; most logic should live in `iq-connect-core`.
3. Enable connectors to participate in IQ's RDF ecosystem via a **quad store + graph IRI** for sync state.
4. Support **federation** and **query-time composition** (e.g. SPARQL `SERVICE`) across connectors.

## Next steps / design notes

See the working notes in `./todo/` for ongoing design work and architectural ideas.

---

**Quick start**

1. Explore `iq-connect-core/` for the shared kernel and connector abstractions.
2. Pick an existing connector (e.g., `iq-connect-github`) and study how it implements the core interfaces.
3. Add a new connector by implementing the core abstractions and registering it in the IQ registration process.
