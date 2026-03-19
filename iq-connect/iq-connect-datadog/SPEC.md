# IQ Connector: Datadog

## Purpose

This connector syncs Datadog monitoring and observability state into IQ as RDF, enabling queries over dashboards, monitors, and incidents.

## Scope / Resources scanned

- Monitors and alerts
- Dashboards and widgets
- Logs and log indexes
- Synthetics tests
- Security rules (Detection Rules / Threat Management)

## Architecture

- **Kernel**: Polls Datadog APIs and updates connector graph.
- **Model**: Exposes Datadog state as an RDF `Model`.
- **Checkpoint**: Tracks last scanned timestamp or API cursor.

## SDK / API

- Uses Datadog REST API (v1/v2) via HTTP client.
- Auth via API key + app key stored in IQ secret vault.

## Config & Secrets

- **Config**:
  - `datadog.pollInterval`, `datadog.scanAreas`, `datadog.graphIri`
- **Secrets**:
  - `DATADOG_API_KEY`, `DATADOG_APP_KEY` stored in IQ secret store.

## Risks & Issues

- **Rate limiting**: Datadog APIs enforce rate limits; implement retries/backoff.
- **Data volume**: Log/config objects can be large; support filtering.

## State Model

Graph example: `urn:iq:connector:datadog`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Datadog objects (`:monitor123 a :DatadogMonitor`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Datadog state into IQ.
- **Write-only**: (Optional) Update Datadog via API.
- **Read-write**: Full sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, events)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Add webhook support for events (e.g., monitor alerts).
- Support ingesting Datadog security findings as governance graphs.
