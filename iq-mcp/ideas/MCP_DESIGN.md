---
id: mcp:design/pragmatic-surface
type: mcp:DesignDocument
version: "1.0"
status: living
updated: "2026-03-19"
audience: [architect, developer]
relates-to: MCP_CAP.md, MCP_ARCHITECTURE.md
---

# Pragmatic MCP Design: The Minimal Abstract Surface

> **The Mandate**
> Do not over-engineer. Do not write a Java class for every capability. IQ is already a hyper-dynamic, RDF-first system powered by RDF4J. A pragmatic MCP integration should expose the absolute minimum Java surface area required to let an LLM leverage the full Turing-complete power of SPARQL, SHACL, and IQ Agents. 
> 
> We will handle all capabilities defined in `MCP_CAP.md` without remodeling the dynamic domains into static Java objects.

---

## 1. The Core Philosophy: Collapse Tools into Queries

Many MCP implementations make the mistake of creating a rigid tool for every domain action (e.g., `getUser`, `listOrders`, `getNeighbors`). In a graph ecosystem, this is a fatal anti-pattern. Every new domain requires new Java code, new tests, and new deployments.

**The Pragmatic Solution:**
The graph *is* the API. We expose generic semantic primitives. If the LLM wants `get_neighbors`, it doesn't need a `getNeighbors` tool; it needs to know the correct SPARQL `CONSTRUCT` query to run via a generic `sparql.query` tool. 

Capabilities shift from being **Java code (Tools)** to **Text templates (MCP Prompts)**.

---

## 2. The Minimal Surface Area (The 4 Pillars)

To support the entire L0-L5 capability matrix, we only need to implement **four** primary Java primitives mapped to MCP concepts:

### Pillar A: The Universal Read (Resource & Tool)
- **MCP Tool**: `sparql.query`
- **Backend Map**: Routes directly to `RepositoryConnection.prepareTupleQuery()` or `prepareGraphQuery()`.
- **Why**: Handles 80% of `MCP_CAP.md` (Schema Awareness, Entity Access, Search, Aggregation).
- **Pragmatic Hack**: Let RDF4J handle Lucene indexing (for semantic search), federation (`SERVICE` keyword), and inferencing (via custom Sails). The MCP Java adapter does nothing but authorize the query and format the result as JSON-LD.

### Pillar B: The Universal Write (Tool)
- **MCP Tool**: `sparql.update`
- **Backend Map**: Routes to `RepositoryConnection.prepareUpdate()`.
- **Why**: Handles Graph Mutation (Create, Update, Link, Delete, Annotate, Checkpoint).

### Pillar C: The Agent Bridge (Tool)
- **MCP Tool**: `actor.trigger` (or `intent.execute`)
- **Backend Map**: Routes to agentic event buses.
- **Why**: Graph mutation isn't enough for system side-effects (e.g., sending an email, triggering a pipeline). We map one generic tool that takes an `iq:Intent` URI and an `arguments` map.

### Pillar D: Context & Schema (Resources)
- **MCP Resources**: `iq://realm/schema`, `iq://realm/policy`, `iq://self/namespaces`.
- **Backend Map**: Read-only static endpoints serving cached or materialized graph views in Turtle/JSON-LD.
- **Why**: The LLM needs context *before* it can write a valid SPARQL query. These resources load into the LLM's context window automatically.

---

## 3. Handling the Capability Matrix (Capability -> Implementation)

Instead of writing 30 Java classes, we fulfill `MCP_CAP.md` using the 4 Pillars. We bridge the gap using **MCP Prompts** (pre-defined instructions the LLM can pull to know *how* to use the 4 pillars).

| Capability Domain | How we achieve it pragmatically |
|-------------------|---------------------------------|
| **Schema Awareness** | Served via `iq://{{realm}}/schema` Resource. |
| **SHACL Awareness** | Served via `iq://{{realm}}/SHACL` Resource. |
| ** SPARQL Catalog** | Served via `iq://{{realm}}/SPARQL` Resource. |
| **Entity Access** | LLM uses `sparql.query` with `DESCRIBE <uri>`. |
| **Search & Retrieval** | LLM uses `sparql.query`. Text search utilizes RDF4J Lucene Sail (`?s search:matches "query"`). |
| **Aggregation & Analytics**| LLM uses `sparql.query` with `COUNT`, `GROUP BY`. |
| **Reasoning & Inference** | We mount an RDF4J Inferencer Sail on the repository. `sparql.query` automatically returns inferred triples. |
| **Graph Mutation** | LLM uses `sparql.update` with `INSERT DATA` or `DELETE/INSERT`. |
| **Cross-Realm Federation** | LLM uses `sparql.query` utilizing the SPARQL `SERVICE <url>` keyword. |
| **Query Composition** | Delivered via an MCP Prompt: `prompt/compose_query` that tells the LLM "Translate the user's intent into a SPARQL query over the following schema..." |

---

## 4. Java / Quarkus Implementation Map

We will touch very few files to make this work. The core logic lives in `iq-mcp/src/main/java/systems/symbol/mcp/`

**1. `MCPServerEndpoint.java`**
- Standard Quarkus HTTP POST endpoint accepting JSON-RPC MCP payload.

**2. `tool/SparqlQueryAdapter.java`**
- Input: `{"query": "SELECT ..."}`
- Logic: Validates via ACL middleware -> Executes via `iq-rdf4j` -> Maps `BindingSet` to JSON array.

**3. `tool/SparqlUpdateAdapter.java`**
- Input: `{"update": "INSERT ..."}`
- Logic: Validates via ACL middleware -> Executes via `iq-rdf4j` -> Returns success/fail status.

**4. `tool/IntentTriggerAdapter.java`**
- Input: `{"intentUri": "iq:SyncData", "params": {...}}`
- Logic: Publishes event to Quarkus Vertex/Camel.

**5. `resource/SchemaResourceProvider.java`**
- Answers `resources/read` for URIs starting with `iq://`. Serializes named graphs dynamically.

---

## 5. Controlling the Blast Radius (Security)

A pragmatic architecture that exposes raw SPARQL assumes significant risk. We manage this entirely within the existing RDF/Gateway layers, not by complicating the core tool logic.

1. **Read/Write Segregation**: `sparql.query` strictly rejects `UPDATE`/`INSERT`/`DELETE` syntax.
2. **Access Control**: Handled via `MCPConnectPipeline.java` (as defined in `MCP_CONNECT.md`). Queries are intercepted and wrapped, or evaluated against restricted named graphs based on the `mcp:policy` for the current LLM principal.
3. **Timeouts**: Strictly enforced natively on the RDF4J `RepositoryConnection`.
4. **Result Pagination**: The `SparqlQueryAdapter` enforces hard Limits (e.g., `LIMIT 100`) via query rewriting or stream termination to prevent the LLM from blowing out its own context window and returning massive payload responses.

---

## 6. Conclusion 

By combining the **Turing-completeness of SPARQL** with the **instructional power of MCP Prompts**, we eliminate the need for heavy Java-side tool mappings. 

We can map a hyper-abstract cognitive framework to the IQ codebase in under 1,000 lines of Java code, shifting all the operational logic into standard RDF vocabularies, SPARQL strings, and LLM-side prompting.