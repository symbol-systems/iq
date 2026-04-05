---
title: IQ Installation and Setup
audience: ["devops", "developer"]
sections: ["installation", "initialization", "verification"]
---

# Installation and Setup Guide

Complete setup instructions for IQ deployment environments.

## System Requirements

### Minimum

- **Java**: 21 or later
- **Memory**: 4GB RAM
- **Disk**: 20GB free space
- **CPU**: 2 cores
- **OS**: Linux, macOS, or Windows (with WSL2)

### Recommended (Production)

- **Java**: 21 LTS
- **Memory**: 16GB+ RAM
- **Disk**: 100GB+ (SSD recommended)
- **CPU**: 8+ cores
- **Network**: 1Gbps+
- **OS**: Ubuntu 22.04 LTS or RHEL 8+

## Installation Steps

### 1. Prepare Environment

```bash
# Set home directory
export IQ_HOME="${HOME}/.iq"
mkdir -p "${IQ_HOME}"

# Set instance ID (identifies this IQ instance)
export MY_IQ="prod-01"

# Optional: Set data directory separately
export IQ_DATA_DIR="/data/iq-prod"
mkdir -p "${IQ_DATA_DIR}"
```

### 2. Clone Repository

```bash
git clone https://github.com/symbol-systems/iq.git
cd iq

# Verify Git LFS (for large files)
git lfs version
```

### 3. Build from Source

```bash
# Full clean build (all modules)
./mvnw clean install

# Fast build (skip integration tests for dev)
./mvnw clean install -DskipITs=true

# Build specific module
./mvnw -pl iq-apis -am clean install
```

**Build outputs:**
- `iq-apis/target/iq-apis-*.jar` — API server JAR
- `iq-cli/target/iq-cli-*.jar` — CLI tool JAR
- `iq-mcp/target/iq-mcp-*.jar` — MCP server JAR

### 4. Initialize Workspace

```bash
# Set writable location for data
cd "${IQ_HOME}"

# Create directory structure
mkdir -p repositories/{default,tenants}
mkdir -p vault
mkdir -p jwt/keys
mkdir -p oauth
mkdir -p logs
mkdir -p backups
mkdir -p lake/ingestion

# Copy boot config (optional)
# cp /path/to/iq/iq-apis/.iq/repositories/*.ttl ./repositories/
```

### 5. Start the Server

```bash
# Development mode (live reload)
cd /path/to/iq
./bin/iq

# Production mode (native image, if built)
./bin/iq-prod

# Background (systemd or supervisor)
nohup ./bin/iq > "${IQ_HOME}/logs/server.log" 2>&1 &
```

**Expected startup output:**
```
[io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed
[io.quarkus.runtime.Application] started in 2.345s
[io.quarkus.runtime.Application] Listening on: http://0.0.0.0:8080
```

### 6. Verify Installation

```bash
# Health check
curl http://localhost:8080/health

# Dev UI
open http://localhost:8080/q/dev/

# Check realms
./bin/iq-cli status
```

## Configuration Files

### Directory Layout

```
${IQ_HOME}/
├── repositories/  # RDF graph storage
│   ├── default/  # Default repository
│   │   ├── contexts.dat
│   │   ├── triples-*.dat
│   │   └── values.*
│   └── tenants/  # Per-tenant repos
├── vault/ # Encrypted secrets
│   ├── master.key# Encryption key
│   └── secrets.enc   # Secrets store
├── jwt/   # JWT configuration
│   ├── keys/
│   │   ├── iq-keys.pem
│   │   └── iq-keys.pub
│   └── claims.json   # Claim mappings
├── oauth/ # OAuth providers
│   ├── google.json
│   ├── github.json
│   └── azure.json
├── lake/  # Data ingestion
│   ├── ingestion/# Source files
│   └── catalogs/ # Schema definitions
├── logs/  # Application logs
└── backups/   # Graph backups
```

### Key Configuration Files

Create `.iq/config.ttl` in your workspace:

```turtle
@prefix iq: <urn:iq:> .
@prefix dcterms: <http://purl.org/dc/terms/> .

# Default realm
iq:realm-default
a iq:Realm ;
iq:name "default" ;
iq:repository iq:repo-default ;
iq:llmConfig iq:llm-gpt4 ;
iq:description "Default working realm" ;
dcterms:created "2024-01-01T00:00:00Z" .

# RDF4J Native Repository
iq:repo-default
a iq:NativeRepository ;
iq:repositoryID "default" ;
iq:basePath "./repositories/default" ;
iq:persistent true ;
iq:indexed true .

# LLM Configuration
iq:llm-gpt4
a iq:LLMConfig ;
iq:provider "openai" ;
iq:model "gpt-4" ;
iq:temperature 0.7 ;
iq:maxTokens 2048 .
```

## Environment Variables

### Core Settings

```bash
# Java options
export JAVA_OPTS="-Xmx8g -XX:+UseG1GC"

# IQ instance identifier
export MY_IQ="prod-01"

# Workspace location
export IQ_HOME="/opt/iq/data"

# Server configuration
export IQ_HTTP_PORT=8080
export IQ_HTTP_HOST="0.0.0.0"
```

### LLM Configuration

```bash
# OpenAI
export OPENAI_API_KEY="sk-..."
export OPENAI_ORG_ID="org-..."

# Groq
export GROQ_API_KEY="gsk_..."

# Anthropic
export ANTHROPIC_API_KEY="sk-ant-..."
```

### Database and Storage

```bash
# RDF4J remote repository
export RDF4J_SERVER="http://localhost:8080/rdf4j-server"
export RDF4J_REPO="prod-repo"

# PostgreSQL backend (via JDBC connector)
export JDBC_URL="jdbc:postgresql://pg.example.com/iq_db"
export JDBC_USER="iq_user"
export JDBC_PASSWORD="***"
```

### Cluster Configuration

```bash
# Cluster mode
export IQ_CLUSTER_ENABLE=true
export IQ_CLUSTER_PEERS="node1:8081,node2:8081,node3:8081"
export IQ_CLUSTER_NODE_ID="node1"
```

## Validation Checklist

- [ ] Java 21+ installed and in PATH
- [ ] Maven builds successfully: `./mvnw clean package -DskipTests`
- [ ] `IQ_HOME` directory exists and is writable
- [ ] Server starts without errors: `./bin/iq`
- [ ] Health endpoint responds: `curl http://localhost:8080/health`
- [ ] Dev UI accessible: `http://localhost:8080/q/dev/`
- [ ] Default realm available: `./bin/iq-cli status`
- [ ] CLI tests pass: `./mvnw -pl iq-cli -am test`

## Upgrade and Maintenance

### Backup Before Upgrades

```bash
# Backup entire workspace
tar -czf "${IQ_HOME}/backups/backup-$(date +%s).tar.gz" \
  "${IQ_HOME}/repositories" \
  "${IQ_HOME}/vault"
```

### Rolling Update (Multi-Node)

1. Stop node N: `./bin/iq-cli-server server api stop`
2. Update binaries
3. Start node N: `./bin/iq-cli-server server api start`
4. Wait for sync: `./bin/iq-cli-server server cluster list`
5. Repeat for next node

## Troubleshooting

### Build Issues

| Error | Solution |
|-------|----------|
| `Cannot find Maven home` | Ensure Maven wrapper: `chmod +x mvnw` |
| `OutOfMemoryError` | Increase heap: `export MAVEN_OPTS="-Xmx4g"` |
| `Git LFS not found` | Install: `brew install git-lfs && git lfs install` |

### Startup Issues

| Error | Solution |
|-------|----------|
| `Port already in use` | Kill process: `lsof -ti:8080 \| xargs kill -9` |
| `Cannot create directory` | Check permissions: `chmod -R 755 ${IQ_HOME}` |
| `Repository initialization failed` | Remove `.dat` files and restart |

### Runtime Issues

| Error | Solution |
|-------|----------|
| `Realm not found` | Check `.iq/config.ttl` and restart |
| `OutOfMemory during query` | Increase heap or limit query results |
| `Connection refused` | Ensure server is running and listening |

## Systemd Service (Linux)

Create `/etc/systemd/system/iq.service`:

```ini
[Unit]
Description=IQ Knowledge Engine
After=network.target

[Service]
Type=simple
User=iq
WorkingDirectory=/opt/iq
Environment="IQ_HOME=/opt/iq/data"
Environment="JAVA_OPTS=-Xmx16g"
ExecStart=/opt/iq/bin/iq
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable iq
sudo systemctl start iq
sudo systemctl status iq
```

## Docker Deployment

```bash
# Build container image
./bin/build-image

# Run container
docker run -it \
  -p 8080:8080 \
  -e IQ_HOME=/data \
  -v iq-data:/data \
  iq:latest

# With volume mount
docker run -it \
  -p 8080:8080 \
  -v /opt/iq/data:/data \
  iq:latest
```

## Next Steps

1. **[Configure Realms](02-CONFIGURATION.md)** — Set up tenants and environments
2. **[Manage Secrets](03-SECRETS.md)** — Configure vault backends
3. **[Set up Connectors](05-CONNECTORS.md)** — Connect to external systems
4. **[Deploy for Production](08-CLUSTERING.md)** — Multi-node clustering
