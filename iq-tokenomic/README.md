# iq-tokenomic — Token Cost Tracking and Budget Enforcement

`iq-tokenomic` manages the economics of LLM usage within IQ. It tracks how many tokens agent runs consume, enforces spending budgets, and provides the accounting layer that keeps AI costs observable and controllable.

## What it provides

- **Budget** — defines a spending limit for a given agent run or realm, expressed in token units
- **Treasury** — manages multiple budgets and allocates token allowances across concurrent agent operations
- **Funded / I_Fund** — marks agent components and intent executions as budget-aware, so costs are tracked through the full execution chain
- **BudgetException** — raised when an operation would exceed its allocated budget, cleanly halting execution before overspending

## Role in the system

`iq-tokenomic` is wired into `iq-agentic`. When an agent is constructed with a budget, every LLM call deducts from that budget. Exceeding the limit raises a `BudgetException` that the agent lifecycle handles gracefully.

This is particularly useful in multi-tenant or production deployments where you need to ensure no single agent run consumes unbounded LLM resources.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-tokenomic -am compile`
