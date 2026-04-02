# IQ Connector: Confluence

## Purpose

This connector syncs content and metadata from Confluence (pages, spaces, attachments, permissions) into IQ as RDF,
enabling queries over documentation structure, access policies, and content governance.

## Scope / Resources scanned

- Spaces (team/project spaces)
- Pages and page hierarchies
- Attachments and embedded content
- Comments and reactions
- Page permissions and access rules
- Labels and metadata
- User/group permissions

## Architecture

- **Kernel**: Polls Confluence REST APIs, detects changes, and updates the connector graph
- **Model**: Presents the Confluence snapshot as an RDF `Model` for IQ queries
- **Checkpoint**: Tracks last sync timestamp or page update token for incremental sync

## SDK / API

- Uses Confluence REST API (v3 preferred) with HTTP client
- Auth via API token or basic auth (token strongly preferred)
- Recommended: Use a service account with minimal required permissions

## Configuration & Secrets

- **Config**:
  - `confluence.baseUrl`: Base URL of Confluence instance (e.g., `https://company.atlassian.net/wiki`)
  - `confluence.pollInterval`: Polling interval in seconds
  - `confluence.graphIri`: Named graph IRI for state (e.g., `urn:iq:connector:confluence`)
  - `confluence.scanAreas`: Which content to include (spaces, pages, attachments, etc.)
  - `confluence.includeArchived`: Include archived spaces/pages (true/false)

- **Secrets**:
  - `CONFLUENCE_API_TOKEN`: API token for authentication
  - Or: `CONFLUENCE_USERNAME` + `CONFLUENCE_PASSWORD` for basic auth (not recommended)

## Risks & Issues

- **API rate limits**: Confluence API enforces rate limits; implement retries with backoff
- **Data volume**: Large Confluence instances with many pages/spaces can produce massive graphs
- **Permission complexity**: Access rules can be complex; focus on space-level permissions primarily
- **Content sensitivity**: Avoid storing full page content if it contains sensitive data; focus on metadata
- **Latency**: Multi-level page hierarchies may require multiple API calls for complete sync

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:confluence`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  
- Confluence entities:
  - `:space-eng a :ConfluenceSpace ; :name "Engineering" ; :key "ENG"`
  - `:page-123 a :ConfluencePage ; :title "Architecture Guide" ; :space :space-eng`
  - `:user-456 a :ConfluenceUser ; :name "alice" ; :email "alice@example.com"`
  
- Relationships:
  - `:page-123 :inSpace :space-eng`
  - `:page-123 :parentPage :page-100`
  - `:user-456 :canView :space-eng`
  - `:page-123 :hasLabel "documentation"`
  
- Checkpoint (`connector:checkpoint`) for resuming sync

## Sync Modes

- **Read-only**: Sync Confluence into IQ (most common)
- **Write-only**: (Optional) Push IQ authoring changes into Confluence
- **Read-write**: Maintain bidirectional sync

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("confluence")
- Display name and description
- Capabilities (polling, incremental sync)
- Graph IRI
- Required permissions

## Implementation Checklist

- [ ] `I_Connector` implementation exposing Confluence state as `Model`
- [ ] `I_ConnectorKernel` sync loop (polling REST API)
- [ ] `I_Checkpoint` implementation for sync position (last updated timestamp)
- [ ] RDF vocabulary for Confluence entities (spaces, pages, users)
- [ ] Integration with Confluence REST API client
- [ ] Error handling for rate limits and auth failures
- [ ] Unit tests with mocked Confluence API
- [ ] Configuration for instance URL and credentials
- [ ] Connector descriptor registration

## Extension Points

- **Incremental sync**: Use Confluence content change tracking for efficient updates
- **Full-text search**: Index page content for full-text search in IQ
- **Permission analysis**: Map Confluence space/page permissions to governance models
- **Macro expansion**: Parse and expose Confluence macro configurations
- **Comment mining**: Extract key discussion points from page comments
- **Multi-instance**: Support multiple Confluence instances via separate connector instances
