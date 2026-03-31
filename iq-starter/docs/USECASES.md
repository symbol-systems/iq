# Real-World Use Cases

## 1. Insurance Claims Processing 🏥

**Problem:** Claims reps manually check policies, coverage, risk factors—slow, error-prone.

**Solution with IQ:**
- Model policies, coverage rules, and risk factors in RDF
- LLM (Claude) has MCP tool: `check_claim_eligibility`
- When customer asks "Is my claim covered?", Claude queries the knowledge graph
- SPARQL rules apply business logic: deductibles, exclusions, pre-existing conditions
- Claude explains the answer in natural language

**Files:**
- `examples/usecases/insurance/policies.ttl` — Policy RDF model
- `examples/usecases/insurance/coverage-rules.sparql` — Eligibility rules
- `examples/usecases/insurance/workflows.ttl` — Claims workflow state machine

**Flow:**
```
User Input → Claude (MCP) → query_knowledge_graph(claim_id) 
→ SPARQL rules evaluate → Response: "Approved: $5,000"
```

---

## 2. Customer Data Platform (CDP) 🎯

**Problem:** Customer data scattered across Salesforce, Segment, email platform, warehouse—need unified view.

**Solution with IQ:**
- Connectors aggregate data: `iq-connect-salesforce`, `iq-connect-databricks`
- RDF models: Customer (accounts, contacts, engagement, purchase history)
- SPARQL queries compute: lifetime value, churn risk, segmentation
- MCP exposes `customer_360` tool to any LLM or app

**Files:**
- `examples/usecases/cdp/customer-model.ttl` — Customer ontology
- `examples/usecases/cdp/aggregation.sparql` — Data fusion queries
- `examples/usecases/cdp/connectors.ttl` — Data source configuration

**Flow:**
```
Salesforce ──┐
Databricks ──┼→ IQ Connectors → RDF Graph → SPARQL Queries 
Email API ───┘  → MCP Tool: customer_360
```

---

## 3. Supply Chain Visibility 📦

**Problem:** Track inventory, fulfillment, vendor performance across regions—complex interdependencies.

**Solution with IQ:**
- Model supply chain as knowledge graph: suppliers, warehouses, shipments, orders
- Real-time updates via webhooks (Kafka events)
- SPARQL queries with decision rules: auto-reorder thresholds, vendor SLAs, alternative routes
- Workflows: escalate delays > 5 days, suggest alternative suppliers

**Files:**
- `examples/usecases/supply-chain/entities.ttl` — Suppliers, inventory, shipments
- `examples/usecases/supply-chain/rules.sparql` — SLA checking, reorder logic
- `examples/usecases/supply-chain/workflows.ttl` — Alert & escalation workflows
- `examples/usecases/supply-chain/kafka-listener.yaml` — Event ingestion setup

**Flow:**
```
ERP System Webhook → IQ Event Handler → Update KG → Trigger Workflows
↓ SPARQL Rules Check SLAs
↓ Escalate if breached
```

---

## 4. Internal Knowledge Management (Enterprise Wiki) 📚

**Problem:** Company wiki scattered, outdated, team knowledge siloed.

**Solution with IQ:**
- RDF model: teams, projects, documentation, expertise
- LLM (Claude) has MCP tool: `ask_company_knowledge`
- Employees ask free-form questions → Claude queries RDF → returns relevant docs + experts
- Automatically discover gaps (unanswered questions logged in RDF)

**Files:**
- `examples/usecases/wiki/organizations.ttl` — Teams, projects, domains
- `examples/usecases/wiki/docs.ttl` — Knowledge base links
- `examples/usecases/wiki/expertise.ttl` — Expert directory
- `examples/usecases/wiki/queries.sparql` — Search + ranking logic

**Platform:** Expose via Slack bot (`iq-connect-slack`):
```
@iq "How do we handle PII in logs?"
↓
IQ queries RDF for matching docs + experts
↓
Bot responds with links + expert handles
```

---

## 5. Compliance & Risk Management ⚖️

**Problem:** Audit trails, compliance rules, risk assessments—manual, fragmented.

**Solution with IQ:**
- Model regulations, policies, control mappings in RDF
- Connectors ingest logs and events (`iq-connect-aws`, `iq-connect-github`)
- SPARQL queries enforce: "Is this AWS resource compliant with SOC2?"
- Automatic gap reports generated from RDF diffs

**Files:**
- `examples/usecases/compliance/regulations.ttl` — SOC2, GDPR, HIPAA rules
- `examples/usecases/compliance/controls.ttl` — Control attestations
- `examples/usecases/compliance/audit.sparql` — Compliance verification queries
- `examples/usecases/compliance/workflows.ttl` — Remediation workflows

**Flow:**
```
AWS Logs → Connector → KG Update → SPARQL Compliance Check 
 ↓ Failed control detected
 ↓ Auto-generate report + escalate
```

---

## 6. Multi-Tenant SaaS Platform 🏢

**Problem:** Each tenant has different config, workflows, and integrations—need isolation + shared logic.

**Solution with IQ:**
- IQ runs single instance, multiple knowledge graphs (one per tenant)
- Shared base ontology (customers, orders, etc.)
- Tenant-specific connectors and workflows
- MCP tools auto-namespaced per tenant in URL

**Setup:**
```
/mcp/tenants/{tenant-id}/tools/query_orders → Tenant A's orders
/mcp/tenants/{tenant-id}/tools/query_orders → Tenant B's orders
```

**Files:**
- `examples/usecases/saas/tenant-model.ttl` — Tenant isolation pattern
- `examples/usecases/saas/workflows.yaml` — Multi-tenant workflow config
- `examples/usecases/saas/connectors.yaml` — Per-tenant connector bindings

---

## 7. AI Agent Evaluation Framework 🤖

**Problem:** Evaluate agent quality—speed, accuracy, hallucination, cost—need structured results.

**Solution with IQ:**
- Model agent runs as RDF: prompts, responses, ground truth, metrics
- SPARQL queries compute: accuracy %, latency percentiles, cost per task
- Compare agent performance across versions
- Identify failure patterns

**Files:**
- `examples/usecases/agent-eval/run-model.ttl` — Agent run schema
- `examples/usecases/agent-eval/metrics.sparql` — KPI calculations
- `examples/usecases/agent-eval/dashboard.ttl` — Summary views

**Flow:**
```
Agent Run 1 (v1.0) ──┐
Agent Run 2 (v1.0) ──┼→ RDF → SPARQL Analysis → Comparison Report
Agent Run 3 (v2.0) ──┘ ↓ Identify regressions
```

---

## How to extend these

Each use case is organized as:
```
examples/usecases/{use-case}/
├── *.ttl  # RDF data models & configuration
├── *.sparql   # Query templates
├── workflows.ttl  # Automation & state machines
├── connectors.yaml# External system bindings
├── README.md  # Setup guide
└── test-data/ # Sample test data
```

**To use any example:**

```bash
# 1. Start the server
./bin/start-api

# 2. In another terminal, import example data
curl -X POST http://localhost:8080/api/import \
  -H "Content-Type: application/x-turtle" \
  --data-binary @examples/usecases/supply-chain/entities.ttl

# 3. Run a query
./bin/demo-query examples/usecases/supply-chain/rules.sparql
```

---

## Next steps

Pick a use case that matches your business problem:
1. Read the README in that directory
2. Customize the `.ttl` and `.sparql` files
3. Set up connectors via `iq-connect-*` modules
4. Wire an LLM to your MCP tools
5. Deploy with `docker-compose up`

See [CONNECTORS.md](CONNECTORS.md) for detailed connector setup.
