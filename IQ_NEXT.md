# IQ Next (short-term/medium-term roadmap)

## 1. Product review / meaningful experience

- IQ is an RDF-based knowledge orchestration platform with modular connectors and script-driven automation.
- Core value is SDL-style behavior without heavyweight coding: intent -> graph query -> action.
- Existing modules already cover AI endpoints (`iq-apis`), connectors (`iq-connect/*`), graph engines (`iq-rdf4j`), and CLI/agent interfaces.

## 2. Impactful use cases (high leverage)

1. **Enterprise policy automation**
   - auto-compliance checks across cloud + infra + docs using SPARQL rules in central policy graph.
   - notify + remediate drift with built-in Azure/AWS/GCP connector actions.

2. **Cross-source accountability chain**
   - ingest audit logs (K8s, DB, app events), map to enterprise ontology, derive risk scores with graph rules.
   - query and explain reasoning path via step-by-step trace object.

3. **Hybrid human+AI workflow assistant**
   - conversational goal to action: user says "onboard client" -> IQ invokes Slack/Office365/GitHub connectors and creates tasks + docs.
   - one-click audit trail with `iq-trusted` immutable workflow ledger.

4. **Engineering SOP automation**
   - autoscale and secure deployments using declarative runbook templates (`.ttl/.sparql`) plus `k8s`/`docker` connector.
   - continuous validation of infra config with in-flight change policy rules.

5. **Data mashup and enrichment**
   - connect metadata from Snowflake/JDBC/GraphQL, join with knowledge base, execute model-driven predictions (LLM+SQL hybrid script).

## 3. Idiomatic extensions vs new modules

### 3.1 Core platform (IQ-Platform / IQ-RDF4J)
- add `iq-platform` support for:
  - vector embeddings + semantic graph index (integration with `iq-connect-openai`, `pinecone`, `qdrant` via new connector)
  - incremental graph delta processing (`Patch`/`Diff` APIs) for event-driven updates
  - policy engine plugin interface (rulesets in SPARQL + JS expressions).

- `iq-rdf4j`:
  - add `graph-history` snapshot + `query-time-travel` features.
  - native RDF transaction provenance and audit trail APIs (auto-capture user+source metadata on updates).

### 3.2 Connector ecosystem (`iq-connect/*`)
- `iq-connect-k8s` + `iq-connect-docker`: extend with combined resource drift detector (deployed state vs desired graph state) and remediation actions.
- New connector module `iq-connect-vector`:
  - connect vector DBs (Pinecone/Qdrant/Weaviate) and embedding providers with metadata graph link.
  - expose semantic search as SPARQL virtual table in IQ.
- New connector module `iq-connect-notebook`:
  - trigger repository-based notebook/SQL workflows (Dagster/Prefect/Airflow) from IQ intent graph events.
- New connector module `iq-connect-iam`:
  - centralized policy enforcement for AWS/Azure/GCP IAM secrets/roles; combined with `iq-trusted` auditing.

### 3.3 API / agent layer (`iq-apis`, `iq-cli`, `iq-agentic`)
- built-in workflow “playbook templates” library, versioned and remote-loadable (local `.iq/playbooks` + marketplace fetch).
- telemetry/observability API extension for traceability (each action path labeled and explorable across modules).
- short-lived session credentials + adaptive access.

## 4. New module candidates (bold value shift)
- `iq-analytics`: add graph-native analytics, metrics ingestion, anomaly detection, and dashboard facts generation.
- `iq-risk`: prebuilt compliance risk models, auto-SOCRS, configurable thresholds, and remediation proposals.
- `iq-workflow`: explicit state machine execution module (state diagram in RDF + runtime engine) for managed long-running business flows.
- `iq-llm-runtime`: unified LLM pipeline including prompt rewrites, hallucination guard, provenance logging, and guardrail policy integration.
- `iq-webui`: low-code visual builder for connectors + graph queries with “instant deploy” to CLI.

## 5. Developer ergonomics
- module code generator: `./bin/new-connector <name>`, scaffolds in `iq-connect-<name>`, includes test skeleton, docs, and CI stubs.
- standardized `Capability` interface in `iq-connect-core` for capabilities discovery (“canExecute”, “canRead”, “canWrite”) at runtime.
- auto-generated OpenAPI for `iq-apis` based on declared script endpoints.

## 6. Quick actions (first 90 days)
1. deliver `iq-connect-vector` + `iq-platform` semantic index extension.
2. implement `graph-change` event stream hooks in `iq-rdf4j` and `iq-platform`.
3. build `policy pod` blueprint: `iq-connect-iam`, `iq-connect-azure`, `iq-connect-aws` and `iq-trusted` rule set.
4. expose “runbook smart actions” in `iq-agentic` for commons (incident response, onboarding, patching).

## 7. Risks / mitigations
- drift between script semantics and code behavior: add automated script lints (`iq-test-servers` + CI job).
- provider API churn: isolated adapter layer + codeless endpoint mapping.
- scale heavy graphs: introduce hybrid in-memory + persisted index and query sharding in `iq-rdf4j`.

## 8. Signals of success
- 3 real playbooks in prod (security, ops, data integration).
- 50+ connector combos executed as graph recipes in one pipeline.
- < 3% API defect rate after LLM/agent rollouts.
- marketplace for playbooks / connectors launched.
