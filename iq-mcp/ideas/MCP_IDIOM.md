---
id: mcp:idiom/rdf-first-abstraction
type: mcp:IdiomaticDesign
version: "1.0"
status: active
updated: "2026-03-19"
philosophy: "RDF-First, Graph-as-API, Zero Static Java Bloat"
---

# MCP → RDF Mapping: The Idiomatic & Abstract Layer

## The Problem with Current MCP Integration

The current design (`MCP_DESIGN.md`, the "4 Pillars") is **pragmatic but bottom-up**: it treats Java adapters as the ground truth and RDF as a secondary concern. Each capability (sparql.query, sparql.update, rdf.describe, actor.trigger) is a hardcoded Java class that maps to RDF execution.

This works but has systemic costs:
1. **Cognitive friction**: An LLM using IQ needs to know "use `sparql.query` for reads, `sparql.update` for writes, `actor.trigger` for side effects." Why 3 tools? Why not one?
2. **Discovery gap**: There is no self-describing, runtime-discoverable link between MCP tools and their RDF manifestations. The mapping is implicit in Java code.
3. **Extension inertia**: Adding a new domain-specific workflow, script, or agent requires touching Java. This violates the "RDF-first" principle.
4. **Governance scatter**: Policies, quotas, auditing, and role bindings are split between middleware code and RDF models. No single source of truth.

---

## The Idiomatic Shift: Tools ARE Triples

In an idiomatic RDF-first system, **the tool is the triple; the Java code is the plumbing.**

### Core Insight
Instead of asking "what Java class should implement this capability?" ask:
- **"What RDF triple describes the desired behavior?"** 
- **"Can we materialize that triple into an executable tool at zero Java cost?"**

When an `iq:Script` exists in the graph, it is **intrinsically** an MCP Tool. (skos:prefLabel and skos:definition likely also required)  
When an `iq:Agent` (inferred from `?agent <iq:initial> ?initial` has a valid state transition, that transition is **intrinsically** an MCP Tool.
When a `iq:Query` is registered, it is **intrinsically** an MCP Resource.

The Java adapter layer does not invent tools; it **discovers and executes** them.

---

## 1. RDF Model for MCP Concepts: The Metadata Layer

### 1.1 Ontological Foundation

Define a minimal MCP vocabulary (namespace `mcp:`) that mirrors MCP's own conceptual model:

```turtle
# MCP Vocabulary (mcp:* namespace)

# Tool: Abstract representation of an executable action
mcp:Tool a owl:Class ;
    rdfs:comment "An executable action available to an LLM." .

# Tool properties
mcp:toolName a owl:DatatypeProperty ;
    rdfs:range xsd:string ;
    rdfs:comment "The stable tool identifier (e.g., 'sparql.query')." .

mcp:toolKind a owl:ObjectProperty ;
    rdfs:range [ owl:oneOf (mcp:PrimitiveRead mcp:PrimitiveWrite mcp:ScriptExecution mcp:AgentTransition) ] ;
    rdfs:comment "Classifies the tool by execution strategy." .

mcp:inputSchema a owl:ObjectProperty ;
    rdfs:range rdf:JSON ;
    rdfs:comment "JSON Schema for tool inputs (as RDF JSON literal)." .

mcp:description a owl:DatatypeProperty ;
    rdfs:range xsd:string ;
    rdfs:comment "Human-readable description for the LLM." .

mcp:rateLimit a owl:DatatypeProperty ;
    rdfs:range xsd:integer ;
    rdfs:comment "Default max invocations per minute." .

mcp:isReadOnly a owl:DatatypeProperty ;
    rdfs:range xsd:boolean ;
    rdfs:comment "Whether the tool mutates state." .

# Asset: Generalization of tool, resource, or policy
mcp:Asset a owl:Class ;
    rdfs:comment "Any discoverable artifact (tool, resource, policy, prompt)." .

mcp:executedBy a owl:ObjectProperty ;
    rdfs:domain mcp:Tool ;
    rdfs:range [ owl:unionOf (iq:Script iq:Query iq:Agent ją:Primitive) ] ;
    rdfs:comment "Links a tool to its RDF-native backing." .

# Audit trail
mcp:MCPAuditEvent a owl:Class ;
    rdfs:comment "Structured audit log for MCP invocations." .

mcp:invokedTool a owl:ObjectProperty ;
    rdfs:domain mcp:MCPAuditEvent ;
    rdfs:range mcp:Tool .

mcp:principal a owl:ObjectProperty ;
    rdfs:domain mcp:MCPAuditEvent ;
    rdfs:range iq:Principal .

mcp:startTime a owl:DatatypeProperty ;
    rdfs:range xsd:dateTime ;
    rdfs:domain mcp:MCPAuditEvent .

mcp:success a owl:DatatypeProperty ;
    rdfs:range xsd:boolean ;
    rdfs:domain mcp:MCPAuditEvent .
```

### 1.2 Mapping Existing IQ Concepts to MCP

The ontology above is *minimal*—it doesn't replace `iq:Script`, `iq:Agent`, or `iq:Policy`. Instead, it adds **bridge properties** that *assert* when and how these artifacts become MCP tools:

```turtle
# Example: A registered script becomes an MCP tool

ex:MyReportScript a iq:Script ;
    iq:name "generate_monthly_report" ;
    iq:shape ex:ReportShape ;
    mcp:executedBy ex:ReportScriptTool ;
    rdfs:comment "A reusable reporting script." .

ex:ReportScriptTool a mcp:Tool ;
    mcp:toolName "script.generate_monthly_report" ;
    mcp:toolKind mcp:ScriptExecution ;
    mcp:isReadOnly false ;
    mcp:rateLimit 5 ;
    mcp:executedBy ex:MyReportScript ;
    mcp:description "Generate monthly sales report with filters." ;
    mcp:inputSchema "{\"type\": \"object\", \"properties\": {...}}" .  # JSON-LD serialized
```

---

## 2. The Discovery Engine: Tool Registry as SPARQL

Instead of a hardcoded `MCPToolRegistry` Java class, implement a **SPARQL-backed registry**:

```java
// Pseudo-code: how the tool registry dynamically discovers tools

public class SparqlToolRegistry implements I_ToolRegistry {
    private final Repository repository;

    @Override
    public Collection<I_MCPTool> discoverTools(IRI realm) throws Exception {
        String query = """
            PREFIX mcp: <http://mcp.symbol.systems/>
            PREFIX iq: <http://iq.symbol.systems/>
            
            SELECT ?toolIri ?toolName ?kind ?description ?limit ?readOnly
            WHERE {
              ?toolIri a mcp:Tool ;
                mcp:toolName ?toolName ;
                mcp:toolKind ?kind ;
                mcp:description ?description ;
                mcp:rateLimit ?limit ;
                mcp:isReadOnly ?readOnly .
              FILTER( ! isBlank( ?toolIri ) )
            }
            ORDER BY ?toolName
            """;
        
        try (RepositoryConnection conn = repository.getConnection()) {
            TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
            List<I_MCPTool> tools = new ArrayList<>();
            while (result.hasNext()) {
                BindingSet bs = result.next();
                tools.add(
                    new DynamicMCPToolAdapter(
                        (IRI) bs.getValue("toolIri"),
                        (Literal) bs.getValue("toolName"),
                        // ... other metadata
                        repository
                    )
                );
            }
            return tools;
        }
    }
}
```

**Key advantage**: When you add a new `mcp:Tool` triple to the graph, it automatically becomes discoverable. No Java recompilation.

---

## 3. The Execution Layer: Polymorphic Tool Adapters

Each tool discovered via SPARQL delegates to a **polymorphic executor** based on its `mcp:toolKind`:

```java
/**
 * DynamicMCPToolAdapter: Single polymorphic adapter that executes any tool
 * by looking up its kind and backing artifact in the graph.
 */
public class DynamicMCPToolAdapter implements I_MCPTool {
    
    private final IRI toolIri;
    private final String toolName;
    private final IRI toolKind;  // Discriminator
    private final IRI backingArtifact;  // The iq:Script / iq:Agent / iq:Query it runs
    private final Repository repository;
    
    @Override
    public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
        try {
            // Dispatch by kind
            return switch (toolKind) {
                case MCP_SCRIPT_EXECUTION -> executeScript(backingArtifact, input, ctx);
                case MCP_AGENT_TRANSITION -> executeAgentTransition(backingArtifact, input, ctx);
                case MCP_SPARQL_QUERY -> executeSparqlQuery(input, ctx);
                case MCP_SPARQL_UPDATE -> executeSparqlUpdate(input, ctx);
                default -> throw MCPException.badRequest("Unknown tool kind: " + toolKind);
            };
        } catch (MCPException ex) {
            throw ex;
        } catch (Exception ex) {
            throw MCPException.internal("Tool execution failed: " + ex.getMessage(), ex);
        }
    }
    
    /** Execute an iq:Script identified by backingArtifact */
    private I_MCPResult executeScript(IRI scriptIri, Map<String, Object> input, MCPCallContext ctx)
            throws Exception {
        // Load the script's shape from the graph
        IRI shapeUri = loadScriptShape(scriptIri);
        
        // Validate input against SHACL
        InputValidator.validateAgainstShape(input, shapeUri, repository);
        
        // Execute via IQScriptCatalog
        return scriptCatalog.execute(scriptIri, input, ctx);
    }
    
    /** Execute an iq:Agent transition */
    private I_MCPResult executeAgentTransition(IRI agentIri, Map<String, Object> input, MCPCallContext ctx)
            throws Exception {
        // Construct the intent from input
        Intent intent = Intent.fromMap(input);
        
        // Delegate to agent execution
        return agentService.executeIntent(agentIri, intent, ctx);
    }
    
    // ... similar for SPARQL_QUERY, SPARQL_UPDATE
}
```

**Philosophy**:
- No massive `SparqlQueryAdapter.java`, `ActorTriggerAdapter.java`, etc. Instead, **one adapter with switch logic**.
- The discriminator (`mcp:toolKind`) is embedded in the RDF, not hardcoded.
- New tool kinds can be added without touching the adapter: just define a new `mcp:ToolKind` value and a corresponding RDF query that populates the backing artifact.

---

## 4. Governance as Native RDF: Policies, Quotas, Audit

### 4.1 Policies: SPARQL-Based Access Control

Instead of Java `AuthGuardMiddleware` hardcoding policy rules, policies live as RDF and are evaluated with SPARQL `ASK`:

```turtle
# Example: Policy that allows "analyst" role to invoke sparql.query tools

ex:AllowAnalystSparqlQuery a iq:Policy ;
    iq:target ex:ReportScriptTool ;  # or use iq:targetClass to match all tools of a kind
    iq:rule """
        PREFIX iq: <http://iq.symbol.systems/>
        PREFIX mcp: <http://mcp.symbol.systems/>
        ASK {
          ?principal iq:hasRole iq:Analyst .
          ?principal iq:canInvoke ?tool .
          ?tool mcp:toolKind mcp:ScriptExecution .
        }
    """;
    iq:consequence iq:Permit .
```

**Java evaluator**:
```java
public class SparqlPolicyEvaluator implements I_PolicyEvaluator {
    public boolean evaluate(IRI principal, IRI tool, String policyQuery, Repository repo)
            throws Exception {
        try (RepositoryConnection conn = repo.getConnection()) {
            BooleanQuery query = conn.prepareBooleanQuery(
                policyQuery.replace("?principal", "<" + principal + ">")
                           .replace("?tool", "<" + tool + ">")
            );
            return query.evaluate();
        }
    }
}
```

**Benefit**: Policies are auditable (you can SPARQL over them), versionable (stored in realms), and don't require code changes.

### 4.2 Quotas & Cost Tracking: RDF Counters

Instead of in-memory quota maps, store quotas and usage as RDF triples:

```turtle
# Define a rate limit on a tool
ex:ReportScriptTool 
    iq:rateLimit 5 ;  # Max 5/minute
    iq:costModel ex:StandardCostModel .

# Track usage as triples (periodically materialized, archived, and reset)
ex:AliceUsage a mcp:UsageRecord ;
    mcp:usedTool ex:ReportScriptTool ;
    mcp:executedBy ex:Alice ;
    mcp:timestamp "2026-03-19T14:32:00Z"^^xsd:dateTime ;
    mcp:success true .

ex:AliceUsage2 a mcp:UsageRecord ;
    mcp:usedTool ex:ReportScriptTool ;
    mcp:executedBy ex:Alice ;
    mcp:timestamp "2026-03-19T14:33:15Z"^^xsd:dateTime ;
    mcp:success true .

# Query: How many times did Alice use ReportScriptTool in the last minute?
# SELECT (COUNT(?usage) as ?count) 
# WHERE { 
#   ?usage mcp:usedTool ex:ReportScriptTool ;
#          mcp:executedBy ex:Alice ;
#          mcp:timestamp ?ts .
#   FILTER( ?ts > "2026-03-19T14:32:00Z"^^xsd:dateTime ) 
# }
```

**Benefit**: Quotas are queryable, auditable, and can be dynamically adjusted without code changes.

### 4.3 Audit Trail: Structured RDF Events

Every MCP invocation produces an `mcp:MCPAuditEvent`:

```turtle
ex:Audit_20260319_143200_a1b2c3 a mcp:MCPAuditEvent ;
    mcp:invokedTool ex:ReportScriptTool ;
    mcp:principal ex:Alice ;
    mcp:realm ex:SalesRealm ;
    mcp:startTime "2026-03-19T14:32:00Z"^^xsd:dateTime ;
    mcp:endTime "2026-03-19T14:32:02.531Z"^^xsd:dateTime ;
    mcp:inputSchema "{\"filters\": [...], \"format\": \"csv\"}" ;
    mcp:success true ;
    mcp:outputSize 12541 ;  # bytes
    mcp:trace "trace-a1b2c3-xyz" .
```

Audits are appended to a time-partitioned named graph (e.g., `iq://realm/sales/audit/2026-03`). This makes audit analysis trivial:

```sparql
# How many tool invocations succeeded in the last hour?
SELECT (COUNT(?evt) as ?successCount)
WHERE {
  GRAPH iq://realm/sales/audit/2026-03 {
    ?evt a mcp:MCPAuditEvent ;
         mcp:success true ;
         mcp:startTime ?ts .
    FILTER( ?ts > NOW() - PT1H )
  }
}
```

---

## 5. Resources as RDF Views: Self-Describing Context

The current design treats Resources (e.g., `iq://realm/schema`) as static snapshots. Idiomatically, **a Resource is a SPARQL query result**, cached/materialized as needed:

```java
public class DynamicRdfResourceProvider implements I_ResourceProvider {
    
    private final Repository repository;
    
    @Override
    public I_MCPResource provideResource(String resourceUri) throws MCPException {
        // Parse the resource URI
        // e.g., "iq://sales/schema" → realm="sales", view="schema"
        
        String query = switch (view) {
            case "schema" -> """
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                CONSTRUCT {
                  ?class a owl:Class ;
                         rdfs:comment ?comment ;
                         rdfs:label ?label .
                  ?prop a owl:Property ;
                        rdfs:domain ?domain ;
                        rdfs:range ?range .
                }
                WHERE {
                  GRAPH ?realm {
                    ?class a owl:Class ;
                           rdfs:comment ?comment ;
                           rdfs:label ?label .
                    ?prop rdfs:domain ?class ;
                           rdfs:range ?range .
                  }
                }
                """;
                
            case "policy" -> """
                PREFIX iq: <http://iq.symbol.systems/>
                CONSTRUCT {
                  ?policy a iq:Policy ;
                         iq:target ?target ;
                         iq:rule ?rule .
                }
                WHERE { ... }
                """;
                
            default -> throw MCPException.badRequest("Unknown resource: " + view);
        };
        
        // Execute and cache
        try (RepositoryConnection conn = repository.getConnection()) {
            GraphQueryResult result = conn.prepareGraphQuery(query).evaluate();
            Model model = QueryResults.asModel(result);
            String turtle = RDFWriter.write(model, RDFFormat.TURTLE);
            
            return MCPResource.builder()
                .uri(resourceUri)
                .mimeType("text/turtle")
                .contents(turtle)
                .build();
        } catch (Exception ex) {
            throw MCPException.internal("Failed to load resource: " + ex.getMessage(), ex);
        }
    }
}
```

**Benefit**: Resources are derived from live RDF, always in sync, and queryable.

---

## 6. The "Self" Resource: Self-Describing IQ Topology

Add a special resource `iq://self/` that describes the entire MCP landscape:

```turtle
# iq://self/topology (returned as RDF)

iq:MCP a mcp:System ;
    mcp:version "0.91.5" ;
    mcp:hasRealm ex:SalesRealm, ex:AnalyticsRealm, ex:AdminRealm ;
    mcp:hasToolCount 47 ;
    mcp:hasScriptCount 12 ;
    mcp:hasAgentCount 5 .

ex:SalesRealm a iq:Realm ;
    rdfs:label "Sales Data Realm" ;
    iq:hasSize 120000 ;  # triple count
    iq:hasGraph iq://sales/schema, iq://sales/data, iq://sales/audit ;
    mcp:availableTools [ 
        a rdf:List ; 
        rdf:first ex:ReportScriptTool ; 
        rdf:rest [ 
            rdf:first ex:SalesAgentTool ; 
            rdf:rest rdf:nil 
        ] 
    ] .
```

The LLM can fetch `iq://self/*` to understand the complete topology before making requests.

---

## 7. Prompt Injection: Self-Contained Tool Metadata

Each `mcp:Tool` carries its own usage instructions in the RDF:

```turtle
ex:ReportScriptTool
    mcp:description "Generate monthly sales report with optional filters." ;
    mcp:examples """
        Example 1: Generate report for Q1 2026
        {
          "month": "2026-03",
          "format": "csv",
          "includeForecasts": true
        }
        
        Example 2: Generate report for specific salesperson
        {
          "salesperson": "alice@example.com",
          "format": "json"
        }
    """ ;
    mcp:notes """
        - Requires 'analyst' role.
        - Returns up to 100k rows.
        - Processing time: 2-10 seconds depending on filters.
        - Cost: 1 unit per invocation.
    """ .
```

When the tool is discovered, these metadata fields are included in the MCP Tool definition sent to the LLM. **Zero hardcoding in Java prompts.**

---

## 8. Implementation Roadmap: Idiom-First

### Phase 0: Ontology & Foundation
- [ ] Define the `mcp:` vocabulary (owl, shacl).
- [ ] Add `mcp:` triples to bootstrap realms (dev, test, production).
- [ ] Write SPARQL queries for registry, policy evaluation, quota checking.

### Phase 1: Discovery & Execution
- [ ] Implement `SparqlToolRegistry` (replace hardcoded list).
- [ ] Implement `DynamicMCPToolAdapter` (single polymorphic adapter).
- [ ] Integrate with existing `IQScriptCatalog`, `AgentService`.

### Phase 2: Governance
- [ ] Implement `SparqlPolicyEvaluator` (replace hardcoded `AuthGuardMiddleware`).
- [ ] Implement RDF-based quota tracking.
- [ ] Implement structured audit trail writing.

### Phase 3: Resources & Context
- [ ] Implement `DynamicRdfResourceProvider`.
- [ ] Add `iq://self/topology`, `iq://self/realms`, etc.
- [ ] Wire resource discovery into MCP capability lists.

### Phase 4: Integration & Testing
- [ ] E2E test: Add an `iq:Script` to the graph → verify it appears in MCP tools.
- [ ] E2E test: Invoke a dynamically discovered tool → verify it executes.
- [ ] E2E test: Add a policy rule to the graph → verify it enforces access.
- [ ] Backward compatibility: Ensure existing tools (sparql.query, sparql.update) still work.

---

## 9. Comparison: Pragmatic vs. Idiomatic

| Aspect | Current (Pragmatic) | Idiomatic |
|--------|---------------------|-----------|
| **Tool Discovery** | Hardcoded Java classes | SPARQL query over mcp:Tool triples |
| **Adding a new tool** | Write Java adapter class, recompile, deploy | Add `mcp:Tool` triple, done |
| **Policies** | Java `AuthGuardMiddleware` | RDF triples + SPARQL ASK |
| **Quotas** | In-memory maps | RDF counters (auditable, queryable) |
| **Audit** | Logged to strings | Structured RDF events in named graphs |
| **Resources** | Static snapshots | SPARQL-derived views, live |
| **Governance** | Scattered across files | Single source of truth in RDF |
| **LLM discovers tools** | Fixed manifest | Dynamic based on graph state |

---

## 10. Why This Matters: The RDF-First Principle

The entire IQ ecosystem is built on the premise that **the graph is the program**. LLMs, agents, workflows, governance—all are expressible as RDF triples first, Java code second.

MCP integration was treating it backwards: Java adapters were ground truth, RDF was support. This idiom flips it: **RDF triples describe the MCP landscape**, and Java adapters are thin, polymorphic, mechanical layers.

Benefits:
1. **Consistency**: MCP tools obey the same governance and auditability rules as agents and scripts.
2. **Extensibility**: New domains (scripts, agents, workflows) require only RDF triples, not Java.
3. **Queryability**: The entire tool landscape is SPARQL-queryable: policies, quotas, usage, audit trails.
4. **Self-Description**: IQ can explain itself to the LLM via RDF Resources.
5. **Zero Cognitive Friction**: One universal interface (SPARQL + RDF), not three (sparql.query, sparql.update, actor.trigger).

---

## 11. References & Inspirations

- **IQ Philosophy**: RDF-first semantics, SPARQL as lingua franca, agents as state machines over RDF models.
- **API Agent Pattern** (`iq-agentic`): Uses `Model` (RDF4J) as the core abstraction, not Java POJOs. This idiom brings that pattern to MCP.
- **Pragmatic Design** (`MCP_DESIGN.md`): Good pragmatic baseline; this idiom is the aspirational completion.
- **Governance-as-RDF**: Matches IQ's existing policy and audit patterns.

