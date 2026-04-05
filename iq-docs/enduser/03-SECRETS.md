---
title: Secrets Management and Vault
audience: ["devops", "security"]
sections: ["backends", "encryption", "rotation", "audit"]
---

# Secrets Management Guide

Securely store and manage credentials, API keys, and sensitive configuration.

## Vault Backends

Choose the secrets backend for your environment.

### 1. Local Filesystem Vault

**Best for:** Development, testing, self-contained environments.

**Setup:**

```bash
# Initialize local vault
$ mkdir -p "${IQ_HOME}/vault"
$ chmod 700 "${IQ_HOME}/vault"

# Generate master key (first time)
$ ./bin/iq-cli vault init-local
Master key saved to: ${IQ_HOME}/vault/master.key
```

**Export to use:**

```bash
export IQ_SECRETS_BACKEND="local"
export IQ_VAULT_PATH="${IQ_HOME}/vault"
```

**Store a secret:**

```bash
./bin/iq-cli vault set \
  --name "openai/api-key" \
  --value "sk-..." \
  --realm prod
```

**Configuration in `.iq/config.ttl`:**

```turtle
iq:vault-local
a iq:LocalVault ;
iq:basePath "./vault" ;
iq:encryptionAlgorithm "AES-256-GCM" ;
iq:keyDerivation "PBKDF2" ;
iq:iterations 100000 .
```

### 2. HashiCorp Vault

**Best for:** Enterprise, multi-team, audit requirements.

**Setup:**

```bash
# Install Vault (https://www.vaultproject.io/downloads)
vault --version

# Start development server
vault server -dev

# Export access URL and token
export VAULT_ADDR="http://127.0.0.1:8200"
export VAULT_TOKEN="s.xxxxx"

# Enable KV v2 secrets engine (if needed)
vault secrets enable -version=2 kv

# Authenticate IQ
export IQ_SECRETS_BACKEND="hashicorp"
export VAULT_ADDR="https://vault.example.com:8200"
export VAULT_TOKEN="s.xxxxxxxxxxxxxxxx"
export VAULT_NAMESPACE="secret"
```

**Store credentials:**

```bash
vault kv put secret/iq/prod/openai \
  api-key="sk-..." \
  org-id="org-..."

vault kv put secret/iq/prod/aws \
  access-key-id="AKIA..." \
  secret-access-key="***"

vault kv put secret/iq/prod/rdf4j \
  username="repo_user" \
  password="***"
```

**List secrets:**

```bash
vault kv list secret/iq/prod
```

**Configuration in `.iq/config.ttl`:**

```turtle
iq:vault-hashicorp
a iq:HashiCorpVault ;
iq:address "https://vault.example.com:8200" ;
iq:token "${VAULT_TOKEN}" ;
iq:namespace "secret" ;
iq:path "iq" ;
iq:tlsSkipVerify false ;
iq:authMethod "token" .
```

### 3. AWS Secrets Manager

**Best for:** AWS deployments, integration with IAM.

**Setup:**

```bash
export IQ_SECRETS_BACKEND="aws"
export AWS_REGION="us-east-1"
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="***"
```

**Store secret via AWS CLI:**

```bash
aws secretsmanager create-secret \
  --name iq/prod/openai-api-key \
  --secret-string '{"api_key":"sk-..."}'

aws secretsmanager create-secret \
  --name iq/prod/rdf4j-creds \
  --secret-string '{"username":"user","password":"***"}'

aws secretsmanager list-secrets \
  --filters Key=name,Values=iq/prod
```

**Configuration in `.iq/config.ttl`:**

```turtle
iq:vault-aws
a iq:AWSSecretsManagerVault ;
iq:region "us-east-1" ;
iq:path "iq" ;
iq:credentials iq:aws-creds .

iq:aws-creds
a iq:AWSCredentials ;
iq:accessKeyId "${AWS_ACCESS_KEY_ID}" ;
iq:secretAccessKey "${AWS_SECRET_ACCESS_KEY}" .
```

### 4. Azure Key Vault

**Best for:** Azure deployments, RBAC integration.

**Setup:**

```bash
# Create Key Vault in Azure
az keyvault create \
  --resource-group my-rg \
  --name iq-secrets \
  --location eastus

# Add secret
az keyvault secret set \
  --vault-name iq-secrets \
  --name openai-api-key \
  --value "sk-..."

# Export for IQ
export IQ_SECRETS_BACKEND="azure"
export AZURE_KEYVAULT_URL="https://iq-secrets.vault.azure.net/"
export AZURE_TENANT_ID="00000000-0000-0000-0000-000000000000"
export AZURE_CLIENT_ID="..."
export AZURE_CLIENT_SECRET="***"
```

**Configuration in `.iq/config.ttl`:**

```turtle
iq:vault-azure
a iq:AzureKeyVaultVault ;
iq:vaultUrl "https://iq-secrets.vault.azure.net/" ;
iq:tenantId "${AZURE_TENANT_ID}" ;
iq:clientId "${AZURE_CLIENT_ID}" ;
iq:clientSecret "${AZURE_CLIENT_SECRET}" .
```

### 5. GCP Secret Manager

**Best for:** GCP deployments, Workload Identity.

**Setup:**

```bash
export IQ_SECRETS_BACKEND="gcp"
export GOOGLE_CLOUD_PROJECT="my-project"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"

# Create secret
gcloud secrets create iq-openai-key \
  --replication-policy="automatic" \
  --data-file=- <<< "sk-..."

# List secrets
gcloud secrets list --filter="name:iq-*"
```

**Configuration in `.iq/config.ttl`:**

```turtle
iq:vault-gcp
a iq:GCPSecretManagerVault ;
iq:projectId "my-project" ;
iq:credentialsFile "/path/to/service-account.json" .
```

## Reference by Credential Type

### OpenAI API Key

```bash
# Local vault
./bin/iq-cli vault set \
  --name "llm/openai/api-key" \
  --value "sk-..." \
  --realm prod

# HashiCorp Vault
vault kv put secret/iq/prod/openai api-key="sk-..."

# AWS Secrets Manager
aws secretsmanager create-secret \
  --name iq/prod/openai-api-key \
  --secret-string "sk-..."

# Azure Key Vault
az keyvault secret set \
  --vault-name iq-secrets \
  --name openai-api-key \
  --value "sk-..."
```

### Database Credentials

```bash
# PostgreSQL
./bin/iq-cli vault set \
  --name "database/postgres/prod" \
  --value '{"host":"pg.internal","port":5432,"user":"iq_user","password":"***"}' \
  --realm prod

# Store in HashiCorp
vault kv put secret/iq/prod/database/postgres \
  host="pg.internal" \
  port=5432 \
  user="iq_user" \
  password="***"
```

### Cloud Credentials

```bash
# AWS
vault kv put secret/iq/prod/aws \
  access_key_id="AKIA..." \
  secret_access_key="***" \
  region="us-east-1"

# GCP
vault kv put secret/iq/prod/gcp \
  project_id="my-project" \
  service_account="$(cat /path/to/sa.json)"

# Azure
vault kv put secret/iq/prod/azure \
  subscription_id="..." \
  tenant_id="..." \
  client_id="..." \
  client_secret="***"
```

### RDF4J Remote Repository

```bash
./bin/iq-cli vault set \
  --name "repositories/rdf4j/prod" \
  --value '{"username":"repo_user","password":"***"}' \
  --realm prod
```

### Slack/GitHub/Jira Tokens

```bash
vault kv put secret/iq/prod/slack \
  xoxb_token="xoxb-..." \
  workspace_id="T123..."

vault kv put secret/iq/prod/github \
  token="ghp_..." \
  org="myorg"

vault kv put secret/iq/prod/jira \
  username="user@example.com" \
  api_token="***"
```

## Reference Secrets in Configuration

Use variable substitution in `.iq/config.ttl`:

```turtle
@prefix iq: <urn:iq:> .

iq:connector-aws-prod
a iq:Connector ;
iq:name "aws-prod" ;
iq:credentials [
iq:accessKeyId "${secret:aws/access-key-id}" ;
iq:secretAccessKey "${secret:aws/secret-access-key}" ;
iq:region "${secret:aws/region}"
] .

iq:repo-prod
a iq:RemoteRepository ;
iq:serverURL "http://rdf4j-prod:8080/rdf4j-server" ;
iq:username "${secret:rdf4j/username}" ;
iq:password "${secret:rdf4j/password}" .
```

## Key Rotation

Rotate secrets safely without downtime.

### Manual Rotation

```bash
# 1. Create new secret in vault
vault kv put secret/iq/prod/openai api-key="sk-new-..."

# 2. Test with new credential
./bin/iq-cli llm test prod

# 3. Update realm config reference (if needed)

# 4. Archive old secret
vault kv delete secret/iq/prod/openai-archived
vault kv put secret/iq/prod/openai-archived \
  api-key="sk-old-..." \
  rotated="2024-01-15T10:30:00Z"
```

### Automated Rotation

Configure rotation policies in HashiCorp Vault:

```bash
# Enable database secrets engine
vault secrets enable database

# Configure PostgreSQL connection
vault write database/config/postgresql \
  plugin_name=postgresql-database-plugin \
  allowed_roles="readonly" \
  connection_url="postgresql://admin:***@postgres.internal/postgres"

# Create role
vault write database/roles/readonly \
  db_name=postgresql \
  creation_statements="CREATE USER \"{{name}}\"  VALID UNTIL '{{expiration}}' IN ROLE readonly;" \
  default_ttl="1h" \
  max_ttl="24h"

# Request rotated credentials
vault read database/creds/readonly
```

## Encryption at Rest

### Enable Encryption

**Local Vault (`.iq/config.ttl`):**

```turtle
iq:vault-local
a iq:LocalVault ;
iq:encryptionAlgorithm "AES-256-GCM" ;
iq:keyDerivation "PBKDF2" ;
iq:iterations 100000 ;
iq:encryptionKeyPath "${IQ_HOME}/vault/master.key" .
```

**HashiCorp Vault (automatic):**
- All secrets encrypted with Shamir keys
- Set up additional seal wrapping for HSM

**Cloud Vaults:**
- AWS Secrets Manager: Encrypted with KMS
- Azure Key Vault: Encrypted with service-managed keys
- GCP Secret Manager: Encrypted with Google-managed keys

## Audit and Compliance

### Enable Audit Logging

```bash
# HashiCorp Vault
vault audit enable file file_path=/var/log/vault-audit.log

# AWS Secrets Manager
aws cloudtrail create-trail \
  --name iq-secrets-trail \
  --s3-bucket-name iq-audit-logs \
  --region us-east-1

# Azure Key Vault
az monitor diagnostic-settings create \
  --name iq-audit \
  --resource /subscriptions/.../resourceGroups/my-rg/providers/Microsoft.KeyVault/vaults/iq-secrets \
  --logs '[{"category":"AuditEvent","enabled":true}]' \
  --workspace /subscriptions/.../resourcegroups/my-rg/providers/microsoft.operationalinsights/workspaces/iq-logs
```

### Access Secrets

```bash
# List who accessed secrets (HashiCorp)
vault audit list

# View access logs
tail -f /var/log/vault-audit.log | jq '.auth.client_token, .type, .request.path'
```

## Security Best Practices

1. **Never commit secrets to git** — Use environment variables or vault
2. **Rotate regularly** — Set up quarterly key rotation
3. **Use strong master keys** — Generate with `openssl rand -base64 32`
4. **Audit access** — Enable logging in all backends
5. **Least privilege** — Grant minimum required permissions
6. **Encrypt transport** — Always use HTTPS/TLS for vault access
7. **Separate by environment** — Use different vaults for dev/stage/prod
8. **Monitor suspicious access** — Alert on unusual patterns

## Integrations

### Kubernetes Integration

Store IQ secrets as Kubernetes secrets:

```bash
kubectl create secret generic iq-secrets \
  --from-***REMOVED***=openai-api-key="sk-..." \
  --from-***REMOVED***=aws-access-key-id="AKIA..." \
  --from-***REMOVED***=aws-secret-access-key="***"

# Reference in deployment
env:
  - name: OPENAI_API_KEY
valueFrom:
  secretKeyRef:
name: iq-secrets
key: openai-api-key
```

### Docker Secrets

Pass secrets to Docker at runtime:

```bash
docker run \
  -e OPENAI_API_KEY="sk-..." \
  -e AWS_ACCESS_KEY_ID="AKIA..." \
  iq:latest
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Secret not found` | Check secret path and backend type mismatch |
| `Permission denied` | Verify IAM/RBAC policies for service account |
| `Decryption failed` | Master key may be corrupted; restore from backup |
| `Vault unreachable` | Check network connectivity and VAULT_ADDR |

## Next Steps

1. **[Repositories](04-REPOSITORIES.md)** — Configure RDF storage with credentials
2. **[Connectors](05-CONNECTORS.md)** — Securely connect to external systems
3. **[Operations](10-OPERATIONS.md)** — Monitor and maintain secrets
