---
id: mcp:todo
type: mcp:ProjectPlan
version: "2.0"
status: active
updated: "2026-03-19"
philosophy: "Dynamic First-Class Abstraction"
---

# MCP Integration Todo: The First-Class Dynamic Abstraction

## The Philosophy
Instead of forcing the LLM to write raw SPARQL or guess the internal ontology schema every time, we dynamically project the graph's native execution elements (`iq:Script`, `iq:Query`, `iq:Agent`, Named Graphs) into **first-class MCP Resources and Tools**. 

When a user adds a new `iq:Script` or `iq:Query` to the graph, it instantly becomes a native MCP tool (e.g., `script.generate_report`). When an `iq:Agent` is deployed, its transitions become first-class tools (e.g., `agent.claim.approve`). This leverages IQ's dynamic nature while providing the LLM with an intuitive, self-describing API surface.

The LLM will read/write JSON-LD (unless content-negotiated).


---

## 🛠 Phase 1: Foundation (Transport & Gateway)

**Goal:** Establish the MCP lifecycle, connection handlers, and the `MCPConnectPipeline` middleware.

- [ ] **1.1 MCP Java SDK Integration**
  - Add `mcp` and `mcp-jackson` dependencies to `iq-mcp/pom.xml`.
  - Create the raw Quarkus `MCPServerEndpoint` (HTTP/POST).
  - Implement the fundamental Server capability registry.

- [ ] **1.2 Gateway & Middleware (`MCPConnectPipeline`)**
  - Implement `AuthGuard` (validate IAM/JWT tokens).
  - Implement `ACLFilter` (evaluate standard `iq:policy` rules).
  - Implement `QuotaGuard` (rate limits).

---

## 📚 Phase 2: Dynamic Resources (Context & Geography)

**Goal:** Treat realms, named graphs, and schemas as first-class read-only ambient contexts (MCP Resources). 

- [ ] **2.1 Global Orientation**
  - `mcp://self/realms`: Resource listing all active realms and size metrics.
  - `mcp://self/namespaces`: Prefix mappings for SPARQL contexts.

- [ ] **2.2 Schema & Governance**
  - `mcp://realm/<id>/schema`: Resource holding the OWL/SHACL export of a realm.
  - `mcp://realm/<id>/policy`: Resource returning the active ACL mapping.

- [ ] **2.3 First-Class Graph Abstractions**
  - Implement `realm.list_graphs` (Resource template).
  - Implement `realm.graph_profile` (Resource template profiling types & instance counts).
  - Implement entity canonical reads (`entity.describe` taking a URI).

---

## 🚀 Phase 3: Dynamic Tools (Scripts, Queries & Agents)

**Goal:** The heart of the integration. Map IQ's catalog components dynamically into MCP Tool Manifests. No new Java needed per domain.

- [ ] **3.1 Dynamic `iq:Query` / `iq:Script` Adapter**
  - Bridge `IQScriptCatalog` (or `ModelScriptCatalog`) directly to the MCP Tool Registry.
  - Map `iq:Script` name strictly to tool name `script.<name>`.
  - Extract SHACL bindings / `iq:binding` properties from the script to build the MCP JSON Schema.
  - Implement the `ToolAdapter` that maps back to `ScriptRunner`/`SPARQLExecutor`.

- [ ] **3.2 First-class Agent Transitions (`iq:Agent`)**
  - Bridge the Agent fleet configurations to properties.
  - Auto-generate tools for valid state transitions: `agent.<actor_name>.<intent>`.
  - Wire to `systems.symbol.controller.ux.IntentAPI` (or equivalent `AgentService` logic).

- [ ] **3.3 The Core SPARQL Escapes**
  - Implement `self.vocab`: The registered prefixes for a realm.
  - Implement `self.void`: The VOID dataset for a realm.
  - Implement `self.prefixes`: The registered prefixes.
  - Implement `sparql.query`: The universal read fallback.
  - Implement `sparql.update`: The universal write fallback (guarded strictly).

---

## 🧠 Phase 4: Cognitive & Observability Edge 

**Goal:** Fully equip the LLM with the meta-tools to plan, test hypotheses, and verify constraints.

- [ ] **4.2 Advanced Search Contexts**
  - `search.semantic`: Map to `iq-finder` .
  - `search.suggest`: Auto-complete utility.

