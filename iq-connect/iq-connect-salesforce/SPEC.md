# IQ Connector: Salesforce

## Purpose

This connector syncs Salesforce CRM data (Accounts, Contacts, Opportunities, Leads, Cases) into IQ as RDF,
enabling queries over sales pipeline, customer data, and business process workflows.

## Scope / Resources scanned

- **Core CRM objects**: Accounts, Contacts, Opportunities, Leads, Cases
- **Activity tracking**: Tasks, events, call logs
- **Sales processes**: Quotes, orders, contracts
- **Customizations**: Custom objects, picklist values, field metadata
- **Users & teams**: User hierarchy, team membership
- **Attachments**: Documents and files (metadata only)

## Architecture

- **Kernel**: Polls Salesforce APIs and updates the connector graph
- **Model**: Exposes Salesforce state as an RDF `Model`
- **Checkpoint**: Tracks last sync timestamps or query cursors for incremental sync

## SDK / API

- Uses Salesforce REST API (SOAP/GraphQL optional)
- Preferred SDK: Force.com REST API or Salesforce Java library
- Auth: OAuth2 (recommended) or username/password + security token

## Configuration & Secrets

- **Config**:
  - `salesforce.instanceUrl`: Salesforce org instance URL (e.g., `https://xxx.salesforce.com`)
  - `salesforce.pollInterval`: Polling frequency in seconds
  - `salesforce.graphIri`: Named graph IRI (e.g., `urn:iq:connector:salesforce`)
  - `salesforce.scanObjects`: Which custom/standard objects to sync
  - `salesforce.batchSize`: Records per API call

- **Secrets**:
  - `SALESFORCE_CLIENT_ID`: OAuth client ID
  - `SALESFORCE_CLIENT_SECRET`: OAuth client secret
  - Or: `SALESFORCE_USERNAME`, `SALESFORCE_PASSWORD`, `SALESFORCE_SECURITY_TOKEN`

## Risks & Issues

- **API limits**: Salesforce enforces strict API call limits (typically 15K/24h for standard edition); batch queries efficiently
- **Data volume**: Large orgs with many records can produce massive graphs; use filtering and incremental sync
- **Permission model**: User-based security may restrict some data visibility
- **Custom fields**: Metadata must be queried separately; document field mappings
- **Governor limits**: Monitor Salesforce governor limits to avoid throttling

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:salesforce`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  - `connector:apiCallsUsed` (for monitoring quotas)

- Salesforce objects:
  - `:account-123 a :SalesforceAccount ; :name "Acme Corp" ; :industry "Technology"`
  - `:contact-456 a :SalesforceContact ; :firstName "Alice" ; :lastName "Smith" ; :email "alice@acme.com"`
  - `:opp-789 a :SalesforceOpportunity ; :name "Q2 Expansion" ; :stage "Negotiation" ; :amount 50000`
  - `:user-012 a :SalesforceUser ; :username "alice@acme.com" ; :role :manager`

- Relationships:
  - `:contact-456 :relatedTo :account-123`
  - `:opp-789 :relatedTo :account-123`
  - `:opp-789 :ownedBy :user-012`

- Checkpoint (`connector:checkpoint`) for query cursors

## Sync Modes

- **Read-only**: Sync Salesforce state into IQ (most common)
- **Write-only**: (Optional) Create/update records from IQ
- **Read-write**: Bidirectional sync for CRM data management

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("salesforce")
- Capabilities (polling, bulk API, streaming)
- Graph IRI
- Required org access level

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with API limit tracking
- [ ] `I_Checkpoint` implementation for query cursors
- [ ] RDF vocabulary for Salesforce object types
- [ ] Salesforce API client integration
- [ ] SOQL query builder for efficient data retrieval
- [ ] Custom object handling
- [ ] Error handling (rate limits, auth failures)
- [ ] Unit tests with mock Salesforce API
- [ ] Configuration for org URL and credentials
- [ ] Connector descriptor registration

## Extension Points

- Use Salesforce streaming API (Pub/Sub API) for real-time updates
- Implement Salesforce Bulk API 2.0 for large-scale data sync
- Map Salesforce fields to external data models
- Track record changes via Salesforce Change Data Capture
- Integrate with approval processes for workflow RDF
- Support multi-org federation
