---
title: IQ End-User Manual - Table of Contents
audience: ["all"]
priority: "very-high"
---

# IQ End-User Manual

Complete guide to deploying, configuring, and operating IQ for end users and DevOps teams.

## Quick Links

- **[Quick Start](00-QUICK_START.md)** — Get running in 5 minutes
- **[REST API Reference](06-REST_APIS.md)** — API endpoints and examples
- **[CLI Command Reference](09-CLI_REFERENCE.md)** — All commands explained
- **[Troubleshooting](12-TROUBLESHOOTING.md)** — Common issues and solutions

## Installation & Setup

1. **[Installation and Setup Guide](01-SETUP.md)**
   - System requirements and prerequisites
   - Step-by-step installation from source
   - Directory structure and initial configuration
   - Systemd service setup and Docker deployment
   - Validation checklist

## Configuration

2. **[Configuration Guide](02-CONFIGURATION.md)**
   - Realms (multi-tenant knowledge graphs)
   - Repository types and selection
   - LLM provider configuration
   - Connector enablement
   - Environment-specific settings
   - JWT and OAuth setup

3. **[Secrets Management](03-SECRETS.md)**
   - Vault backends (local, HashiCorp, AWS, Azure, GCP)
   - Credential storage and rotation
   - Encryption at rest
   - Audit logging and compliance

4. **[RDF Repository Guide](04-REPOSITORIES.md)**
   - Native repository (embedded RDF4J)
   - Remote repositories (shared RDF4J server)
   - FedX federated queries
   - GraphDB enterprise
   - Backup, recovery, and migration

## Integration

5. **[Connectors Integration Guide](05-CONNECTORS.md)**
   - AWS, Azure, GCP, DigitalOcean
   - Kubernetes and Docker
   - Slack, GitHub, Jira, Confluence
   - Snowflake, Databricks, JDBC databases
   - Kafka, Redis, and event streaming
   - Configuration and testing

6. **[REST API Reference](06-REST_APIS.md)**
   - Chat endpoint (conversational AI)
   - Agent endpoint (workflow execution)
   - OpenAI-compatible endpoint
   - SPARQL query and update endpoints
   - FedX federation
   - Realm management and health checks
   - Error handling and rate limiting

7. **[Model Context Protocol (MCP)](07-MCP.md)**
   - Setting up MCP server
   - Tool definitions and authorization
   - Multi-protocol support (WebSocket, Stdio, HTTP)
   - Security and access control
   - Integration with Claude and other LLM clients

## Deployment & Operations

8. **[Clustering and High Availability](08-CLUSTERING.md)**
   - 3-node HA setup (minimal production)
   - Regional deployments and scaling to 10+ nodes
   - RDF4J replication and synchronization
   - Load balancing (HAProxy, Nginx, Kubernetes)
   - Backup and disaster recovery procedures
   - Failure scenarios and recovery

9. **[CLI Command Reference](09-CLI_REFERENCE.md)**
   - Realm management commands
   - SPARQL query execution
   - Agent triggering and status
   - Connector management and syncing
   - Server control and monitoring
   - Cluster operations
   - Vault and secrets management
   - Repository operations and data import/export

10. **[Operations and Common Tasks](10-OPERATIONS.md)**
- Daily operational workflows
- Connector synchronization
- Realm lifecycle management
- Data ingestion and ETL
- Agent workflow execution
- Capacity planning and scaling
- Cost optimization

11. **[Monitoring and Observability](11-MONITORING.md)**
- Health checks and status endpoints
- Metrics collection and dashboards
- Logging configuration
- Performance monitoring
- Alerting and notifications
- Request tracing
- Cost tracking and budgets

12. **[Troubleshooting Guide](12-TROUBLESHOOTING.md)**
- Common issues and solutions
- Debugging techniques
- Performance problems
- Connectivity issues
- Data consistency
- Cluster problems
- FAQ

## Quick Reference

### File Structure

```
${IQ_HOME}/
├── repositories/# RDF graph storage
├── vault/  # Encrypted secrets
├── jwt/# JWT keys
├── oauth/  # OAuth configs
├── logs/   # Application logs
├── backups/# Graph backups
├── public/ # Static content
└── runtime/# Runtime state
```

### Key Commands

```bash
# Start server
./bin/iq

# Execute SPARQL
./bin/iq-cli sparql "SELECT ..."

# Trigger agent
./bin/iq-cli agents trigger <agent> --intent <intent>

# Test connector
./bin/iq-cli connectors test <name>

# Cluster status
./bin/iq-cli-server cluster list

# Health check
./bin/iq-cli-server health
```

### Environment Variables

```bash
MY_IQ   # Instance identifier
IQ_HOME # Workspace location
JAVA_OPTS   # JVM settings
OPENAI_API_KEY  # LLM key
AWS_REGION  # Default AWS region
IQ_LOG_LEVEL# DEBUG|INFO|WARN|ERROR
```

### API Base URL

```
http://localhost:8080/api/v1
https://iq.example.com/api/v1  (production)
```

## Key Concepts

### Realm
An isolated knowledge graph with independent agents, connectors, and secrets.

### Repository
RDF4J-based storage for facts, rules, and ontologies. Types: Native, Remote, FedX, GraphDB.

### Connector
Integration bridge to external systems (AWS, K8S, Slack, Snowflake, etc.).

### Agent
Stateful workflow engine for intent-driven automation and decision-making.

### SPARQL
Standard query language for semantic data. Used for both queries (SELECT) and updates (INSERT/DELETE).

### MCP (Model Context Protocol)
Standard protocol exposing IQ tools and knowledge to LLM clients.

### Vault
Secrets storage backend (local, HashiCorp, AWS Secrets Manager, Azure Key Vault, GCP Secret Manager).

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│ User Applications & LLM Clients  │
│  (Claude, GPT-4, etc.)   │
└───────┬──────────────────────────────────────────────┘
│
│ REST API, MCP, WebSocket
│
┌───────▼──────────────────────────────────────────────┐
│IQ API Server (Quarkus)   │
│  ┌──────────────────────────────────────────────┐   │
│  │ Chat | Agents | OpenAI Compat | SPARQL  │   │
│  └──────────────────────────────────────────────┘   │
└──────────┬──────────────────┬────────────────────────┘
   │  │
┌──────▼────────┐  ┌──────▼──────────┐
│  Realm Mgmt   │  │  Connectors │
│  (query planner) │  (cloud, k8s,  │
│  (inference)  │  │   databases)   │
│  (workflows)  │  └─────────────────┘
└──────┬────────┘
   │
┌──────▼──────────────────┐
│  RDF Repository │
│  (facts, rules, │
│   ontologies)   │
│  (Replicable)   │
└─────────────────────────┘
```

## Deployment Models

### Development
- Single node, native repository
- Local secrets vault
- 4GB RAM minimum

### Staging
- 2-3 node cluster
- Remote RDF4J repository
- AWS/Azure secrets

### Production
- 3+ node cluster (HA)
- Replicated RDF4J repository
- Enterprise vault backend
- Load balancer
- Monitoring and alerting

## Best Practices

1. **Secrets**: Never commit credentials; use vault backends
2. **Backups**: Automate daily backups with 30-day retention
3. **Monitoring**: Set up dashboards for key metrics
4. **Testing**: Use separate staging realm before production changes
5. **Scaling**: Start with 3-node cluster for fault tolerance
6. **Documentation**: Record realm purposes and connector configurations
7. **Audit**: Enable logging for compliance and troubleshooting
8. **Updates**: Plan rolling updates to avoid downtime

## Getting Help

- **Issues**: https://github.com/symbol-systems/iq/issues
- **Discussions**: https://github.com/symbol-systems/iq/discussions
- **Slack Community**: (if available)
- **Email**: support@symbol.systems

## Related Documentation

- [Developer Guide](../docs/GUIDE.md) — For extending IQ
- [Architecture Documentation](../docs/IQ.md)
- [API Specifications](../docs/INTERACES.md)
- [Architecture Decisions](../docs/adr/) — Design rationale

## Version Info

This manual documents **IQ v0.91.6+**.

- Latest: https://github.com/symbol-systems/iq/releases
- Development: main branch on GitHub

---

**Last Updated**: January 2024  
**Maintained By**: IQ Core Team  
**License**: See repository for details
