# iq-tropes — Narrative Pattern Library

`iq-tropes` is a library of narrative and stylistic patterns for IQ agent personas. It gives agents a structured vocabulary of character traits, conversational roles, and rhetorical styles — making it easier to define consistent, compelling agent personalities without writing bespoke prompt logic each time.

## What it provides

- A curated set of persona archetypes that can be applied to IQ agents
- Narrative framing patterns that shape how an agent presents information and responds to queries
- Stylistic modifiers — tone, formality, perspective — that combine with agent knowledge to produce distinctive voices
- Integration point with `iq-agentic`'s avatar and prompt construction pipeline

## Role in the system

`iq-tropes` is consumed by the persona engine in `iq-lab` and by custom avatar configurations. It is optional — agents work perfectly well without it — but it accelerates the creation of agents with distinctive, consistent personalities.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-tropes -am compile`
