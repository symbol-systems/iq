# iq-lake — Data Lake and Document Ingestion

`iq-lake` is IQ's document and data ingestion pipeline. It handles bringing unstructured and semi-structured content into the IQ knowledge graph — parsing documents, extracting meaningful content, and making it available for agent reasoning.

## What it provides

- **Document ingestion** — reads files from local paths, S3 buckets, and VFS-accessible sources and extracts content using Apache Tika
- **Tika integration** — supports a wide range of document formats including PDF, Office documents, HTML, and plain text
- **S3 connector** — streams data from Amazon S3 into the lake pipeline
- **Web content fetching** — retrieves and parses HTML pages via jsoup for knowledge graph enrichment
- **Realm-scoped loading** — ingests content into a specific realm's knowledge graph rather than a global store
- **Lake lifecycle** — `Lakes.boot()` initialises all configured lake sources when IQ starts, making content available to agents from the first request

## Role in the system

`iq-lake` feeds raw content into IQ so that agents have something meaningful to reason over. It is initialised by `iq-apis` at startup and adds its ingested content to realm knowledge graphs before agents begin running.

## Requirements

- Java 21
- Maven (wrapper included)
- Apache Tika for document parsing
- AWS credentials if using S3 sources
