# IQ Connector: Stripe

## Purpose

This connector syncs Stripe entities (customers, charges, invoices) into IQ as RDF.

## Architecture

- **Kernel**: Polls Stripe APIs and updates the connector graph.
- **Model**: Exposes Stripe resources as an RDF `Model`.
- **Checkpoint**: Tracks last processed event timestamp or cursor.

## Integration Points

- Stripe API (API keys)
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `urn:iq:connector:stripe`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Stripe resources (`:customer123 a :StripeCustomer`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync Stripe state into IQ.
- **Write-only**: Apply changes from IQ to Stripe.
- **Read-write**: Bidirectional sync for full lifecycle.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, webhooks)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Use Stripe webhooks to drive incremental sync.
- Add support for multiple Stripe accounts via separate connector instances.
