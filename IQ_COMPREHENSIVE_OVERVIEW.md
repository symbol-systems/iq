# IQ Platform — Comprehensive Technical Overview

**System Version:** 0.94.1  
**Build:** Maven (Java 21, Quarkus)  
**Repository:** github.com/symbol-systems/iq  
**Generated:** April 7, 2026

---

## Executive Summary

**IQ** is a cognitive operating system for governing autonomous AI agents and neuro-symbolic workflows. It turns structured knowledge (RDF/TTL) and LLM reasoning into executable, stateful processes that can:

- Maintain persistent state across multi-step workflows using RDF-backed finite state machines
- Make LLM-assisted decisions grounded in real domain knowledge (not just prompt context)
- Connect to 25+ external systems (AWS, GitHub, Slack, Kafka, Salesforce, etc.) via pluggable connectors
- Enforce trust zones, access control, and audit trails through declarative RDF policies
- Scale from personal projects to multi-tenant enterprises with JWT-secured realm isolation

**Core design principle:** *Knowledge-first, declarative, auditable.* Business logic lives as data (TTL + SPARQL), not hardcoded; this enables reuse, versioning, and transparent provenance.

**Primary use cases:**
1. **Autonomous agent systems** — task-driven workflows with LLM decision-making backed by domain data
2. **RAG (Retrieval-Augmented Generation)** — LLM APIs grounded in knowledge graphs
3. **Data orchestration** — multi-cloud/multi-system integration with semantic knowledge layer
4. **Compliance & governance** — auditable AI with explicit policies, role-based access, cost tracking
5. **Enterprise knowledge bases** — unified semantic layer over siloed data sources

---

## 1. Core Purpose and What the System Does

### What IQ Solves

IQ bridges three fundamental gaps in current AI systems:

1. **Context vs. Execution Gap** — LLMs have broad knowledge but no persistent state. IQ gives agents lasting memory, role-based permissions, and multi-step workflows.

2. **Opacity Gap** — LLM reasoning is a black box. IQ records every decision, action, and transition in an RDF fact graph—explainable by design.

3. **Integration Multiplicity** — Teams rewire integration glue code every time a connector changes. IQ's connector library and semantic orchestration layer let you wire services once, then reason over them uniformly via SPARQL.

### The IQ Architecture in One Picture

```
┌─────────────────┐
│   REST API  │  (Chat, Avatar, Intent, Agent endpoints)
│   (iq-apis) │
└────────┬────────┘
 │
┌────────────────────┼────────────────────┐
│││
┌───▼────┐   ┌─────▼──────┐  ┌──────▼────┐
│ Avatar │   │   Agent│  │  Realm│
│(Chat)  │   │(Agentic)   │  │ Manager   │
└───┬────┘   └─────┬──────┘  └──────┬────┘
│  ││
└──────────────────┼────────────────────┘
   │
┌──────────────┴──────────────┐
│   IQ Platform   │
│  ┌────────────────────┐│
│  │ ModelStateMachine  ││  RDF-backed FSM
│  │ LLMFactory ││  LLM resolution
│  │ Intent Registry││  Pluggable intents
│  │ RealmManager   ││  Multi-realm isolation
│  └────────────────────┘│
└──────────────────┬──────────┘
   │
┌──────────────────┼──────────────────┐
│  │  │
  ┌─────▼──────┐   ┌─────▼─────┐   ┌────────▼──────┐
  │  RDF4J │   │ Trusted   │   │  Connectors   │
  │ Repository │   │  (Auth,   │   │  (25+ kinds)  │
  ││   │ Secrets)  │   │   │
  └────────────┘   └───────────┘   └───────────────┘
```

### Key Design Properties

- **Stateful**: Agents track their position in workflows via RDF state triples
- **LLM-integrated**: Decisions can call GPT, Groq, or custom endpoints—configuration is RDF +secrets, not code
- **Auditable**: Every action, transition, and LLM call recorded as RDF triples with provenance (PROV-O)
- **Multi-tenant**: Realms are isolated knowledge graphs with separate JWT tokens, secrets, and policies
- **Extensible**: New connectors, intents, and decision-makers plug in via Maven modules and interface contracts

---

## 2. Key User Personas and Roles

### 1. **Enterprise AI Architect**
   - **Needs:** Govern fleets of agents, enforce compliance, trace decisions for audit
   - **Uses:** RealmManager for isolation, policy/ACL definitions in TTL, MCP for tool governance, cost tracking (iq-tokenomic)
   - **Success metric:** Agents run autonomously, all actions logged, policies enforced without code changes

### 2. **Domain Expert / Knowledge Engineer**
   - **Needs:** Express business rules and domain logic without writing code
   - **Uses:** TTL for ontology/rules, SPARQL for queries, RDF prompts for LLM context, script catalogs
   - **Success metric:** New domain behavior added via `.ttl` + `.sparql`, tests pass, no Java compilation needed

### 3. **Agent Developer**
   - **Needs:** Build agents that make LLM-informed decisions backed by real data
   - **Uses:** AgentBuilder, Avatar, ExecutiveIntent, state machines, LLMFactory, connector SDKs
   - **Success metric:** Agent runs multi-step workflows, calls LLM when needed, tracks state, respects role-based access

### 4. **Integration Engineer**
   - **Needs:** Connect IQ to external systems (cloud, databases, APIs, messaging)
   - **Uses:** iq-connect module family (25+ pre-built connectors), IConnector interface for customs
   - **Success metric:** Data flows bidirectionally; IQ reasons over it via SPARQL alongside internal knowledge

### 5. **Platform Operations**
   - **Needs:** Deploy IQ, manage realms, monitor performance, rotate secrets, scale across clusters
   - **Uses:** Docker images, Kubernetes manifests, ControlPlaneAPI (cluster coordination), JWT rotation, vault management
   - **Success metric:** IQ scales horizontally, agents survive node failures, secrets stay encrypted, audit logs centralize

### 6. **Data Analyst / Compliance Officer**
   - **Needs:** Query agent decisions, validate governance, generate reports for regulators
   - **Uses:** SPARQL endpoints, RDF exports (Turtle/N-Quads), provenance queries, policy validation queries
   - **Success metric:** Any decision can be retracted to its source; policies are machine-readable and machine-checkable

---

## 3. Main Modules and Their Responsibilities

### Core Runtime Modules

#### **iq-apis** — REST API Server (Quarkus)
- **Role:** Public entry point; boots all realms and exposes their capabilities as REST/WebSocket endpoints
- **Key components:**
  - `ChatAPI` — conversational interface to agents; both request/response and streaming
  - `AvatarAPI` — full-featured avatar (agent + conversation history + state)
  - `IntentAPI` — trigger intent-driven state transitions
  - `ModelAPI` — SPARQL SELECT/CONSTRUCT queries against realm knowledge
  - `StateAPI` — read/write agent state triples
  - `ControlPlaneAPI` — cluster coordination (node registration, leader election, policy distribution)
  - `TokenAPI / GuestAPI / NonceAPI` — OAuth / JWT issuance
  - `VisionAPI / DataAPI` — multimodal inputs (images, files, streaming data)
  - `DebugAPI` — introspection (realm contents, agent state, available intents)
- **Configuration:** Realm discovery from `.iq/repositories/`, LLM maps from `.iq/runtime/`, JWT keys from `.iq/jwt/`
- **Lifecycle:** Starts in dev mode (`quarkus:dev`) with live reloading; built as container via `./bin/build-image`

#### **iq-platform** — Core Agent Engine (Library)
- **Role:** Heart of IQ; provides realm lifecycle, state machine execution, LLM wiring, intent dispatch
- **Key components:**
  - `RealmManager` — boots/caches realms from `.iq/repositories` config; manages RDF4J repository manager
  - `Realm` — represents one tenant's isolated knowledge graph, secrets, JWT key pair
  - `ModelStateMachine` — RDF-backed FSM; guards check SPARQL ASK patterns; transitions recorded as triples
  - `LLMFactory` — resolves LLM config from RDF, retrieves API tokens from vault, mints `GPTWrapper` with validation
  - `IQScriptCatalog / ModelScriptCatalog` — registry of named SPARQL/Groovy/prompt scripts; auto-discovered from classpath + `.iq/`
  - Intent runtime — dispatches `ExecutiveIntent`, `Remodel`, `Search`, `Find`, `Learn`, `Think`, `JSR233` (scripting), etc.
- **RDF Configuration:** Repositories defined as TTL; LLM providers as RDF resources with properties injected via `Poke` (reflection)
- **Secrets handling:** Delegates to `EnvsAsSecrets` or `VFSPasswordVault`

#### **iq-agentic** — Agent Builder & Decision Toolkit (Library)
- **Role:** Constructs agents; wires FSM, LLM decision-making, intent execution, and budgeting
- **Key components:**
  - `AgentBuilder` — fluent builder; chains LLM wiring, SPARQL injection, secret binding in a few calls
  - `Avatar` — conversational agent; maintains chat history, executes intents, calls LLM for decisions
  - `ExecutiveAgent` — base agent implementation; manages state, intent queue, delegation
  - `LLMDecision` — wraps I_LLM; constructs prompt from knowledge, calls provider, parses response
  - `ChainOfCommand` — ranks multiple decision-makers; tries first, falls back to next
  - `Budget / Treasury` — accumulates token spend; enforces per-agent limits; emits cost triples to RDF
  - `Remodel` — intent that re-executes SPARQL query to refresh agent's internal model
  - `JSR233` — intent that runs named Groovy/JS scripts against agent state
- **Execution:** Agent ticks; FSM checks guards; if unblocked, calls LLM decision (if configured) or executes next intent

#### **iq-trusted** — Auth, Secrets, Trust Layer (Library)
- **Role:** Identity, secrets, cryptography; enables multi-realm, multi-user, policy-driven access
- **Key components:**
  - `VFSPasswordVault` — encrypted `.iq/vault` files; holds API keys, DB passwords, OAuth tokens
  - `EnvsAsSecrets` — alternative; reads credentials from env vars (for containers/CI)
  - `VFSKeyStore` — per-realm RSA key pairs; used for JWT signing
  - Trust connectors — OAuth integrations (GitHub, Discord, LinkedIn, Web3, mobile auth flows)
  - `TrustedPlatform` — realm bootstrapping with trust-aware agent initialization
  - `IQ_AI` — standalone trusted AI entry point
- **JWT Model:** Each call includes bearer token; token minted per realm, contains actor IRI in claims, signed with realm's private key
- **Per-type policies:** SHACL shapes + SPARQL ASK templates; checked at GraphQL resolution time

---

### RDF and Knowledge Graph Modules

#### **iq-rdf4j** — RDF4J Integration & Graph Transforms
- **Role:** Underlies all RDF operations; provides SPARQL execution, inference, shape validation
- **Key capacities:**
  - Repository management via RDFConfigFactory (TTL-based config to RDF4J RepositoryConfig)
  - Native, memory, and inferencing stores (TrustRepos for encryption)
  - SPARQL SELECT/CONSTRUCT/ASK/UPDATE via `SPARQLMatrix`
  - SHACL validation for schema conformance
  - FedX support for querying remote SPARQL endpoints
- **Templating:** Repositories configured via TTL with placeholders (e.g., `${id}`) interpolated at boot

#### **iq-rdf4j-graphs** — Graph Transforms & JGraphT Integration
- **Role:** Analyzes knowledge structure; pathfinding, clustering, subgraph extraction
- **Use cases:** Agent reachability analysis, impact assessment, knowledge graph visualization
- **Integration:** Bridges RDF4J Model to JGraphT for graph algorithms

#### **iq-rdf4j-graphql** — GraphQL Access Layer
- **Role:** Exposes RDF knowledge as GraphQL; type-safe queries with access control
- **Mechanism:** `SPARQLDataFetcher` translates GraphQL queries to SPARQL; response shapes derived from schema
- **Governance:** Per-type ACL policies checked at resolver time via SPARQL ASK

#### **iq-rdf4j-camel** — Apache Camel + RDF Stream Integration
- **Role:** Event-driven RDF processing; reacts to Camel routes by updating knowledge graph
- **Use cases:** Real-time data ingestion, webhook-driven FSM transitions, stream reification into RDF

---

### Connector and Integration Modules

#### **iq-connect** — Unified Connector Family (25+ pre-built)
- **Architecture:** Each connector is an independent Maven module exposing `IConnector` interface
- **Common Pattern:**
  1. Authenticate to external system (OAuth, API key, service principal)
  2. Fetch resource types (VMs, repos, messages, records) and reify as RDF inside a realm-specific named graph
  3. Optional: listen for events (webhooks, pub/sub) and update RDF in real-time
  4. Optional: reverse-sync — RDF actions (INSERT, DELETE) → external system API calls
- **Pre-built Connectors:**
  - **Cloud:** `iq-connect-aws` (EC2, S3, IAM), `iq-connect-azure`, `iq-connect-gcp`, `iq-connect-digitalocean`
  - **DevOps/Platform:** `iq-connect-github` (repos, issues, PRs), `iq-connect-k8s`, `iq-connect-docker`, `iq-connect-datadog`
  - **Productivity:** `iq-connect-slack`, `iq-connect-confluence`, `iq-connect-jira`, `iq-connect-office-365`, `iq-connect-salesforce`, `iq-connect-stripe`
  - **Data:** `iq-connect-jdbc` (any SQL DB), `iq-connect-snowflake`, `iq-connect-databricks`, `iq-connect-parquet`, `iq-connect-kafka`, `iq-connect-redis`
  - **Semantic:** `iq-connect-sparql` (query remote endpoints), `iq-connect-graphql` (wrap any GraphQL service), `iq-connect-openapi` (Swagger/OpenAPI)
  - **Security:** `iq-connect-scan-cve` (vulnerability scanning)
- **Extension:** `iq-connect-template` provides scaffolding for custom connectors

#### **iq-connect-core** — Common Interfaces & Base Classes
- Shared lifecycle (auth, fetch, sync), credential handling, RDF translation
- `IConnector` contract; base implementations for sync/async patterns

---

### LLM and Decision Making

#### **LLMFactory** — Configuration and Provider Resolution
- **Role:** Centralized LLM configuration; bridges RDF definitions to provider APIs
- **Flow:**
  1. RDF triple → `iq:LLMProvider` resource with properties (name, URL, auth secret name, response format)
  2. `Poke` reflection injection populates `GPTConfig` from RDF properties
  3. Secret lookup retrieves API token from vault
  4. Validation of required fields (name, URL, token)
  5. Return configured `GPTWrapper` ready for inference
- **Supported providers:** OpenAI, Groq, Azure OpenAI, custom endpoints via HTTP
- **Configuration location:** `.iq/runtime/*.ttl` or `iq-apis/src/main/resources/iq/`

#### **Decision Pipeline**
- **LLMDecision:** Wraps an `I_LLM`; constructs prompt from knowledge (e.g., current state + domain facts), calls provider, parses response
- **ChainOfCommand:** Ranks multiple `LLMDecision` instances; tries first, falls back on timeout/error to next
- **Template prompts:** Named prompts in script catalog; parametric with RDF-injected context
- **Cost tracking:** Each LLM call recorded in `Treasury`; cumulative spend checked against agent budget

---

### Auxiliary and Specialized Modules

#### **iq-lake** — Knowledge Ingestion Pipeline
- **Role:** Convert external documents/data into RDF triples for the knowledge graph
- **Inputs:** HTML, PDFs (Tika), plain text, RDF/TTL, structured metadata
- **Process:** Parse → vectorize (embeddings) → reify as RDF triples with provenance
- **Output:** Named graphs added to realm repository

#### **iq-onnx** — Local ML Inference
- **Role:** Run embeddings, reranking, small language models locally without external API calls
- **Use cases:** Semantic search within knowledge graph, cost reduction (avoid API calls for simple tasks)

#### **iq-lab** — Experimental Features
- **Current:** Persona engine, Discord integration, prototype features
- **Status:** Unstable; for exploration before stabilization to main modules

#### **iq-mcp** — Model Context Protocol Server
- **Role:** Expose IQ capabilities (SPARQL, intents, trust, LLM) as MCP tools for external LLMs
- **Tool Categories:**
  - `fact.sparql-query` — SELECT/CONSTRUCT/ASK on realm knowledge
  - `fact.sparql-update` — INSERT/DELETE triples
  - `actor.trigger` — initiate agent state transition
  - `actor.execute` — run action
  - `actor.status` — query agent state
  - `trust.login` — OAuth issuance
  - `llm.invoke` — call LLM within realm
  - `realm.export` — export graph
- **Authorization:** Per-tool SPARQL ASK policies; rate limits and cost tracking

#### **iq-cli / iq-cli-pro** — Command-Line Interfaces
- **iq-cli:** Community edition; basic realm operations, chat, agent triggers
- **iq-cli-pro:** Extended features; advanced orchestration, batch operations, performance tuning
- **Example commands:**
  ```bash
  ./bin/iq-cli chat --realm myapp --actor user "What tasks are pending?"
  ./bin/iq-cli agent-trigger --realm myapp --intent process-invoice --actor bot
  ./bin/iq-cli sparql --realm myapp --query "SELECT ?agent WHERE { ?agent a iq:Agent }"
  ```

#### **iq-tokenomic** — Token Usage & Budget Enforcement
- **Role:** Track cumulative token spend per agent, realm, or user; enforce limits
- **Mechanism:** Every LLM call emits RDF triple with cost; `Treasury` sums costs; agent stops if budget exceeded
- **Auditing:** All costs stored in RDF; reportable via SPARQL

#### **iq-onto** — Ontology Management & Vocabulary Tooling
- **Role:** Manage domain vocabularies, SKOS taxonomies, OWL class hierarchies
- **Use:** Foundation for semantic interoperability across connectors and agents

#### **iq-control-plane** — Cluster Coordination
- **Role:** Leader election, policy distribution, node health monitoring
- **Endpoints:** `ControlPlaneAPI` in `iq-apis`
- **Use case:** Multi-node IQ deployments (HA, load balancing)
- **Mechanisms:**
  - Raft-based leader election
  - Heartbeat monitoring
  - Policy bundle signing and distribution
  - Node state transitions (HEALTHY → DEGRADED → UNHEALTHY → DRAINING → OFFLINE)

#### **iq-abstracts / iq-aspects / iq-kernel / iq-scripting / iq-policy / iq-secrets / iq-starter**
- **Purpose:** Common utilities, base classes, extension points
- **Examples:**
  - `iq-abstract`: Core interfaces (`I_Agent`, `I_Realm`, `I_LLM`, etc.)
  - `iq-aspects`: Identity helpers, date utilities, environment bindings
  - `iq-kernel`: Low-level plumbing (VFS, storage, threading)
  - `iq-scripting`: JSR-223 scripting engine integration (Groovy, JavaScript)
  - `iq-secrets`: Vault management, secret rotation
  - `iq-starter`: Starter templates for new projects

---

## 4. The TTL/RDF Graph System — How It Works

### Core Principle: Knowledge-First Execution

In IQ, business logic is **data**, not code. Domain behavior is expressed as RDF triples (`subject -predicate-> object`), SPARQL queries, and TTL (Turtle text format) files. When you add or change behavior, you modify `.ttl` and `.sparql` files, not Java code.

### Data Model Layers

#### Layer 1: **Domain Ontology** (SKOS, schema.org, domain-specific)
Defines entities, relationships, classes, and properties in your domain.

**Example:**
```turtle
@prefix : <http://acme.com/v0/> .
@prefix iq: <http://acme.com/iq/> .
@prefix schema: <http://schema.org/> .

:Order a schema:Order ;
  schema:customer :cust123 ;
  schema:dateOrdered "2026-04-07"^^xsd:date ;
  iq:status :OrderPlaced ;
  iq:totalPrice "100.00"^^xsd:decimal .

:cust123 a schema:Person ;
  schema:name "Alice" ;
  schema:email "alice@acme.com" .
```

#### Layer 2: **Agent & Workflow Definitions** (iq: vocabulary)
Describes agents, their intents, state machines, permissions, and goals.

**Example — Agent with FSM:**
```turtle
:fulfillmentAgent a iq:Agent ;
  iq:realm <http://acme.com/fulfillment-realm> ;
  iq:hasStateMachine :orderFSM .

:orderFSM a iq:Workflow ;
  iq:initialState :Received ;
  iq:hasState :Received, :Processing, :Shipped, :Delivered ;
  iq:hasTransition [
iq:from :Received ;
iq:to :Processing ;
iq:guard "ASK { :order iq:totalPrice ?price . FILTER(?price > 50) }" ;
iq:requires iq:can-process-orders
  ] ;
  iq:hasTransition [
iq:from :Processing ;
iq:to :Shipped
  ] .
```

#### Layer 3: **Prompts & LLM Bindings** (ai: vocabulary)
Named prompts that ground LLM decisions in domain knowledge.

**Example:**
```turtle
:decideNextAction a ai:Prompt ;
  ai:template """
You are an order fulfillment agent. Current order state:
{current_state}

Domain facts:
{sparql_context}

What should you do next? Respond with one of: PROCESS, SHIP, DELIVER, HOLD.
""" ;
  ai:sparql_context """
  SELECT ?prop ?value WHERE {
{current_order} ?prop ?value .
  }
  """ .
```

#### Layer 4: **Policies & ACLs** (trust: vocabulary)
Define who can do what, with optional SPARQL ASK templates.

**Example:**
```turtle
:PolicyProcessOrders a trust:Policy ;
  trust:forIntent iq:ProcessOrder ;
  trust:askTemplate "ASK { {actor} iq:hasRole iq:fulfillment-operator }" ;
  trust:defaultAllow false .

:PolicyViewFacts a trust:Policy ;
  trust:forType schema:Order ;
  trust:askTemplate """
  ASK {
{actor} iq:hasRole ?role .
?role iq:canRead schema:Order .
  }
  """ ;
  trust:defaultAllow false .
```

#### Layer 5: **Provenance & Audit** (prov: vocabulary)
Records of who did what, when, and why—automatically generated.

**Example:**
```turtle
# Auto-generated after fulfillmentAgent transitions :Received → :Processing
<urn:uuid:abc123> a prov:Activity ;
  prov:agent :fulfillmentAgent ;
  prov:wasAssociatedWith :orderFSM ;
  prov:generated :Order_Status_Processing_at_2026-04-07T10:23:45Z ;
  prov:startedAtTime "2026-04-07T10:23:45Z"^^xsd:dateTime ;
  prov:endedAtTime "2026-04-07T10:23:47Z"^^xsd:dateTime .
```

### SPARQL: The Query and Update Language

SPARQL is IQ's lingua franca for querying and updating knowledge. You write `.sparql` files, register them in catalogs, and reference them from agents and intents.

#### SELECT — Query facts
```sparql
PREFIX : <http://acme.com/v0/>
PREFIX schema: <http://schema.org/>

SELECT ?customer ?totalPrice WHERE {
  ?order a schema:Order ;
 schema:customer ?customer ;
 iq:totalPrice ?totalPrice .
  FILTER(?totalPrice > 100)
}
```

#### CONSTRUCT — Transform and synthesize new facts
```sparql
PREFIX : <http://acme.com/v0/>
PREFIX iq: <http://acme.com/iq/>

CONSTRUCT {
  ?order iq:needsApproval true .
}
WHERE {
  ?order a schema:Order ;
 iq:totalPrice ?price ;
 schema:customer ?cust ;
 iq:status iq:Received .
  FILTER(?price > 1000)
}
```

#### ASK — Test a condition (used in guards, policies)
```sparql
PREFIX : <http://acme.com/v0/>

ASK {
  ?order iq:status iq:Processing ;
 schema:customer ?cust .
  ?cust iq:trustLevel iq:High .
}
```

#### UPDATE — INSERT/DELETE triples
```sparql
PREFIX : <http://acme.com/v0/>

INSERT DATA {
  :Order123 iq:status iq:Shipped .
  :Order123 iq:shipDate "2026-04-07"^^xsd:date .
}
```

### Script Catalogs: Registration & Discovery

Named scripts live in catalogs so agents can reference them by name without knowing file paths.

**In Java (IQScriptCatalog, ModelScriptCatalog):**
```java
public class MyScriptCatalog implements IQScriptCatalog {
  @Override
  public Iterable<I_Script> scripts() {
return List.of(
  new NamedScript("get-pending-orders", 
"classpath:/sparql/orders/pending.sparql"),
  new NamedScript("decide-next-action", 
"classpath:/prompts/fulfillment/next-action.txt"),
  new NamedScript("apply-business-rules", 
"classpath:/groovy/fulfillment/rules.groovy")
);
  }
}
```

**In `.iq/` (Local overrides):**
```
.iq/scripts/
  orders/
pending.sparql
by-customer.sparql
prompts/
  fulfillment/
next-action.txt
groovy/
  custom-rules.groovy
```

At boot, `RealmManager` auto-discovers scripts from classpath + `.iq/` and makes them available to agents.

### Repository Types: Storage Backend Options

IQ uses RDF4J's templated repository configuration. You define repository type (storage backend) via TTL:

**In-Memory (for tests):**
```turtle
@prefix : <http://acme.com/config/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rep: <http://www.openrdf.org/config/repository#> .

:repo1 a rep:Repository ;
  rep:repositoryID "test-mem" ;
  rep:repositoryImpl [ rep:repositoryType "memory" ] .
```

**Native (Persistent local disk):**
```turtle
:repo2 a rep:Repository ;
  rep:repositoryID "prod-native" ;
  rep:repositoryImpl [
rep:repositoryType "native" ;
rep:dataDir "/data/iq/repositories/prod"
  ] .
```

**Federated (Query multiple repos):**
```turtle
:repo3 a rep:Repository ;
  rep:repositoryID "federated" ;
  rep:repositoryImpl [
rep:repositoryType "fedx" ;
rep:federated [ rep:members (:repo1 :repo2) ]
  ] .
```

**Remote SPARQL Endpoint:**
```turtle
:repo4 a rep:Repository ;
  rep:repositoryID "remote" ;
  rep:repositoryImpl [
rep:repositoryType "sparql" ;
rep:endpoint "https://sparql.example.com/query"
  ] .
```

---

## 5. Enterprise-Scale Features

### 5.1 Multi-Tenancy: Realm Isolation

Each realm is a completely isolated tenant with:
- **Separate RDF repository** — no data leakage between realms
- **Independent JWT keypair** — different secret per realm (stored in `.iq/jwt/{realm_name}.key`)
- **Isolated secrets vault** — realm-scoped API keys, DB passwords, OAuth tokens
- **Per-realm agents** — agents belong to one realm; FSMs are realm-local

**How it works:**
```
RealmManager
├── Realm(acme-fulfillment)
│   ├── Repository(acme-fulfillment-prod)
│   ├── Vault(acme-fulfillment-secrets)
│   ├── JWT Key (acme-fulfillment.jks)
│   └── Agents: :fulfillmentAgent, :approvalAgent
│
├── Realm(partner-integration)
│   ├── Repository(partner-integration)
│   ├── Vault(partner-secrets)
│   ├── JWT Key (partner.jks)
│   └── Agents: :syncAgent, :dataAgent
│
└── Realm(internal-ai)
├── Repository(internal-ai)
├── Vault(internal-secrets)
├── JWT Key (internal.jks)
└── Agents: :analyzerAgent
```

**JWT Token Flow:**
1. Client requests token for realm `acme-fulfillment` via `TokenAPI`
2. `TokenAPI` looks up realm; retrieves its RSA private key from `.iq/jwt/acme-fulfillment.jks`
3. Issues JWT with claims: `actor=client-id`, `realm=acme-fulfillment`, `aud=iq`, `exp=+30days`
4. Client includes token as `Authorization: Bearer {jwt}` in subsequent calls
5. `GuardedAPI` base class validates token signature and realm membership on each request

### 5.2 Clustering & High Availability

**Control Plane (iq-control-plane, ControlPlaneAPI):**
- **Leader election:** Raft-based; nodes vote for leader; leader distributes policies
- **Heartbeat monitoring:** Nodes send periodic heartbeats; failure detected and redistributed
- **Node states:** HEALTHY → DEGRADED → UNHEALTHY → DRAINING → OFFLINE
- **Policy distribution:** Leader publishes signed policy bundles; all nodes apply them

**Endpoints:**
```
POST /cluster/nodes# Register a node
GET  /cluster/nodes# List all nodes
GET  /cluster/nodes/{nodeId}   # Get node details
DELETE /cluster/nodes/{nodeId} # Unregister a node
PUT  /cluster/nodes/{nodeId}/state # Update node state
GET  /cluster/leader   # Get current leader
POST /cluster/leader/elect # Attempt election
GET  /cluster/policy/bundle# Get latest policies
POST /cluster/policy/bundle# Publish new policies (leader only)
GET  /cluster/stats# Cluster statistics
```

**Deployment patterns:**
- **Active-Active:** Multiple IQ instances, shared external RDF4J repository (e.g., Fuseki, GraphDB)
- **Active-Passive:** Primary serves requests; secondary replicates via snapshot + streaming updates
- **Kubernetes:** StatefulSet with persistent volume for `.iq/repositories/` and `.iq/vault/`

### 5.3 Security: Auth, Encryption, Secrets Management

#### **JWT-based Access Control**
- Realm-specific keypairs (RSA)
- Token includes actor IRI, roles, scopes, expiry
- Signature validated on every request
- No hardcoded credentials; all derived from audience (realm name) + secret lookup

#### **Secrets Management**

**Option 1: Encrypted Vault File**
```
.iq/vault/
├── acme-fulfillment.enc # Encrypted secrets for realm
├── partner.enc
└── internal.enc
```

`VFSPasswordVault` reads `.enc` files (encrypted with master password, often from `VAULT_PASSWORD` env var).

**Option 2: Environment Variables**
```bash
export MYREALM_OPENAI_API_KEY="sk-..."
export MYREALM_POSTGRES_PASSWORD="secret"
```

`EnvsAsSecrets` reads env vars with naming pattern `{REALM}_{SERVICE}_{KEY}`.

#### **Policy-Driven Access Control (SPARQL ASK)**

Every GraphQL/SPARQL query checked against a policy before execution:

```sparql
# Policy template in RDF
:PolicyQueryOrders a trust:Policy ;
  trust:forType schema:Order ;
  trust:askTemplate """
  ASK {
{actor} iq:hasRole ?role .
?role iq:canRead schema:Order .
  }
  """ .

# At query time:
# 1. Extract {actor} from JWT claim
# 2. Substitute into template: "ASK { <client-id> iq:hasRole ?role . ?role iq:canRead schema:Order . }"
# 3. Execute ASK; if false, deny query; if true, allow and return results
```

This enables row-level, column-level, and type-level access control without code changes.

#### **Encryption at Rest**
- Vaults encrypted with AES-256 (via `VFSPasswordVault`)
- Repositories can use TrustRepos wrapper for triple-level encryption
- Private keys stored in Java keystores (`.jks` files) with password protection

#### **Audit Logging**
- Every action (query, update, agent transition) emitted as PROV-O triples
- Audit trail stored in RDF and queryable via SPARQL
- Cost and usage metrics tracked in `iq-tokenomic` module

### 5.4 Scalability & Performance

#### **RDF Repository Backends**
- **Memory:** Fast for small realms; no persistence
- **Native:** Persistent local disk; good for single-node deployments
- **Remote SPARQL:** Query distributed stores without replication
- **Federated (FedX):** Transparent query shipping to multiple repositories

#### **Index Management**
- RDF4J native stores use B-tree indexes on SPO, OSP, PSO patterns
- Explicit index hints in SPARQL queries (e.g., `SERVICE <http://example.org/sparql>`)
- Partitioning realms by domain or tenant to keep per-realm repositories small

#### **Caching**
- Agent decision cache (LLM responses keyed by prompt + context hash)
- Connector data cache (reduce re-fetching from AWS, GitHub, etc.)
- SPARQL query result caching with TTL

#### **Batch Operations**
- `Construct` intent for bulk RDF transformation
- `Update` intent for bulk INSERT/DELETE
- Connector batch-sync modes (e.g., fetch all AWS regions in parallel)

#### **Streaming & Async**
- WebSocket endpoints for streaming responses (e.g., `/ux/chat/{realm}/{actor}/stream`)
- Quarkus async/reactive handling for non-blocking I/O
- Camel routes for async event processing

---

## 6. Compliance Features: Audit, Governance, RBAC

### 6.1 Role-Based Access Control (RBAC)

Roles defined as RDF triples; bindings checked at query/action time:

```turtle
@prefix : <http://acme.com/v0/> .
@prefix iq: <http://acme.com/iq/> .

:alice a iq:Actor ; iq:hasRole :fulfillmentOp, :manager .
:bob a iq:Actor ; iq:hasRole :viewerOnly .

:fulfillmentOp a iq:Role ;
  iq:canExecuteIntent iq:ProcessOrder, iq:ShipOrder ;
  iq:canRead schema:Order ;
  iq:canWrite [ iq:type schema:Order ; iq:property iq:status ] .

:manager a iq:Role ;
  iq:canRead schema:Order, schema:Invoice ;
  iq:canGenerateReport true .

:viewerOnly a iq:Role ;
  iq:canRead schema:Order .
```

**Enforcement:** Every intent or query checks `ASK { {actor} iq:hasRole ?role . ?role iq:can... }` before proceeding.

### 6.2 Audit & Provenance (PROV-O)

Every significant action recorded as RDF using the PROV-O vocabulary:

```turtle
# Auto-generated when fulfillmentAgent transitions :Received → :Processing
<urn:uuid:action-001> a prov:Activity ;
  prov:agent :fulfillmentAgent ;
  prov:wasAssociatedWith :orderFSM ;
  prov:wasInformedBy <urn:uuid:prev-decision> ;
  prov:generated <urn:uuid:state-change-001> ;
  prov:used ?sparqlQuery ;
  prov:startedAtTime "2026-04-07T10:23:45Z"^^xsd:dateTime ;
  prov:endedAtTime "2026-04-07T10:23:47Z"^^xsd:dateTime .

# LLM invocation record
<urn:uuid:llm-call-001> a prov:Activity ;
  prov:agent :fulfillmentAgent ;
  prov:used :decideNextAction ;  # Prompt template
  prov:generated <urn:uuid:llm-response-001> ;
  iq:inputTokens 150 ;
  iq:outputTokens 42 ;
  iq:cost 0.002 .
```

**Querying audit trail:**
```sparql
PREFIX prov: <http://www.w3.org/ns/prov#>
SELECT ?action ?agent ?when WHERE {
  ?action a prov:Activity ;
  prov:agent ?agent ;
  prov:startedAtTime ?when .
  FILTER(?when > "2026-04-06T00:00:00Z"^^xsd:dateTime)
}
```

### 6.3 Policy Validation & Enforcement

**SHACL (Shapes Constraint Language) for schema validation:**

```turtle
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix schema: <http://schema.org/> .

:OrderShape a sh:NodeShape ;
  sh:targetClass schema:Order ;
  sh:property [
sh:path schema:customer ;
sh:minCount 1 ;
sh:nodeKind sh:IRI
  ] ;
  sh:property [
sh:path iq:totalPrice ;
sh:datatype xsd:decimal ;
sh:minInclusive 0
  ] .
```

**SPARQL rule-based validation:**
```sparql
PREFIX schema: <http://schema.org/>
PREFIX iq: <http://acme.com/iq/>

INSERT {
  ?order iq:violatesPolicy true .
}
WHERE {
  ?order a schema:Order ;
 iq:totalPrice ?price ;
 schema:customer ?cust .
  ?cust iq:creditLimit ?limit .
  FILTER(?price > ?limit)
}
```

### 6.4 Governance Dashboard (via SPARQL Queries)

Example governance queries:

**Who has what roles?**
```sparql
SELECT ?actor ?role WHERE {
  ?actor iq:hasRole ?role .
}
```

**What decisions did the fulfillmentAgent make in the last 24 hours?**
```sparql
SELECT ?when ?decision WHERE {
  :fulfillmentAgent prov:wasAssociatedWith ?decision ;
prov:startedAtTime ?when .
  FILTER(?when > NOW() - "PT24H"^^xsd:duration)
}
```

**Total LLM spend by realm:**
```sparql
SELECT ?realm (SUM(?cost) AS ?totalCost) WHERE {
  ?action iq:realm ?realm ;
  prov:agent ?agent ;
  iq:cost ?cost .
}
GROUP BY ?realm
```

**Policy violations:**
```sparql
SELECT ?order WHERE {
  ?order iq:violatesPolicy true .
}
```

---

## 7. Configuration Mechanisms and Extensibility Points

### 7.1 Configuration File Structure

```
.iq/   # All realm configuration & runtime data
├── repositories/  # RDF store configs
│   ├── default/
│   │   └── repo.xml  # RDF4J native store config
│   ├── acme-fulfillment/
│   │   └── repo.xml
│   └── partner/
│   └── repo.xml
│
├── runtime/# Runtime maps, prompts, catalogs
│   ├── llm-providers.ttl  # LLM configurations
│   ├── prompts/
│   │   ├── fulfillment.ttl
│   │   └── custom-decision.txt
│   └── workflows/
│   └── order-processing.ttl
│
├── vault/  # Encrypted secrets
│   ├── default.enc
│   ├── acme-fulfillment.enc
│   └── partner.enc
│
├── jwt/# Realm JWT keypairs
│   ├── default.jks
│   ├── acme-fulfillment.jks
│   └── partner.jks
│
├── lake/   # Knowledge ingestion
│   ├── documents/
│   │   └── order-policies.pdf
│   └── schemas/
│   └── ecommerce.owl
│
└── scripts/# Local script overrides
├── sparql/
│   └── orders/
│   └── pending.sparql
└── groovy/
└── custom-rules.groovy
```

### 7.2 LLM Provider Configuration (RDF)

**File:** `.iq/runtime/llm-providers.ttl`

```turtle
@prefix : <http://acme.com/llm> .
@prefix iq: <http://acme.com/iq/> .

:openai-gpt4 a iq:LLMProvider ;
  iq:name "openai-gpt4" ;
  iq:endpoint "https://api.openai.com/v1/chat/completions" ;
  iq:model "gpt-4" ;
  iq:contextLength 8192 ;
  iq:secretName "OPENAI_API_KEY" ;
  iq:responseFormat "json" .

:groq-llama a iq:LLMProvider ;
  iq:name "groq-llama" ;
  iq:endpoint "https://api.groq.com/completions" ;
  iq:model "llama-3.1-8b-instant" ;
  iq:contextLength 4096 ;
  iq:secretName "GROQ_API_KEY" ;
  iq:responseFormat "json" .

:azure-gpt a iq:LLMProvider ;
  iq:name "azure-gpt" ;
  iq:endpoint "https://{deployment}.openai.azure.com/v1/chat/completions" ;
  iq:model "gpt-35-turbo" ;
  iq:contextLength 4096 ;
  iq:secretName "AZURE_OPENAI_KEY" ;
  iq:apiVersion "2024-02" .
```

At boot, `LLMFactory.llm(resourceIri, model, contextLength, secrets)` reads this TTL, resolves secret name, and returns configured `GPTWrapper`.

### 7.3 Repository Configuration (RDF4J TTL Templates)

**File:** `.iq/repositories/{realm}/repo.xml` or templated TTL

Example in-memory template:
```turtle
@prefix rep: <http://www.openrdf.org/config/repository#> .
@prefix sr: <http://www.openrdf.org/config/spring#> .

[] a rep:Repository ;
  rep:repositoryID "${id}" ;
  rep:repositoryImpl [
rep:repositoryType "memory" ;
sr:qualifiedLoader sr:DefaultLoaders
  ] .
```

Example native template:
```turtle
@prefix rep: <http://www.openrdf.org/config/repository#> .

[] a rep:Repository ;
  rep:repositoryID "${id}" ;
  rep:repositoryImpl [
rep:repositoryType "native" ;
rep:dataDir "/data/repositories/${id}"
  ] .
```

`RDFConfigFactory.toConfig(realmIri, "memory", context)` substitutes `${id}` and returns a `RepositoryConfig`.

### 7.4 Extensibility Points

#### **Adding a Custom Intent**

**Step 1: Implement `IQIntent`**
```java
public class MyCustomIntent extends AbstractIntent {
  @Override
  public void execute(I_Agent agent, I_Realm realm) throws Exception {
// Do work
realm.getRepository().getConnection().add(...);
  }
}
```

**Step 2: Register in Catalog (or auto-discover via SPI)**
```java
public class MyCatalog implements IQIntent.Factory {
  @Override
  public IQIntent create(String name) {
if (name.equals("my-custom-intent")) {
  return new MyCustomIntent();
}
return null;
  }
}
```

**Step 3: Trigger from Agent**
```java
agent.executeIntent("my-custom-intent");
```

#### **Adding a Custom Connector**

**Step 1: Extend `IConnector`**
```java
public class MyConnector extends AbstractConnector {
  @Override
  public void sync() throws Exception {
// Fetch data from external system
List<Resource> data = fetchFromExternalAPI();

// Reify as RDF
for (Resource item : data) {
  Model triples = rdfTranslator.toModel(item);
  realm.getRepository().add(triples);
}
  }
}
```

**Step 2: Create Module**
```
iq-connect/iq-connect-myservice/
├── pom.xml
├── README.md
└── src/main/java/systems/symbol/connect/myservice/
└── MyConnector.java
```

**Step 3: Add to Parent POM**
```xml
<module>iq-connect/iq-connect-myservice</module>
```

#### **Adding a Custom Decision Maker**

**Step 1: Implement `I_Decision`**
```java
public class MyDecision implements I_Decision {
  @Override
  public Resource decide(I_Agent agent, Resource currentState) {
// Custom logic to pick next state
return nextState;
  }
}
```

**Step 2: Compose into ChainOfCommand**
```java
ChainOfCommand chain = new ChainOfCommand(
  new MyDecision(),
  new LLMDecision(llm, realm),
  new DefaultDecision()
);
agent.setDecisionMaker(chain);
```

#### **Adding a Custom Script/Prompt Catalog**

**Step 1: Implement `IQScriptCatalog`**
```java
public class MyScriptCatalog implements IQScriptCatalog {
  @Override
  public Iterable<I_Script> scripts() {
return List.of(
  new NamedScript("my-query", "classpath:/sparql/my-query.sparql"),
  new NamedScript("my-prompt", "classpath:/prompts/my-prompt.txt")
);
  }
}
```

**Step 2: Register via SPI**
```
src/main/resources/META-INF/services/systems.symbol.platform.IQScriptCatalog
```
```
com.mycompany.MyScriptCatalog
```

#### **Adding Custom GraphQL Types/Resolvers**

TTL + directives:
```graphql
type Order @rdf(iri:"http://schema.org/Order") {
  id: String! @rdf(property:"http://schema.org/identifier")
  customer: Person @rdf(property:"http://schema.org/customer")
  totalPrice: Float @rdf(property:"http://acme.com/totalPrice")
  status: String @sparql(query:"get-order-status.sparql")
}

type Query {
  orders(limit: Int = 10): [Order]
@sparql(query:"get-all-orders.sparql", args: ["limit"])
}
```

The `SPARQLDataFetcher` translates GraphQL queries to SPARQL, executes them, and returns typed results.

### 7.5 Environment-Specific Configuration

Use env vars for runtime control:

```bash
# Realm selection
export IQ=default

# LLM provider selection
export IQ_LLM_PROVIDER=groq-llama

# Secret sources
export VAULT_PASSWORD=master-key-for-vault

# Realm-specific secrets
export DEFAULT_OPENAI_API_KEY=sk-...
export ACME_POSTGRES_PASSWORD=...

# Tuning
export IQ_AGENT_THREAD_POOL_SIZE=20
export IQ_SPARQL_TIMEOUT_MS=30000
export IQ_LLM_CACHE_SIZE=1000
export IQ_REPOSITORY_TYPE=native   # or "memory", "sparql" for remote, etc.

# Cluster
export IQ_CLUSTER_ENABLED=true
export IQ_CLUSTER_NODE_ID=node-1
export IQ_CLUSTER_SEED_NODES=node-1:5555,node-2:5555

# Tracing & Observability
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_SDK_DISABLED=false
```

---

## 8. Complete User Story Foundation

With the above architecture, you can now write user stories covering:

### Epic 1: Agent Autonomy & Workflow
- As an operations manager, I want agents to execute multi-step workflows (e.g., order fulfillment) with LLM-assisted decisions so that routine processes run 24/7 without human intervention
- As a domain expert, I want to modify agent behavior (add new transitions, change decision rules) by editing RDF files, not Java code, so that business rule changes are fast and audit-clear
- As a compliance officer, I want every agent decision traceable to its source (LLM input, guard condition, domain fact) so that I can explain decisions to regulators

### Epic 2: Data Integration & Knowledge Graph
- As a data engineer, I want to ingest data from 25+ cloud systems (AWS, Salesforce, Snowflake, etc.) into a unified RDF knowledge graph so that agents can reason across silos
- As a data analyst, I want to query the knowledge graph with SPARQL to answer business questions (e.g., "Which orders are stuck in Processing state?") without writing SQL
- As a semantic architect, I want to define domain ontologies (classes, relationships, constraints) so that all integrations map consistently to a shared vocabulary

### Epic 3: Security & Governance
- As a security architect, I want per-realm JWT tokens, encrypted vaults, and SPARQL-based access policies so that tenant data stays isolated even in multi-node deployments
- As a compliance officer, I want all actions recorded in an audit trail (PROV-O) with actor, timestamp, outcome, and cost so that I can generate compliance reports
- As a policy owner, I want to define role-based access control rules in RDF (no code) so that policy changes take effect immediately

### Epic 4: Scalability & Performance
- As an ops engineer, I want IQ to scale horizontally across multiple nodes with automatic leader election and policy distribution so that we can handle 1M agents
- As a cost manager, I want to track LLM token spend per agent/realm/tenant and enforce budget limits so that runaway costs are prevented
- As a performance engineer, I want caching, batching, and lazy-loading of agent state so that response times stay under 100ms for typical chat queries

### Epic 5: LLM Integration & Decision Making
- As an AI product manager, I want to configure multiple LLM providers (OpenAI, Groq, Azure, custom) in RDF so that we can swap providers without redeployment
- As an agent developer, I want named prompt templates (e.g., "decide-next-action") registered in catalogs so that agents reference them by name
- As a reliability engineer, I want graceful fallback if LLM is slow/down (use default decision, cached response, or escalate to human) so that agents remain operational

### Epic 6: Multi-Tenancy & Isolation
- As a platform architect, I want to run multiple realms (customer orgs, business units, projects) in one IQ instance with complete isolation so that we have a true SaaS platform
- As an enterprise customer, I want to define my own agents, workflows, and connectors within my realm without affecting other tenants
- As an admin, I want to migrate a realm (export/import) between IQ instances without downtime

---

## Summary Table: Key Concepts

| Concept | Definition | Example |
|---------|-----------|---------|
| **Realm** | Isolated tenant; has repo, agents, secrets, JWT key | `acme-fulfillment`, `partner`, `internal-ai` |
| **Agent** | Autonomous entity; has FSM, intents, decision-maker, budget | `fulfillmentAgent`, `approvalAgent` |
| **Intent** | Atomic action (execute script, call LLM, update state, query knowledge) | `ExecutiveIntent`, `Remodel`, `Think`, `Search` |
| **FSM** | RDF-backed finite state machine; guards (SPARQL ASK) control transitions | `:orderFSM` with states Received→Processing→Shipped |
| **Connector** | Adapts external system (AWS, Slack, DB) into RDF knowledge graph | `iq-connect-aws`, `iq-connect-slack` |
| **LLMFactory** | Resolves LLM config from RDF; retrieves secrets; returns wrapper | Configures GPT-4, Llama, Azure OpenAI |
| **Script Catalog** | Registry of named SPARQL queries, prompts, Groovy scripts | `IQScriptCatalog`, `ModelScriptCatalog` |
| **Policy** | SPARQL ASK template + roles; enforces access control | `:PolicyProcessOrders`, `:PolicyViewFacts` |
| **Audit Trail** | PROV-O triples recording every action, actor, time, outcome | Auto-generated after each agent transition |
| **Vault** | Encrypted keystorage; holds API keys, DB passwords, OAuth tokens | `VFSPasswordVault` (`.enc` files), `EnvsAsSecrets` |
| **MCP** | Model Context Protocol; exposes IQ tools (SPARQL, intents) to external LLMs | `iq-mcp` module with tool manifests |

---

## Key Files to Review for Implementation

1. **Architecture & Design:**
   - `/README.md` — System overview
   - `/iq-docs/docs/RATIONALE.md` — Design decisions
   - `/iq-docs/docs/SEMANTICS.md` — RDF-first philosophy
   - `/iq-docs/docs/GUIDE.md` — Getting started

2. **Core API:**
   - `/iq-apis/README.md` — REST API endpoints
   - `/iq-platform/README.md` — Agent engine, realm lifecycle
   - `/iq-trusted/README.md` — Auth, secrets, JWT
   - `/iq-agentic/README.md` — Agent builder, decisions

3. **Implementation Details:**
   - `/iq-platform/src/main/java/systems/symbol/realm/RealmManager.java` — Realm bootstrap
   - `/iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` — LLM configuration
   - `/iq-platform/src/main/java/systems/symbol/fsm/ModelStateMachine.java` — FSM execution
   - `/iq-apis/src/main/java/systems/symbol/controller/` — REST controllers
   - `/iq-apis/src/main/java/systems/symbol/controller/control/ControlPlaneAPI.java` — Clustering

4. **Configuration Examples:**
   - `/.iq/repositories/` — Repository configs
   - `/.iq/runtime/` — LLM providers, prompts, runtime maps
   - `/iq-platform/src/main/resources/rdf4j/` — Repository templates (TTL)

5. **Connectors & Integration:**
   - `/iq-connect/README.md` — Connector architecture
   - `/iq-connect/iq-connect-template/` — Template for new connectors
   - Individual connector modules for examples

6. **Authorization & Governance:**
   - `/iq-docs/docs/POLICY.md` — Policy templates, ACL examples
   - `/iq-docs/docs/ONTOLOGY.md` — What can be modeled and executed

7. **Advanced Topics:**
   - `/iq-docs/docs/SEARCH.md` — Semantic search, embeddings, knowledge graph navigation
   - `/iq-docs/docs/MCP.md` — Model Context Protocol tool definitions
   - `/iq-docs/docs/USECASES.md` — Concrete use cases with links to examples

---

**End of Comprehensive Overview**
