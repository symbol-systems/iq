# IQ Connector: GitHub

## Purpose

This connector syncs GitHub resources (repositories, issues, PRs, teams) into IQ as RDF.

## Architecture

- **Kernel**: Polls GitHub APIs or listens to webhooks and updates the connector graph.
- **Model**: Presents GitHub state as an RDF `Model`.
- **Checkpoint**: Tracks last processed event/updated timestamp.

## Integration Points

- GitHub REST API / GraphQL
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `spiffe://{domain}/connector:github`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- GitHub entities (`:repo a :GitHubRepository`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync GitHub state into IQ.
- **Write-only**: Apply changes from IQ to GitHub (e.g., create issues).
- **Read-write**: Full two-way sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID + name
- capabilities (polling, webhooks)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` and sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add webhook support to drive sync instead of polling.
- Support GitHub Apps authorization and token refresh.
