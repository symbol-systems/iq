# What is IQ? — A plain-language explanation

If you're new to IQ, start here. This document explains what IQ is, why it exists, and what problems it solves—**without jargon**.

## The problem IQ solves

You have:
- Data scattered across 10+ systems (databases, cloud platforms, APIs)
- AI models (GPT, Claude, etc.) that hallucinate without grounding
- Workflows that require human approval (procurement, support tickets, hiring)
- Secrets (API keys, passwords, tokens) that are scattered and hard to manage
- Team members (developers and non-developers) who need to understand what the AI decided

**The cost:**
- 6 months to build custom integration logic
- Constant rewrites when systems change
- Expensive hallucinations from LLMs ("Yes, we have 10,000 users" when you actually have 100)
- Manual handoffs and lost context in approval workflows

## What IQ does

Think of IQ as an **operating system for AI agents**. It provides:

### 1. **A unified knowledge layer**
Instead of each AI query reaching out to 10 different systems, IQ maintains a single "knowledge graph" (a structured fact database). Your AI queries this graph. You can now see exactly what facts the AI used to make a decision.

```
   Your systems (AWS, Slack, Snowflake, GitHub, Jira)
              ↓
        IQ Knowledge Graph (what the AI sees)
              ↓
         AI Models (grounded, accurate decisions)
```

### 2. **Stateful workflows**
Real-world processes have state: a procurement request moves from `pending` → `approved` → `ordered` → `received`. IQ tracks this state and only allows valid transitions. The AI doesn't just "respond to a message"—it **makes decisions that change state**.

```
Request: "approve this purchase order"
  ↓
IQ checks: Is this order in "pending" state? Is the requester authorized?
  ↓
If yes: Decision engine runs → state moves to "approved" → triggers actions (send email, bill account)
  ↓
Response: "✓ Purchase order approved. Email sent to procurement@..."
```

### 3. **Pluggable connectors**
IQ comes with connectors for 20+ systems. Plug one in, and IQ automatically:
- Reads from it (AWS S3, Slack channels, GitHub issues)
- Writes to it (create tickets, send messages, update databases)
- Transforms the data into its knowledge graph

No custom code. Just configuration.

### 4. **Multi-tenant isolation**
Each team, customer, or project gets its own "realm"—a private knowledge graph, secrets store, and rule set. Realms don't see each other's data. Great for SaaS and large organizations.

### 5. **Explainable decisions**
Every decision IQ makes (via an AI or a rule) is logged. You can see:
- What facts it used
- What decision it made
- What actions it triggered
- Whether a human approved it

### 6. **Token tracking & budget control**
LLMs are expensive. IQ tracks how many tokens you're using per request, per user, per month. Set budgets and have IQ enforce them.

---

## Three ways to use IQ

### 1. **REST API** (for apps and integrations)
Embed IQ into your web app, mobile app, or third-party system.

```bash
POST /chat
{
  "message": "What's the status of order #12345?",
  "realm": "acme-corp"
}

Response:
{
  "response": "Order #12345 is in shipped status. It left our warehouse on Mar 28 and should arrive Mar 31.",
  "grounded_in": ["Order fact from Shopify", "Shipping fact from FedEx"]
}
```

### 2. **Command-line (CLI)**
For operations teams, data engineers, you get an interactive shell to explore and manage your knowledge.

```bash
iq> search "customers from California"
→ Returns 342 customers

iq> run-query examples/queries/revenue-by-region.sparql
→ Executes SPARQL directly against your knowledge graph

iq> agent process-approval --ticket-id=5678
→ Runs a workflow to approve or deny a request
```

### 3. **LLM integration (MCP)**
Give your LLM (Claude, ChatGPT, local Llama) direct access to your enterprise data. When the LLM needs information, it asks IQ.

```
User: "Tell me about our biggest customer"
  ↓
LLM asks IQ: "Who is the customer with the highest lifetime value?"
  ↓
IQ consults: Shopify (revenue), Slack (support tickets), GitHub (feature requests)
  ↓
LLM responds: "Acme Corp, $2.3M revenue, open critical issue..."
```

---

## What IQ is NOT

| Don't confuse IQ with... | Why they're different |
|---|---|
| ChatGPT | IQ uses LLMs but grounds them in your data. You own the knowledge. |
| A database | IQ is a *knowledge graph* (relationships matter), not just rows and columns. |
| An ETL tool | IQ *ingests* data like ETL but also makes decisions and triggers actions. |
| A workflow engine | IQ has workflows *and* knowledge reasoning *and* connector control. |
| A Zapier clone | IQ connects systems but keeps them intelligently coordinated via a shared fact base. |

---

## The "knowledge graph" explained simply

A **knowledge graph** is structured data where relationships matter as much as facts.

**Traditional database (wrong for AI):**
```
Customer table:
  id | name | revenue
  1  | Acme | 2300000
```

The AI see "2300000" but doesn't know: is that revenue per year? Per month? In dollars? Did we already bill them?

**Knowledge graph (right for AI):**
```
acme-corp a Customer ;
  foaf:name "Acme Corp" ;
  schema:revenue [ 
    schema:price 2300000 ;
    schema:currency "USD" ;
    schema:period "year" ;
    schema:fromDate "2024-01-01" 
  ] ;
  schema:status "active" ;
  iq:lastBillingDate "2024-03-15" .
```

Now the AI knows the full context. No hallucinations.

---

## Five real use cases

### 1. **Knowledge Ingestion & Discovery**
Ingest PDFs, docs, databases → turn into a searchable, queryable knowledge graph.
- ~weeks of manual work → automation with IQ

### 2. **Agent Workflows with Approval**
"Request #123 is pending approval. Check: is requester authorized? Is budget available? Then move to approved state and send notifications." IQ handles the state machine and decision logic.

### 3. **Multi-Cloud & Multi-Connector Orchestration**
One SPARQL query across AWS + Azure + Slack + Snowflake. IQ keeps them in sync.

### 4. **Trusted Identity & Multi-Tenancy**
Each customer gets their own realm. Secrets stored in one vault. Fine-grained access control.

### 5. **LLM-Grounded Decision Making**
Don't let the LLM guess. "What's our NPS for 2024?" → LLM asks IQ for the survey data → grounded answer.

---

## Key takeaways

| Concept | What it means |
|---|---|
| **Knowledge Graph** | Your data + relationships, told to the AI in a way that prevents hallucinations |
| **Stateful Workflows** | Requests move through states (pending → approved → completed). IQ enforces valid transitions. |
| **Connector** | A plug-in that reads from or writes to an external system (Slack, AWS, GitHub, etc.). |
| **Realm** | A tenant's private knowledge graph, secrets store, and decision rules. Realms don't see each other. |
| **Agent** | An AI worker that makes decisions based on knowledge, follows workflows, and triggers actions. |
| **Grounded** | The AI's answer is based on your actual data, not its training. Much more accurate. |
| **Ingest** | The process of taking unstructured data (PDFs, HTML) and turning it into structured knowledge. |

---

## Next steps

- **I want IQ running in 5 minutes:** [QUICKSTART.md](QUICKSTART.md)
- **I want to understand the three runtimes:** [RUNTIMES.md](RUNTIMES.md)
- **I want to see real use cases:** [USECASES.md](USECASES.md)
- **I want to add my own data:** [Connector Setup](CONNECTORS.md)
- **I need to deploy this:** [Docker & Cloud](DOCKER.md)

---

**Convinced?** Go to [QUICKSTART.md](QUICKSTART.md) 🚀
