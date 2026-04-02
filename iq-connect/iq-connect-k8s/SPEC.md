# IQ Connector: Kubernetes

## Purpose

This connector syncs Kubernetes cluster resources (pods, deployments, services, config maps, RBAC) into IQ as RDF,
enabling queries over workload inventory, access policies, and cluster governance.

## Scope / Resources scanned

- **Workloads**: Pods, Deployments, StatefulSets, DaemonSets, Jobs, CronJobs
- **Configuration**: ConfigMaps, Secrets, service accounts
- **Networking**: Services, Ingress rules, network policies
- **Storage**: PersistentVolumes, PersistentVolumeClaims
- **Access control**: RBAC roles, role bindings, service accounts
- **Cluster infrastructure**: Nodes, namespaces
- **ResourceQuotas and LimitRanges**

## Architecture

- **Kernel**: Polls the Kubernetes API server (or listens to watches) and updates the connector graph
- **Model**: Exposes current cluster state as an RDF `Model`
- **Checkpoint**: Tracks last resource versions or watch bookmarks for incremental sync

## SDK / API

- Uses Kubernetes Java client (fabric8io/kubernetes-client or official client)
- Connection via kubeconfig file or in-cluster service account
- Auth: Certificates, bearer token, or OIDC provider

## Configuration & Secrets

- **Config**:
  - `k8s.kubeconfig`: Path to kubeconfig file (or in-cluster auto-discovery)
  - `k8s.context`: Kubernetes context to use (optional)
  - `k8s.namespace`: Specific namespace to scan (or "*" for all)
  - `k8s.pollInterval`: Polling frequency in seconds
  - `k8s.graphIri`: Named graph IRI (e.g., `urn:iq:connector:k8s`)
  - `k8s.includeLogs`: Include pod log metadata

- **Secrets**:
  - `KUBECONFIG`: Path/content of kubeconfig
  - Or: Automatic service account token (if running in-cluster)

## Risks & Issues

- **API server load**: Large clusters generate significant API traffic; use watches and field selectors
- **RBAC complexity**: Kubernetes RBAC can be complex; simplified models may miss edge cases
- **Secret exposure**: Avoid storing Secret content in RDF; focus on metadata only
- **Cluster volatility**: Pods are transient; state changes constantly
- **Multi-cluster**: Support multiple clusters via separate connector instances

## State Model

Connector state lives in a dedicated graph, e.g., `urn:iq:connector:k8s`.

### Key RDF concepts

- Connector metadata:
  - `connector:lastSyncedAt` (timestamp)
  - `connector:syncStatus` ("healthy", "degraded", "error")
  - `connector:clusterVersion` (Kubernetes version)

- Kubernetes resources:
  - `:pod-123 a :KubernetesPod ; :name "web-server-0" ; :namespace :prod ; :status "Running"`
  - `:deployment-456 a :KubernetesDeployment ; :replicas 3 ; :image "myapp:v1.0"`
  - `:svc-789 a :KubernetesService ; :type "ClusterIP" ; :port 8080`
  - `:node-abc a :KubernetesNode ; :status "Ready" ; :cpuCapacity 4`

- Relationships:
  - `:pod-123 :inNamespace :prod`
  - `:deployment-456 :ownerOf :pod-123`
  - `:svc-789 :selectsPods :pod-123`
  - `:role-def a :KubernetesRole ; :allowsVerb ("get" "list")`

- Checkpoint (`connector:checkpoint`) for watch bookmarks

## Sync Modes

- **Read-only**: Observe cluster state in IQ (most common)
- **Write-only**: (Optional) Apply manifests or updates from IQ
- **Read-write**: Full cluster management + observation

## Registration

Provide a `ConnectorDescriptor` RDF model describing:

- Connector ID ("k8s")
- Capabilities (polling, watch API, bulk queries)
- Graph IRI
- Required cluster access level

## Implementation Checklist

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` sync loop with Kubernetes watch API
- [ ] `I_Checkpoint` implementation for resource versions/bookmarks
- [ ] RDF vocabulary for Kubernetes resource types
- [ ] Kubernetes Java client integration
- [ ] Support for kubeconfig and in-cluster auth
- [ ] RBAC analysis and mapping
- [ ] Error handling for API throttling and disconnects
- [ ] Unit tests with mock Kubernetes API
- [ ] Configuration for cluster access
- [ ] Connector descriptor registration

## Extension Points

- Use Kubernetes watch API for event-driven incremental sync instead of polling
- Add pod metrics integration (via metrics-server)
- Map RBAC to fine-grained access control models
- Support multiple clusters via aggregated views
- Integrate with cluster autoscaler for cost analysis
- Handle CRDs (Custom Resource Definitions) generically
