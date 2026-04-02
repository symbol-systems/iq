# IQ Connector: Slack

## Purpose

This connector syncs Slack workspace metadata (channels, users, permissions) into IQ as RDF,
enabling queries over organization structure, access policies, and workspace configuration.

## Scope / Resources scanned

- Channels (public, private, shared channels)
- Users and user groups
- Workspace settings and permissions (SSO, SCIM policies)
- App installations and OAuth scopes
- Member activity metadata (message counts, last activity)

## Architecture

- **Kernel**: Polls Slack Web API and/or listens to Events API and updates connector graph
- **Model**: Exposes Slack state as an RDF `Model` that IQ can query
- **Checkpoint**: Tracks last processed event/timestamp for incremental sync

## SDK / API

- Uses Slack Web API via HTTP client or Slack Java SDK
- Auth via Bot Token stored in IQ secrets
- Preferred: Use Bot Tokens for least privilege; support User Tokens for broader access if needed

## Configuration & Secrets

- **Config**:
  - `slack.workspace`: Workspace ID or workspace URL
  - `slack.pollInterval`: How often to check for changes (in seconds)
  - `slack.graphIri`: Named graph IRI for this workspace's state (e.g., `urn:iq:connector:slack`)
  - `slack.scanAreas`: Which resources to scan (channels, users, apps, etc.)

- **Secrets**:
  - `SLACK_BOT_TOKEN`: OAuth token with permissions for reading workspace state
  - Optional: `SLACK_SIGNING_SECRET` for webhook verification if using Events API

## Risks & Issues

- **Rate limits**: Slack APIs enforce rate limits; implement retries with exponential backoff
- **Data privacy**: Avoid storing full message content; focus on channel/user metadata
- **Permission drift**: Missing scopes may result in incomplete scans; surface in connector status

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:slack`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Slack entities:
  - `:channel123 a :SlackChannel ; :name "general" ; :isPrivate false`
  - `:user456 a :SlackUser ; :name "alice" ; :email "alice@example.com"`
  - `:app789 a :SlackApp ; :name "Jira" ; :installedAt "2026-01-15T10:00:00Z"`
- Relationships:
  - `:user456 :memberOf :channel123`
  - `:app789 :installedInWorkspace <urn:iq:connector:slack>`
- Checkpoint (`connector:checkpoint`) for resuming after interruption

## Sync Modes

- **Read-only**: Sync Slack state into IQ (most common)
- **Write-only**: (Optional) Post messages or manage channels from IQ
- **Read-write**: Full sync with metadata + action capability

## Registration

The connector should publish a `ConnectorDescriptor` RDF document describing:

- connector ID ("slack")
- display name
- capabilities (polling, events)
- graph IRI (`urn:iq:connector:slack`)

## Implementation Checklist

- [ ] `I_Connector` implementation exposing workspace state as `Model`
- [ ] `I_ConnectorKernel` sync loop (polling Web API)
- [ ] `I_Checkpoint` implementation for sync position (cursor tokens, timestamps)
- [ ] RDF vocabulary for Slack entities (channels, users, apps)
- [ ] Error handling and connector status reporting (rate limits, auth failures)
- [ ] Unit tests with mocked Slack API
- [ ] Configuration provider for Slack-specific settings
- [ ] Connector descriptor registration in discovery graph

## Extension Points

- Support Slack Events API (webhooks) for near-real-time sync instead of polling
- Add Rich Presence tracking (active status)
- Map security policies (SSO settings, IP restrictions) into governance RDF
- Support multiple Slack workspaces via separate connector instances
- Implement incremental sync using channel/user change tokens
