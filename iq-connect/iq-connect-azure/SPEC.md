# IQ Connector: Azure

## Purpose

This connector brings Azure environment and governance state into IQ as RDF.
It scans a tenant/subscription to provide visibility into runtime resources, security posture, and compliance state.

## Scope / Resources scanned

The connector scans and surfaces:

- **Governance & compliance**: Azure Policy assignments, resource locks, RBAC (roles, assignments), Azure Security Center findings
- **Compute & containers**: Virtual Machines, App Service plans, AKS clusters, Container Instances
- **Storage & databases**: Storage Accounts, Azure SQL, Cosmos DB, SQL Data Warehouse
- **Networking**: Virtual Networks, Network Security Groups, Application Gateways, Load Balancers, VPN Gateways
- **Observability**: Monitor alerts, Log Analytics workspaces, Application Insights, Activity Logs
- **Identity & security**: Azure AD users/groups, service principals, managed identities, Key Vault secrets
- **Integration**: Service Bus, Event Hubs, API Management

## Architecture

- **Kernel**: Runs a reactive sync loop, polling Azure APIs (Resource Graph, Management) and updating the connector graph
- **Model**: Exposes connector state as an RDF `Model` that IQ can query
- **Checkpoint**: Persists sync position (continuation tokens, last poll timestamp) to avoid full scans every run

## SDK / API

- Uses Azure SDK for Java (Azure Resource Manager libraries, Graph API, Monitor API)
- Supports authentication via:
  - Managed Identity (recommended for Azure VMs/AKS)
  - Service Principal (client secret or certificate)
  - Shared Key authentication
  - Token cache (for local development)

## Configuration & Secrets

- **Config**:
  - `azure.subscriptionId`: Target subscription ID
  - `azure.tenantId`: Azure AD tenant ID
  - `azure.pollInterval`: Polling frequency (seconds)
  - `azure.scanServices`: List of services to include (compute, storage, networking, etc.)
  - `azure.graphIri`: Named graph IRI for state (e.g., `urn:iq:connector:azure`)
  - `azure.resourceGroupFilter`: (Optional) Filter to specific resource groups

- **Secrets**:
  - For Service Principal auth:
- `AZURE_CLIENT_ID`: Service principal app ID
- `AZURE_CLIENT_SECRET`: Client secret
- `AZURE_TENANT_ID`: Tenant ID
  - For Managed Identity auth: None (automatic in Azure VMs/AKS)

## Risks & Issues

- **API throttling**: Resource Graph and Management API calls can hit rate limits; implement backoff via `RetryPolicy`
- **Data volume**: Subscriptions with many resources can produce large RDF graphs; support filtering and incremental sync
- **Permission scope**: Least privilege is critical; missing roles will result in incomplete scans—surface permission gaps in connector status
- **Cost**: Some queries (Resource Graph, Security Center) may incur charges; optimize query patterns
- **Stale data**: Resource Graph may have 5-minute latency behind real state

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:azure`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  - `connector:resourceCount` (total resources scanned)
  
- Azure resource snapshots:
  - `:vm123 a :AzureVirtualMachine ; :name "prod-web-01" ; :resourceGroup :rg-prod ; :powerState "running"`
  - `:sa456 a :AzureStorageAccount ; :name "storageacct" ; :kind "StorageV2"` 
  - `:policy789 a :AzurePolicy ; :displayName "Require TLS 1.2"` 

- Relationships:
  - `:vm123 :inResourceGroup :rg-prod`
  - `:sa456 :inSubscription :sub-123`
  - `:user a :AzureADUser ; :hasMemberOf :group`

- Checkpoint data (`connector:checkpoint`) for resuming sync

## Sync Modes

- **Read-only**: Poll and refresh Azure state into IQ (most common)
- **Write-only**: (Optional) Apply changes from IQ to Azure (create resources, assign policies)
- **Read-write**: Two-way sync with conflict resolution

## Registration

The connector publishes a `ConnectorDescriptor` RDF model describing:

- Connector ID ("azure")
- Display name and description
- Capabilities (polling, batch queries, delta query)
- Target graph IRI
- Required permissions

## Implementation Checklist

- [ ] `I_Connector` implementation exposing subscription state as `Model`
- [ ] `I_ConnectorKernel` sync loop with polling and backoff
- [ ] `I_Checkpoint` implementation for sync position storage
- [ ] RDF vocabulary for Azure resource types
- [ ] Integration with Azure SDK for Resource Manager
- [ ] Integration with Azure Resource Graph for efficient bulk queries
- [ ] Error handling and permission gap reporting
- [ ] Unit tests with mocked Azure APIs
- [ ] Configuration provider for tenant/subscription settings
- [ ] Connector descriptor registration

## Extension Points

- **Real-time sync**: Use Event Grid events and Service Bus for near-real-time updates instead of polling
- **Delta queries**: Implement incremental sync via Resource Graph change tracking (filter by `updatedTime`)
- **Multi-subscription**: Support multiple subscriptions via separate connector instances or multi-sub aggregation
- **Policy analysis**: Map Azure Policy compliance to RDF governance models
- **Cost analysis**: Integrate with Azure Cost Management API for cost attribution
- **Security posture**: Map Azure Security Center findings and vulnerabilities into risk RDF
