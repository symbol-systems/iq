# IQ Connector: Google Apps# IQ Connector: Google Apps






















































- Add support for domain-wide delegation for multiple users.- Add delta-sync support using Gmail/Drive change tokens.## Extension ideas- [ ] Registry descriptor- [ ] `Model` view for state- [ ] `I_Checkpoint` implementation- [ ] `I_ConnectorKernel` and sync loop- [ ] `I_Connector` implementation## Implementation checklist- graph IRI- supported capabilities- connector ID and nameProvide a `ConnectorDescriptor` RDF model describing:## Registration- **Read-write**: Full bidirectional sync.- **Write-only**: (Optional) Apply changes from IQ to Google Workspace.- **Read-only**: Sync Google Workspace state into IQ.## Sync Modes- Checkpoint (`connector:checkpoint`)- Workspace entities (e.g., `:user123 a :GmailMessage`)- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)### RDF conceptsGraph example: `spiffe://{domain}/connector:google-apps`.## State Model- Connector registry for discovery- IQ RDF repository via `Model`- Google Workspace APIs (OAuth2 service account or user consent)## Integration Points- **Checkpoint**: Tracks last synced timestamp / page token.- **Model**: Exposes synced state as an RDF `Model`.- **Kernel**: Polls Google Workspace APIs and updates connector graph.## ArchitectureThis connector syncs Google Workspace (Gmail, Drive, Calendar, etc.) data into IQ as RDF.## Purpose
## Purpose

This connector syncs Google Workspace entities (Users, Groups, Drive files, Calendar events) into IQ as RDF.

## Architecture

- **Kernel**: Polls Google Workspace APIs and updates connector state.
- **Model**: Exposes state via an RDF `Model`.
- **Checkpoint**: Tracks last sync timestamps or change tokens.

## Integration Points

- Google Workspace APIs (Admin, Drive, Calendar, etc.)
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `spiffe://{domain}/connector:google-apps`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Workspace entities (`:user123 a :GoogleUser`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Workspace state into IQ.
- **Write-only**: Apply changes from IQ to Workspace.
- **Read-write**: Bidirectional sync with conflict handling.

## Registration

Provide a `ConnectorDescriptor` RDF model with:

- connector ID and display name
- supported capabilities
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add support for Google Apps Script events.
- Add incremental sync using change tokens where available.
