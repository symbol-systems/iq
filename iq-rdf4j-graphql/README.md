# iq-rdf4j-graphql — GraphQL over the Knowledge Graph

`iq-rdf4j-graphql` adds a GraphQL interface to IQ's RDF knowledge graph. It lets clients query structured knowledge using GraphQL syntax, backed by SPARQL execution and a policy engine that controls what each caller is allowed to see.

## What it provides

- **GQL servlet** — serves a GraphQL endpoint backed by an RDF4J repository, translating GraphQL field resolutions into SPARQL queries
- **PolicyEngine** — evaluates access control policies defined in the knowledge graph before returning results, ensuring callers only see data they are authorised to access
- **PolicyEngineChain** — composes multiple policy evaluators into a priority-ordered chain
- **IRIDirective** — a GraphQL schema directive that binds GraphQL fields to specific RDF predicates or resources
- **RDFSDomain inferences** — uses RDFS domain information from the knowledge graph to guide field resolution
- **LocalAssetRepository** — an in-memory repository loaded from classpath resources, useful for testing and local development

## Role in the system

`iq-rdf4j-graphql` is an optional API layer. It is useful when you want to expose IQ knowledge graph content to front-end clients or tools that expect a GraphQL interface rather than a SPARQL endpoint.

## Requirements

- Java 21
- RDF4J 5+
- Part of the IQ mono-repo; build with `./mvnw -pl iq-rdf4j-graphql -am compile`
