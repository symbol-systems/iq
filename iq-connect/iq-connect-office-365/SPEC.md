# IQ Connector: Office 365

## Purpose

This connector syncs Office 365 (Microsoft 365) resources (Users, Groups, Mail, SharePoint, etc.) into IQ as RDF.

## Architecture

- **Kernel**: Polls Microsoft Graph API (or subscribes to change notifications) and updates connector graph.
- **Model**: Exposes synced state as an RDF `Model`.
- **Checkpoint**: Tracks last sync timestamps or change tokens.

## Integration Points

- Microsoft Graph API (auth via OAuth2 / App Registration)
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `urn:iq:connector:office-365`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Office 365 entities (`:user123 a :Office365User`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Graph data into IQ.
- **Write-only**: Apply changes from IQ to Graph.
- **Read-write**: Bidirectional sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, subscriptions)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Use Graph change notifications (webhooks) for real-time updates.
- Support tenant-wide delegation for multi-tenant sync.
