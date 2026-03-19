# IQ Connector: GCP

## Purpose

This connector brings Google Cloud Platform resources and governance state into IQ as RDF.
It should scan across a project (or organization) to provide visibility into runtime, security, and audit data.

## Scope / Resources scanned

The connector should be able to scan and expose resources such as:

- **Governance / audit**: Cloud Audit Logs, Organization Policy, IAM bindings, Security Command Center findings
- **Compute & containers**: Compute Engine, GKE clusters, Cloud Run, App Engine
- **Storage**: Cloud Storage buckets, Filestore, BigQuery datasets
- **Networking**: VPCs, subnetworks, firewall rules, load balancers
- **Observability**: Cloud Monitoring alerts, logs, uptime checks

## Architecture

- **Kernel**: Polls GCP APIs and updates the connector graph.
- **Model**: Exposes synced state as an RDF `Model` that can be queried.
- **Checkpoint**: Stores continuation tokens and last update timestamps for incremental sync.

## SDK / API

- Uses Google Cloud Java client libraries (recommended) or REST endpoints.
- Auth via service account key, workload identity, or application default credentials.

## Configuration & Secrets

- **Config**:
  - `gcp.projectId`, `gcp.organizationId`, `gcp.pollInterval`, `gcp.scanServices`
  - `gcp.graphIri` (named graph for state)
- **Secrets**:
  - Store service account key material in IQ secret store.
  - Prefer workload identity / ADC to avoid long-lived keys.

## Risks & Issues

- **API quotas**: Large orgs may exceed rate limits; implement throttling/backoff.
- **Data volume**: GCP inventory can be huge; support filtering and incremental sync.
- **Permission drift**: Insufficient roles may lead to incomplete snapshots — surface missing permission details.

## State Model

Graph example: `urn:iq:connector:gcp`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (`:instance123 a :GcpComputeInstance`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Import GCP state into IQ.
- **Write-only**: (Optional) Apply changes from IQ to GCP.
- **Read-write**: Bidirectional sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- supported capabilities
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add Pub/Sub subscription to receive real-time audit events.
- Surface Cloud Asset Inventory as RDF for large-scale resource discovery.
