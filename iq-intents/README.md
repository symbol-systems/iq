# iq-intents — Intent Definitions and Repository Configuration

`iq-intents` provides the intent model and RDF repository configuration that underpin IQ's stateful agent execution. It defines how intent state is stored, how repositories are typed and configured, and provides the RDF4J store templates used across the platform.

## What it provides

- **RDF4J repository store templates** — a complete set of Turtle configuration templates for native, memory, RDFS-inferencing, SHACL, Lucene, SPIN, SPARQL, and remote stores — all using the current RDF4J 5.x config vocabulary
- **Intent model** — the RDF vocabulary and Java types for expressing what an agent can do at each point in its state machine
- **Default repository configuration** — the baseline store configuration used when IQ creates new realms

## Role in the system

`iq-intents` is a foundational library. `iq-platform` depends on it for repository creation and intent resolution. If you need to add a new store type or extend the intent vocabulary, this is where those additions live.

## Requirements

- Java 21
- RDF4J 5+
- Part of the IQ mono-repo; build with `./mvnw -pl iq-intents -am compile`
