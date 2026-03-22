# iq-skeleton — Connector Starter Template

`iq-skeleton` is the official starting point for building a new IQ connector. It provides the minimal structure, wiring, and configuration that every connector in `iq-connect` follows — so you can focus on the integration logic, not the scaffolding.

## What it includes

- The Maven module structure expected by the IQ build system
- Baseline dependencies on `iq-connect-core` for shared connector abstractions
- Placeholder implementations of the connector lifecycle interfaces
- A test harness skeleton for verifying connector behaviour in isolation
- The RDF4J configuration templates and resource layout used by all connectors

## How to use it

Copy `iq-skeleton` to a new directory under `iq-connect/`, rename the artifact, and start implementing the connector operations. The `iq-connect/README.md` describes the uniform connector contract that your implementation should satisfy.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-skeleton -am compile`
