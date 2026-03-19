# IQ Connector: Azure

## Purpose

This connector brings Azure environment and governance state into IQ as RDF.
It should scan and expose the full scope of an Azure tenant/subscription needed for
runtime visibility, security posture, and compliance.

## Scope / Resources scanned

The connector should be able to scan and surface:

- **Governance & compliance**: Azure Policy, resource locks, RBAC (roles, assignments), Azure Security Center findings
- **Inventory**: Resource Manager resources (VMs, Storage Accounts, SQL, AKS, App Service, Key Vault)
- **Networking**: VNets, NSGs, Application Gateways, Load Balancers
- **Observability**: Monitor alerts, Log Analytics workspaces, Activity Logs
- **Identity**: Azure AD users/groups, service principals, managed identities

## Architecture

- **Kernel**: Runs a reactive sync loop, polling Azure APIs (Resource Graph, Management APIs) and updating the connector graph.
- **Model**: Exposes connector state as an RDF `Model` that IQ can query.
- **Checkpoint**: Persists sync position (continuation tokens, last poll timestamp) to avoid full scans every run.

## SDK / API

- Uses Azure SDK for Java (Azure Resource Manager libraries, Graph API, Monitor API).
- Supports authentication via Managed Identity, Service Principal (client secret / cert), or token cache.

## Configuration & Secrets

- **Config**:
  - `azure.subscriptionId`, `azure.tenantId`, `azure.pollInterval`, `azure.scanServices`
  - `azure.graphIri` (named graph for state)
- **Secrets**:
  - Store client secrets/keys in IQ secret vault.
  - Prefer managed identity where possible to avoid secret leakage.

## Risks & Issues

- **API throttling**: Resource Graph and Management API calls can be limited; implement backoff.
- **Data volume**: Subscriptions with many resources can produce large RDF graphs; support filtering.
- **Permission scope**: Least privilege is critical; missing permissions should surface clearly.

## State Model

Graph example: `urn:iq:connector:azure`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (`:vm123 a :AzureVirtualMachine`)
- Checkpoint tokens (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Poll and refresh Azure state into IQ.
- **Write-only**: (Optional) Apply changes from IQ to Azure.
- **Read-write**: Two-way sync with conflict resolution.

## Registration

The connector should publish a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, webhook, delta query)
- graph IRI

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop (polling + error handling)
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state and metadata
- [ ] Registry descriptor in discovery graph

## Extension Notes

- Support event-driven sync using Event Grid / Activity Log endpoints.
- Add delta sync via Resource Graph queries (filter by `updatedTime`).
