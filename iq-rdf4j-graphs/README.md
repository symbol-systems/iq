# iq-rdf4j-graphs — Graph Transforms and JGraphT Bridge

`iq-rdf4j-graphs` bridges RDF4J knowledge graphs with JGraphT, the Java graph theory library. It enables graph-theoretic analysis — shortest paths, connectivity, centrality, and topology — over the same data that IQ uses for knowledge representation and agent reasoning.

## What it provides

- **ModelGraph** — wraps an RDF4J `Model` as a JGraphT `Graph`, mapping RDF statements to directed edges and resources to vertices
- **Graphs utility** — helper methods for constructing, traversing, and transforming graph instances built from RDF content
- Graph algorithm access via JGraphT — shortest path, spanning tree, topological sort, and cycle detection over knowledge graph subsets

## Role in the system

`iq-rdf4j-graphs` is used when agent logic or data pipelines need structural graph analysis beyond what SPARQL provides. It is an optional dependency — include it when your use case involves graph topology or structural reasoning over the knowledge graph.

## Requirements

- Java 21
- JGraphT
- RDF4J 5+
- Part of the IQ mono-repo; build with `./mvnw -pl iq-rdf4j-graphs -am compile`
