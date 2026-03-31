# Architecture — How IQ works (technical overview)

IQ is built on three core layers: Knowledge, Reasoning, and Integration.

## Layer 1: Knowledge Graph

The foundation. All facts about your business live here.

```
┌─────────────────────────────────┐
│    RDF Triples (Facts)          │
├─────────────────────────────────┤
│ Customer(Alice) | Name | "ACME" │
│ Customer(Bob) | Revenue | $2.3M │
│ Order(123) | Status | "active"  │
│ ... millions of facts ...       │
└─────────────────────────────────┘
           ↓ (SPARQL queries)
      Index Layer
        (Vector, BM25, Graph)
```

### Storage

**Development (default):** In-memory RDF store (fast, no setup)

**Production:** Persistent PostgreSQL backend

```bash
# Switch to PostgreSQL
export DATABASE_URL=postgresql://...
./bin/start-api
```

### Format

Facts are stored as **RDF triples** (subject-predicate-object):

```turtle
alice a Customer ;
  name "ACME Corp" ;
  revenue 2300000 ;
  status "active" .
```

This is equivalent to three facts:
1. `alice` is a `Customer`
2. `alice` has `name` "ACME Corp"
3. `alice` has `revenue` 2300000

### Querying

Query the knowledge graph with **SPARQL** (not SQL):

```sparql
SELECT ?customer ?name ?revenue WHERE {
  ?customer a Customer ;
    name ?name ;
    revenue ?revenue .
  FILTER (?revenue > 1000000)
}
```

Result: All customers with revenue > $1M.

---

## Layer 2: Reasoning Engine

Where decisions happen.

```
    Knowledge Graph
         ↓
  ┌─────────────┐
  │  Rules      │  (if-then logic, priorities)
  │  Policies   │  (business constraints)
  │  Workflows  │  (state machines)
  └─────────────┘
         ↓
  ┌─────────────────────┐
  │ Decision Engine     │
  │ (evaluates rules)   │
  └─────────────────────┘
         ↓
  Action: approve/deny/escalate
```

### Rules

Define decision logic in RDF:

```turtle
rule-auto-approve-small
  a Rule ;
  condition: amount < $5000 AND requester is manager ;
  action: approve ;
  priority: 10 .
```

The decision engine evaluates rules in priority order and picks the first match.

### State Machines

Enforce valid workflows:

```
      pending
      /  |  \
    /    |    \
  approve reject escalate
   |      |       |
 approved rejected escalated
```

Only allowed transitions are executed. Prevents invalid states.

### Polling & Webhooks

Instead of constant querying, workflows can wait for events:

```turtle
wait-for-human-approval
  onState: awaiting-manager ;
  waitFor: manager-decision ;
  timeout: 24h ;
  onTimeout: escalate-to-director .
```

---

## Layer 3: Integration Layer

Connect to the outside world.

```
         IQ Core (Knowledge + Reasoning)
         /    |      \      \      \
       /      |       \      \      \
    Slack   GitHub   AWS    Jira  Snowflake
    ↓        ↓        ↓      ↓      ↓
Connectors (read, transform, write)
```

### Connector Lifecycle

1. **Read Phase** → Pull data from external system into knowledge graph
2. **Reasoning Phase** → Make decisions based on knowledge
3. **Write Phase** → Push decisions back out to external systems

```
AWS S3 Bucket
   ↓ (read)
[CSV → RDF]
   ↓
Knowledge Graph
   ↓
Rule Engine decides: "order this item"
   ↓ (write)
[RDF → JSON API call]
   ↓
Slack Message: "Item ordered"
```

### Available Connectors

| Category | Systems |
|---|---|
| Cloud | AWS, Azure, GCP, DigitalOcean |
| SaaS | Slack, GitHub, Jira, Salesforce, Stripe, Datadog |
| Data | PostgreSQL, Snowflake, Databricks, Parquet |
| Containers | Kubernetes, Docker |
| Messaging | Kafka |

---

## Complete Example: Approval Workflow

Shows all three layers working together.

```
┌────────────────────────────────────────────┐
│ Layer 1: Knowledge Graph                   │
├────────────────────────────────────────────┤
│ PO-12345 | status | pending                │
│ PO-12345 | amount | 15000                  │
│ bob | role | manager                       │
│ vendors/acme | approved | true             │
└────────────────────────────────────────────┘
            ↓ (agent triggered)
┌────────────────────────────────────────────┐
│ Layer 2: Reasoning                         │
├────────────────────────────────────────────┤
│ Rule 1: If amount < $5k AND manager        │
│         → APPROVE                          │
│                                            │
│ Rule 2: If amount > $100k                  │
│         → ESCALATE                         │
│                                            │
│ Winner: Rule 1 (priority 10)               │
│ Decision: APPROVE                          │
└────────────────────────────────────────────┘
            ↓ (action triggered)
┌────────────────────────────────────────────┐
│ Layer 3: Integration                       │
├────────────────────────────────────────────┤
│ Action 1: Send email to bob                │
│ Action 2: Update budget tracker            │
│ Action 3: Create Jira ticket               │
│ Action 4: Post to #procurement in Slack    │
└────────────────────────────────────────────┘
            ↓ (state transition)
        PO-12345 status → "approved"
```

---

## Three runtime entry points

All three share the same knowledge graph but expose different interfaces:

### 1. REST API (for apps)
```
Client (web/mobile/service) 
  → HTTP POST /chat 
    → IQ REST Server 
      → Knowledge Graph
      → LLM (optional)
  → JSON response
```

**Best for:** Building user interfaces, integrations, microservices.

### 2. CLI (for humans & scripts)
```
User  
  → iq> query my-query.sparql
    → IQ CLI 
      → Knowledge Graph
  → Results (table/CSV/JSON)
```

**Best for:** Exploration, ad-hoc analysis, batch jobs.

### 3. MCP (for LLMs)
```
Claude/ChatGPT
  → "What is our revenue?"
    → MCP Tool Call
      → IQ Reasoning
        → Knowledge Graph
  → LLM responds with grounded answer
```

**Best for:** Giving LLMs access to enterprise data.

---

## Data flow: End-to-end example

You want to ask: **"Which customers are at risk of churn?"**

```
User → REST API
  ↓
POST /chat {message: "at risk customers"}
  ↓
Chat Handler
  ├─ Parses question
  ├─ Decides intent: discover_at_risk_customers
  ├─ Constructs SPARQL query
  └─ Calls Knowledge Graph
       ↓
  Query: SELECT ?customer WHERE {
           ?customer a Customer ;
             churnScore ?score .
           FILTER (?score > 0.7)
         }
       ↓
  Index (optimized search) 
       ↓
  Results: [acme-corp, beta-llc, gamma-co]
       ↓
  Calls LLM (GPT, Groq, local)
  "Here are customers at risk... Acme Corp 
   has had 5 support tickets this week..."
       ↓
JSON Response → Client
```

---

## Performance layers

IQ uses multiple index strategies for fast queries:

```
Knowledge Graph (RDF Triples)
    ↓
┌─────────────────────────────────┐
│ Indexes (choose at query time)  │
├─────────────────────────────────┤
│ Vector Index      (semantic)    │
│ BM25 Index        (keyword)     │
│ Graph Index       (relationships)│
│ Full-text Index   (text search) │
└─────────────────────────────────┘
    ↓
Results (milliseconds)
```

### Examples

- **Vector:** "Find similar customers to Acme Corp"
- **BM25:** "Find customers with 'cloud' or 'AI' in description"
- **Graph:** "Find all vendors that supply customers in California"
- **Full-text:** "Find orders containing 'urgent'"

---

## Multi-tenancy

Realms isolate knowledge and secrets:

```
IQ Instance
├── Realm: customer-a
│   ├── Knowledge Graph (isolated)
│   ├── Secrets Vault (isolated)
│   └── Rules (isolated)
│
├── Realm: customer-b
│   ├── Knowledge Graph (isolated)
│   ├── Secrets Vault (isolated)
│   └── Rules (isolated)
│
└── Realm: customer-c
    ├── Knowledge Graph (isolated)
    ├── Secrets Vault (isolated)
    └── Rules (isolated)
```

**No customer sees another's data.** Realms don't share indexes or storage.

Authentication: JWT tokens include realm.

```bash
curl -H "Authorization: Bearer token-for-customer-a" \
  http://localhost:8080/chat

# Returns only customer-a's data
```

---

## Connector architecture

Each connector is a module that implements:

```java
interface IQConnector {
  void read() ;    // Pull external → knowledge
  void write() ;   // Push knowledge → external
  void sync() ;    // read() on schedule
}
```

Example: Slack connector

```java
SlackConnector implements IQConnector {
  read() {
    // Fetch messages from #engineering
    // Transform to RDF facts
    // Store in knowledge graph
  }
  
  write() {
    // Read pending actions from knowledge
    // Format Slack message
    // POST to Slack API
  }
}
```

---

## Scalability

### Single instance
- 1-10GB knowledge graph
- 50-100 req/sec
- Suitable for: Teams, proof-of-concepts

### Horizontal scaling (Kubernetes)
```
Load Balancer
  ├─ IQ Pod 1 (shares same knowledge graph)
  ├─ IQ Pod 2 (shares same knowledge graph)
  └─ IQ Pod 3 (shares same knowledge graph)
    ↓
PostgreSQL Database (shared)
```

- 10+ pods
- Shared PostgreSQL backend
- 500+req/sec
- Suitable for: Production SaaS

### Multi-realm sharding
Different knowledge graphs for different data:

```
IQ Cluster
├─ Instance 1: Realms A, B, C
├─ Instance 2: Realms D, E, F
└─ Instance 3: Realms G, H, I
```

Scales to thousands of realms.

---

## Security model

### Authentication
- **JWT tokens** — stateless, can be cached
- **Realm isolation** — each token bound to a realm
- **API keys** — for programmatic access

### Secrets management
- **Vault** — encrypted storage for API keys, passwords
- **Environment variables** — for development
- **Secrets manager** — AWS Secrets Manager, Azure Key Vault, etc.

### Audit logging
Every action is logged:
- **Who** did it
- **What** they did
- **When** they did it
- **Why** (decision reasoning)
- **Result** (approved/denied/error)

---

## Architecture diagram

```
                        ┌─────────────────┐
                        │  Users / Apps   │
                        └────────┬────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              ┌─────▼──────┐ ┌──▼────────┐ ┌─▼──────────┐
              │  REST API  │ │    CLI    │ │   MCP      │
              └─────┬──────┘ └──┬────────┘ └─┬──────────┘
                    │            │            │
                    └────────────┼────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │ Chat/Agent/Query        │
                    │ Handlers                │
                    └────────────┬────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
        ┌───────▼────────┐ ┌────▼──────┐ ┌──────▼─────┐
        │ LLM Integration│ │   Rules   │ │State       │
        │ (GPT, Groq,   │ │  Engine   │ │Machines   │
        │  Local)       │ │           │ │            │
        └────────────────┘ └────┬──────┘ └────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  Knowledge Graph (RDF)  │
                    │  (PostgreSQL or Memory) │
                    └────────────┬────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌───────▼────┐
        │  Connectors  │ │   Indexes   │ │   Vault    │
        │              │ │ (Vector,    │ │ (Secrets)  │
        │ AWS/Slack/   │ │  BM25,      │ │            │
        │ GitHub/etc   │ │  Graph)     │ │            │
        └──────────────┘ └─────────────┘ └────────────┘
                │
        ┌───────▼──────────────┐
        │  External Systems    │
        │                      │
        │ AWS, Slack, GitHub,  │
        │ Jira, Salesforce,    │
        │ Databases, etc.      │
        └──────────────────────┘
```

---

## Next steps

- **Understand the runtimes:** [RUNTIMES.md](RUNTIMES.md)
- **Build workflows:** [AGENTS.md](AGENTS.md)
- **Deploy:** [DOCKER.md](DOCKER.md)
