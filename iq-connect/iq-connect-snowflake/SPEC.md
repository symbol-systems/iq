# IQ Connector: Snowflake

## Purpose

This connector syncs Snowflake metadata (databases, schemas, tables, warehouses, roles) into IQ as RDF.

## Scope / Resources scanned

- Databases, schemas, tables, views
- Warehouses and compute configuration
- Roles, grants, and access policies
- Query history (optional) for audit use cases

## Architecture

- **Kernel**: Queries Snowflake metadata views and updates connector graph.
- **Model**: Exposes Snowflake metadata as an RDF `Model`.
- **Checkpoint**: Tracks last scan time or cursor for incremental sync.

## SDK / API

- Uses Snowflake JDBC driver (or Snowflake Java SDK) for metadata queries.
- Auth via username/password, keypair, or OAuth.

## Config & Secrets

- **Config**:
  - `snowflake.account`, `snowflake.warehouse`, `snowflake.database`, `snowflake.schema`
  - `snowflake.graphIri`
- **Secrets**:
  - Store Snowflake credentials in IQ secret store.

## Risks & Issues

- **Query cost**: Snowflake compute cost for metadata scans; keep scans targeted.
- **Permission drift**: Missing privileges can lead to incomplete inventory.

## State Model

Graph example: `urn:iq:connector:snowflake`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Snowflake objects (`:table123 a :SnowflakeTable`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Snowflake metadata into IQ.
- **Write-only**: (Optional) Apply changes (granting roles, creating objects).
- **Read-write**: Full sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, metadata)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Incremental sync via Snowflake’s `information_schema` change tracking.
- Map row-level access policies to RDF for governance analysis.
