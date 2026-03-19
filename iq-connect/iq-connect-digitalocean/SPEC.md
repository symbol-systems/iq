# IQ Connector: DigitalOcean

## Purpose

This connector syncs DigitalOcean resources (droplets, volumes, spaces, apps, networking, etc) into IQ as RDF.

## Architecture

- **Kernel**: Polls DigitalOcean APIs and updates connector state.
- **Model**: Exposes synced state as a `Model` for query and reasoning.
- **Checkpoint**: Stores pagination tokens or last-updated timestamps.

## Integration Points

- DigitalOcean API (token-based auth)
- IQ RDF repository via `Model`
- Connector registry for discovery and metadata

## State Model

Graph example: `spiffe://{domain}/connector:digitalocean`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource representations (`:droplet123 a :DigitalOceanDroplet`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Keep IQ in sync with DigitalOcean state.
- **Write-only**: (Optional) Apply changes to DigitalOcean.
- **Read-write**: Two-way sync with conflict handling.

## Registration

Use a `ConnectorDescriptor` RDF model with fields like:

- connector ID / name
- capabilities (polling, webhook, etc.)
- graph reference

## Implementation checklist

- [ ] Implement `I_Connector` + `I_ConnectorKernel`
- [ ] Add `I_Checkpoint` support
- [ ] Expose `Model` for connector state
- [ ] Register connector descriptor in the registry graph

## Extension ideas

- Add webhook support for DigitalOcean events.
- Add incremental sync for large resource sets.
