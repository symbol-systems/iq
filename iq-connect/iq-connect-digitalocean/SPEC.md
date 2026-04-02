# IQ Connector: DigitalOcean

## Purpose

This connector syncs DigitalOcean infrastructure resources (droplets, volumes, spaces, apps, networking, etc.) into IQ as RDF,
enabling infrastructure audit, configuration governance, and resource management queries.

## Scope / Resources scanned

- **Compute**: Droplets, reserved IPs, snapshots, custom images
- **Storage**: Volumes, volume snapshots, Spaces (object storage)
- **Networking**: VPCs, firewalls, load balancers, DNS records, CDN
- **Apps**: App Platform applications and deployments
- **Databases**: Managed databases (PostgreSQL, MySQL, Redis, etc.)
- **Container Registry**: Container images and registries
- **Networking**: Floating IPs, SSH keys,  VPN connections
- **Monitoring**: Monitoring alerts and metrics

## Architecture

- **Kernel**: Polls DigitalOcean APIs and updates connector state
- **Model**: Exposes synced state as an RDF `Model` for query and reasoning
- **Checkpoint**: Stores pagination tokens or last-updated timestamps for resumable sync

## SDK / API

- Uses DigitalOcean API v2 via HTTP client or official Java SDK
- Authentication: Bearer token (personal access token or OAuth token)

## Configuration & Secrets

- **Config**:
  - `digitalocean.pollInterval`: Polling frequency in seconds
  - `digitalocean.graphIri`: Named graph IRI (e.g., `urn:iq:connector:digitalocean`)
  - `digitalocean.scanRegions`: Which regions to scan
  - `digitalocean.scanServices`: Which resource types to include (droplets, volumes, apps, etc.)

- **Secrets**:
  - `DIGITALOCEAN_API_TOKEN`: Personal access token with read permissions

## Risks & Issues

- **API rate limits**: DigitalOcean enforces rate limits (5000 requests/hour); implement backoff
- **Data volume**: Large accounts with many resources require pagination and filtering
- **Cost**: Querying large resource inventories may incur API costs
- **Billing data**: Resource pricing and billing information requires separate API access

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:digitalocean`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")

- DigitalOcean resources:
  - `:droplet-123 a :DigitalOceanDroplet ; :name "web-server-01" ; :status "active" ; :region :sfo3`
  - `:volume-456 a :DigitalOceanVolume ; :name "data-store" ; :size 100`
  - `:app-789 a :DigitalOceanApp ; :name "my-app" ; :tier "professional"`
  - `:vpc-012 a :DigitalOceanVPC ; :name "production-vpc"`

- Relationships:
  - `:droplet-123 :inRegion :sfo3`
  - `:volume-456 :attachedTo :droplet-123`
  - `:droplet-123 :inVPC :vpc-012`

- Checkpoint (`connector:checkpoint`) for resuming incremental sync

## Sync Modes

- **Read-only**: Keep IQ in sync with DigitalOcean state (most common)
- **Write-only**: (Optional) Apply infrastructure changes from IQ to DigitalOcean
- **Read-write**: Two-way sync with conflict handling

## Registration

Provide a `ConnectorDescriptor` RDF model with:

- Connector ID ("digitalocean")
- Capabilities (polling, bulk queries)
- Graph IRI
- Required permissions

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with polling and backoff
- [ ] `I_Checkpoint` implementation
- [ ] RDF vocabulary for DigitalOcean resource types
- [ ] Integration with DigitalOcean API client
- [ ] Pagination support for large resource sets
- [ ] Error handling (rate limits, auth failures)
- [ ] Unit tests with mocked API
- [ ] Configuration provider
- [ ] Connector descriptor registration

## Extension Points

- Use DigitalOcean webhooks for event-driven sync instead of polling
- Implement cost analysis by querying billing API
- Map firewall rules to security policy RDF
- Support multi-team DigitalOcean accounts via connector instances
- Add droplet metrics integration from monitoring API
