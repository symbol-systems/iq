# IQ Connector: Parquet

## Purpose

This connector syncs Parquet data files (local or cloud) into IQ as RDF facts.

## Architecture

- **Kernel**: Reads Parquet files on a schedule, converts rows into RDF triples, and updates the connector graph.
- **Model**: Exposes Parquet-derived triples as an RDF `Model`.
- **Checkpoint**: Tracks processed file names, last-modified times, or offsets.

## Integration Points

- Parquet files accessed via local filesystem or cloud storage (S3, Azure Blob, etc.)
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `urn:iq:connector:parquet`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Data rows as RDF triples (`:row123 a :ParquetRow`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Import Parquet data into IQ.
- **Write-only**: (Optional) Export changes from IQ back to Parquet (requires write logic).
- **Read-write**: Full ETL-style sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (batch import, incremental)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Support schema inference from Parquet schema.
- Add incremental sync using file system watch events.
