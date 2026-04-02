# IQ Connector: Office 365

## Purpose

This connector syncs Office 365 (Microsoft 365) resources and organizational data (Users, Groups, Mail, SharePoint, Teams) into IQ as RDF,
enabling queries over organizational structure, file governance, and collaboration policies.

## Scope / Resources scanned

- **Identity**: Users, groups, organizational units, roles
- **Exchange**: Mailboxes, distribution lists, mail rules
- **SharePoint**: Sites, libraries, folders, document metadata
- **Teams**: Team channels, members, installed apps
- **OneDrive**: File storage and sharing permissions
- **Calendar**: Shared calendars and meeting rooms
- **Security**: Conditional access policies, MFA status

## Architecture

- **Kernel**: Polls Microsoft Graph API (or subscribes to webhooks) and updates connector graph
- **Model**: Exposes synced state as an RDF `Model`
- **Checkpoint**: Tracks last sync timestamps or change tokens for incremental sync

## SDK / API

- Uses Microsoft Graph API via HTTP client or Microsoft Graph Java SDK
- Auth via OAuth2 / Service Principal (recommended) or user consent
- Preferred: Application permissions (app-owned rather than user-delegated)

## Configuration & Secrets

- **Config**:
  - `office365.tenantId`: Azure AD tenant ID
  - `office365.pollInterval`: Polling frequency in seconds
  - `office365.graphIri`: Named graph IRI (e.g., `urn:iq:connector:office-365`)
  - `office365.scanAreas`: Which resources to include (users, teams, sharepoint, etc.)
  - `office365.includeTeamChat`: Whether to index Teams chat messages (true/false)

- **Secrets**:
  - `OFFICE365_CLIENT_ID`: Service principal application ID
  - `OFFICE365_CLIENT_SECRET`: Service principal secret
  - `OFFICE365_TENANT_ID`: Azure AD tenant ID

## Risks & Issues

- **API throttling**: Microsoft enforces strict rate limits; implement backoff and retry-after
- **Data volume**: Large organizations produce massive graphs; support filtering and incremental sync
- **Permission complexity**: SharePoint and Teams permissions are complex; focused models recommended
- **PII concerns**: Avoid storing full email/chat content; focus on metadata
- **Compliance**: Some data may be sensitive; implement filtering based on retention policies

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:office-365`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")

- Office 365 resources:
  - `:user-123 a :Office365User ; :mail "alice@company.com" ; :displayName "Alice" ; :mfaEnabled true`
  - `:group-456 a :Office365Group ; :mail "team@company.com" ; :displayName "Engineering Team"`
  - `:site-789 a :SharePointSite ; :displayName "Project Alpha" ; :owner :user-123`
  - `:team-012 a :MicrosoftTeam ; :displayName "Engineering" ; :privacy "Private"`

- Relationships:
  - `:user-123 :memberOf :group-456`
  - `:user-123 :owns :site-789`
  - `:user-123 :memberOf :team-012`
  - `:file a :SharePointFile ; :inSite :site-789 ; :sharedWith :user-456`

- Checkpoint (`connector:checkpoint`) for change tracking

## Sync Modes

- **Read-only**: Sync Microsoft 365 state into IQ (most common)
- **Write-only**: (Optional) Create users/groups, update permissions from IQ
- **Read-write**: Bidirectional sync with bi-directional provisioning

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("office365")
- Capabilities (polling, change notifications, delta query)
- Graph IRI
- Required permissions

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with API throttling handling
- [ ] `I_Checkpoint` implementation for change tokens
- [ ] RDF vocabulary for Office 365 resource types
- [ ] Microsoft Graph SDK integration
- [ ] Support for service principal auth
- [ ] Error handling and permission checks
- [ ] Unit tests with mock Graph API
- [ ] Configuration for tenant and credentials
- [ ] Connector descriptor registration

## Extension Points

- Use Microsoft Graph change notifications (webhooks) for real-time updates
- Implement delta queries for incremental sync of large collections
- Map conditional access policies to RDF security models
- Integrate with Defender/Security Center findings
- Support multiple tenants via separate connector instances
- Add DLP (Data Loss Prevention) policy enforcement
