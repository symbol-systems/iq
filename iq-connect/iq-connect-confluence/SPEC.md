# IQ Connector: Confluence

## Purpose

This connector syncs content from Confluence (pages, spaces, attachments) into IQ as RDF.

## Architecture

- **Kernel**: Polls Confluence REST APIs, detects changes, and updates the connector graph.
- **Model**: Presents the Confluence snapshot as an RDF `Model` for IQ queries.
- **Checkpoint**: Tracks last sync timestamp / page update token.

## Integration Points

- Confluence REST API (authenticated via API token)
- IQ repository via `Model`
- Connector registry for discovery

## State Model

Graph IRI example: `spiffe://{domain}/connector:confluence`.

### RDF concepts

- Connector metadata: `connector:lastSyncedAt`, `connector:syncStatus`
- Content representation: `:page a :ConfluencePage ; :title "..." ; :space :X .`
- Sync checkpoint stored as `connector:checkpoint`

## Sync Modes

- **Read-only**: Sync Confluence into IQ.
- **Write-only**: (Optional) Push IQ authoring changes into Confluence.
- **Read-write**: Maintain bidirectional sync.

## Registration

Expose a `ConnectorDescriptor` (RDF) describing:

- Connector identity and name
- Capabilities
- Target system (Confluence)

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync runner
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Connector descriptor registry entry

## Extension ideas

- Implement incremental sync using Confluence content change tracking.
- Add caching layer for API rate limiting.
