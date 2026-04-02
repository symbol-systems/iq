# IQ Connector: Stripe

## Purpose

This connector syncs Stripe payment entities (customers, charges, invoices, subscriptions, payments) into IQ as RDF,
enabling queries over payment state, billing history, and financial transaction flows.

## Scope / Resources scanned

- **Core entities**: Customers, invoices, charges, refunds, disputes
- **Subscriptions**: Subscription plans, active subscriptions, billing cycles
- **Payment methods**: Cards, bank accounts, payment intents
- **Webhooks**: Event logs and integration status
- **Financial reporting**: Transactions, balance transfers, payouts
- **Metadata**: Tags, discounts, coupons

## Architecture

- **Kernel**: Polls Stripe APIs and updates the connector graph
- **Model**: Exposes Stripe resources as an RDF `Model`
- **Checkpoint**: Tracks last processed event timestamp or cursor for incremental sync

## SDK / API

- Uses Stripe Java SDK (com.stripe)
- Authentication via API keys (secret key for backend)
- Recommended: Use restricted API keys with minimal scopes

## Configuration & Secrets

- **Config**:
  - `stripe.pollInterval`: Polling frequency in seconds
  - `stripe.graphIri`: Named graph IRI (e.g., `urn:iq:connector:stripe`)
  - `stripe.scanEntities`: Which entity types to sync (customers, charges, subscriptions, etc.)
  - `stripe.batchSize`: Records per API call

- **Secrets**:
  - `STRIPE_SECRET_KEY`: Stripe secret API key
  - `STRIPE_RESTRICTED_KEY`: (Optional) Use restricted key for reduced blast radius

## Risks & Issues

- **API rate limits**: Stripe allows 100 requests/sec; implement rate limiting and backoff
- **Data volume**: Large merchants with many transactions create sizable graphs
- **PII handling**: Customer data may contain sensitive information; implement filtering
- **Webhook security**: Verify webhook signatures to prevent spoofing
- **Transaction immutability**: Stripe transactions are immutable; only additions sync

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:stripe`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")

- Stripe entities:
  - `:customer-123 a :StripeCustomer ; :email "alice@example.com" ; :description "Alice Smith"`
  - `:charge-456 a :StripeCharge ; :amount 9999 ; :currency "usd" ; :status "succeeded"`
  - `:invoice-789 a :StripeInvoice ; :customerId :customer-123 ; :paid true ; :total 9999`
  - `:subscription-012 a :StripeSubscription ; :customerId :customer-123 ; :status "active" ; :planId :plan-monthly`

- Relationships:
  - `:charge-456 :chargedTo :customer-123`
  - `:invoice-789 :pays :charge-456`
  - `:subscription-012 :subscribedBy :customer-123`
  - `:payment-method a :StripePaymentMethod ; :belongsTo :customer-123`

- Checkpoint (`connector:checkpoint`) for event sequences

## Sync Modes

- **Read-only**: Sync Stripe state into IQ (most common, recommended for accounting/analytics)
- **Write-only**: (Optional) Create/update customers, charges from IQ
- **Read-write**: Full billing system integration with bi-directional sync

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("stripe")
- Capabilities (polling, webhooks, exports)
- Graph IRI
- Required API access level

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with rate limit handling
- [ ] `I_Checkpoint` implementation for event tracking
- [ ] RDF vocabulary for Stripe object types
- [ ] Stripe Java SDK integration
- [ ] Support for webhook event verification
- [ ] Error handling and retry logic
- [ ] Unit tests with mock Stripe API (or Stripe sandbox)
- [ ] Configuration for API keys
- [ ] Connector descriptor registration

## Extension Points

- Use Stripe webhooks for near-real-time transaction sync instead of polling
- Integrate with accounting systems (mapping to AR/GL accounts)
- Support multi-account Stripe setups
- Implement revenue recognition workflows via SPARQL rules
- Map disputes to RDF conflict models
- Track subscriber churn and lifetime value
