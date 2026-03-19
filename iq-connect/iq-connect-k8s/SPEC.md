# IQ Connector: Kubernetes

## Purpose

This connector syncs Kubernetes cluster resources (pods, deployments, services) into IQ as RDF.

## Architecture

- **Kernel**: Polls the Kubernetes API server (or listens to watches) and updates the connector graph.
- **Model**: Exposes current cluster state as an RDF `Model`.
- **Checkpoint**: Tracks last resource versions or watch bookmarks.

## Integration Points

- Kubernetes API (client-go-style) using kubeconfig / in-cluster config
- IQ RDF repository via `Model`
- Connector registry for discovery

## State Model

Graph example: `urn:iq:connector:k8s`.

### RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Kubernetes resources (`:pod123 a :K8sPod`)
- Checkpoint (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Observe cluster state in IQ.
- **Write-only**: (Optional) Apply changes to cluster resources.
- **Read-write**: Full control + sync.

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- connector ID and name
- capabilities (polling, watch)
- graph IRI

## Implementation checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` and sync loop
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state
- [ ] Registry descriptor

## Extension ideas

- Support Kubernetes watch API for incremental updates.
- Add support for multiple clusters via separate connector instances.
