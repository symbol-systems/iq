# IQ Connector: Docker

## Purpose

This connector syncs Docker engine and container state into IQ as RDF, enabling queries against running containers, images, networks, and volumes
for runtime inventory, container governance, and infrastructure compliance.

## Scope / Resources scanned

- **Containers**: Running and stopped containers with configuration and state
- **Images**: Local and registry images with metadata
- **Networks**: Custom networks and connections
- **Volumes**: Local and named volumes
- **Services**: Docker Compose/Swarm services (if applicable)
- **Registry configuration**: Connected registries
- **Container logs**: Metadata and log configuration

## Architecture

- **Kernel**: Polls the Docker Engine API (or listens to events) and updates the connector graph
- **Model**: Exposes Docker runtime state as an RDF `Model`
- **Checkpoint**: Tracks last event timestamp or inspected resource state

## SDK / API

- Uses Docker Engine API via Docker Java SDK or HTTP client
- Connection: Unix socket (recommended), TCP socket, or SSH
- Auth: TLS certificates or API token

## Configuration & Secrets

- **Config**:
  - `docker.socketPath`: Path to Docker socket (default: `/var/run/docker.sock`)
  - `docker.host`: Docker daemon hostname/IP (for remote connections)
  - `docker.port`: Docker daemon port (default: 2375 for unencrypted, 2376 for TLS)
  - `docker.pollInterval`: Polling frequency in seconds
  - `docker.graphIri`: Named graph IRI (e.g., `urn:iq:connector:docker`)
  - `docker.includeLogs`: Whether to include log metadata

- **Secrets**:
  - `DOCKER_CERT_PATH`: Path to TLS certificates (for remote connections)
  - `DOCKER_TLS_VERIFY`: Enable/disable TLS verification

## Risks & Issues

- **Socket permissions**: Docker socket access requires elevated privileges
- **Data volume**: Large numbers of containers/images can produce sizable graphs
- **Container volatility**: Containers are transient; state changes frequently
- **Log volume**: Including container logs can cause storage issues
- **Remote API security**: Exposing Docker daemon over network requires strong auth

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:docker`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  - `connector:engineVersion` (Docker version)

- Docker resources:
  - `:container-abc123 a :DockerContainer ; :name "web-server" ; :status "running" ; :image :image-xyz`
  - `:image-xyz a :DockerImage ; :repoTags ("myapp:latest" "myapp:v1.0") ; :size 250000000`
  - `:volume-data a :DockerVolume ; :name "app-data" ; :driver "local"`
  - `:network-prod a :DockerNetwork ; :name "production" ; :driver "bridge"`

- Relationships:
  - `:container-abc123 :usesImage :image-xyz`
  - `:container-abc123 :mountsVolume :volume-data`
  - `:container-abc123 :connectedToNetwork :network-prod`
  - `:image-xyz :parentImage :base-ubuntu`

- Checkpoint (`connector:checkpoint`) for event tracking

## Sync Modes

- **Read-only**: Observe Docker state in IQ (most common)
- **Write-only**: (Optional) Issue Docker actions (start/stop/remove) from IQ
- **Read-write**: Full round-trip control + state monitoring

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("docker")
- Capabilities (polling, events)
- Graph IRI
- Required socket/network access

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with exponential backoff
- [ ] `I_Checkpoint` implementation for event timestamps
- [ ] RDF vocabulary for Docker resource types
- [ ] Docker Java SDK integration
- [ ] Support for local and remote Docker connections
- [ ] Error handling for socket/network failures
- [ ] Unit tests with mock Docker API
- [ ] Configuration for socket paths and TLS
- [ ] Connector descriptor registration

## Extension Points

- Use Docker events API (via WebSocket) for near-real-time sync instead of polling
- Add container metrics integration (CPU, memory, network)
- Map Docker Compose configurations to RDF service models
- Integrate with container registries for image provenance
- Support Docker Swarm mode for clustering
