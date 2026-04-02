# IQ Connector: Parquet

## Purpose

This connector syncs Parquet data files (local or cloud-based) into IQ as RDF facts,
enabling analysis of structured data via SPARQL queries and integration with other organizational datasets.

## Scope / Resources scanned

- **Parquet files**: Local filesystem or cloud storage (S3, Azure Blob, GCS)
- **Schema inference**: Automatically derive schema from Parquet files
- **Row data**: Convert tabular rows into RDF facts
- **Nested structures**: Flatten or preserve complex data types (arrays, maps)
- **Partitioning**: Respect Parquet partitioning for incremental sync

## Architecture

- **Kernel**: Reads Parquet files on a schedule, converts rows into RDF triples, and updates the connector graph
- **Model**: Exposes Parquet-derived triples as an RDF `Model`
- **Checkpoint**: Tracks processed file names, last-modified times, or row offsets for resumable sync

## SDK / API

- Uses Apache Parquet library for reading
- Cloud storage: AWS S3 SDK, Azure SDK, or Google Cloud Storage library
- Local filesystem: Standard file I/O

## Configuration & Secrets

- **Config**:
  - `parquet.paths`: List of file paths or glob patterns (e.g., `s3://bucket/data/*.parquet`)
  - `parquet.pollInterval`: How often to check for new files (seconds)
  - `parquet.graphIri`: Named graph IRI for RDF facts (e.g., `urn:iq:connector:parquet`)
  - `parquet.batchSize`: Rows to process per sync cycle
  - `parquet.inferSchema`: Auto-infer schema vs. use provided schema

- **Secrets**:
  - For S3: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
  - For Azure: `AZURE_STORAGE_ACCOUNT_NAME`, `AZURE_STORAGE_ACCOUNT_KEY`
  - For GCS: `GOOGLE_APPLICATION_CREDENTIALS`

## Risks & Issues

- **Data volume**: Large Parquet files can produce huge RDF graphs; implement row filtering and pagination
- **Schema complexity**: Nested types may not map perfectly to RDF; document transformation logic
- **File format variations**: Different Parquet encodings may have edge cases
- **Memory usage**: Reading entire Parquet files into memory may overflow; stream processing recommended
- **Data sensitivity**: Parquet may contain PII; implement filtering policies

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:parquet`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAtTime` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  - `connector:filesProcessed` (count)

- Data facts (example for a users dataset):
  - `:row-123 a :ParquetRow ; :column_id 123 ; :column_name "Alice" ; :column_email "alice@example.com"`
  - Schema relationships:
- `:ParquetRow :hasColumn :column_id`, etc.

- File metadata:
  - `:file a :ParquetFile ; :path "s3://bucket/users.parquet" ; :rowCount 50000`

- Checkpoint (`connector:checkpoint`) for tracking last offset

## Sync Modes

- **Read-only**: Import Parquet data into IQ (most common)
- **Write-only**: (Optional) Export changes from RDF back to Parquet files
- **Read-write**: Full ETL-style bidirectional sync

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("parquet")
- Capabilities (batch import, incremental sync)
- Graph IRI
- Supported schema types

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with file watching
- [ ] `I_Checkpoint` implementation for file offsets
- [ ] Parquet library integration
- [ ] Cloud storage client support (S3, Azure, GCS)
- [ ] Schema inference logic
- [ ] Row-to-RDF conversion
- [ ] Nested type handling (flattening vs. preservation)
- [ ] Error handling for malformed files
- [ ] Unit tests with sample Parquet files
- [ ] Configuration for file paths and credentials
- [ ] Connector descriptor registration

## Extension Points

- Implement incremental sync using file modification times
- Support columnar queries for analytical workloads
- Add schema versioning and evolution handling
- Map Parquet schemas to SHACL shapes
- Integrate with data quality frameworks (e.g., Great Expectations)
- Support partitioned Parquet datasets (Hive-style)
- Add compression format detection and handling
