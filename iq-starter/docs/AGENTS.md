# Agent & Workflow Setup

This guide covers how to configure autonomous agents and complex workflows in IQ.

## Three levels of automation

### Level 1: Simple Tool Call (Easy)
Your LLM calls an MCP tool—IQ returns a result.

```
Claude: "Show me top customers"
  ↓
MCP Tool: query_knowledge_graph(sparql)
  ↓
IQ returns results
  ↓
Claude formats response
```

### Level 2: Workflow (Medium)
A sequence of steps triggered by a condition.

```
IF customer.risk > 0.8
THEN send_alert → assign_risk_reviewer → update_case_status
```

### Level 3: Agentic Loop (Advanced)
Multi-turn reasoning with long-running tasks, memory, and decision trees.

```
Agent reads: "Investigate top 10 at-risk accounts"
  ↓ Loop 1: Query at-risk accounts
  ↓ Loop 2: For each, analyze churn signals
  ↓ Loop 3: Generate retention recommendations
  ↓ Loop 4: Execute recommended actions
  ↓ Final: Report summary
```

---

## Level 1: Simple MCP Tools

Your LLM already has these via MCP. Examples:

```bash
# List available tools
curl http://localhost:8080/mcp/tools | jq '.tools[] | .name'

# Returns:
# - query_knowledge_graph
# - execute_workflow
# - get_entity
# - list_entities
# - update_knowledge_graph
# - send_alert
# - etc.
```

To **add a new MCP tool**, write a SPARQL query or workflow and it auto-exposes:

```turtle
# In config/tools.ttl
tool:list-high-value-customers a iq:SPARQLTool ;
iq:label "List High-Value Customers" ;
iq:query """
SELECT DISTINCT ?id ?name ?ltv 
WHERE {
?entity a crm:Customer ;
id:id ?id ;
crm:name ?name ;
crm:lifetime_value ?ltv .
FILTER (?ltv > 1000000)
}
ORDER BY DESC(?ltv)
""" ;
iq:parameters [
iq:param [
iq:name "threshold" ;
iq:type xsd:decimal ;
iq:default 1000000
]
] .
```

IQ auto-generates the MCP schema and exposes it. Your LLM can call it immediately.

---

## Level 2: Workflows

Workflows are state machines with decision logic. Define in RDF:

### Basic workflow
```turtle
@prefix iq: <http://systems.symbol/iq/> .
@prefix workflow: <http://example.org/workflows/> .
@prefix slack: <http://systems.symbol/connectors/slack/> .

workflow:OnboardNewCustomer a iq:Workflow ;
iq:label "Onboard New Customer" ;
iq:trigger crm:CustomerCreated ;
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:SendNotification ;
iq:recipient "{{workflow.actor}}" ;
iq:message "New customer: {{customer.name}}"
] ;
rdf:_2 [
iq:action iq:CreateTask ;
iq:assignee "sales-team" ;
iq:title "Schedule kickoff call" ;
iq:dueDate "PT3D"^^xsd:duration
] ;
rdf:_3 [
iq:action iq:UpdateKnowledgeGraph ;
iq:statement [
iq:subject "{{customer.id}}" ;
iq:predicate crm:onboarding_status ;
iq:object "IN_PROGRESS"
]
]
] .
```

### Conditional workflow (decision gates)
```turtle
workflow:RiskAssessment a iq:Workflow ;
iq:label "Assess Account Risk" ;
iq:trigger crm:AccountUpdated ;
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:ComputeRiskScore ;
iq:query """
SELECT DISTINCT ?risk_score {
?account crm:days_to_payment ?dtp ;
 crm:invoice_total ?total ;
 crm:churn_probability ?churn .
BIND (((?dtp / 30) * 0.3 + 
   (?total> 10000 ? 0.2 : 0) + 
   ?churn * 0.5) AS ?risk_score)
}
""" ;
iq:storeAs "risk_score"
] ;
rdf:_2 [
iq:action iq:ConditionalBranch ;
iq:condition "{{risk_score}} > 0.7" ;
iq:thenStep [
iq:action iq:SendAlert ;
iq:severity "HIGH" ;
iq:message "Account {{account.name}} at risk"
] ;
iq:elseStep [
iq:action iq:LogMetric ;
iq:metric "low_risk_account" ;
iq:value 1
]
]
] .
```

### Parallel workflows
```turtle
workflow:ProcessLargeOrder a iq:Workflow ;
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:ParallelFor ;
iq:iterable "{{order.line_items}}" ;
iq:itemVariable "item" ;
iq:steps [
iq:action iq:ReserveInventory ;
iq:item "{{item.product_id}}" ;
iq:quantity "{{item.qty}}"
]
] ;
rdf:_2 [
iq:action iq:WaitForAll ;
iq:timeout "PT5M"^^xsd:duration
] ;
rdf:_3 [
iq:action iq:UpdateOrderStatus ;
iq:status "READY_TO_SHIP"
]
] .
```

---

## Level 3: Agentic Loops

For multi-step reasoning with an LLM, use the **agentic runtime**:

```bash
# Start agentic server (separate module)
./mvnw -pl iq-agentic -am quarkus:dev
# Listens on http://localhost:8081/agent
```

### Example: Customer Retention Agent
```turtle
@prefix agent: <http://systems.symbol/agents/> .
@prefix iq: <http://systems.symbol/iq/> .

agent:RetentionSpecialist a iq:Agent ;
iq:label "Customer Retention Specialist" ;
iq:model "claude-3-sonnet" ;
iq:temperature 0.7 ;
iq:system-prompt """You are a retention specialist. 
Your job is to prevent customer churn by analyzing at-risk accounts,
understanding their concerns, and recommending targeted retention actions.
Use the available tools to gather data, execute actions, and track outcomes.
""" ;
iq:tools [
a rdf:Seq ;
rdf:_1 agent:ListAtRiskAccounts ;
rdf:_2 agent:AnalyzeAccountHistory ;
rdf:_3 agent:GetPeerBenchmarks ;
rdf:_4 agent:GenerateRetentionOffer ;
rdf:_5 agent:SendOutreach ;
rdf:_6 agent:LogOutcomeAndMetrics
] ;
iq:memory [
iq:type "rolling_window" ;
iq:max_turns 20 ;
iq:summarize_interval 5
] ;
iq:timeout "PT30M"^^xsd:duration ;
iq:max_retries 3 .
```

### Running the agent
```bash
# Start a long-running agent task
curl -X POST http://localhost:8081/agent/start \
  -H "Content-Type: application/json" \
  -d '{
"agent": "agent:RetentionSpecialist",
"task": "Analyze and create retention plans for our top 10 at-risk accounts"
  }' | jq .

# Returns: { "task_id": "task-2024-03-31-001" }

# Check progress
curl http://localhost:8081/agent/task-2024-03-31-001/status | jq .

# Get results (when done)
curl http://localhost:8081/agent/task-2024-03-31-001/results | jq .
```

---

## Workflow Examples

### Use case 1: Auto-escalation
```turtle
workflow:SupportTicketEscalation a iq:Workflow ;
iq:trigger support:TicketCreated ;
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:Wait ;
iq:duration "PT4H"^^xsd:duration
] ;
rdf:_2 [
iq:action iq:QueryUnresolved ;
iq:query "SELECT DISTINCT ?ticket WHERE { ?ticket support:status 'OPEN' ; support:age ?age . FILTER (?age > PT4H) }" ;
iq:storeAs "old_tickets"
] ;
rdf:_3 [
iq:action iq:ConditionalBranch ;
iq:condition "count({{old_tickets}}) > 0" ;
iq:thenStep [
iq:action iq:Escalate ;
iq:toTeam "senior-support"
]
]
] .
```

### Use case 2: Data sync on schedule
```turtle
workflow:DailySalesforceSync a iq:Workflow ;
iq:schedule "0 2 * * *"^^iq:CronFormat ;  # 2 AM daily
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:FetchFromConnector ;
iq:connector "iq-connect-salesforce" ;
iq:query "SELECT * FROM Account WHERE LastModifiedDate > {{last_sync}}"
] ;
rdf:_2 [
iq:action iq:UpsertToKnowledgeGraph
] ;
rdf:_3 [
iq:action iq:UpdateSyncTimestamp
]
] .
```

### Use case 3: Remediation workflow
```turtle
workflow:RemediateComplianceViolation a iq:Workflow ;
iq:trigger compliance:ViolationDetected ;
iq:steps [
a rdf:Seq ;
rdf:_1 [
iq:action iq:LogIncident
] ;
rdf:_2 [
iq:action iq:CreateTicket ;
iq:priority "CRITICAL" ;
iq:assignee "{{violation.responsible_team}}"
] ;
rdf:_3 [
iq:action iq:NotifyStakeholders ;
iq:channels ["slack:security", "email:ciso"]
]
] .
```

---

## Testing Workflows

### Dry-run a workflow
```bash
curl -X POST http://localhost:8080/api/workflow/test \
  -H "Content-Type: application/json" \
  -d '{
"workflow": "workflow:OnboardNewCustomer",
"trigger_data": {
  "customer.id": "cust-12345",
  "customer.name": "Test Corp",
  "workflow.actor": "alice@example.com"
},
"dry_run": true
  }' | jq .
```

### Monitor active workflows
```bash
curl http://localhost:8080/api/workflows/active | jq '.[] | {workflow, status, started_at, progress}'
```

---

## Performance & Scaling

### For simple workflows (< 1 sec)
- Run inline with REST API
- Auto-timeout after 30 seconds

### For complex workflows (minutes)
- Store in job queue (Redis, RabbitMQ, Kafka)
- Return `job_id` immediately
- Client polls for completion

Configuration in `.iq/config/workflows.yaml`:
```yaml
job_queue:
  type: kafka
  broker: kafka:9092
  topic: iq-workflows
  partitions: 10
  replication_factor: 3
  group_id: iq-workflows-processor
  
workflow_executor:
  threads: 50
  queue_size: 10000
  timeout_secs: 1800  # 30 min max
  retry_policy: exponential  # 1s, 2s, 4s, 8s...
```

---

## Next Steps

1. **Start simple:** Define 1-2 MCP tools as SPARQL queries
2. **Add workflows:** Create first workflow in RDF
3. **Test:** Use dry-run mode before going live
4. **Monitor:** Check workflow logs and metrics
5. **Scale:** Deploy with agents for multi-step reasoning

See [USECASES.md](USECASES.md) for complete examples.
