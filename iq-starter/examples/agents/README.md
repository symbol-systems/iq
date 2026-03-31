# /agents — Workflow definitions

This folder contains workflow definitions: state machines, decision rules, and actions.

## What goes here

Define multi-step processes:

```turtle
# State machine
ex:ApprovalWorkflow
  iq:states (pending approved rejected) ;
  iq:transitions (
    [iq:from pending ; iq:to approved] ;
    [iq:from approved ; iq:to completed]
  ) .

# Rules
ex:auto-approve
  iq:condition "amount < 5000 AND role = manager" ;
  iq:action ex:approve-action ;
  iq:priority 10 .

# Actions
ex:approve-action
  iq:onState approved ;
  iq:triggers (send-email bill-account create-ticket) .
```

## Files included

- `purchase-order-approval.ttl` — PO approval workflow (complete example)

## How to use

1. **Load the workflow:**
   ```bash
   ./bin/import-example
   # Or manually: iq> import-file agents/purchase-order-approval.ttl
   ```

2. **Trigger it:**
   ```bash
   curl -X POST http://localhost:8080/agent/trigger \
     -d '{"workflow":"PurchaseOrderWorkflow","po_id":"PO-123",...}'
   ```

3. **View the result:**
   ```json
   {
     "final_state": "approved",
     "decision_trace": [...],
     "actions_executed": [...]
   }
   ```

## Components of a workflow

### 1. State Machine

Define valid states and transitions:

```turtle
:MyWorkflow a iq:StateMachine ;
  iq:states (:pending :approved :completed) ;
  iq:transitions (
    [ iq:from :pending ; iq:to :approved ]
  ) .
```

**Enforces:** Can't skip to `:completed` without going through `:approved`.

### 2. Decision Rules

Evaluate conditions and pick actions:

```turtle
:rule-1 a iq:Rule ;
  iq:condition "?order :amount ?a . FILTER (?a < 5000)" ;
  iq:action :approve ;
  iq:priority 10 .
```

**Priority:** Higher numbers evaluated first. First match wins.

### 3. Actions

What happens when state changes:

```turtle
:approve a iq:Action ;
  iq:onState :approved ;
  iq:triggers (
    :send-email
    :create-ticket
    :post-slack
  ) .
```

## Writing your own workflow

Step-by-step:

1. **Identify states** — What states does this workflow have?
   ```
   pending → approved → completed
                    ↓
                  rejected
   ```

2. **Define rules** — When should we transition?
   ```
   If amount < $5k AND requester is manager → approve
   If amount > $100k → escalate
   If vendor not approved → reject
   ```

3. **Define actions** — What happens on each state?
   ```
   On approved:  send email, bill account, create ticket
   On rejected:  send email, log reason
   On escalated: notify supervisor, create urgent ticket
   ```

4. **Encode in RDF:**

```turtle
@prefix ex: <http://example.com/workflows/> .
@prefix iq: <http://iq.systems/> .

ex:MyWorkflow a iq:StateMachine ;
  iq:initialState ex:pending ;
  iq:states (ex:pending ex:approved ex:rejected) ;
  iq:transitions (...) .

ex:rule-approve a iq:Rule ;
  iq:condition "your-sparql-condition" ;
  iq:action ex:approve ;
  iq:priority 10 .

ex:approve a iq:Action ;
  iq:onState ex:approved ;
  iq:triggers (...) .
```

5. **Load and test:**

```bash
./bin/import-example agents/my-workflow.ttl
iq> agent trace my-request
```

## Tips

- **Start simple:** 1-2 states, 1-2 rules (avoid combinatorial explosion)
- **Test in isolation:** Test rules before building full workflow
- **Use meaningful names:** `rule-auto-approve-small` is better than `r1`
- **Document priorities:** Why is this rule priority 5 not 10?
- **Dry runs:** Simulate triggers before enabling for real

## Common workflows

**Approval:**
```
pending → in-review → approved → completed
                  ↓
               rejected
```

**Support Routing:**
```
new → assigned → in-progress → resolved
         ↓
      escalated
```

**Deployment:**
```
pending → review → staging → production → complete
            ↓
         rejected
```

---

**Next:** Test workflows with the REST API. See [../docs/AGENTS.md](../docs/AGENTS.md)
