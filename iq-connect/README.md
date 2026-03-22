# iq-connect — Connector Library

`iq-connect` is the family of integration adapters that connect IQ to the outside world. Each connector is a self-contained module that brings data from an external system into IQ's knowledge graph — or pushes IQ's knowledge out to that system.

Connectors follow a uniform pattern: they have a lifecycle, a local graph representing their current state, and a set of operations (read, write, or both). This consistency means you can reason over data from completely different sources using the same query language.

## Available connectors

| Connector | What it connects |
|---|---|
| `iq-connect-aws` | Amazon Web Services — S3, EC2, IAM, and more |
| `iq-connect-azure` | Microsoft Azure resources and services |
| `iq-connect-gcp` | Google Cloud Platform |
| `iq-connect-digitalocean` | DigitalOcean infrastructure |
| `iq-connect-github` | GitHub repositories, issues, and pull requests |
| `iq-connect-gitlab` | GitLab projects and pipelines |
| `iq-connect-jira` | Jira projects, tickets, and workflows |
| `iq-connect-confluence` | Confluence spaces and pages |
| `iq-connect-slack` | Slack workspaces and channels |
| `iq-connect-office-365` | Microsoft 365 — mail, calendar, and files |
| `iq-connect-google-apps` | Google Workspace |
| `iq-connect-jdbc` | Any SQL database via JDBC |
| `iq-connect-snowflake` | Snowflake data warehouse |
| `iq-connect-databricks` | Databricks lakehouse |
| `iq-connect-parquet` | Parquet files and columnar data |
| `iq-connect-redis` | Redis key-value store |
| `iq-connect-kafka` | Kafka topics and event streams |
| `iq-connect-k8s` | Kubernetes clusters and workloads |
| `iq-connect-docker` | Docker images and container state |
| `iq-connect-datadog` | Datadog metrics and monitors |
| `iq-connect-salesforce` | Salesforce CRM objects and flows |
| `iq-connect-stripe` | Stripe payments and subscriptions |
| `iq-connect-graphql` | Any GraphQL endpoint |
| `iq-connect-openapi` | Any OpenAPI-described REST service |
| `iq-connect-sparql` | Remote SPARQL endpoints |
| `iq-connect-scan-cve` | CVE vulnerability scanning |
| `iq-connect-core` | Shared base for all connectors |
| `iq-connect-template` | Starting point for building a new connector |

## Adding a new connector

Start from `iq-connect-template`, which provides the scaffolding and interfaces every connector implements. Each connector is an independent Maven module and can be built, tested, and deployed separately.

## Requirements

- Java 21
- Maven (wrapper included at repository root)
- Credentials for the target system, provided via environment variables or `.iq/vault`
