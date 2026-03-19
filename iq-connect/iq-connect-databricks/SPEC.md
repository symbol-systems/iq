# IQ Connector: Databricks

## Purpose

This connector syncs Databricks workspace state into IQ as RDF, covering clusters, jobs, notebooks, and workspace artifacts.

## Scope / Resources scanned

- Workspace artifacts (notebooks, libraries, repos)
- Compute resources (clusters, pools)
- Jobs and runs (history, triggers)
- Secrets and scopes (metadata only)
- Audit logs (via Unity Catalog / audit log export)

## Architecture

- **Kernel**: Polls Databricks REST APIs (or listens to audit log exports) and updates connector graph.
- **Model**: Exposes Databricks state as an RDF `Model`.
- **Checkpoint**: Tracks last scan timestamp or cursor token for incremental sync.

## SDK / API

- Uses Databricks REST API via HTTP client (preferred) or Databricks SDK for Java.
- Supports PAT (personal access token) and optional Azure/AWS credential integration.

## Config & Secrets

- **Config**:
  - `databricks.url`, `databricks.pollInterval`, `databricks.scanAreas`
  - `databricks.graphIri`
- **Secrets**:
  - Store Databricks PAT or server token in IQ secret vault.

## Risks & Issues

- **Rate limiting**: API calls can be throttled; implement backoff.
- **Large workspaces**: Inventory can be large; support pagination and filtering.
- **Sensitive data**: Avoid storing notebook contents; focus on metadata.

## State Model

Graph example: `urn:iq:connector:databricks`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Databricks resources (`:cluster123 a :DatabricksCluster`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Databricks metadata into IQ.
- **Write-only**: (Optional) Apply changes via API (create cluster, update job).
- **Read-write**: Full sync with change propagation.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, audit log)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add support for Unity Catalog auditing events.
- Add delta sync using workspace event streams.
