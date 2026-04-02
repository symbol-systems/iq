# IQ Connector: Google Workspace

## Purpose

This connector syncs Google Workspace (formerly G Suite) entities (Users, Groups, Drive files, Calendar events) into IQ as RDF,
enabling queries over organizational structure, file governance, and calendar/meeting data.

## Scope / Resources scanned

- Users and organizational structure
- Groups and group memberships
- Drive files, folders, and sharing permissions
- Calendar events and attendee information
- Apps and OAuth integrations
- Security settings and device management (via Admin SDK)

## Architecture

- **Kernel**: Polls Google Workspace APIs and updates connector state
- **Model**: Exposes state via an RDF `Model`
- **Checkpoint**: Tracks last sync timestamps or change tokens for incremental sync

## SDK / API

- Uses Google Workspace APIs (Admin SDK, Drive API, Calendar API, etc.)
- OAuth2 service account or user consent flow
- Preferred: Service account with domain-wide delegation for organization-level access

## Configuration & Secrets

- **Config**:
  - `google.domain`: Google Workspace domain name
  - `google.pollInterval`: Polling interval in seconds
  - `google.graphIri`: Named graph IRI (e.g., `urn:iq:connector:google-apps`)
  - `google.scanAreas`: Which resources to scan (users, groups, drive, calendar, etc.)
  - `google.adminEmail`: Admin account email for API delegated access

- **Secrets**:
  - `GOOGLE_SERVICE_ACCOUNT_KEY`: Service account JSON key
  - Or: `GOOGLE_OAUTH_CLIENT_ID` + `GOOGLE_OAUTH_CLIENT_SECRET` for user OAuth flow

## Risks & Issues

- **API quotas**: Google enforces per-API quotas; implement backoff and retries
- **Data volume**: Large workspaces with many files/calendars can produce massive graphs
- **Permission complexity**: File sharing and access policies require complex RDF modeling
- **PII concerns**: Avoid storing personal calendar details; focus on meeting metadata

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:google-apps`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Google Workspace entities:
  - `:user123 a :GoogleUser ; :email "alice@domain.com" ; :name "Alice" ; :suspended false`
  - `:group456 a :GoogleGroup ; :email "team@domain.com" ; :name "Team"`
  - `:file789 a :GoogleDriveFile ; :title "Report.pdf" ; :owner :user123`
  - `:event012 a :GoogleCalendarEvent ; :title "Meeting" ; :startTime "2026-04-01T10:00:00Z"`
- Relationships:
  - `:user123 :memberOf :group456`
  - `:user123 :owns :file789`
  - `:user123 :attendee :event012`
- Checkpoint (`connector:checkpoint`) for resuming incremental sync

## Sync Modes

- **Read-only**: Sync Workspace state into IQ (most common)
- **Write-only**: Apply changes from IQ to Workspace (create users, groups)
- **Read-write**: Bidirectional sync with conflict handling

## Registration

Provide a `ConnectorDescriptor` RDF model with:

- connector ID and display name
- supported capabilities (polling, change tracking)
- graph IRI

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop
- [ ] `I_Checkpoint` implementation for incremental sync
- [ ] RDF vocabulary for Google Workspace entities
- [ ] Error handling and quota management
- [ ] Unit tests with mocked Google APIs
- [ ] Configuration for domain and admin access
- [ ] Connector descriptor registration

## Extension Ideas

- Add support for domain-wide delegation for multi-tenant setups
- Add delta-sync support using Gmail/Drive change tokens
- Map security policies (MFA enforcement, IP restrictions) to RDF
- Support multiple Google Workspace domains via separate connector instances
- Integrate with Cloud Identity for device management queries
