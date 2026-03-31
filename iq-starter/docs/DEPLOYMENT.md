# Docker & Cloud Deployment

Deploy IQ to production with confidence using Docker, Docker Compose, or cloud platforms.

## Quick Start: Docker Compose (Local)

```bash
# 1. Build and start all services
docker-compose up -d

# 2. Check status
docker-compose ps
# Expected: iq-apis is healthy

# 3. Test the server
curl http://localhost:8080/health | jq .

# 4. List MCP tools
curl http://localhost:8080/mcp/tools | jq '.tools | length'

# 5. View logs
docker-compose logs -f iq-apis
```

### Stopping & cleaning up
```bash
docker-compose down  # Stop containers, keep data
docker-compose down -v   # Stop + remove volumes
```

---

## Production: Docker Image Only

### Build
```bash
# Build image locally
./bin/build-image
# or
docker build -t your-org/iq-starter:v1.0 .

# Tag for registry
docker tag iq-starter:latest your-registry.azurecr.io/iq-starter:v1.0
docker push your-registry.azurecr.io/iq-starter:v1.0
```

### Run
```bash
docker run -d \
  --name iq-apis \
  -p 8080:8080 \
  -e IQ_KNOWLEDGE_GRAPH_TYPE=sparql-endpoint \
  -e IQ_SPARQL_ENDPOINT=http://rdf4j:8080/rdf4j-server \
  -v /data/iq-knowledge-graphs:/app/data \
  your-registry.azurecr.io/iq-starter:v1.0
```

---

## Azure Container Instances (ACI)

### 1. Push image to Azure Container Registry
```bash
az acr build --registry your-registry \
  --image iq-starter:v1.0 .
```

### 2. Deploy to ACI
```bash
az container create \
  --resource-group your-rg \
  --name iq-starter \
  --image your-registry.azurecr.io/iq-starter:v1.0 \
  --dns-name-label iq-starter \
  --ports 8080 \
  --environment-variables \
IQ_KNOWLEDGE_GRAPH_TYPE=sparql-endpoint \
IQ_SPARQL_ENDPOINT=http://rdf4j:8080 \
  --registry-login-server your-registry.azurecr.io \
  --registry-username <username> \
  --registry-password <password>
```

### 3. Access
```bash
# Get public IP
az container show --resource-group your-rg --name iq-starter --query ipAddress.fqdn

# Test
curl http://iq-starter.<region>.azurecontainers.io:8080/health
```

---

## Azure Container Apps

For managed, serverless deployment:

### 1. Create environment
```bash
az containerapp env create \
  --name iq-env \
  --resource-group your-rg \
  --location eastus
```

### 2. Deploy
```bash
az containerapp create \
  --name iq-starter \
  --resource-group your-rg \
  --environment iq-env \
  --image your-registry.azurecr.io/iq-starter:v1.0 \
  --target-port 8080 \
  --registry-server your-registry.azurecr.io \
  --registry-username <username> \
  --registry-password <password> \
  --env-vars \
IQ_KNOWLEDGE_GRAPH_TYPE=sparql-endpoint \
IQ_SPARQL_ENDPOINT=http://rdf4j-container-app:8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3
```

### 3. Access
```bash
# Get FQDN
az containerapp show --name iq-starter --resource-group your-rg --query properties.configuration.ingress.fqdn

# Test
curl https://iq-starter.<random>.eastus.azurecontainerapps.io/health
```

---

## Kubernetes (AKS)

For production-grade, self-healing deployments:

### Helm Chart
If available, use Helm (reference chart coming soon):
```bash
helm install iq-starter ./helm \
  --namespace iq \
  --set image.repository=your-registry.azurecr.io/iq-starter \
  --set image.tag=v1.0 \
  --set replicas=3
```

### Manual YAML deployment
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iq-starter
  namespace: iq
spec:
  replicas: 3
  selector:
matchLabels:
  app: iq-starter
  template:
metadata:
  labels:
app: iq-starter
spec:
  containers:
  - name: iq-api
image: your-registry.azurecr.io/iq-starter:v1.0
ports:
- containerPort: 8080
env:
- name: IQ_KNOWLEDGE_GRAPH_TYPE
  value: "sparql-endpoint"
- name: IQ_SPARQL_ENDPOINT
  value: "http://rdf4j:8080"
- name: JAVA_OPTS
  value: "-Xmx1g -XX:+UseG1GC"
resources:
  requests:
cpu: 500m
memory: 1Gi
  limits:
cpu: 2000m
memory: 2Gi
livenessProbe:
  httpGet:
path: /health
port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
path: /health
port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  imagePullSecrets:
  - name: acr-secret
---
apiVersion: v1
kind: Service
metadata:
  name: iq-starter-service
  namespace: iq
spec:
  selector:
app: iq-starter
  ports:
  - protocol: TCP
port: 8080
targetPort: 8080
  type: LoadBalancer
```

### Deploy to AKS
```bash
# Create namespace
kubectl create namespace iq

# Create image pull secret
kubectl create secret docker-registry acr-secret \
  --docker-server=your-registry.azurecr.io \
  --docker-username=<username> \
  --docker-password=<password> \
  -n iq

# Deploy
kubectl apply -f deployment.yaml

# Check rollout
kubectl rollout status deployment/iq-starter -n iq

# Get service IP
kubectl get svc iq-starter-service -n iq
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `IQ_KNOWLEDGE_GRAPH_TYPE` | `in-memory` | `in-memory`, `sparql-endpoint`, `rdf4j` |
| `IQ_SPARQL_ENDPOINT` | (none) | SPARQL endpoint URL (if type=sparql-endpoint) |
| `IQ_MCP_ENABLED` | `true` | Enable MCP endpoints |
| `IQ_REST_API_ENABLED` | `true` | Enable REST API |
| `IQ_WEB_UI_ENABLED` | `true` | Enable web dashboard |
| `JAVA_OPTS` | (see docker-compose.yml) | JVM arguments |
| `IQ_LOG_LEVEL` | `INFO` | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `IQ_VAULT_KEY` | (looks in `.iq/vault/master.key`) | Master encryption key for secrets |

---

## Data Persistence

### Option A: Volume mount (Docker Compose)
```yaml
services:
  iq-apis:
volumes:
  - ./data:/app/data   # RDF triple store
  - ./config:/app/config   # Configuration files
```

### Option B: Cloud storage (Azure)
```yaml
# docker-compose.yml
volumes:
  iq-data:
driver: azure
driver_opts:
  share_name: iq-data
  storage_account_name: your-storage-account
```

### Backup & restore
```bash
# Backup RDF data
docker exec iq-apis tar czf - /app/data > iq-backup.tar.gz

# Restore from backup
docker exec iq-apis tar xzf - < iq-backup.tar.gz
```

---

## Monitoring & Logging

### Health checks
```bash
# Basic health
curl http://localhost:8080/health

# Detailed metrics
curl http://localhost:8080/metrics | head -50

# Component status
curl http://localhost:8080/api/status | jq .
```

### Logs
```bash
# View logs
docker-compose logs -f iq-apis

# Export logs to file
docker-compose logs iq-apis > logs.txt

# Filter logs
docker-compose logs iq-apis | grep -i error
```

### Prometheus metrics
If you enabled Prometheus (add to `docker-compose.yml`):
```yaml
services:
  prometheus:
image: prom/prometheus:latest
volumes:
  - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
ports:
  - "9090:9090"
```

---

## Security Considerations

### 1. Network
- Run IQ behind a reverse proxy (nginx, Azure Application Gateway)
- Use TLS for all external traffic
- Restrict network access (firewall rules, network policies)

### 2. Secrets
- Never commit `.iq/vault/` to git
- Use managed secret stores (Azure Key Vault, AWS Secrets Manager)
- Rotate credentials regularly

### 3. Images
- Scan images for vulnerabilities: `docker scan your-registry.azurecr.io/iq-starter:v1.0`
- Use minimal base images (openjdk:21-jdk-slim)
- Keep dependencies updated

### 4. Access Control
- Require authentication for `/api/*` endpoints
- Use API keys or OAuth2 for MCP access
- Audit all tool invocations

Example with nginx reverse proxy:
```nginx
server {
listen 443 ssl;
server_name api.yourcompany.com;

ssl_certificate /etc/ssl/certs/cert.pem;
ssl_certificate_key /etc/ssl/private/key.pem;

# Require API key
location / {
# Check Authorization header
if ($http_authorization = "") {
return 401;
}
proxy_pass http://iq-apis:8080;
}
}
```

---

## Scaling

### Horizontal scaling
Run multiple IQ instances behind a load balancer:

```yaml
# docker-compose with multiple replicas
services:
  iq-apis:
deploy:
  replicas: 3
# or use load balancer service
```

### Vertical scaling
Increase JVM heap:
```bash
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
docker-compose up -d
```

---

## Troubleshooting

### Port already in use
```bash
# Use different port
docker-compose up -d -p "8081:8080"
# or change docker-compose.yml
```

### Out of memory
```bash
# Check JVM heap
docker stats iq-apis

# Increase limit in docker-compose.yml
environment:
  JAVA_OPTS: "-Xmx4g"
```

### Slow startup
- First run includes graph indexing (~30 sec)
- Subsequent runs are faster
- Check logs: `docker-compose logs iq-apis | grep -i "index"`

### API returns 502
- Check server health: `curl http://localhost:8080/health`
- View logs: `docker-compose logs iq-apis | tail -50`
- Increase timeout in load balancer config

---

## Next Steps

1. **Local testing:** `docker-compose up -d`
2. **Connect your LLM:** See [MCP.md](MCP.md)
3. **Load data:** `./bin/import-example`
4. **Deploy to cloud:** Choose Azure/AWS/GCP guide above
5. **Monitor & scale:** Set up logging and auto-scaling

**Support:** See [FAQ.md](FAQ.md) for common issues.
