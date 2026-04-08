# IQ Connector: Core

## Purpose

`iq-connect-core` provides the foundational abstractions and utilities for building all IQ connectors.
It defines the core lifecycle, state management, and integration patterns that all connectors must implement.

## Scope / Responsibilities

- **I_Connector**: Core interface defining connector identity, state exposure, and lifecycle hooks
- **I_ConnectorKernel**: Execution runtime for sync loops, polling, and error recovery
- **I_Checkpoint**: Mechanism for persisting and restoring sync position (pagination tokens, timestamps)
- **ConnectorRegistry**: Service for discovering and managing available connectors
- **ConnectorModels**: Helper utilities for synchronizing RDF models and state
- **RDF state persistence**: Converting connectors' in-memory models to persistent RDF

## Architecture

- **No external dependencies** beyond RDF4J (`rdf4j-model`, `rdf4j-model-api`) and SLF4J
- **Model-based**: All connector state is exposed as `org.eclipse.rdf4j.model.Model`
- **Checkpoint pattern**: Save/restore snapshots of RDF state to enable incremental sync
- **Kernel abstraction**: Decouples connector lifecycle logic from transport (polling, webhooks, etc.)

## Key Abstractions

### I_Connector

```java
public interface I_Connector {
String getId();
Model getState();
void refresh(Model state);
void close();
}
```

All connectors implement this interface to expose their state graph and respond to refresh events.

### I_ConnectorKernel

```java
public interface I_ConnectorKernel {
void start();
void stop();
ConnectorStatus getStatus();
}
```

Kernels manage the sync lifecycle: polling intervals, error handling, checkpoint recovery, and state updates.

### I_Checkpoint

```java
public interface I_Checkpoint {
Model getState();
void save();
void restore();
}
```

Checkpoints persist sync position (e.g., pagination cursors, last-synced timestamps) to enable resumption without full re-scan.

## Configuration

Core provides minimal configuration:

- **connector.registry.type**: In-memory or persistent (e.g., SPARQL endpoint)
- **connector.state.graph**: Named graph IRI where connector states are stored

Specific connectors extend this with their own config namespace (e.g., `aws.*`, `slack.*`).

## State Model

All connector state lives in a dedicated RDF named graph per connector. Example:

```turtle
<urn:iq:connector:aws> a :ConnectorInstance ;
:id "aws" ;
:lastSyncedAt "2026-04-01T12:00:00Z"^^xsd:dateTime ;
:syncStatus "healthy" ;
:resourceCount 1248 .

# Connector-specific state (e.g., EC2 instances, S3 buckets) also stored in this graph
```

## Sync Modes

All connectors inherit these fundamental modes:

- **Read**: Ingest external state into IQ's knowledge graph
- **Write**: Apply changes from IQ back to external systems
- **Bidirectional**: Keep state in sync both directions

## Registry Pattern

Connectors register themselves via `I_ConnectorRegistry`:

```java
registry.register(new ConnectorDescriptor(
"aws",// ID
"AWS",// Display name
new AWSConnector(), // Implementation
"urn:iq:connector:aws"  // Graph IRI
));
```

Discovery is then possible via SPARQL:

```sparql
SELECT DISTINCT ?connector WHERE {
  ?connector a :ConnectorInstance ;
 :id "aws" .
}
```

## Checkpoint Persistence

Checkpoints are stored as named graphs alongside connector state:

```turtle
<urn:iq:connector:aws:checkpoint> a :ConnectorCheckpoint ;
:forConnector <urn:iq:connector:aws> ;
:savedAt "2026-04-01T12:00:00Z"^^xsd:dateTime ;
:data _:b1 .  # Opaque checkpoint data
```

## SDK / API

**Dependencies for connector builders**:

```xml
<dependency>
<groupId>systems.symbol</groupId>
<artifactId>iq-connect-core</artifactId>
<version>${project.version}</version>
</dependency>
<dependency>
<groupId>org.eclipse.rdf4j</groupId>
<artifactId>rdf4j-model</artifactId>
</dependency>
<dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-api</artifactId>
</dependency>
```

**Helper utilities**:

- `Checkpoints.of(model)`: Create a checkpoint snapshot
- `Checkpoints.restore(model, checkpoint)`: Apply a checkpoint
- `ConnectorModels.merge(baseModel, updateModel)`: Merge connector state graphs

## Risks & Considerations

- **Graph explosion**: Large connector inventories can produce massive RDF graphs; clients must implement filtering/pagination
- **Sync frequency**: Polling too often can exhaust external API quotas; kernel should respect backoff headers
- **Error modes**: Missing permissions or credentials should be surfaced clearly in connector metadata
- **State consistency**: If sync is interrupted, checkpoint recovery must restore a consistent state

## Testing & Extension

The core library provides:

- In-memory `InMemoryConnectorRegistry` for unit testing
- Fixtures for creating mock connector state
- No network calls — all I/O is handled by concrete connector implementations

## Module Layout

```
iq-connect-core/
├── src/main/java/systems/symbol/iq/connect/
│   ├── I_Connector.java
│   ├── I_ConnectorKernel.java
│   ├── I_Checkpoint.java
│   ├── I_ConnectorRegistry.java
│   ├── ConnectorDescriptor.java
│   ├── ConnectorModels.java (utilities)
│   ├── ConnectorStatus.java
│   └── ...
└── src/test/java/systems/symbol/iq/connect/
└── ...
```

## Integration Points

1. **IQ Runtime**: Connectors registered via `I_ConnectorRegistry` are discovered and managed by IQ's connector orchestrator
2. **SPARQL queries**: All connector state is queryable via standard SPARQL; no special query syntax
3. **Secrets**: Connectors access credentials via IQ's secret store (e.g., `VFSPasswordVault`)
4. **Models**: Connector state is any valid RDF `Model` (in-memory or backed by a repository connection)

## Implementation Checklist for Connector Builders

- [ ] Extend/implement `I_Connector` interface
- [ ] Implement `I_ConnectorKernel` for sync loop and lifecycle
- [ ] Implement `I_Checkpoint` for state persistence
- [ ] Define connector-specific configuration keys
- [ ] Create `ConnectorDescriptor` for registration
- [ ] Add unit tests using `InMemoryConnectorRegistry`
- [ ] Document RDF vocabulary used in state model
- [ ] Add error handling and logging via SLF4J

## Extension Points

- **Credential handling**: Connectors integrate with IQ's secret store
- **Monitoring**: Expose connector health/status via standard metrics
- **Bulk operations**: Batch updates via model merge utilities
- **Custom RDF vocabularies**: Connectors define their own ontologies for state representation
