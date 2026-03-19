# iq-connect-core

Core abstractions and utilities for building IQ connectors.

This module provides:

- `I_Connector` / `I_ConnectorKernel` — the core connector runtime abstraction.
- `I_SyncState` / `I_Checkpoint` — a simple way to represent and persist sync status.
- `I_ConnectorRegistry` — a minimal service interface for managing connectors (see `InMemoryConnectorRegistry`).
- `ConnectorModels` / `Checkpoints` — helper utilities to sync models and create checkpoint snapshots.

## Usage patterns

- Connectors should expose a `Model` view of their state (not a `RepositoryConnection`).
- Checkpoints can be created via `Checkpoints.of(model)` and applied back to a model to restore state.
- The connector state is intended to be stored as RDF and queried using IQ's existing SPARQL tooling.
