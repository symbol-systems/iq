# IQ Connector: Azure

## Purpose

This connector integrates IQ with Azure services (e.g., Resource Manager, Storage, AD, etc.). It is designed to represent Azure state as RDF and keep it synchronized into IQ.

## Architecture

- **Kernel**: Runs a reactive sync loop, polling Azure APIs and updating the connector graph.
- **Model**: Exposes connector state as an RDF `Model` that can be queried by IQ.
- **Checkpoint**: Persists sync position (e.g., continuation tokens, last poll timestamp).

## Integration Points

- Azure SDK clients (via managed identity / service principal)
- IQ RDF repository (via `Model` view)
- Connector registry (descriptor stored in a discovery graph)

## State Model

State lives in a dedicated graph such as `spiffe://{domain}/connector:azure`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (e.g., `:storageAccount a :AzureStorageAccount`)
- Checkpoint tokens (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Poll and refresh Azure state into IQ.
- **Write-only**: Apply updates from IQ to Azure (optional).
- **Read-write**: Bidirectional sync with conflict handling.

## Registration

The connector should provide a `ConnectorDescriptor` RDF model with:

- connector ID and display name
- supported capabilities
- graph IRI

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] Exposed `Model` for state and metadata
- [ ] Registry descriptor stored in discovery graph

## Extension Notes

- Support for additional Azure services should map each resource type to RDF classes/properties.
- Add hooks for webhook/event-driven sync where supported (e.g., Azure Event Grid).
