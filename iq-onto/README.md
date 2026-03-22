# iq-onto — Ontology Management

`iq-onto` provides vocabulary and ontology management tooling for the IQ platform. It handles the definition, loading, and maintenance of the shared conceptual schemas that give IQ's knowledge graphs their meaning and structure.

## What it provides

- Ontology loading and resolution from local and remote sources
- Vocabulary management for IQ's own namespace and domain-specific extensions
- Support for maintaining consistency between ontology versions and live knowledge graphs
- Tooling for validating knowledge graph content against defined schemas

## Role in the system

`iq-onto` underpins the semantic layer of IQ. When a knowledge graph references concepts from a shared vocabulary, `iq-onto` ensures those definitions are available and coherent. It is used by `iq-platform` and domain-specific realm configurations.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-onto -am compile`
