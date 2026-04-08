---
title: REST API Reference
audience: ["developer", "devops"]
sections: ["chat", "agents", "openai", "sparql", "fedx", "authentication"]
---

# REST API Reference

IQ exposes REST APIs for chat, agents, LLM inference, and semantic queries.

## Base URL

```
http://localhost:8080/api/v1
# or production
https://iq.example.com/api/v1
```

## Authentication

All requests require Bearer token or JWT:

```bash
# Get token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
"username": "user@example.com",
"password": "***"
  }'

# Response
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}

# Use token in API calls
curl http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer eyJ..."
```

Or set environment variable:

```bash
export IQ_API_TOKEN="eyJ..."
```

## Chat Endpoint

Send messages to an AI avatar and get grounded responses.

### Request

```http
POST /api/v1/realms/{realm}/chat
Content-Type: application/json
Authorization: Bearer {token}

{
  "message": "What AWS resources are we running?",
  "context": {
"userId": "user@example.com",
"sessionId": "sess_123abc",
"variables": {
  "environment": "production"
}
  },
  "options": {
"timeout": 60,
"temperature": 0.7,
"maxTokens": 2048
  }
}
```

### Response

```json
{
  "id": "msg_456def",
  "realm": "prod",
  "avatar": "system",
  "message": "Based on your AWS connector, we have:\n- 12 EC2 instances (t3.medium)\n- 3 RDS databases (PostgreSQL)\n- 8 S3 buckets\n\nWould you like details on any specific resources?",
  "confidence": 0.95,
  "sources": [
{
  "type": "connector",
  "name": "aws-prod",
  "query": "query that retrieved the data"
}
  ],
  "tokens": {
"prompt": 156,
"completion": 89,
"total": 245
  },
  "cost": 0.0073,
  "latencyMs": 2341,
  "timestamp": "2024-01-15T10:30:45Z"
}
```

### Examples

```bash
# Simple query
curl -X POST http://localhost:8080/api/v1/realms/prod/chat \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
"message": "List all failed deployments"
  }'

# With context
curl -X POST http://localhost:8080/api/v1/realms/prod/chat \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -d '{
"message": "What changed in the last 24 hours?",
"context": {
  "userId": "devops@example.com",
  "variables": {
"timespan": "24h"
  }
}
  }'

# With custom LLM settings
curl -X POST http://localhost:8080/api/v1/realms/prod/chat \
  -d '{
"message": "Draft incident report",
"options": {
  "model": "gpt-4",
  "temperature": 0.3,
  "maxTokens": 4096
}
  }'
```

## Agent Endpoint

Trigger intent-driven workflows backed by domain knowledge.

### Request

```http
POST /api/v1/realms/{realm}/agents/{agentName}/trigger
Content-Type: application/json
Authorization: Bearer {token}

{
  "intent": "deploy_version",
  "parameters": {
"service": "api-gateway",
"version": "2.5.1",
"environment": "staging",
"rolloutPercentage": 25
  },
  "context": {
"userId": "operator@example.com",
"approverEmail": "ops-manager@example.com"
  }
}
```

### Response

```json
{
  "agentId": "agent_789ghi",
  "realm": "prod",
  "intent": "deploy_version",
  "status": "in_progress",
  "currentState": "approval_pending",
  "nextStates": ["approved", "rejected"],
  "progress": {
"step": 1,
"totalSteps": 5,
"description": "Waiting for approval from ops-manager"
  },
  "transitions": [
{
  "action": "approve",
  "condition": "requires approval role"
},
{
  "action": "reject",
  "condition": "requires approval role"
}
  ],
  "executionId": "exec_ghi789",
  "timestamp": "2024-01-15T10:31:00Z"
}
```

### Poll for Completion

```bash
curl http://localhost:8080/api/v1/realms/prod/agents/agent_789ghi/status \
  -H "Authorization: Bearer $IQ_API_TOKEN"

# Response
{
  "agentId": "agent_789ghi",
  "status": "completed",
  "result": {
"success": true,
"deployment": {
  "versionDeployed": "2.5.1",
  "instancesUpdated": 4,
  "rolloutTime": "00:05:30"
}
  }
}
```

### Examples

```bash
# Trigger workflow
curl -X POST http://localhost:8080/api/v1/realms/prod/agents/incident-response/trigger \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -d '{
"intent": "declare_incident",
"parameters": {
  "severity": "critical",
  "service": "payment-api",
  "description": "High error rate detected"
}
  }'

# Check agent list
curl http://localhost:8080/api/v1/realms/prod/agents \
  -H "Authorization: Bearer $IQ_API_TOKEN"

# List active executions
curl http://localhost:8080/api/v1/realms/prod/agents/active-executions \
  -H "Authorization: Bearer $IQ_API_TOKEN"
```

## OpenAI-Compatible Endpoint

Drop-in replacement for OpenAI API.

### Request

```http
POST /api/v1/openai/chat/completions
Content-Type: application/json
Authorization: Bearer {token}

{
  "model": "gpt-4",
  "messages": [
{
  "role": "system",
  "content": "You are a helpful Kubernetes expert. Answer questions based on cluster state."
},
{
  "role": "user",
  "content": "Why is the payment-service deployment failing?"
}
  ],
  "temperature": 0.7,
  "max_tokens": 1024
}
```

### Response

```json
{
  "id": "chatcmpl_123",
  "object": "text_completion",
  "created": 1704974445,
  "model": "gpt-4",
  "usage": {
"prompt_tokens": 45,
"completion_tokens": 189,
"total_tokens": 234
  },
  "choices": [
{
  "message": {
"role": "assistant",
"content": "The payment-service deployment is failing due to insufficient memory. The pods are requesting 2Gi but only 1Gi is available. You need to either increase node capacity or reduce the memory request for the deployment."
  },
  "finish_reason": "stop"
}
  ]
}
```

### Use with Python OpenAI SDK

```python
from openai import OpenAI

client = OpenAI(
api_key="YOUR_IQ_TOKEN",
base_url="http://localhost:8080/api/v1/openai"
)

response = client.chat.completions.create(
model="gpt-4",
messages=[
{
"role": "system",
"content": "You answer based on infrastructure state"
},
{
"role": "user",
"content": "What's the status of prod cluster?"
}
]
)

print(response.choices[0].message.content)
```

## SPARQL Query Endpoint

Execute semantic queries directly.

### GET /api/v1/realms/{realm}/sparql

```bash
curl -G "http://localhost:8080/api/v1/realms/prod/sparql" \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  --data-urlencode 'query=SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10'

# Response
{
  "head": {
"vars": ["s", "p", "o"]
  },
  "results": {
"bindings": [
  {
"s": { "type": "uri", "value": "urn:aws:instance:i-123456" },
"p": { "type": "uri", "value": "urn:aws:state" },
"o": { "type": "***REMOVED***", "value": "running" }
  }
]
  }
}
```

### POST /api/v1/realms/{realm}/sparql

For larger queries or updates:

```bash
curl -X POST "http://localhost:8080/api/v1/realms/prod/sparql" \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'query=INSERT DATA { 
<urn:incident:123> <urn:status> "resolved" . 
  }'
```

### Examples

```sparql
# Find all running EC2 instances in AWS
PREFIX aws: <urn:aws:>
SELECT DISTINCT ?instanceId ?type ?launchTime WHERE {
  ?instance a aws:EC2Instance ;
aws:instanceId ?instanceId ;
aws:instanceType ?type ;
aws:launchTime ?launchTime ;
aws:state "running" .
}

# Find Kubernetes pods in error state
PREFIX k8s: <urn:k8s:>
SELECT DISTINCT ?podName ?namespace ?reason WHERE {
  ?pod a k8s:Pod ;
k8s:name ?podName ;
k8s:namespace ?namespace ;
k8s:phase "Failed" ;
k8s:reason ?reason .
}

# Update service status
PREFIX svc: <urn:service:>
INSERT DATA {
  <urn:service:payment-api> svc:status "degraded" ;
svc:errorRate "15.3" ;
svc:lastUpdate "2024-01-15T10:35:00Z" .
}
```

## FedX Federated Endpoint

Query across multiple endpoints seamlessly.

### Request

```http
POST /api/v1/realms/{realm}/sparql/federated
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {token}

query=SELECT DISTINCT ?service ?status WHERE {
  ?service a <urn:Service> ;
<urn:status> ?status .
}
```

### Response

```json
{
  "head": {
"vars": ["service", "status"]
  },
  "results": {
"bindings": [
  {
"service": { "type": "uri", "value": "urn:service:aws-ec2" },
"status": { "type": "***REMOVED***", "value": "healthy" }
  },
  {
"service": { "type": "uri", "value": "urn:service:gcp-compute" },
"status": { "type": "***REMOVED***", "value": "healthy" }
  }
]
  }
}
```

## Realm Management

### List Realms

```bash
curl http://localhost:8080/api/v1/realms \
  -H "Authorization: Bearer $IQ_API_TOKEN"

# Response
{
  "realms": [
{
  "name": "prod",
  "description": "Production environment",
  "status": "active",
  "tripleCount": 5234891,
  "lastSync": "2024-01-15T10:30:00Z"
},
{
  "name": "staging",
  "description": "Staging environment",
  "status": "active",
  "tripleCount": 1823456,
  "lastSync": "2024-01-15T10:28:00Z"
}
  ]
}
```

### Get Realm Info

```bash
curl http://localhost:8080/api/v1/realms/prod \
  -H "Authorization: Bearer $IQ_API_TOKEN"

# Response
{
  "name": "prod",
  "repository": {
"type": "RemoteRepository",
"serverUrl": "http://rdf4j-prod:8080/rdf4j-server",
"size": "5.2GB",
"tripleCount": 5234891
  },
  "agents": [
{
  "name": "incident-response",
  "states": 8,
  "transitions": 15
}
  ],
  "connectors": [
{
  "name": "aws-prod",
  "lastSync": "2024-01-15T10:30:00Z",
  "status": "healthy"
}
  ]
}
```

## Health and Status

### Health Check

```bash
curl http://localhost:8080/health \
  -H "Authorization: Bearer $IQ_API_TOKEN"

# Response
{
  "status": "UP",
  "components": {
"database": { "status": "UP" },
"diskSpace": { "status": "UP", "details": { "free": "45GB" } },
"kafka": { "status": "UP" },
"kubernetes": { "status": "UP" }
  }
}
```

## Error Responses

Common error codes:

```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token",
  "statusCode": 401
}
```

```json
{
  "error": "realm_not_found",
  "message": "Realm 'unknown' does not exist",
  "statusCode": 404
}
```

```json
{
  "error": "query_invalid",
  "message": "SPARQL syntax error at line 3",
  "statusCode": 400
}
```

## Rate Limiting

IQ enforces rate limits per API key:

- **Chat**: 100 requests/hour
- **Agent**: 50 executions/hour
- **SPARQL**: 1000 queries/hour
- **OpenAI**: 5000 tokens/minute

Response headers indicate remaining quota:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1704974700
```

## Client Libraries

### Python

```bash
pip install openai requests
```

```python
import requests

class IQClient:
def __init__(self, base_url, token):
self.base_url = base_url
self.headers = {"Authorization": f"Bearer {token}"}

def chat(self, realm, message):
return requests.post(
f"{self.base_url}/api/v1/realms/{realm}/chat",
headers=self.headers,
json={"message": message}
).json()

client = IQClient("http://localhost:8080", "YOUR_TOKEN")
response = client.chat("prod", "Status?")
print(response["message"])
```

### JavaScript

```bash
npm install axios
```

```javascript
const axios = require('axios');

const iq = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
'Authorization': `Bearer ${process.env.IQ_API_TOKEN}`
  }
});

async function chat(realm, message) {
  const response = await iq.post(`/realms/${realm}/chat`, {
message
  });
  return response.data.message;
}

chat('prod', 'List K8S nodes').then(console.log);
```

## Webhooks and Events

Subscribe to realm events:

```bash
# Create webhook
curl -X POST http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -d '{
"realm": "prod",
"events": ["agent.completed", "connector.synced"],
"url": "https://myapp.example.com/webhooks/iq"
  }'

# IQ POSTs to your URL when events occur:
{
  "event": "agent.completed",
  "realm": "prod",
  "data": {
"agentId": "agent_123",
"result": "success"
  }
}
```

## Next Steps

1. **[MCP Integration](07-MCP.md)** — Expose APIs as LLM tools
2. **[Clustering](08-CLUSTERING.md)** — Deploy APIs at scale
3. **[Monitoring](11-MONITORING.md)** — Track API usage and performance
