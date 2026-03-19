# IQ Connector: GitHub

## Purpose

This connector brings GitHub organization state into IQ as RDF, focusing on both **resource inventory** and **governance signals**.
It should be able to scan all relevant resources that affect runtime operations and security posture.

## Scope / Resources scanned

The connector should scan and expose:

- **Organization / repository inventory**: repos, teams, members, branches
- **Issues & PRs**: metadata, status, labels, review state
- **Policies & governance**: branch protection rules, required checks, secrets scanning
- **Actions & workflows**: workflow definitions, runs, artifacts
- **Security findings**: dependabot alerts, code scanning findings

## Architecture

- **Kernel**: Polls GitHub APIs or listens to webhooks and updates the connector graph.
- **Model**: Presents GitHub state as an RDF `Model`.
- **Checkpoint**: Tracks last processed event or updated timestamp for incremental sync.

## SDK / API

- Uses GitHub REST API (Octokit, GraphQL) with token-based auth.
- Supports GitHub Apps (JWT + installation tokens) and personal access tokens.

## Configuration & Secrets

- **Config**:
  - `github.org`, `github.pollInterval`, `github.scanRepos`
  - `github.graphIri` (named graph for state)
- **Secrets**:
  - Store GitHub tokens in IQ secret store.
  - Prefer GitHub App tokens for least privilege.

## Risks & Issues

- **API rate limits**: GitHub enforces strict quotas. Use conditional requests (ETag) and backoff.
- **Large orgs**: Scanning many repos can be heavy; support filtering and incremental sync.
- **Security posture**: Missing permissions can result in incomplete scans; surface that clearly.

## State Model

Graph example: `urn:iq:connector:github`.

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- GitHub resources (`:repo a :GitHubRepository`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Sync GitHub state into IQ.
- **Write-only**: Apply changes from IQ to GitHub (e.g., create issues).
- **Read-write**: Bidirectional sync.

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

- Use webhooks (push events) for near-real-time updates.
- Map GitHub security findings into IQ governance and alerting models.
