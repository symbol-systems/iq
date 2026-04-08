---
title: Model Context Protocol (MCP) Setup
audience: ["developer", "devops"]
sections: ["setup", "tools", "servers", "security", "integration"]
---

# Model Context Protocol (MCP) Guide

Expose IQ as an MCP server to make domain knowledge and agent actions available to any LLM client.

## What is MCP?

The Model Context Protocol (MCP) is a standard for LLMs to request structured data and execute tools. IQ's MCP server connects your LLM to:

- **SPARQL queries** — Access your entire knowledge graph
- **Agent workflows** — Trigger state machines and business logic
- **Cloud resources** — Query AWS, GCP, Azure via connectors
- **External integrations** — Slack, GitHub, Jira, Snowflake, and more

## Quick Start

### 1. Start MCP Server

```bash
cd /path/to/iq
./bin/iq-mcp

# Server starts and listens for connections
# Default: ws://localhost:3000/mcp
```

### 2. Configure Claude (Anthropic)

Create `$HOME/.config/claude/config.json`:

```json
{
  "mcpServers": {
"iq": {
  "command": "ws",
  "args": ["ws://localhost:3000/mcp"],
  "env": {
"IQ_REALM": "prod",
"IQ_TOKEN": "YOUR_API_TOKEN"
  }
}
  }
}
```

### 3. Use in Claude

```
User: "What AWS resources are we running in production?"

Claude (with MCP):
I'll check your AWS infrastructure through IQ.

[Uses MCP tool: sparql-query]
[Query: SELECT DISTINCT ?instance ?type ?state WHERE { ?instance a aws:EC2Instance ... }]

Response: Based on IQ's current data, you have:
- 12 t3.medium instances (9 running, 3 stopped)
- 3 RDS PostgreSQL databases
- 5 S3 buckets
- Total monthly compute cost: ~$4,200
```

## MCP Tools Available

### RDF/SPARQL Tools

#### `sparql-query`

Execute SELECT/ASK queries on knowledge graph.

```json
{
  "name": "sparql-query",
  "inputSchema": {
"type": "object",
"properties": {
  "query": {
"type": "string",
"description": "SPARQL SELECT or ASK query"
  },
  "realm": {
"type": "string",
"description": "Realm name (default: prod)"
  },
  "timeout": {
"type": "integer",
"description": "Query timeout in seconds (max 300)"
  }
},
"required": ["query"]
  }
}
```

**Example:**

```sparql
SELECT DISTINCT ?service ?cpu ?memory WHERE {
  ?instance a aws:EC2Instance ;
aws:instanceType ?type ;
aws:state "running" .
  ?instance aws:tags [
rdfs:label "service" ;
rdf:value ?service
  ] .
}
ORDER BY DESC(?cpu) LIMIT 10
```

#### `sparql-update`

Execute INSERT/DELETE/CONSTRUCT updates.

```json
{
  "name": "sparql-update",
  "inputSchema": {
"type": "object",
"properties": {
  "query": {
"type": "string",
"description": "SPARQL INSERT/DELETE/CONSTRUCT query"
  },
  "realm": { "type": "string" }
},
"required": ["query"]
  }
}
```

**Example:**

```sparql
INSERT DATA {
  <urn:incident:2024-001> a iq:Incident ;
iq:severity "critical" ;
iq:service "payment-api" ;
iq:startTime "2024-01-15T10:35:00Z" ;
iq:status "open" .
}
```

#### `rdf-describe`

Get RDF description of a resource.

```bash
Tool: rdf-describe
Input: { "resource": "urn:aws:instance:i-0123456789abcdef0", "realm": "prod" }

Output:
urn:aws:instance:i-0123456789abcdef0
  a aws:EC2Instance ;
  aws:instanceId "i-0123456789abcdef0" ;
  aws:state "running" ;
  aws:instanceType "t3.medium" ;
  aws:launchTime "2023-12-01T08:00:00Z" ;
  aws:availabilityZone "us-east-1a" ;
  aws:tags [
rdfs:label "Name" ;
rdf:value "api-server-p1"
  ] .
```

### Agent Tools

#### `agent-trigger`

Execute intent-driven workflows.

```json
{
  "name": "agent-trigger",
  "inputSchema": {
"type": "object",
"properties": {
  "agent": {
"type": "string",
"description": "Agent name (e.g., 'incident-response')"
  },
  "intent": {
"type": "string",
"description": "Intent to trigger"
  },
  "parameters": {
"type": "object",
"description": "Intent parameters as JSON"
  },
  "realm": { "type": "string" }
},
"required": ["agent", "intent"]
  }
}
```

**Example:**

```json
{
  "agent": "incident-response",
  "intent": "declare_incident",
  "parameters": {
"severity": "critical",
"service": "payment-api",
"description": "High error rate (15%) detected"
  }
}
```

#### `agent-status`

Get status of agent execution.

```bash
Tool: agent-status
Input: { "agentId": "agent_123abc" }

Output:
{
  "agentId": "agent_123abc",
  "status": "in_progress",
  "currentState": "notification_sent",
  "progress": "Step 2 of 5: Notifying on-call team",
  "nextActions": ["escalate_if_not_acked", "page_manager"]
}
```

### Connector Tools

#### `connector-query`

Query data from a specific connector.

```bash
Tool: connector-query
Input: {
  "connector": "aws-prod",
  "query": "list_ec2_instances",
  "filters": {
"state": "running",
"tag:Environment": "production"
  }
}
```

#### `connector-list`

List available connectors.

```bash
Tool: connector-list
Output:
{
  "connectors": [
{ "name": "aws-prod", "type": "aws", "status": "healthy", "lastSync": "2024-01-15T10:30:00Z" },
{ "name": "k8s-prod", "type": "kubernetes", "status": "healthy", "lastSync": "2024-01-15T10:32:00Z" },
{ "name": "slack-prod", "type": "slack", "status": "healthy", "lastSync": "2024-01-15T10:31:00Z" }
  ]
}
```

## Server Configuration

### Environment Variables

```bash
# Server settings
export IQ_MCP_HOST="0.0.0.0"
export IQ_MCP_PORT=3000
export IQ_MCP_PROTOCOL="ws"  # or "stdio" or "http"

# DeFAULT realm and token
export IQ_MCP_DEFAULT_REALM="prod"
export IQ_MCP_API_TOKEN="eyJ..."

# Log level
export IQ_LOG_LEVEL="DEBUG"

# Security
export IQ_MCP_TLS_ENABLED=true
export IQ_MCP_TLS_CERT="/path/to/cert.pem"
export IQ_MCP_TLS_KEY="/path/to/key.pem"
```

### Configuration File

Create `.iq/mcp-config.ttl`:

```turtle
@prefix iq: <urn:iq:> .

iq:mcp-server
a iq:MCPServer ;
iq:host "0.0.0.0" ;
iq:port 3000 ;
iq:protocol "websocket" ;
iq:tlsEnabled true ;
iq:tlsCertPath "/etc/iq/certs/mcp-cert.pem" ;
iq:tlsKeyPath "/etc/iq/certs/mcp-key.pem" ;
iq:defaultRealm "prod" ;
iq:allowedRealms (
"prod"
"staging"
) ;
iq:rateLimit [
iq:requestsPerSecond 1000 ;
iq:maxOpenConnections 100 ;
iq:connectionTimeoutSecs 300
] ;
iq:tools (
iq:tool-sparql-query
iq:tool-sparql-update
iq:tool-agent-trigger
iq:tool-connector-query
) ;
iq:authorization [
iq:enforceAuth true ;
iq:validationQuery "ASK { ?user iq:hasRole iq:MCP-Access }"
] .

# Define which tools are enabled
iq:tool-sparql-query
a iq:MCPTool ;
iq:name "sparql-query" ;
iq:category "semantic" ;
iq:enabled true ;
iq:costPerCall 10 ;
iq:rateLimit 1000 .

iq:tool-agent-trigger
a iq:MCPTool ;
iq:name "agent-trigger" ;
iq:category "workflow" ;
iq:enabled true ;
iq:costPerCall 50 ;
iq:rateLimit 100 ;
iq:requiresApproval true .
```

## Multi-Protocol Support

### WebSocket (Default)

Best for: Interactive LLM clients, long-lived connections.

```bash
export IQ_MCP_PROTOCOL="websocket"
export IQ_MCP_PORT=3000

# Access
ws://localhost:3000/mcp
wss://iq.example.com/mcp  # with TLS
```

### Stdio

Best for: CLI tools, direct process invocation.

```bash
# Wrap MCP server
iq_mcp_stdio() {
  ./bin/iq-mcp --protocol=stdio
}

# Use with Claude
cat > ~/.config/claude/config.json << 'EOF'
{
  "mcpServers": {
"iq": {
  "command": "/path/to/iq_mcp_stdio"
}
  }
}
EOF
```

### HTTP Server

Best for: REST clients, polling-based access.

```bash
export IQ_MCP_PROTOCOL="http"
export IQ_MCP_PORT=3001

# Call tools
curl -X POST http://localhost:3001/tools/sparql-query \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -d '{
"query": "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10"
  }'
```

## Authorization and Security

### Tool-Level Authorization

Define which roles can invoke which tools:

```turtle
iq:tool-agent-trigger-acl
a iq:AccessControl ;
iq:tool iq:tool-agent-trigger ;
iq:allowedRoles (
"admin"
"operator"
) ;
iq:denyRoles (
"read-only"
) ;
iq:requiresApproval [
iq:enabled true ;
iq:approverRole "manager" ;
iq:costThreshold 100
] .

iq:tool-sparql-update-acl
a iq:AccessControl ;
iq:tool iq:tool-sparql-update ;
iq:allowedRoles (
"admin"
"knowledge-engineer"
) ;
iq:auditLogging true .
```

### Token Validation

All MCP requests must include valid JWT:

```bash
# Request with token
curl -X POST http://localhost:3000/tools/sparql-query \
  -H "Authorization: Bearer eyJ..." \
  -d '{"query": "SELECT ..."}'

# Token validation:
# - Signature verification
# - Expiry check
# - Realm membership
# - Operation permissions
```

### Audit Logging

All tool invocations are logged:

```bash
# View MCP audit log
tail -f "${IQ_HOME}/logs/mcp-audit.log"

# Sample audit entry
{
  "timestamp": "2024-01-15T10:35:22Z",
  "user": "user@example.com",
  "tool": "sparql-query",
  "realm": "prod",
  "parameters": { "query": "SELECT ...", "timeout": 30 },
  "status": "success",
  "cost": 15,
  "latencyMs": 342,
  "tokenUsage": { "consumed": 512 }
}
```

## Integration Examples

### With Claude Desktop

```json
{
  "mcpServers": {
"iq-prod": {
  "command": "ws",
  "args": ["wss://iq.example.com/mcp"],
  "env": {
"IQ_REALM": "prod",
"IQ_TOKEN": "token_prod_...",
"IQ_LOG_LEVEL": "INFO"
  }
},
"iq-staging": {
  "command": "ws",
  "args": ["wss://iq.example.com/mcp"],
  "env": {
"IQ_REALM": "staging",
"IQ_TOKEN": "token_staging_..."
  }
}
  }
}
```

### With Custom LLM Client

```python
import asyncio
import json
from websockets import connect

class IQMCPClient:
def __init__(self, url, token):
self.url = url
self.token = token

async def call_tool(self, tool_name, params):
async with connect(self.url) as websocket:
request = {
"jsonrpc": "2.0",
"id": 1,
"method": "tools/call",
"params": {
"tool": tool_name,
"arguments": params
},
"auth": f"Bearer {self.token}"
}
await websocket.send(json.dumps(request))
response = await websocket.recv()
return json.loads(response)

# Usage
client = IQMCPClient("wss://iq.example.com/mcp", "token_...")
result = asyncio.run(
client.call_tool("sparql-query", {
"query": "SELECT DISTINCT ?s WHERE { ?s a aws:EC2Instance }",
"realm": "prod"
})
)
```

### With LangChain

```python
from langchain.llms import Anthropic
from langchain.tools import Tool
from langchain_community.tools.mcp import MCPTool

# Create MCP tools
mcp_tools = [
MCPTool.from_mcp_server("ws://localhost:3000/mcp", {
"name": "sparql-query",
"description": "Query IQ knowledge graph"
}),
MCPTool.from_mcp_server("ws://localhost:3000/mcp", {
"name": "agent-trigger",
"description": "Trigger IQ agents"
})
]

# Create agent
from langchain.agents import create_openai_tools_agent

agent = create_openai_tools_agent(
Anthropic(),
mcp_tools,
"""You are an expert infrastructure assistant with access to real-time 
cloud and on-premise system state via IQ knowledge graph."""
)
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Connection refused` | Check server is running: `./bin/iq-mcp` |
| `Unauthorized` | Verify token is valid and not expired |
| `Tool not found` | Check tool is enabled in `mcp-config.ttl` |
| `Query timeout` | Increase timeout or optimize SPARQL query |
| `TLS certificate error` | Verify cert path in config, check cert validity |

## Next Steps

1. **[REST APIs](06-REST_APIS.md)** — Alternative API access method
2. **[Clustering](08-CLUSTERING.md)** — Scale MCP servers
3. **[Operations](10-OPERATIONS.md)** — Monitor MCP service
