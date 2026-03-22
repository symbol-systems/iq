# iq-lab — Experimental Features and Persona Engine

`iq-lab` is the exploratory corner of the IQ platform. It hosts features that are in active development, prototypes that are being evaluated, and integrations that serve specific use cases before being promoted to core modules.

## What it currently provides

- **Persona engine** — defines and manages personality profiles for IQ agents, enabling role-consistent conversational behaviour across interactions
- **Discord integration** — a `DiscordBot` that connects IQ agents to Discord channels, allowing your knowledge-graph-backed agents to participate in conversations directly in Discord
- **Experimental AI patterns** — early-stage implementations of new agent interaction models being evaluated for the core platform

## Role in the system

`iq-lab` is optional and does not affect the core IQ runtime. It is a good place to look if you want to see new ideas being explored, or if you want to contribute experimental features that aren't yet ready for `iq-platform` or `iq-agentic`.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-lab -am compile`
- Discord bot token required for the Discord integration
