# IQ Connector: AWS

## Purpose

This connector integrates IQ with AWS services (e.g., S3, IAM, EC2, etc.). It is designed to represent AWS state as RDF and keep it synchronized into IQ.

## Architecture

- **Kernel**: Runs a reactive sync loop, polling AWS APIs, and updating the connector graph.
- **Model**: Exposes connector state as an RDF `Model` that can be queried by IQ.
- **Checkpoint**: Uses a checkpoint object to persist sync position (e.g., next token, last AWS datetime).

## Integration Points

- AWS SDK clients (configured via environment or secrets vault)
- IQ RDF repository (via `Model` view)
- Connector registry (descriptor stored in a discovery graph)

## State Model

Connector state is stored in a dedicated graph, e.g. `spiffe://{domain}/connector:aws`.

### Key RDF concepts

- Connector state (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (e.g., `:s3Bucket a :S3Bucket`)
- Checkpoint tokens (e.g., `connector:checkpoint`)

## Sync Modes

- **Read-only**: Poll and refresh AWS state into IQ.
- **Write-only**: (Optional) Apply changes from IQ to AWS.
- **Read-write**: Keep state in sync with bidirectional mapping.

## Registration

The connector should publish a `ConnectorDescriptor` RDF document describing:

- connector ID and name
- capabilities (polling, webhook, etc.)
- supported sync modes

## Checklist for implementation

- [ ] Provide `I_Connector` implementation
- [ ] Provide `I_ConnectorKernel` (sync loop)
- [ ] Provide `I_Checkpoint` implementation
- [ ] Expose `Model` for state (state graph + metadata)
- [ ] Register connector in the registry graph

## Extension Points

- Add additional AWS services by adding new RDF vocab terms and sync routines.
- Implement a webhook-based variant for services that support event notifications.
