# iq-finder — Semantic Search and Corpus Indexing

`iq-finder` provides the search and semantic retrieval capabilities for IQ. It manages corpus indexing, semantic similarity ranking, and retrieval over knowledge graph content — enabling agents and APIs to find relevant facts and documents efficiently.

## What it provides

- **SearchMatrix** — maintains per-realm semantic indexes that can be queried by concept, value, or similarity
- **I_Corpus / I_Search** implementation — fulfils the search contracts defined in `iq-abstract`, used by realm agents and the REST API
- **Indexing lifecycle** — integrates with `RealmManager` to keep indexes current as the knowledge graph changes
- **Semantic retrieval** — supports ranked retrieval of RDF statement collections by subject relevance

## Role in the system

`iq-finder` feeds the search dimension of IQ agents. When an agent needs to retrieve relevant context before making an LLM decision, the finder provides the ranked candidates. It is consumed by `iq-platform` and `iq-apis`.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-finder -am compile`
