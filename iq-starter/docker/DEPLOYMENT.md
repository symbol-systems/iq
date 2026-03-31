# DOCKER & CLOUD — Run IQ in production

Deploy IQ to your cloud (AWS, Azure, GCP) with Docker, Kubernetes, or serverless platforms.

## Quick start: Docker Compose (all-in-one)

```bash
cd iq-starter/docker

# 1. Create environment file
cp .env.example .env
# Edit .env with your settings:
#   OPENAI_API_KEY=sk-...
#   SLACK_BOT_TOKEN=xoxb-...
#   AWS_ACCESS_KEY_ID=AKIA...
#   etc.

# 2. Start everything
docker-compose up -d

# 3. Verify
curl http://localhost:8080/q/dev/
# Opens the dev UI at http://localhost:8080
```

What's running:
- IQ API server on port 8080
- PostgreSQL database for persistence (optional, default is in-memory)
- Volume mounts for `.iq/` config directory
- Predefined networks for multi-tier setup

## Docker: Single image

### Build

From the repo root:

```bash
./bin/build-image

# Output:
# Building iq-api:latest
# ✓ Successfully built iq-api:latest
```

Or manually:

```bash
cd iq-apis
./mvnw -Dquarkus.container-image.build=true clean install
```

### Run

```bash
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e IQ_HOME=/opt/iq/.iq \
  -v ~/.iq:/opt/iq/.iq \
  iq-api:latest

# Server is ready at http://localhost:8080
```

### Environment variables

```bash
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e SLACK_BOT_TOKEN=xoxb-... \
  -e AWS_ACCESS_KEY_ID=AKIA... \
  -e AWS_SECRET_ACCESS_KEY=... \
  -e GITHUB_TOKEN=github_pat_... \
  -e DATABASE_URL=postgresql://user:pass@postgres:5432/iq \
  -e IQ_REALM_DEFAULT=acme-corp \
  -e IQ_AUTH_JWT_SECRET=your-secret-key-here \
  iq-api:latest
```

---

## Kubernetes

### Deploy with Helm (simplest)

```bash
helm repo add symbol-systems https://charts.symbol.systems
helm repo update

helm install iq-release symbol-systems/iq \
  --namespace iq-system --create-namespace \
  --set image.tag=latest \
  --set openai.apiKey=$OPENAI_API_KEY \
  --set replicas=3 \
  --set persistence.enabled=true \
  --set persistence.size=20Gi
```

### Manual YAML (GitOps-friendly)

File: `k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iq-api
  namespace: iq-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: iq-api
  template:
    metadata:
      labels:
        app: iq-api
    spec:
      containers:
      - name: iq-api
        image: iq-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: iq-secrets
              key: openai-key
        - name: IQ_HOME
          value: /opt/iq/.iq
        volumeMounts:
        - name: iq-config
          mountPath: /opt/iq/.iq
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: iq-config
        persistentVolumeClaim:
          claimName: iq-config-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: iq-api
  namespace: iq-system
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: iq-api
```

Deploy:

```bash
kubectl apply -f k8s/
kubectl get pods -n iq-system
kubectl logs -f deployment/iq-api -n iq-system
```

### Horizontal auto-scaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: iq-api
  namespace: iq-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: iq-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## AWS deployment

### With ECS Fargate (easiest)

```bash
# 1. Push image to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
docker tag iq-api:latest <account>.dkr.ecr.us-east-1.amazonaws.com/iq-api:latest
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/iq-api:latest

# 2. Create task definition
# See ecs-task-definition.json in docker/

# 3. Create service
aws ecs create-service \
  --cluster iq-prod \
  --service-name iq-api \
  --task-definition iq-api:1 \
  --desired-count 3 \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=iq-api,containerPort=8080
```

### With Lambda (serverless, for low-traffic)

IQ is too heavy for Lambda's memory limits. Use **ECS Fargate** instead.

### With API Gateway + Load Balancer

```bash
# Enable load balancer in front
./bin/setup-aws-deployment \
  --region us-east-1 \
  --cluster iq-prod \
  --service iq-api \
  --enable-autoscaling
```

---

## Azure deployment

### With Container Instances (simple)

```bash
az container create \
  --resource-group iq-prod \
  --name iq-api \
  --image iq-api:latest \
  --port 8080 \
  --environment-variables OPENAI_API_KEY=sk-... \
  --cpu 2 \
  --memory 4 \
  --dns-name-label iq-prod
```

Access: `http://iq-prod.<region>.azurecontainers.io:8080`

### With Azure Container Apps (recommended)

```bash
az containerapp create \
  --name iq-api \
  --resource-group iq-prod \
  --environment iq-env \
  --image iq-api:latest \
  --target-port 8080 \
  --ingress external \
  --min-replicas 2 \
  --max-replicas 10 \
  --cpu 2 \
  --memory 4Gi \
  --secrets openai-key=$OPENAI_API_KEY \
  --env-vars OPENAI_API_KEY=secretref:openai-key
```

### With App Service (if you prefer VMs)

Use Azure DevOps:
1. Push image to Azure Container Registry
2. Deploy via App Service with continuous deployment

---

## GCP deployment

### With Cloud Run (simplest)

```bash
# Build and push
gcloud builds submit --tag gcr.io/$PROJECT_ID/iq-api

# Deploy
gcloud run deploy iq-api \
  --image gcr.io/$PROJECT_ID/iq-api:latest \
  --platform managed \
  --region us-central1 \
  --memory 4Gi \
  --cpu 2 \
  --set-env-vars OPENAI_API_KEY=sk-...
```

### With GKE (Kubernetes on GCP)

```bash
gcloud container clusters create iq-prod \
  --num-nodes 3 \
  --machine-type n1-standard-2

gcloud container clusters get-credentials iq-prod

# Then apply Kubernetes manifests above
kubectl apply -f k8s/
```

---

## Persistent storage

### Docker Compose

Uses a named volume:

```yaml
volumes:
  iq-data:
    driver: local

services:
  iq-api:
    volumes:
      - iq-data:/opt/iq/.iq
```

### Kubernetes

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: iq-config-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
  storageClassName: fast-ssd
```

### PostgreSQL backend (instead of in-memory)

For production, use persistent PostgreSQL:

```bash
export DATABASE_URL=postgresql://user:pass@postgres.example.com/iq_prod

docker run ... \
  -e DATABASE_URL=$DATABASE_URL \
  iq-api:latest
```

---

## Monitoring & logging

### Docker logs

```bash
docker logs -f <container-id>
```

### Kubernetes logs

```bash
kubectl logs -f deployment/iq-api -n iq-system
```

### Structured logging

IQ outputs JSON logs (Quarkus). Use your favorite log aggregator:

**ELK Stack:**
```yaml
logging:
  driver: "splunk"
  options:
    splunk-token: "your-token"
    splunk-url: "https://input-*.cloud.splunk.com:8088"
```

**CloudWatch (AWS):**
```bash
docker run --log-driver awslogs \
  --log-opt awslogs-group=/ecs/iq-api \
  --log-opt awslogs-region=us-east-1 \
  iq-api:latest
```

### Health checks

IQ exposes health endpoints:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready
```

---

## Security in production

### API keys from secrets manager

Don't put secrets in environment variables. Use:

**AWS Secrets Manager:**
```bash
aws secretsmanager create-secret \
  --name iq-secrets \
  --secret-string '{
    "openai_api_key": "sk-...",
    "slack_bot_token": "xoxb-...",
    "jwt_secret": "..."
  }'
```

Then reference in ECS task:

```json
{
  "name": "OPENAI_API_KEY",
  "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:iq-secrets:openai_api_key::"
}
```

**Azure Key Vault:**
```bash
az keyvault secret set \
  --vault-name iq-vault \
  --name openai-api-key \
  --value sk-...
```

**Kubernetes Secrets:**
```bash
kubectl create secret generic iq-secrets \
  --from-literal=openai_api_key=sk-... \
  --from-literal=slack_bot_token=xoxb-... \
  -n iq-system
```

### Network security

Use private subnets + Load Balancer:

```
Internet
  ↓
Load Balancer (public)
  ↓
IQ API (in private subnet, auto-scaled)
  ↓
PostgreSQL (in private subnet)
```

### TLS/HTTPS

Every cloud platform handles this. Example with Kubernetes:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: iq-ingress
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - iq.example.com
    secretName: iq-tls-cert
  rules:
  - host: iq.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: iq-api
            port:
              number: 8080
```

---

## Troubleshooting

| Problem | Solution |
|---|---|
| "Container exits immediately" | Check logs: `docker logs <id>` |
| "Port 8080 in use" | Use a different port: `docker run -p 8081:8080 ...` |
| "Knowledge not persisting" | Check volume is mounted correctly |
| "OOM (out of memory)" | Increase container memory: `--memory 8Gi` |
| "Slow startup" | First run indexes knowledge. Subsequent starts are faster. |
| "Connectors fail" | Verify credential environment variables are set |

---

## Next steps

- **Monitor your deployment:** Set up logging & alerts
- **Add more replicas:** Scale horizontally for more throughput
- **Use GitOps:** Deploy from Git (ArgoCD, Flux)
- **Backup strategy:** Persistent storage + regular backups

