# iq-connect — Connector Library

`iq-connect` is the family of integration adapters that connect IQ to the outside world. Each connector is a self-contained module that brings data from an external system into IQ's knowledge graph — or pushes IQ's knowledge out to that system.

Connectors follow a uniform pattern: they have a lifecycle, a local graph representing their current state, and a set of operations (read, write, or both). This consistency means you can reason over data from completely different sources using the same query language.

## Connector status

| Connector | Status | What it connects |
|---|---|---|
| `iq-connect-core` | **Active** | Shared base for all connectors |
| `iq-connect-template` | **Active** | Starting point for building a new connector |
| `iq-connect-aws` | **Active** | Amazon Web Services — S3, EC2, IAM, and more |
| `iq-connect-github` | **Active** | GitHub repositories, issues, and pull requests |
| `iq-connect-azure` | Planned | Microsoft Azure resources and services |
| `iq-connect-gcp` | Planned | Google Cloud Platform |
| `iq-connect-digitalocean` | Planned | DigitalOcean infrastructure |
| `iq-connect-confluence` | Planned | Confluence spaces and pages |
| `iq-connect-slack` | Planned | Slack workspaces and channels |
| `iq-connect-office-365` | Planned | Microsoft 365 — mail, calendar, and files |
| `iq-connect-google-apps` | Planned | Google Workspace |
| `iq-connect-snowflake` | Planned | Snowflake data warehouse |
| `iq-connect-databricks` | Planned | Databricks lakehouse |
| `iq-connect-parquet` | Planned | Parquet files and columnar data |
| `iq-connect-k8s` | Planned | Kubernetes clusters and workloads |
| `iq-connect-docker` | Planned | Docker images and container state |
| `iq-connect-datadog` | Planned | Datadog metrics and monitors |
| `iq-connect-salesforce` | Planned | Salesforce CRM objects and flows |
| `iq-connect-stripe` | Planned | Stripe payments and subscriptions |

**Status key:**
- **Active** — buildable Maven module with implementation and tests
- **Planned** — directory scaffold exists; not yet a buildable module

## Adding a new connector

Start from `iq-connect-template`, which provides the scaffolding and interfaces every connector implements. Each connector is an independent Maven module and can be built, tested, and deployed separately.

## Requirements

- Java 21
- Maven (wrapper included at repository root)
- Credentials for the target system, provided via environment variables or `.iq/vault`
