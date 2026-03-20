# IQ Connector: AWS

## Purpose

This connector brings AWS into IQ as an RDF-first index of both **runtime** and **governance** state.

It is intended to scan and model **everything within an AWS account/region** that is relevant to operations,
audit, and compliance.

## Scope / Resources scanned

The connector should be able to scan and expose resources such as:

- **Governance / audit**: AWS Config, CloudTrail, Security Hub findings, IAM roles/policies, Organizations, SCPs
- **Runtime inventory**: EC2 instances, ECS/EKS clusters, Lambda functions, RDS, ElastiCache, VPCs
- **Storage & networking**: S3 buckets, EBS volumes, NLB/ALB, Route 53, CloudFront
- **Observability**: CloudWatch log groups, alarms, events
- **Security**: GuardDuty, IAM password policies, KMS keys

## Architecture

- **Kernel**: Runs a reactive sync loop, polling AWS APIs and updating the connector graph.
- **Model**: Exposes connector state as an RDF `Model` that IQ can query.
- **Checkpoint**: Persists sync position (e.g. pagination tokens, last polled timestamps) so the connector can resume without re-scanning everything.

## SDK / API

- Uses AWS SDK for Java (v2 recommended) and standard AWS `Region`/`CredentialsProvider` patterns.
- Supports IAM roles (EC2/EKS), environment vars, profile config, and explicit keys.
- Recommended services: AWS Config, CloudTrail, Resource Groups Tagging API, and individual service clients for deeper scans.

## Configuration & Secrets

- **Config**:
  - `aws.region`, `aws.pollInterval`, `aws.scanServices` (list of service families to include)
  - `aws.graphIri` (named graph used for state)
- **Secrets**:
  - Use IQ's secret store / vault to provide `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`.
  - Support IAM role assumption for least privilege.

## Risks & Considerations

- **API rate limits & throttling**: AWS can throttle heavy inventory scans (Config/CloudTrail). Respect `Retry-After` and backoff.
- **Cost**: Some services (Config, CloudTrail, Security Hub) incur costs when enabled.
- **Data volume**: Large accounts can produce huge RDF graphs; consider pagination, filtering, and incremental sync.
- **Permissions**: Need least-privilege IAM policies; missing permissions should surface clearly in connector state.

## State Model

Connector state lives in a dedicated graph (e.g., `urn:iq:connector:aws`).

### Key RDF concepts

- Connector metadata (`connector:lastSyncedAt`, `connector:syncStatus`)
- Resource snapshots (e.g., `:s3Bucket a :S3Bucket`)
- Checkpoint tokens (`connector:checkpoint`)

## Sync Modes

- **Read-only**: Poll and refresh AWS state into IQ.
- **Write-only**: (Optional) Apply changes from IQ to AWS.
- **Read-write**: Keep state in sync with bidirectional mapping.

## Registration

The connector should publish a `ConnectorDescriptor` RDF document describing:

- connector ID and name
- capabilities (polling, webhook, delta stream)
- supported sync modes

## Checklist for implementation

- [ ] `I_Connector` implementation
- [ ] `I_ConnectorKernel` (sync loop, backoff, error handling)
- [ ] `I_Checkpoint` implementation
- [ ] `Model` view for state (state graph + metadata)
- [ ] Register connector descriptor in the registry graph

## Extension Points

- Add additional AWS services by adding new RDF vocab terms and sync routines.
- Implement event-driven sync using CloudTrail/Config/CloudWatch Events instead of polling.
