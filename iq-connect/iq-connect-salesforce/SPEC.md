# IQ Connector: Salesforce

## Purpose

This connector syncs Salesforce entities (Accounts, Contacts, Opportunities, etc.) into IQ as RDF.

## Architecture

- **Kernel**: Polls Salesforce APIs and updates the connector graph.
- **Model**: Exposes Salesforce state as an RDF `Model`.
- **Checkpoint**: Tracks last sync timestamps or query cursors.

## Integration Points

- Salesforce REST/SOAP API
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `urn:iq:connector:salesforce`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Salesforce objects (`:account123 a :SalesforceAccount`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Salesforce state into IQ.
- **Write-only**: Apply changes from IQ to Salesforce.
- **Read-write**: Bidirectional sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, streaming)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Support Salesforce Streaming API / PushTopic for near-real-time updates.
- Add mapping for Salesforce metadata (schema, fields) into RDF vocabulary.
