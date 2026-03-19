# IQ Connector: Docker

## Purpose

This connector syncs Docker engine/container state into IQ as RDF, enabling queries against running containers, images, networks, and volumes.

## Architecture

- **Kernel**: Polls the Docker Engine API (or listens to events) and updates the connector graph.
- **Model**: Exposes Docker runtime state as an RDF `Model`.
- **Checkpoint**: Tracks last event time or last inspected state.

## Integration Points

- Docker Engine API (unix socket / TCP)
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `spiffe://{domain}/connector:docker`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Docker objects (`:container123 a :DockerContainer`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Observe Docker state in IQ.
- **Write-only**: (Optional) Issue Docker actions (start/stop) from IQ.
- **Read-write**: Full round-trip control + state imaging.

## Registration

Provide a `ConnectorDescriptor` RDF model that includes:

- connector ID and type
- capabilities (polling, events)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor entry

## Extension ideas

- Support Docker events (via WebSocket) for near-real-time sync.
- Add mapping for docker-compose resources.
