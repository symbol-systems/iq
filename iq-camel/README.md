# iq-camel — Apache Camel Integration

`iq-camel` integrates IQ with Apache Camel, enabling event-driven and stream-based data flows into and out of the IQ knowledge graph. It provides Camel processors, routes, and components that treat RDF graphs as first-class Camel exchange payloads.

## What it provides

- **RDF4J Camel component** — a native Camel component that reads and writes RDF repositories as part of a route
- **Graph query processor** — executes SPARQL CONSTRUCT queries as a Camel processing step, transforming messages into graph results
- **Import model processor** — ingests RDF data from upstream Camel sources directly into the knowledge graph
- **NLP processor** — applies natural language processing as a pipeline stage, enriching graph data with extracted entities and relations
- **IQ route policy** — Camel route policy aware of IQ's knowledge lifecycle, enabling routes that react to graph state changes
- **KBMS integration** — connects Camel routes to IQ's knowledge base management system for persistent graph operations

## Role in the system

`iq-camel` is an optional integration layer for teams that already use Apache Camel or want event-driven data ingestion. It is not required for the core IQ runtime.

## Requirements

- Java 21
- Apache Camel 4+
- Part of the IQ mono-repo; build with `./mvnw -pl iq-camel -am compile`
