---
title: IQ Quick Start
audience: ["all"]
priority: "high"
---

# IQ Quick Start Guide

Get IQ running in 5 minutes.

## Prerequisites

- **Java 21+** — `java --version`
- **Maven** (wrapper included)
- **4GB RAM** minimum
- **Linux/macOS/Windows** with bash

## Installation

```bash
# Clone the repository
git clone https://github.com/symbol-systems/iq.git
cd iq

# Build everything
./mvnw clean install -DskipITs=true

# Initialize your workspace
mkdir -p ~/.iq-workspace
cd ~/.iq-workspace
```

## Start the API Server

```bash
# From the repo root
./bin/iq

# Server starts at http://localhost:8080
# Dev UI: http://localhost:8080/q/dev/
```

## Send Your First Chat Request

```bash
# In another terminal
./bin/curl_chat
```

**Response:**
```json
{
  "realm": "default",
  "avatar": "system",
  "message": "Hello! I'm ready to help."
}
```

## Try the Agent Endpoint

```bash
./bin/curl_agent
```

## What's Next?

- **[Setup Guide](01-SETUP.md)** — Detailed installation and configuration
- **[Configuration](02-CONFIGURATION.md)** — Realms, repositories, environments
- **[REST APIs](06-REST_APIS.md)** — Full API reference
- **[Connectors](05-CONNECTORS.md)** — Connect to external systems
- **[CLI Reference](09-CLI_REFERENCE.md)** — Command-line tools

## Key Directories

| Directory | Purpose |
|-----------|---------|
| `.iq/repositories/` | RDF graph storage |
| `.iq/vault/` | Encrypted secrets |
| `.iq/jwt/` | JWT keys and tokens |
| `.iq/oauth/` | OAuth provider configs |

## Common Commands

| Command | Purpose |
|---------|---------|
| `./bin/iq` | Start API server (dev mode) |
| `./bin/iq-cli status` | Check system status |
| `./bin/iq-cli list` | List graph triples |
| `./bin/build-image` | Build container image |
| `./mvnw test -DskipITs=true` | Run unit tests |

## Troubleshooting

- **Server won't start?** Check Java version: `java -version` (need 21+)
- **Port 8080 in use?** Kill existing process: `lsof -i :8080`
- **Build fails?** Check `.mvn/extensions.xml` and clear cache: `./mvnw clean`

## Next Steps

- Set up a realm: see [Configuration](02-CONFIGURATION.md)
- Connect to cloud providers: see [Connectors](05-CONNECTORS.md)
- Manage secrets: see [Secrets Management](03-SECRETS.md)
