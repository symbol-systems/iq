# IQ Connector: GCP

## Purpose

This connector syncs Google Cloud Platform resources (Compute, Storage, IAM, etc.) into IQ as RDF.

## Architecture

- **Kernel**: Polls GCP APIs and converts resource data into RDF.
- **Model**: Exposes connected state as an RDF `Model`.
- **Checkpoint**: Stores continuation tokens / last updated timestamps for incremental sync.

## Integration Points

- GCP APIs (authenticated via service account)
- IQ RDF repository (`Model` view)
- Connector registry for discovery

## State Model

Graph example: `spiffe://{domain}/connector:gcp`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (e.g., `:instance123 a :GcpComputeInstance`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Keep IQ up-to-date with GCP.
- **Write-only**: (Optional) Apply configuration changes from IQ to GCP.
- **Read-write**: Bidirectional synchronisation.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- supported capabilities
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` and sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add audit trail mapping via RDF to represent who changed what.
- Support real-time updates using Pub/Sub subscriptions.
