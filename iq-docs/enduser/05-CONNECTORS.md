---
title: Connectors and External System Integration
audience: ["devops", "developer", "architect"]
sections: ["aws", "cloud", "saas", "data", "databases", "messaging", "kubernetes"]
---

# Connectors Integration Guide

Connect IQ to 20+ external systems: AWS, Azure, GCP, Kubernetes, Slack, GitHub, Jira, Snowflake, and more.

## Quick Reference

| Connector | Purpose | Read | Write | Sync | Setup Effort |
|-----------|---------|------|-------|------|-------------|
| **iq-connect-aws** | AWS services | ✓ | ✓ | ✓ | Medium |
| **iq-connect-azure** | Azure resources | ✓ | ✓ | ✓ | Medium |
| **iq-connect-gcp** | Google Cloud | ✓ | ✓ | ✓ | Medium |
| **iq-connect-kubernetes** | K8S clusters | ✓ | ✓ | ✓ | Low |
| **iq-connect-github** | GitHub repos/issues | ✓ | ✓ | ✓ | Low |
| **iq-connect-gitlab** | GitLab projects | ✓ | ✓ | ✓ | Low |
| **iq-connect-slack** | Slack workspaces | ✓ | ✓ | ✓ | Low |
| **iq-connect-jira** | Jira issues/projects | ✓ | ✓ | ✓ | Low |
| **iq-connect-confluence** | Confluence pages | ✓ | ✓ | ✓ | Low |
| **iq-connect-jdbc** | SQL databases | ✓ | ✓ | ✓ | Medium |
| **iq-connect-snowflake** | Snowflake warehouse | ✓ | ✓ | ✓ | Low |
| **iq-connect-databricks** | Databricks lakehouse | ✓ | ✓ | ✓ | Low |
| **iq-connect-parquet** | Parquet files | ✓ | ✓ | - | Low |
| **iq-connect-kafka** | Kafka topics | ✓ | ✓ | ✓ | Medium |
| **iq-connect-redis** | Redis cache | ✓ | ✓ | ✓ | Low |
| **iq-connect-docker** | Docker daemon | ✓ | ✓ | ✓ | Low |
| **iq-connect-digitalocean** | DigitalOcean | ✓ | ✓ | ✓ | Medium |
| **iq-connect-salesforce** | Salesforce CRM | ✓ | ✓ | ✓ | Medium |
| **iq-connect-stripe** | Stripe payments | ✓ | - | ✓ | Low |
| **iq-connect-datadog** | Datadog monitoring | ✓ | - | ✓ | Low |
| **iq-connect-office-365** | Microsoft 365 | ✓ | ✓ | ✓ | Medium |
| **iq-connect-google-apps** | Google Workspace | ✓ | ✓ | ✓ | Medium |

## Cloud Platforms

### AWS Connector

**What it connects:** EC2, S3, RDS, Lambda, IAM, CloudWatch, SNS, SQS, DynamoDB, and 100+ AWS services.

**Setup:**

```bash
# 1. Create IAM user or role with programmatic access
aws iam create-user --user-name iq-connector

# 2. Attach policy (example: read-only)
aws iam attach-user-policy \
  --user-name iq-connector \
  --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# 3. Create access keys
aws iam create-access-key --user-name iq-connector

# 4. Store credentials in vault
vault kv put secret/iq/prod/aws \
  access_key_id="AKIA..." \
  secret_access_key="***" \
  region="us-east-1"
```

**Configure in `.iq/config.ttl`:**

```turtle
@prefix iq: <urn:iq:> .

iq:connector-aws-prod
a iq:Connector ;
iq:name "aws-prod" ;
iq:type "iq-connect-aws" ;
iq:enabled true ;
iq:realm iq:realm-prod ;
iq:credentials [
iq:accessKeyId "${secret:aws/access_key_id}" ;
iq:secretAccessKey "${secret:aws/secret_access_key}" ;
iq:region "us-east-1" ;
iq:assumeRole "arn:aws:iam::123456789012:role/IQConnectorRole"
] ;
iq:services (
"ec2"   # Compute
"s3"# Storage
"rds"   # Database
"dynamodb"  # NoSQL
"lambda"# Functions
"iam"   # Identity
) ;
iq:sync [
iq:enabled true ;
iq:schedule "0 * * * *" ;  # Hourly
iq:backoff "exponential" ;
iq:maxRetries 3
] ;
iq:rateLimiting [
iq:requestsPerSecond 50 ;
iq:burst 500
] .
```

**Environmental Setup (Alternative to vault):**

```bash
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="***"
export AWS_REGION="us-east-1"
export AWS_ROLE_ARN="arn:aws:iam::123456789012:role/IQConnectorRole"
```

**Example Queries:**

```sparql
# List all EC2 instances
PREFIX aws: <urn:aws:> 
SELECT DISTINCT ?instanceId ?state ?type WHERE {
  ?instance a aws:EC2Instance ;
aws:instanceId ?instanceId ;
aws:state ?state ;
aws:instanceType ?type .
}

# Find all S3 buckets
SELECT DISTINCT ?bucketName ?region WHERE {
  ?bucket a aws:S3Bucket ;
aws:bucketName ?bucketName ;
aws:region ?region .
}

# List RDS databases
SELECT DISTINCT ?dbName ?engine ?status WHERE {
  ?db a aws:RDSDatabase ;
aws:dbInstanceIdentifier ?dbName ;
aws:engine ?engine ;
aws:dbInstanceStatus ?status .
}
```

### Azure Connector

**What it connects:** VMs, App Service, AKS, Cosmos DB, Storage Accounts, Key Vault, Application Insights.

**Setup:**

```bash
# 1. Create service principal
az ad sp create-for-rbac \
  --name iq-connector \
  --role Contributor \
  --scopes /subscriptions/YOUR_SUBSCRIPTION_ID

# 2. Store credentials
vault kv put secret/iq/prod/azure \
  tenant_id="..." \
  client_id="..." \
  client_secret="***" \
  subscription_id="..."
```

**Configuration:**

```turtle
iq:connector-azure-prod
a iq:Connector ;
iq:name "azure-prod" ;
iq:type "iq-connect-azure" ;
iq:enabled true ;
iq:credentials [
iq:tenantId "${secret:azure/tenant_id}" ;
iq:clientId "${secret:azure/client_id}" ;
iq:clientSecret "${secret:azure/client_secret}" ;
iq:subscriptionId "${secret:azure/subscription_id}"
] ;
iq:services (
"compute"
"storage"
"databases"
"kubernetes"
) ;
iq:sync [
iq:enabled true ;
iq:schedule "0 * * * *"
] .
```

### GCP Connector

**What it connects:** Compute Engine, Cloud Storage, BigQuery, Cloud SQL, GKE, Kubernetes Engine.

**Setup:**

```bash
# 1. Create service account
gcloud iam service-accounts create iq-connector \
  --display-name="IQ Connector"

# 2. Grant roles
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:iq-connector@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/viewer"

# 3. Create key
gcloud iam service-accounts keys create ~/iq-sa.json \
  --iam-account=iq-connector@PROJECT_ID.iam.gserviceaccount.com

# 4. Store credential
vault kv put secret/iq/prod/gcp \
  service_account_json="@~/iq-sa.json" \
  project_id="my-project"
```

**Configuration:**

```turtle
iq:connector-gcp-prod
a iq:Connector ;
iq:name "gcp-prod" ;
iq:type "iq-connect-gcp" ;
iq:enabled true ;
iq:credentials [
iq:serviceAccountJson "${secret:gcp/service_account_json}" ;
iq:projectId "${secret:gcp/project_id}"
] ;
iq:services (
"compute"
"storage"
"bigquery"
"kubernetes"
) .
```

## Infrastructure & Kubernetes

### Kubernetes Connector

**What it connects:** Pods, Deployments, Services, Ingresses, ConfigMaps, Secrets, PersistentVolumes.

**Setup:**

```bash
# 1. Create service account for IQ
kubectl create serviceaccount iq-connector -n default

# 2. Create role
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: iq-connector
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "statefulsets"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["networking.k8s.io"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch"]
EOF

# 3. Bind role
kubectl create clusterrolebinding iq-connector \
  --clusterrole=iq-connector \
  --serviceaccount=default:iq-connector

# 4. Get API token
kubectl create token iq-connector

# 5. Store
vault kv put secret/iq/prod/k8s \
  api_server="https://kubernetes.example.com" \
  api_token="eyJ..." \
  ca_cert="@ca.crt"
```

**Configuration:**

```turtle
iq:connector-k8s-prod
a iq:Connector ;
iq:name "k8s-prod" ;
iq:type "iq-connect-k8s" ;
iq:enabled true ;
iq:credentials [
iq:apiServer "${secret:k8s/api_server}" ;
iq:apiToken "${secret:k8s/api_token}" ;
iq:caCertificate "${secret:k8s/ca_cert}" ;
iq:namespace "default"
] ;
iq:sync [
iq:enabled true ;
iq:schedule "*/5 * * * *"  # Every 5 minutes
] .
```

**Example Queries:**

```sparql
# List all pods in cluster
PREFIX k8s: <urn:k8s:>
SELECT DISTINCT ?podName ?namespace ?phase WHERE {
  ?pod a k8s:Pod ;
k8s:name ?podName ;
k8s:namespace ?namespace ;
k8s:phase ?phase .
}

# Find failed deployments
SELECT DISTINCT ?deploymentName ?replicas ?readyReplicas WHERE {
  ?dep a k8s:Deployment ;
k8s:name ?deploymentName ;
k8s:replicas ?replicas ;
k8s:readyReplicas ?readyReplicas .
  FILTER (?readyReplicas < ?replicas)
}
```

### Docker Connector

**What it connects:** Docker daemon, containers, images, networks, volumes.

**Setup:**

```bash
# 1. Expose Docker daemon (or use socket)
export DOCKER_HOST="unix:///var/run/docker.sock"

# 2. Configure socket mount in vault or env
vault kv put secret/iq/prod/docker \
  sock_path="/var/run/docker.sock" \
  api_version="1.45"
```

**Configuration:**

```turtle
iq:connector-docker-local
a iq:Connector ;
iq:name "docker-local" ;
iq:type "iq-connect-docker" ;
iq:enabled true ;
iq:credentials [
iq:sockPath "/var/run/docker.sock" ;
iq:apiVersion "1.45"
] ;
iq:sync [
iq:enabled true ;
iq:schedule "*/10 * * * *"
] .
```

## SaaS and Communication

### Slack Connector

**What it connects:** Workspaces, channels, messages, users, files.

**Setup:**

```bash
# 1. Create Slack App at https://api.slack.com/apps
# 2. Add permissions:
#- channels:read
#- messages:read
#- files:read
#- users:read

# 3. Generate bot token
# Token starts with xoxb-

vault kv put secret/iq/prod/slack \
  bot_token="xoxb-..." \
  app_id="A..." \
  signing_secret="***"
```

**Configuration:**

```turtle
iq:connector-slack-prod
a iq:Connector ;
iq:name "slack-prod" ;
iq:type "iq-connect-slack" ;
iq:enabled true ;
iq:credentials [
iq:botToken "${secret:slack/bot_token}" ;
iq:appId "${secret:slack/app_id}" ;
iq:signingSecret "${secret:slack/signing_secret}"
] ;
iq:channels (
"general"
"engineering"
"incidents"
) ;
iq:sync [
iq:enabled true ;
iq:schedule "*/5 * * * *"
] .
```

**Example Queries:**

```sparql
# List channels
PREFIX slack: <urn:slack:>
SELECT DISTINCT ?channelName ?members WHERE {
  ?ch a slack:Channel ;
slack:name ?channelName ;
slack:memberCount ?members .
}

# Find messages in #engineering
SELECT DISTINCT ?author ?text ?timestamp WHERE {
  ?msg a slack:Message ;
slack:channel "engineering" ;
slack:user ?author ;
slack:text ?text ;
slack:timestamp ?timestamp .
} LIMIT 100
```

### GitHub Connector

**What it connects:** Repositories, issues, pull requests, commits, workflows, releases.

**Setup:**

```bash
# 1. Create GitHub personal access token
# https://github.com/settings/tokens/new
# Scopes: repo, read:org, read:user

vault kv put secret/iq/prod/github \
  token="ghp_..." \
  org="myorg"
```

**Configuration:**

```turtle
iq:connector-github-prod
a iq:Connector ;
iq:name "github-prod" ;
iq:type "iq-connect-github" ;
iq:enabled true ;
iq:credentials [
iq:personalAccessToken "${secret:github/token}" ;
iq:organization "${secret:github/org}"
] ;
iq:repositories (
"symbol-systems/iq"
"symbol-systems/docs"
) ;
iq:sync [
iq:enabled true ;
iq:schedule "0 */6 * * *"  # Every 6 hours
] .
```

### Jira Connector

**What it connects:** Projects, issues, custom fields, workflows, transitions.

**Setup:**

```bash
# 1. Create API token at https://id.atlassian.com/manage-profile/security/api-tokens
vault kv put secret/iq/prod/jira \
  api_url="https://mycompany.atlassian.net" \
  username="iq@example.com" \
  api_token="***"
```

**Configuration:**

```turtle
iq:connector-jira-prod
a iq:Connector ;
iq:name "jira-prod" ;
iq:type "iq-connect-jira" ;
iq:enabled true ;
iq:credentials [
iq:apiUrl "${secret:jira/api_url}" ;
iq:username "${secret:jira/username}" ;
iq:apiToken "${secret:jira/api_token}"
] ;
iq:projects (
"ENG"
"OPS"
"INFRA"
) ;
iq:sync [
iq:enabled true ;
iq:schedule "0 */4 * * *"
] .
```

## Data & Warehouses

### Snowflake Connector

**What it connects:** Databases, schemas, tables, views, materialized views.

**Setup:**

```bash
# 1. Create role and user in Snowflake
CREATE ROLE iq_role;
CREATE USER iq_user PASSWORD='***' DEFAULT_ROLE=iq_role;
GRANT USAGE ON DATABASE analytics TO ROLE iq_role;
GRANT USAGE ON SCHEMA analytics.public TO ROLE iq_role;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics.public TO ROLE iq_role;

vault kv put secret/iq/prod/snowflake \
  account="xy12345.us-east-1" \
  user="iq_user" \
  password="***" \
  warehouse="compute" \
  database="analytics"
```

**Configuration:**

```turtle
iq:connector-snowflake-prod
a iq:Connector ;
iq:name "snowflake-prod" ;
iq:type "iq-connect-snowflake" ;
iq:enabled true ;
iq:credentials [
iq:account "${secret:snowflake/account}" ;
iq:user "${secret:snowflake/user}" ;
iq:password "${secret:snowflake/password}" ;
iq:warehouse "${secret:snowflake/warehouse}" ;
iq:database "${secret:snowflake/database}"
] ;
iq:sync [
iq:enabled true ;
iq:schedule "0 * * * *"
] .
```

### JDBC Connector (SQL Databases)

**What it connects:** PostgreSQL, MySQL, Oracle, SQL Server, and any JDBC-compatible database.

**Setup:**

```bash
vault kv put secret/iq/prod/postgres \
  jdbc_url="jdbc:postgresql://pg.example.com:5432/analytics" \
  username="iq_user" \
  password="***" \
  driver="org.postgresql.Driver"
```

**Configuration:**

```turtle
iq:connector-postgres-prod
a iq:Connector ;
iq:name "postgres-prod" ;
iq:type "iq-connect-jdbc" ;
iq:enabled true ;
iq:credentials [
iq:jdbcUrl "${secret:postgres/jdbc_url}" ;
iq:username "${secret:postgres/username}" ;
iq:password "${secret:postgres/password}" ;
iq:driverClassName "${secret:postgres/driver}"
] ;
iq:connectionPool [
iq:poolSize 10 ;
iq:maxWaitMillis 30000
] ;
iq:sync [
iq:enabled true ;
iq:schedule "0 */6 * * *"
] .
```

## Messaging & Events

### Kafka Connector

**What it connects:** Kafka brokers, topics, consumer groups, messages.

**Setup:**

```bash
vault kv put secret/iq/prod/kafka \
  bootstrap_servers="kafka-0:9092,kafka-1:9092,kafka-2:9092" \
  consumer_group="iq-consumer-prod" \
  security_protocol="SASL_SSL" \
  sasl_username="iq_user" \
  sasl_password="***"
```

**Configuration:**

```turtle
iq:connector-kafka-prod
a iq:Connector ;
iq:name "kafka-prod" ;
iq:type "iq-connect-kafka" ;
iq:enabled true ;
iq:credentials [
iq:bootstrapServers "${secret:kafka/bootstrap_servers}" ;
iq:consumerGroup "${secret:kafka/consumer_group}" ;
iq:securityProtocol "${secret:kafka/security_protocol}" ;
iq:saslUsername "${secret:kafka/sasl_username}" ;
iq:saslPassword "${secret:kafka/sasl_password}"
] ;
iq:topics (
"events.domain"
"events.compliance"
"events.audit"
) ;
iq:sync [
iq:enabled true ;
iq:fromEarliest false
] .
```

### Redis Connector

**What it connects:** Keys, hashes, lists, sets, streams.

**Setup:**

```bash
vault kv put secret/iq/prod/redis \
  host="redis-prod.internal" \
  port=6379 \
  password="***" \
  db=0
```

**Configuration:**

```turtle
iq:connector-redis-prod
a iq:Connector ;
iq:name "redis-prod" ;
iq:type "iq-connect-redis" ;
iq:enabled true ;
iq:credentials [
iq:host "${secret:redis/host}" ;
iq:port "${secret:redis/port}" ;
iq:password "${secret:redis/password}" ;
iq:database "${secret:redis/db}"
] ;
iq:poolSize 10 ;
iq:sync [
iq:enabled true ;
iq:schedule "*/1 * * * *"  # Every minute
] .
```

## Testing Connectors

### Validate Configuration

```bash
# List all enabled connectors
./bin/iq-cli connectors list

# Test a specific connector
./bin/iq-cli connectors test aws-prod

# Get connector status
./bin/iq-cli connectors status --realm prod

# Run sync manually
./bin/iq-cli connectors sync aws-prod --wait
```

### Debug Connector Issues

```bash
# Check logs
tail -f "${IQ_HOME}/logs/connector.log"

# Enable debug logging
export IQ_LOG_LEVEL="DEBUG"

# Run in foreground
./bin/iq-cli connectors test aws-prod --verbose

# Check credentials
./bin/iq-cli vault get secret:aws/access_key_id

# Network connectivity
telnet aws-endpoint.example.com 443
```

## Rate Limiting and Quotas

Configure rate limits per connector:

```turtle
iq:connector-aws-prod
iq:rateLimiting [
iq:requestsPerSecond 50 ;
iq:burst 500 ;
iq:retryOnThrottle true ;
iq:maxRetries 3 ;
iq:backoffMultiplier 2.0 ;
iq:backoffMaxDelayMs 32000
] .
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Authentication failed` | Double-check vault secrets and token expiry |
| `Connection timeout` | Verify firewall/security groups allow access |
| `Rate limit exceeded` | Reduce sync frequency or increase `requestsPerSecond` |
| `Data not syncing` | Check connector logs, verify IAM/RBAC permissions |
| `Memory leak` | Check connection pool settings, review logs |

## Next Steps

1. **[REST APIs](06-REST_APIS.md)** — Query your connected systems
2. **[MCP Integration](07-MCP.md)** — Expose connectors as MCP tools
3. **[Operations](10-OPERATIONS.md)** — Monitor connector health
