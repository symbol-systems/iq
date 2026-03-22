# iq-aspects — Shared Utilities

`iq-aspects` is the utility belt of the IQ platform. It provides small, well-tested helpers that are used across modules — things that every module needs but that don't belong anywhere specific.

## What it provides

- **IdentityHelper** — generates UUID-based IRIs and stable identifiers for knowledge graph nodes
- **Stopwatch** — lightweight elapsed-time tracking for logging and performance measurement
- **DateXSD** — formats dates in XSD-compatible strings for RDF ***REMOVED***s
- **HumanDate / TodayDate** — human-friendly date formatting used in agent bindings and prompts
- **Env** — safe environment variable reading with defaults
- **URLHelper** — URL parsing, encoding, and normalisation utilities
- **PrettyString** — humanises IRI local names and sanitises strings for file paths and labels

## Role in the system

`iq-aspects` is a low-level dependency used by `iq-platform`, `iq-agentic`, `iq-apis`, and most connector modules. It has no circular dependencies and no runtime framework requirements.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-aspects compile`
