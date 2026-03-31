# AGENTS — Building intelligent workflows

Agents are the decision-making workers in IQ. They take input, consult knowledge rules, decide what to do, and trigger actions—all logged and auditable.

## What's an agent?

An agent is a combination of three things:

1. **Knowledge Rules** (RDF/SPARQL) — the logic for decisions
2. **State Machine** (FSM) — valid state transitions and constraints
3. **Actions** (Connectors, APIs, webhooks) — what happens after a decision

When you trigger an agent, it:
1. Reads the current state
2. Evaluates rules against your knowledge
3. Decides what to do
4. Transitions to a new state
5. Executes actions
6. Logs everything for audit

## Simple example: Approval workflow

### Define the state machine

File: `examples/agents/workflows/approval.ttl`

```turtle
@prefix : <http://example.com/approval/> .
@prefix iq: <http://iq.systems/> .

:ApprovalWorkflow a iq:StateMachine ;
  iq:initialState :pending ;
  iq:states (
    :pending
    :approved
    :rejected
    :escalated
  ) ;
  iq:transitions (
    [ iq:from :pending ; iq:to :approved ; iq:label "approve" ]
    [ iq:from :pending ; iq:to :rejected ; iq:label "reject" ]
    [ iq:from :pending ; iq:to :escalated ; iq:label "escalate" ]
  ) .
```

### Define decision rules

File: `examples/agents/workflows/approval-rules.ttl`

```turtle
@prefix : <http://example.com/approval/> .
@prefix iq: <http://iq.systems/> .
@prefix schema: <https://schema.org/> .

# Rule 1: Auto-approve if amount < $5k AND requester is manager
:auto-approve-small a iq:Rule ;
  iq:condition [ 
    iq:and (
      [ iq:query "?request schema:price ?price . FILTER (?price < 5000)" ]
      [ iq:query "?requester schema:role ?role . FILTER (?role = 'manager')" ]
    )
  ] ;
  iq:action :approve ;
  iq:priority 10 .

# Rule 2: Escalate if amount > $100k
:escalate-large a iq:Rule ;
  iq:condition [
    iq:query "?request schema:price ?price . FILTER (?price > 100000)"
  ] ;
  iq:action :escalate ;
  iq:priority 5 .

# Rule 3: Reject if vendor not in approved list
:reject-unapproved-vendor a iq:Rule ;
  iq:condition [
    iq:query """
      ?request schema:vendor ?vendor .
      FILTER NOT EXISTS { ?vendor iq:approved true }
    """
  ] ;
  iq:action :reject ;
  iq:priority 1 .
```

### Define side effects (actions)

File: `examples/agents/workflows/approval-actions.ttl`

```turtle
@prefix : <http://example.com/approval/> .
@prefix iq: <http://iq.systems/> .

:approve-action a iq:Action ;
  iq:onState :approved ;
  iq:triggers (
    [ iq:action :send-email-to-requester ; iq:template "templates/approved.html" ]
    [ iq:action :send-email-to-procurement ; iq:template "templates/notify-procurement.html" ]
    [ iq:action :bill-account ; iq:from "project-budget" ]
    [ iq:action :create-jira-ticket ; iq:project "ACME" ; iq:type "Procurement" ]
  ) .

:reject-action a iq:Action ;
  iq:onState :rejected ;
  iq:triggers (
    [ iq:action :send-email-to-requester ; iq:template "templates/rejected.html" ; iq:include-reason true ]
  ) .

:escalate-action a iq:Action ;
  iq:onState :escalated ;
  iq:triggers (
    [ iq:action :slack-post ; iq:channel "#approvals" ; iq:mentions "@finance-lead" ]
  ) .
```

### Trigger the agent

```bash
curl -X POST http://localhost:8080/agent/trigger \
  -d '{
    "workflow": "ApprovalWorkflow",
    "request_id": "po-98765",
    "requester": "alice@acme.com",
    "vendor": "vendor-abc",
    "amount": 12000
  }'
```

### Response

```json
{
  "request_id": "po-98765",
  "initial_state": "pending",
  "evaluation": {
    "rule_auto_approve_small": {
      "condition_passed": true,
      "details": "Amount $12,000 < $5,000 threshold? NO, but requester is manager, so HIGH priority"
    },
    "rule_escalate_large": {
      "condition_passed": false,
      "details": "Amount $12,000 > $100,000? NO"
    },
    "rule_reject_unapproved_vendor": {
      "condition_passed": false,
      "details": "Vendor vendor-abc IS in approved vendor list"
    }
  },
  "final_state": "approved",
  "decision_reasoning": "Applied rule auto_approve_small (priority 10)",
  "actions_executed": [
    {
      "action": "send-email-to-requester",
      "status": "success",
      "details": "Email sent to alice@acme.com"
    },
    {
      "action": "send-email-to-procurement",
      "status": "success",
      "details": "Email sent to procurement@acme.com"
    },
    {
      "action": "bill-account",
      "status": "success",
      "details": "Charged $12,000 to project-budget"
    },
    {
      "action": "create-jira-ticket",
      "status": "success",
      "details": "Created ACME-5432"
    }
  ],
  "timestamp": "2024-03-31T12:34:56Z",
  "execution_time_ms": 234
}
```

---

## Real-world example: Customer support routing

### Scenario
When a support ticket arrives, route it to the right team based on:
- Severity (escalate critical to senior)
- Topic (route to domain expert)
- Customer value (VIP gets priority)
- Workload (distribute evenly)

### State machine

```turtle
@prefix support: <http://acme.com/support/> .
@prefix iq: <http://iq.systems/> .

support:RoutingWorkflow a iq:StateMachine ;
  iq:states (
    support:new
    support:assigned
    support:in-progress
    support:waiting-customer
    support:resolved
    support:escalated
  ) ;
  iq:transitions (
    [ iq:from support:new ; iq:to support:assigned ]
    [ iq:from support:new ; iq:to support:escalated ]
    [ iq:from support:assigned ; iq:to support:in-progress ]
    [ iq:from support:in-progress ; iq:to support:waiting-customer ]
    [ iq:from support:waiting-customer ; iq:to support:in-progress ]
    [ iq:from support:in-progress ; iq:to support:resolved ]
  ) .
```

### Routing rules

```turtle
support:route-critical a iq:Rule ;
  iq:condition "?ticket schema:severity 'critical'" ;
  iq:action support:escalate-to-senior ;
  iq:priority 100 .

support:route-by-topic a iq:Rule ;
  iq:condition "?ticket schema:topic ?topic . ?expert schema:expertise ?topic" ;
  iq:action support:assign-to-expert ;
  iq:priority 50 .

support:prioritize-vip a iq:Rule ;
  iq:condition "?ticket schema:customer ?c . ?c iq:tier 'vip'" ;
  iq:action support:vip-priority-queue ;
  iq:priority 75 .

support:load-balance a iq:Rule ;
  iq:condition "?candidate rdf:type support:TeamMember . FILTER (?candidate has fewest open tickets)" ;
  iq:action support:assign-to-least-busy ;
  iq:priority 25 .
```

### Actions

```turtle
support:escalate-to-senior a iq:Action ;
  iq:onState support:escalated ;
  iq:triggers (
    [ iq:action support:notify-on-call-senior ; iq:channel "slack" ]
    [ iq:action support:create-urgent-ticket ; iq:label "CRITICAL" ]
  ) .

support:assign-to-expert a iq:Action ;
  iq:onState support:assigned ;
  iq:triggers (
    [ iq:action support:email-assignment ; iq:include-context true ]
    [ iq:action support:start-timer ; iq:response-sla "4-hours" ]
  ) .

support:vip-priority-queue a iq:Action ;
  iq:queue-position "top" ;
  iq:auto-escalate-on-delay "2-hours" .
```

### Trigger

```bash
./bin/demo-agent process-support-ticket \
  --ticket-id=SUP-12345 \
  --severity=critical \
  --topic=billing \
  --customer=acme-corp
```

Result:
```
Ticket SUP-12345 routed to Sarah (Billing Expert)
- Status: assigned
- SLA: 4 hours response
- Escalation: auto-escalate to Senior Support Manager if no response in 2 hours
- Customer: Acme Corp (VIP, $2.3M/year) → receives priority handling
```

---

## Advanced: Multi-step workflows with waiting

Sometimes an agent needs to stop and wait for human input or an external event.

```turtle
:purchase-approval a iq:StateMachine ;
  iq:states (
    :pending
    :manager-review       # ← Agent pauses here
    :finance-approval     # ← Agent pauses here
    :vendor-order         # ← Agent pauses here
    :completed
  ) ;
  iq:waitOnState [
    iq:state :manager-review ;
    iq:waitFor :manager-decision ;
    iq:timeout "24 hours" ;
    iq:onTimeout :escalate-to-director
  ] .
```

The agent:
1. Checks approval rules
2. If needs manager review, transitions to `:manager-review` and stops
3. Sends notification to manager
4. Waits for `:manager-decision` event
5. When manager decides, continues to next state
6. If no decision in 24 hours, escalates automatically

---

## Monitoring agents

### View agent status

```bash
iq> agent list
iq> agent status ApprovalWorkflow
iq> agent history ApprovalWorkflow --limit 20
```

### Trace a specific decision

```bash
iq> agent trace po-98765

Output:
Workflow: ApprovalWorkflow
Request: po-98765
Initial State: pending

Evaluation:
  ✓ Rule auto-approve-small: MATCHED
    - Amount $12,000 < $5,000 threshold? NO
    - Requester role is 'manager'? YES
    - Priority: 10

  ✗ Rule escalate-large: NOT matched
    - Amount $12,000 > $100,000? NO

  ✗ Rule reject-unapproved-vendor: NOT matched
    - Vendor vendor-abc is in approved list

Winner: auto-approve-small (priority 10)
Final State: approved
Actions Executed: 4
  - send-email-to-requester [success]
  - send-email-to-procurement [success]
  - bill-account [success]
  - create-jira-ticket [success]

Duration: 234ms
Timestamp: 2024-03-31T12:34:56Z
```

### Query recent decisions

```bash
iq> query examples/queries/agent-decisions-today.sparql

Result:
Time | Workflow | Request | Decision | Reason | Duration
-----|----------|---------|----------|--------|----------
12:34 | ApprovalWorkflow | po-98765 | approved | auto-approve-small | 234ms
12:28 | ApprovalWorkflow | po-98764 | escalated | escalate-large | 512ms
12:15 | SupportRouter | SUP-12345 | escalated | critical-severity | 89ms
```

---

## Troubleshooting agents

| Problem | Solution |
|---|---|
| "Agent not found" | Check agent name in workflow file |
| "Condition doesn't match" | Test rule in isolation: `iq> query rules.sparql` |
| "Action failed" | Check connector status: `iq> connector logs slack-connector` |
| "State transition not allowed" | Check FSM definition allows this transition |
| "Timeout before completion" | Agent is waiting for external event. Check status. |

---

## Next steps

- **See real use cases:** [USECASES.md](USECASES.md#2-agent-workflows-multi-step-decision-making-with-state-tracking)
- **Examples:** `examples/agents/` in the starter kit
- **Deploy:** [Docker & Cloud](DOCKER.md)

