---
title: IQ Configuration Guide
audience: ["devops", "developer"]
sections: ["realms", "repositories", "environments", "llm", "connectors"]
---

# Configuration Guide

Configure IQ for your environment, tenants, knowledge graphs, and integrations.

## Quick Reference

| Configuration | File | Env Var | Scope |
|---------------|------|---------|-------|
| Realms | `.iq/config.ttl` | - | Global |
| Repositories | `.iq/repositories/*/config.ttl` | `RDF4J_*` | Per-realm |
| Secrets | `.iq/vault/` | `IQ_SECRETS_BACKEND` | Global |
| LLM Providers | `.iq/llm-config.ttl` | `OPENAI_API_KEY`, etc. | Global |
| Connectors | `.iq/connectors/*.ttl` | `CONNECTOR_*_*` | Per-realm |
| OAuth | `.iq/oauth/*.json` | `OAUTH_*_*` | Global |
| JWT | `.iq/jwt/claims.json` | `JWT_SECRET` | Global |

## Realms

A **realm** is an isolated knowledge graph and agent space. Think of it as a tenant, environment, or namespace.

### Create a Realm

Create `.iq/config.ttl`:

```turtle
@prefix iq: <urn:iq:> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

# Realm 1: Production
iq:realm-prod
a iq:Realm ;
iq:name "prod" ;
iq:description "Production environment" ;
iq:repository iq:repo-prod-native ;
iq:llmConfig iq:llm-prod-gpt4 ;
iq:connectors iq:connector-aws-prod, iq:connector-slack-prod ;
iq:secretsBackend "hashicorp" ;
iq:vaultPath "secret/iq/prod" ;
iq:requiredRole "admin", "user:prod" ;
dcterms:created "2024-01-01T00:00:00Z" ;
dcterms:modified "2024-01-15T10:30:00Z" .

# Realm 2: Staging
iq:realm-staging
a iq:Realm ;
iq:name "staging" ;
iq:description "Staging environment for testing" ;
iq:repository iq:repo-staging-native ;
iq:llmConfig iq:llm-staging-gpt35 ;
iq:connectors iq:connector-aws-staging ;
iq:requiredRole "admin", "user:staging" ;
dcterms:created "2024-01-01T00:00:00Z" .

# Realm 3: Multi-tenant Customer A
iq:realm-customer-a
a iq:Realm ;
iq:name "customer-a" ;
iq:description "Dedicated realm for Customer A" ;
iq:repository iq:repo-customer-a ;
iq:llmConfig iq:llm-customer-a-gpt4 ;
iq:requiredRole "admin", "user:customer-a" ;
iq:isolation "strict" ;
dcterms:created "2024-01-20T14:00:00Z" .
```

### Realm Properties

| Property | Type | Required | Notes |
|----------|------|----------|-------|
| `iq:name` | String | Yes | Must be unique; used in URLs |
| `iq:description` | String | No | Display name and docs |
| `iq:repository` | IRI | Yes | Reference to RDF store |
| `iq:llmConfig` | IRI | Yes | Reference to LLM settings |
| `iq:connectors` | IRI[] | No | Available connectors |
| `iq:secretsBackend` | String | No | `local`, `hashicorp`, `aws`, `azure`, `gcp` |
| `iq:requiredRole` | String[] | No | Roles with access |
| `iq:isolation` | String | No | `none`, `standard`, `strict` |
| `iq:agent` | IRI | No | Default avatar/agent |

## Repositories

An RDF **repository** is a graph database that stores facts, rules, and domain knowledge.

### Repository Types

#### 1. Native Repository (Embedded RDF4J)

Best for: Single-node, development, small to medium graphs.

```turtle
@prefix iq: <urn:iq:> .

iq:repo-default
a iq:NativeRepository ;
iq:repositoryID "default" ;
iq:basePath "./repositories/default" ;
iq:persistent true ;
iq:indexed true ;
iq:indexedQueries [
iq:queries (
"SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER(STRLEN(STR(?o)) < 255) }"
)
] .
```

**Configuration:**
- `iq:repositoryID` — Unique identifier
- `iq:basePath` — Filesystem path to store data
- `iq:persistent` — Save to disk (default: true)
- `iq:indexed` — Enable query indexes (default: true)

#### 2. RDF4J Remote Server

Best for: Distributed, shared repository across multiple IQ instances.

```turtle
iq:repo-shared
a iq:RemoteRepository ;
iq:repositoryID "shared-repo" ;
iq:serverURL "http://rdf4j-server.example.com:8080/rdf4j-server" ;
iq:username "iq_user" ;
iq:password "***SECRET***" ;
iq:timeout 30 ;
iq:maxConnections 10 .
```

#### 3. FedX Federated Repository

Best for: Querying across multiple distributed SPARQL endpoints.

```turtle
iq:repo-federated
a iq:FedXRepository ;
iq:repositoryID "federated-queries" ;
iq:members (
iq:endpoint-aws
iq:endpoint-gcp
iq:endpoint-snowflake
) ;
iq:optimizer "dynamic" ;
iq:pruning true .

iq:endpoint-aws
a iq:SPARQLEndpoint ;
iq:url "http://knowledge.aws.example.com/sparql" ;
iq:username "user" ;
iq:password "***" .

iq:endpoint-gcp
a iq:SPARQLEndpoint ;
iq:url "http://knowledge.gcp.example.com/sparql" ;
iq:timeout 60 .

iq:endpoint-snowflake
a iq:SPARQLEndpoint ;
iq:url "https://snowflake.example.com/sparql" ;
iq:username "snowflake_user" ;
iq:password "***" .
```

#### 4. GraphDB Repository

Best for: Enterprise knowledge graphs with semantic reasoning.

```turtle
iq:repo-graphdb
a iq:GraphDBRepository ;
iq:repositoryID "enterprise-kg" ;
iq:serverURL "http://graphdb.example.com:7200" ;
iq:username "admin" ;
iq:password "***" ;
iq:reasoner "owlim-lite" ;
iq:inferenceEnabled true .
```

#### 5. Custom SPARQL Endpoint

Wrap any SPARQL-compliant endpoint:

```turtle
iq:repo-custom
a iq:CustomSPARQLRepository ;
iq:url "http://custom-endpoint.example.com/sparql" ;
iq:queryMethod "POST" ;
iq:updateMethod "POST" ;
iq:headers [
rdf:type rdf:List ;
rdf:first [ iq:header "Authorization: Bearer token" ] ;
rdf:rest rdf:nil
] .
```

### Repository Configuration in Realm

Link repository to realm:

```turtle
iq:realm-prod
iq:repository iq:repo-prod-native ;
iq:replicationServers (
"node1.example.com:8081"
"node2.example.com:8081"
"node3.example.com:8081"
) .
```

## LLM Configuration

Configure which LLM providers and models are available.

### Define LLM Configs

```turtle
@prefix iq: <urn:iq:> .

# GPT-4 for production
iq:llm-prod-gpt4
a iq:LLMConfig ;
iq:provider "openai" ;
iq:model "gpt-4" ;
iq:temperature 0.5 ;
iq:maxTokens 4096 ;
iq:topP 0.95 ;
iq:frequencyPenalty 0.0 ;
iq:presencePenalty 0.0 ;
iq:systemPrompt "You are a helpful knowledge assistant..." ;
iq:costPerMTok 0.03 ;
iq:costPerKTok 0.06 .

# GPT-3.5 for staging (cheaper)
iq:llm-staging-gpt35
a iq:LLMConfig ;
iq:provider "openai" ;
iq:model "gpt-3.5-turbo" ;
iq:temperature 0.7 ;
iq:maxTokens 2048 ;
iq:costPerMTok 0.0005 ;
iq:costPerKTok 0.0015 .

# Groq for fast inference
iq:llm-groq-fast
a iq:LLMConfig ;
iq:provider "groq" ;
iq:model "mixtral-8x7b-32768" ;
iq:temperature 0.3 ;
iq:maxTokens 4096 ;
iq:costPerToken 0.0001 .

# Local ONNX model (via iq-onnx)
iq:llm-local-onnx
a iq:LLMConfig ;
iq:provider "onnx-local" ;
iq:modelPath "./models/mistral-7b.onnx" ;
iq:temperature 0.5 ;
iq:maxTokens 1024 ;
iq:gpuEnabled true ;
iq:costPerToken 0.0 .

# Anthropic Claude
iq:llm-claude
a iq:LLMConfig ;
iq:provider "anthropic" ;
iq:model "claude-3-opus-20240229" ;
iq:temperature 0.7 ;
iq:maxTokens 4096 ;
iq:costPerMTok 0.015 ;
iq:costPerKTok 0.075 .
```

### Override via Environment

```bash
# Override provider endpoint
export OPENAI_BASE_URL="http://localhost:8000"  # For local proxy

# Custom API keys
export OPENAI_API_KEY="sk-..."
export GROQ_API_KEY="gsk_..."
export ANTHROPIC_API_KEY="sk-ant-..."

# Token limits
export LLM_MAX_TOKENS=8192
export LLM_TEMPERATURE=0.5

# Cost tracking
export IQ_ENABLE_TOKEN_COUNTING=true
export IQ_MAX_MONTHLY_COST=10000
```

## Connectors

Enable and configure connector modules for external system access.

### Connector Configuration Template

Each connector section in `.iq/config.ttl`:

```turtle
iq:connector-aws-prod
a iq:Connector ;
iq:name "aws-prod" ;
iq:type "iq-connect-aws" ;
iq:enabled true ;
iq:credentials iq:creds-aws-prod ;
iq:regions ("us-east-1" "eu-west-1") ;
iq:sync [
iq:enabled true ;
iq:schedule "0 * * * *" ;  # Every hour
iq:backoff "exponential" ;
iq:maxRetries 3
] ;
iq:rateLimiting [
iq:requestsPerSecond 100 ;
iq:burst 500
] .

iq:creds-aws-prod
a iq:AWSCredentials ;
iq:accessKeyId "${AWS_ACCESS_KEY_ID}" ;
iq:secretAccessKey "${AWS_SECRET_ACCESS_KEY}" ;
iq:region "us-east-1" .
```

### Environment-Based Configuration

```bash
# AWS Connector
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="***"
export AWS_REGION="us-east-1"

# GitHub Connector
export GITHUB_OWNED_MAX_REPOS=100
export GITHUB_PUBLIC_REPOS=true
export GITHUB_PRIVATE_REPOS=true
export GITHUB_ORG="myorg"

# Slack Connector
export SLACK_XOXB_TOKEN="xoxb-..."
export SLACK_WORKSPACE_ID="T123..."

# Snowflake Connector
export SNOWFLAKE_ACCOUNT="xy12345.us-east-1"
export SNOWFLAKE_USER="iq_user"
export SNOWFLAKE_PASSWORD="***"
export SNOWFLAKE_DATABASE="ANALYTICS"
export SNOWFLAKE_SCHEMA="PUBLIC"

# GCP
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
export GCP_PROJECT_ID="my-project"

# Azure
export AZURE_SUBSCRIPTION_ID="..."
export AZURE_TENANT_ID="..."
export AZURE_CLIENT_ID="..."
export AZURE_CLIENT_SECRET="***"
```

See [Connectors Guide](05-CONNECTORS.md) for full per-connector setup.

## Environments

Organize realms by environment pattern.

### Development Environment

```turtle
iq:realm-dev
a iq:Realm ;
iq:name "dev" ;
iq:environment "development" ;
iq:repository iq:repo-dev-native ;
iq:llmConfig iq:llm-dev-gpt35 ;
iq:skipValidation true ;
iq:logLevel "DEBUG" ;
iq:disablePersistence false ;
iq:cacheEnabled false .
```

### Staging Environment

```turtle
iq:realm-staging
a iq:Realm ;
iq:name "staging" ;
iq:environment "staging" ;
iq:repository iq:repo-staging-remote ;
iq:llmConfig iq:llm-staging-gpt4 ;
iq:skipValidation false ;
iq:logLevel "INFO" ;
iq:cacheEnabled true ;
iq:cacheTTL 3600 .
```

### Production Environment

```turtle
iq:realm-prod
a iq:Realm ;
iq:name "prod" ;
iq:environment "production" ;
iq:repository iq:repo-prod-fedx ;
iq:llmConfig iq:llm-prod-gpt4 ;
iq:replicationServers (
"rdf4j-node1.internal:8080"
"rdf4j-node2.internal:8080"
"rdf4j-node3.internal:8080"
) ;
iq:skipValidation false ;
iq:enforceHttps true ;
iq:logLevel "WARN" ;
iq:cacheEnabled true ;
iq:cacheTTL 7200 ;
iq:circuitBreakerEnabled true ;
iq:maxConcurrentQueries 50 ;
iq:queryTimeout 60 ;
iq:backupSchedule "0 2 * * *" ;
iq:backupRetention 30 .
```

## JWT Configuration

Configure JWT token generation and validation.

### Create `.iq/jwt/claims.json`

```json
{
  "issuer": "iq.symbol.systems",
  "audience": ["iq-api", "iq-mcp"],
  "expirySeconds": 3600,
  "refreshExpirySeconds": 86400,
  "algorithm": "RS256",
  "keyId": "iq-prod-2024",
  "claims": {
"realm": {
  "required": true,
  "type": "string"
},
"user_id": {
  "required": true,
  "type": "string"
},
"roles": {
  "required": false,
  "type": "array"
},
"email": {
  "required": false,
  "type": "string"
},
"organization": {
  "required": false,
  "type": "string"
},
"scopes": {
  "required": false,
  "type": "array",
  "values": ["read", "write", "admin"]
}
  }
}
```

### Request JWT Token

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
"client_id": "iq-cli",
"client_secret": "***",
"realm": "prod",
"user_id": "user@example.com",
"roles": ["admin"]
  }'
```

## Example: Complete Workspace Config

Create `.iq/config.ttl`:

```turtle
@prefix iq: <urn:iq:> .
@prefix dcterms: <http://purl.org/dc/terms/> .

# =====================
# REALMS
# =====================

iq:realm-prod
a iq:Realm ;
iq:name "prod" ;
iq:repository iq:repo-prod ;
iq:llmConfig iq:llm-prod-gpt4 ;
iq:secretsBackend "hashicorp" ;
iq:vaultPath "secret/iq/prod" ;
iq:requiredRole "admin", "user:prod" ;
dcterms:created "2024-01-01T00:00:00Z" .

# =====================
# REPOSITORIES
# =====================

iq:repo-prod
a iq:RemoteRepository ;
iq:repositoryID "prod-repo" ;
iq:serverURL "http://rdf4j-prod:8080/rdf4j-server" ;
iq:username "prod_user" ;
iq:password "${RDF4J_PASSWORD}" .

# =====================
# LLM CONFIGS
# =====================

iq:llm-prod-gpt4
a iq:LLMConfig ;
iq:provider "openai" ;
iq:model "gpt-4" ;
iq:temperature 0.5 ;
iq:maxTokens 4096 .

# =====================
# CONNECTORS
# =====================

iq:connector-aws-prod
a iq:Connector ;
iq:name "aws-prod" ;
iq:type "iq-connect-aws" ;
iq:enabled true ;
iq:regions ("us-east-1") .
```

## Validation

Validate configuration:

```bash
# Check config syntax
./bin/iq-cli config validate

# List loaded realms
./bin/iq-cli realms list

# Show realm details
./bin/iq-cli realms describe prod

# Test LLM access
./bin/iq-cli llm test prod

# Test connector
./bin/iq-cli connectors test aws-prod
```

## Next Steps

1. **[Secrets Management](03-SECRETS.md)** — Secure your credentials
2. **[Repositories](04-REPOSITORIES.md)** — Deep dive into RDF storage options
3. **[Connectors](05-CONNECTORS.md)** — Configure external systems
