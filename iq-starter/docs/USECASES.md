# USECASES — Five real-world scenarios (with working examples)

Each use case below has working examples in `examples/` that you can run immediately.

---

## 1. Knowledge Ingestion & Retrieval-Augmented Generation (RAG)

### The problem
You have 500 customer support articles, 200 product specs, and 50 internal playbooks scattered across Google Drive, Confluence, and internal wikis. When a customer asks "How do I reset my password?", you want to give them the right answer, not guess.

### What IQ does
1. **Ingest:** Import all those documents into IQ's knowledge graph
2. **Index:** Create multiple search indexes (keyword, semantic vector, graph-based)
3. **Ground:** When answering, show the customer which article you based it on
4. **Track:** Monitor what docs are used, update them when they're out of date

### Example: Customer support bot

```bash
cd iq-starter

# 1. Import support articles
./bin/import-domain examples/domains/support-kb/

# 2. Start the API
./bin/start-api &

# 3. Ask a question
curl -X POST http://localhost:8080/chat \
  -d '{
    "realm": "support",
    "message": "How do I reset my password?"
  }'

Response:
{
  "response": "To reset your password, go to login.app.com/forgot and...",
  "sources": [
    "docs/reset-password.md",
    "faq/account-security.md"
  ]
}
```

### Files to explore
- `examples/domains/support-kb/knowledge.ttl` — Support articles as RDF facts
- `examples/queries/search-by-topic.sparql` — Find related articles
- `examples/agents/customer-response/` — Generate helpful replies

### Business impact
- **50% faster response time** (knowledge is organized, not scattered)
- **Fewer escalations** (bot answers more questions correctly)
- **Measurable accuracy** (you can see which sources were used)

---

## 2. Agent Workflows: Multi-Step Decision Making with State Tracking

### The problem
Your procurement team processes 100+ purchase orders per day. Each one needs:
1. Budget check ("Do we have budget?")
2. Vendor check ("Is this vendor approved?")
3. Compliance check ("Is the requester authorized?")
4. Approval ("Approved" or "Denied")
5. Action ("Bill account, notify procurement, create ticket")

Currently, this is manual email back-and-forth. 30% of requests get stuck in the wrong state.

### What IQ does
Automates the entire workflow with visible state transitions and explainable decisions.

### Example: Purchase order approval workflow

```bash
cd iq-starter

# 1. Import org rules and current status
./bin/import-domain examples/domains/procurement/

# 2. Start API
./bin/start-api &

# 3. Trigger workflow
curl -X POST http://localhost:8080/agent/trigger \
  -d '{
    "realm": "acme-corp",
    "intent": "approve-purchase-order",
    "po_id": "PO-45678",
    "requester": "alice@acme.com",
    "vendor": "vendor-supplies-inc",
    "amount": 15000
  }'

Response:
{
  "initial_state": "pending",
  "decision_trace": [
    "✓ Requester Alice has manager role",
    "✓ Budget available: $50,000/month, spent: $12,500, available: $37,500",
    "✓ Vendor in approved vendor list",
    "✓ Amount $15,000 < approval limit $20,000",
    "→ DECISION: APPROVED"
  ],
  "final_state": "approved",
  "actions_taken": [
    "send_email: alice@acme.com",
    "send_email: procurement@acme.com",
    "create_jira: ACME-5432",
    "update_budget_tracking: -$15,000"
  ],
  "timestamp": "2024-03-31T12:34:56Z"
}
```

### Files to explore
- `examples/domains/procurement/rules.ttl` — Budget rules, approval thresholds
- `examples/agents/workflows/po-approval.ttl` — The state machine definition
- `examples/queries/pending-approvals.sparql` — Find stuck requests

### Business impact
- **90% automation** (only edge cases need manual review)
- **Clear audit trail** (why was this approved/denied?)
- **1 hour faster** turnaround per request
- **Same-day escalation** (not lost in email)

---

## 3. Multi-Connector Orchestration: Query Across Your Entire Tech Stack

### The problem
To answer "What's the impact of this GitHub issue?", you need to:
- Find the GitHub issue
- Find related Jira tickets
- Check AWS CloudWatch logs
- Look up the customer in Salesforce
- Find Slack conversations with the customer
- Cross-reference Slack with tickets

Today: Manual tab-switching, 20 minutes of work.

### What IQ does
One SPARQL query reaches across all 6 systems simultaneously. IQ gets the data, correlates it, and gives you the answer.

### Example: Full incident investigation

```bash
cd iq-starter

# 1. Configure connectors (AWS, GitHub, Jira, Slack, Salesforce)
./bin/setup-connectors examples/connectors/orchestration/

# 2. Sync data once
./bin/sync-all-connectors

# 3. Run unified query
iq-query examples/queries/gh-issue-impact.sparql gh-issue-5678

Results (from SINGLE query across 6 systems):
GitHub Issue: gh-5678 - "Login fails for admin users"
  Status: open
  Assignee: bob@acme.com

Related Jira Tickets: 
  ACME-1234 (blocked on this issue)
  ACME-1235 (customer complaint)
  ACME-1236 (hotfix deployed)

CloudWatch Logs:
  Sep 28 12:35 ERROR in auth service (1,247 occurrences, 3.2% of logins)
  
Impacted Customer:
  Globex Corp - 500 admin users
  Contract: Enterprise, $50k/year
  Support tickets: 3 open, 1 critical

Slack Conversations:
  #incident-response: "is this a widespread issue?" (25 messages)
  @customer-globex: "When will this be fixed?" (8 messages)
```

### Files to explore
- `examples/connectors/orchestration/` — Connector configs
- `examples/queries/gh-issue-impact.sparql` — The unified query
- `.iq-templates/connectors/` — How to add new ones

### Business impact
- **5x faster incident response** (20 min → 4 min)
- **No context switching** (all info in one place)
- **Reduced escalations to engineers** (you can see the scope)

---

## 4. Trusted Identity & Multi-Tenant Isolation

### The problem
You have 50 customers. Each uses your IQ instance. If customer A's data leaks to customer B, that's a breach. Today, that risk is real because:
- Secrets (API keys) are scattered
- No clear audit trail of who accessed what
- Realms share indexes

### What IQ does
- Each realm is cryptographically isolated
- Secrets stored in a single vault
- Fine-grained role-based access control (RBAC)
- Audit log of every access

### Example: Multi-tenant SaaS with IQ

```bash
cd iq-starter

# 1. Initialize with secure realm setup
./bin/setup-realms examples/.iq-templates/multi-tenant/

# 2. Start API
./bin/start-api

# 3. Customer A calls with their token
curl -X POST http://localhost:8080/chat \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "realm": "customer-a",
    "message": "What is my revenue?"
  }'

Response:
{
  "response": "Your Q1 2024 revenue is $245,000",
  "realm": "customer-a",
  "token_usage": 45
}

# 4. The same request from Customer B returns their data
curl -X POST http://localhost:8080/chat \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d'{
    "realm": "customer-b",
    "message": "What is my revenue?"
  }'

Response:
{
  "response": "Your Q1 2024 revenue is $1,234,000",
  "realm": "customer-b",
  "token_usage": 47
}

# 5. Audit log shows everything
iq> audit-log --realm=customer-a
2024-03-31 12:34:56 alice@customer-a.com   GET   revenue   APPROVED
2024-03-31 12:35:12 bob@customer-a.com     GET   budget    DENIED (insufficient role)
2024-03-31 12:35:45 system@iq.com          SYNC  s3-bucket APPROVED
```

### Files to explore
- `.iq-templates/multi-tenant/` — Realm and role configuration
- `examples/queries/audit-access.sparql` — See who accessed what
- [docs/SECURITY.md](SECURITY.md) — Deep dive on vault and encryption

### Business impact
- **Zero data leakage risk** (cryptographic isolation)
- **Compliance-ready** (detailed audit logs for GDPR/HIPAA/SOC2)
- **Customer trust** (they know their data is safe)

---

## 5. LLM-Grounded Decision Making (Stop Hallucinations)

### The problem
You ask ChatGPT "What's our NPS score?" and it says "87%" because that sounds plausible. Your actual NPS is 64%. The LLM hallucinated.

Or you ask "Which customers churned last month?" and it makes up three names that don't exist in your database.

### What IQ does
The LLM asks IQ for facts, not guesses. IQ returns verified data. Response is grounded in reality.

### Example: Accurate business analytics

```bash
cd iq-starter

# 1. Load analytics knowledge
./bin/import-domain examples/domains/analytics/

# 2. Start MCP (for Claude)
./bin/start-mcp &

# 3. In Claude, ask a question

User: "What's our customer churn rate?"

Claude: [calls IQ tool] → get_metric("churn_rate", period="2024-Q1")
IQ: 
{
  "value": 4.2,
  "unit": "percent",
  "period": "2024-Q1", 
  "calculated_from": "customer_churn.ttl",
  "updated": "2024-03-30T23:59:00Z"
}

Claude: "Your customer churn rate for Q1 2024 is 4.2%, down 
from 5.1% in Q4 2023. The main drivers were improved onboarding 
and the new retention program."
```

No hallucination. Grounded in your actual data.

### Files to explore
- `examples/domains/analytics/metrics.ttl` — Your metrics as RDF
- `examples/queries/trending-metrics.sparql` — Time-series queries
- [docs/RUNTIMES.md#3-model-context-protocol](docs/RUNTIMES.md#3-model-context-protocol) — MCP integration

### Business impact
- **Accurate reporting** (no more made-up numbers)
- **Fast iteration** (LLM iterates on real data, not hunches)
- **Trusted AI** (you can show the source of every claim)

---

## Quick comparison

| Use Case | Setup time | Typical ROI | Best team |
|---|---|---|---|
| **RAG** | 1-2 days | 6 months | Support, Knowledge |
| **Workflows** | 2-3 weeks | 3 months | Operations, Procurement |
| **Multi-Connector** | 1-2 weeks | 2 months | Eng, Ops, Analytics |
| **Multi-Tenant** | 3-4 weeks | ongoing | Product, DevOps |
| **LLM Grounding** | 1 week | 3 months | Analytics, AI teams |

---

## How to pick your first use case

1. **Which problem hurts most?** (highest cost, most manual work)
2. **Which has the clearest ROI?** (most time saved)
3. **Which team is most excited?** (adoption is key)

**Our recommendation:** Start with **#1 (RAG)** or **#2 (Workflows)**. Both have fast ROI and clear business impact.

---

## Next steps

- **Build on use case #1:** [Ingestion Guide](docs/INGESTION.md)
- **Build on use case #2:** [Agent Setup](docs/AGENTS.md)
- **Build on use case #3:** [Connectors Guide](docs/CONNECTORS.md)
- **Build on use case #4:** [Security & Multi-Tenancy](docs/SECURITY.md)
- **Build on use case #5:** [MCP & Integration](docs/RUNTIMES.md#3-model-context-protocol)

