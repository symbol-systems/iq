# Examples Directory

This folder contains working examples for different use cases.

## Quick reference

| Folder | What it is | Start here |
|---|---|---|
| `domains/` | RDF knowledge domains (customers, orders, etc.) | [domains/README.md](domains/README.md) |
| `queries/` | SPARQL query examples | [queries/README.md](queries/README.md) |
| `connectors/` | Connector configurations (Slack, AWS, GitHub, etc.) | [connectors/README.md](connectors/README.md) |
| `agents/` | Workflow definitions (approval, routing, etc.) | [agents/README.md](agents/README.md) |

## How to use examples

All examples are designed to be **copy-paste ready**. Pick one, follow the instructions, and adapt to your needs.

### Example: Use the purchase order approval workflow

1. **Read the definition:** `agents/purchase-order-approval.ttl`
2. **Import it:** 
   ```bash
   ./bin/import-example  # Or manually load the .ttl file
   ```
3. **Trigger it:**
   ```bash
   curl -X POST http://localhost:8080/agent/trigger \
     -d '{"workflow":"PurchaseOrderWorkflow","po_id":"PO-123","requester":"alice","amount":15000}'
   ```
4. **View the result:** Check the JSON response with decision trace

### Example: Query your knowledge graph

1. **Write a SPARQL query:** See `queries/customers.sparql`
2. **Run it:**
   ```bash
   ./bin/demo-query queries/customers.sparql
   ```
3. **Adapt it:** Modify the query for your data

### Example: Connect a connector

1. **Pick a connector:** `connectors/slack-config.ttl`
2. **Set credentials:** `export SLACK_BOT_TOKEN=xoxb-...`
3. **Load the config:** `./bin/setup-connectors connectors/`
4. **Sync data:** `iq> connector sync slack-connector`

## Contents

### domains/
Knowledge domain definitions (RDF Turtle format).

- `customers.ttl` — Customer entities, metadata, segmentation
- `orders.ttl` — Order/transaction definitions
- `employees.ttl` — (example) Employee, org structure
- `products.ttl` — (example) Product catalog

Use these as templates to define your own domain.

### queries/
SPARQL queries you can run immediately.

- `00-hello.sparql` — "Hello world" (list first 5 facts)
- `customers.sparql` — Find all customers
- `orders-by-customer.sparql` — Orders for a specific customer
- `top-revenue.sparql` — Customers by revenue

### connectors/
Configuration for each connector (copy-paste ready).

- `slack-config.ttl` — Slack connector setup
- `github-config.ttl` — GitHub connector setup
- `aws-config.ttl` — AWS connector setup
- `postgres-config.ttl` — SQL database connector setup

Edit to match your system. Set environment variables with credentials.

### agents/
Workflow definitions (state machines + decision rules).

- `purchase-order-approval.ttl` — PO approval workflow
- `customer-support-routing.ttl` — Route support tickets
- `incident-response.ttl` — Multi-step incident handling

Each includes:
- State machine (what states exist, what transitions are allowed)
- Decision rules (if X, then do Y)
- Actions (what happens when state changes)

---

## How to build your own

### 1. Define a domain (RDF)

Create `my-domain.ttl`:

```turtle
@prefix ex: <http://example.com/> .

# Define an entity type
ex:Customer a rdfs:Class ;
  rdfs:label "A customer" ;
  rdfs:comment "Represents a customer account" .

# Add a specific instance
ex:alice-corp
  a ex:Customer ;
  ex:name "Alice Corporation" ;
  ex:revenue 2300000 ;
  ex:industry "Technology" .
```

Save to `domains/my-domain.ttl`, then import.

### 2. Write a query (SPARQL)

Create `my-query.sparql`:

```sparql
PREFIX ex: <http://example.com/>

SELECT ?customer ?revenue
WHERE {
  ?customer a ex:Customer ;
    ex:revenue ?revenue .
  FILTER (?revenue > 1000000)
}
ORDER BY DESC(?revenue)
```

Save to `queries/my-query.sparql`, then run with `./bin/demo-query`.

### 3. Connect a system (Connector config)

Create `my-connector.ttl`:

```turtle
@prefix: ex: <http://example.com/connectors/> .
@prefix iq: <http://iq.systems/> .

ex:my-connector
  a iq:Connector ;
  iq:name "my-connector" ;
  iq:connectorType "slack" ;
  iq:enabled true ;
  iq:credentialSource "env:MY_API_KEY" .
```

Save to `connectors/my-connector.ttl`, set env vars, then use.

### 4. Define a workflow (Agent)

Create `my-workflow.ttl`:

```turtle
@prefix ex: <http://example.com/workflows/> .
@prefix iq: <http://iq.systems/> .

ex:MyWorkflow
  a iq:StateMachine ;
  iq:initialState ex:pending ;
  iq:states ( ex:pending ex:approved ex:completed ) ;
  iq:transitions (
    [ iq:from ex:pending ; iq:to ex:approved ]
    [ iq:from ex:approved ; iq:to ex:completed ]
  ) .

ex:my-rule
  a iq:Rule ;
  iq:condition "SELECT ?obj WHERE { ?obj ex:priority 'high' }" ;
  iq:action ex:approve-action ;
  iq:priority 10 .
```

Save to `agents/my-workflow.ttl`, load it, then trigger.

---

## Complete workflow: Build and deploy

```bash
# 1. Create your domain
cat > domains/my-app.ttl << 'EOF'
@prefix ex: <http://example.com/my-app/> .
ex:user-1 ex:name "Alice" .
EOF

# 2. Create a query
cat > queries/my-app.sparql << 'EOF'
PREFIX ex: <http://example.com/my-app/>
SELECT ?user WHERE { ?user ex:name ?name . }
EOF

# 3. Load and test
./bin/start-api &
./bin/demo-query queries/my-app.sparql

# 4. Deploy to cloud
./bin/build-image
docker run -p 8080:8080 iq-api:latest
```

---

## Need help?

- **Query syntax:** [SPARQL Tutorial](https://www.w3.org/2001/sw/rdf/)
- **RDF format:** [Turtle Spec](https://www.w3.org/TR/turtle/)
- **Workflow design:** [AGENTS.md](../docs/AGENTS.md)
- **Troubleshooting:** [FAQ.md](../docs/FAQ.md)

---

**Next:** Pick a use case and start adapting these examples!
