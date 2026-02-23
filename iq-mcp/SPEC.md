# MCP Tool Specification for IQ


## Table of Contents

1. [Tool Naming & Categorization](#tool-naming--categorization)
2. [Fact Adapter Tools](#fact-adapter-tools)
3. [Actor Adapter Tools](#actor-adapter-tools)
4. [Trust Adapter Tools](#trust-adapter-tools)
5. [LLM Adapter Tools](#llm-adapter-tools)
6. [Realm Adapter Tools](#realm-adapter-tools)
7. [Common Audit Trail Format](#common-audit-trail-format)

---

## Tool Naming & Categorization

Tools follow the pattern: `{category}.{operation}`

**Output Format:** MCP outputs JSON-LD by default. When tools return RDF results, they may offer alternative RDF serializations (Turtle, N-Quads, N-Triples) upon client request.

| Category | Purpose | Examples |
|----------|---------|----------|
| `fact` | RDF graph queries and updates | `fact.sparql.query`, `fact.sparql.update`, `fact.describe` |
| `actor` | Agent lifecycle and transitions | `actor.trigger`, `actor.execute`, `actor.status` |
| `trust` | Identity and token management | `trust.login`, `trust.refresh`, `trust.verify` |
| `llm` | LLM invocation with context | `llm.invoke`, `llm.search`, `llm.explain` |
| `realm` | Realm inspection and governance | `realm.export`, `realm.import`, `realm.policy` |

---

## Fact Adapter Tools

Exposes RDF4J repository operations via SPARQL and graph inspection.

**Note:** MCP output is JSON-LD by default. Many tools return RDF results; clients MAY request other RDF serializations (e.g., Turtle, N-Quads, N-Triples) or plain types (e.g., xsd:boolean for ASK).

### Tool: `fact.sparql.query`

**Description**: Execute a SPARQL SELECT, ASK, or CONSTRUCT query and return results as RDF.

**Input Schema** (SHACL):
```ttl
:SparqlQueryInput a sh:NodeShape ;
  sh:targetNode :input ;
  sh:property [
    sh:path rdf:value ;
    sh:datatype xsd:string ;
    sh:name "SPARQL Query" ;
    sh:description "SPARQL 1.1 SELECT, ASK, or CONSTRUCT query" ;
    sh:minCount 1
  ] ;
  sh:property [
    sh:path iq:timeout ;
    sh:datatype xsd:integer ;
    sh:name "Query Timeout" ;
    sh:description "Maximum execution time in milliseconds (default: 30000)" ;
    sh:minInclusive 1000 ;
    sh:maxInclusive 300000
  ] .
```

**Output Schema** (SHACL):
```ttl
:SparqlQueryOutput a sh:NodeShape ;
  sh:targetNode :output ;
  sh:property [
    sh:path rdf:value ;
    sh:name "Result Set" ;
    sh:description "Result set as RDF; MCP outputs JSON-LD by default. Clients may request other RDF serializations (e.g., N-Triples, Turtle, N-Quads). For ASK queries the result is xsd:boolean." ;
    sh:minCount 0
  ] ;
  sh:property [
    sh:path iq:resultCount ;
    sh:datatype xsd:integer ;
    sh:name "Result Count" ;
    sh:description "Number of bindings or triples returned"
  ] .
```

**Authorization Query** (SPARQL ASK):
```sparql
ASK WHERE {
  ?caller iq:canQuery [
    sh:targetClass ?queryPattern
  ] ;
  BIND (
    IF(STRSTARTS(STR(?queryPattern), "SELECT"), "SELECT",
    IF(STRSTARTS(STR(?queryPattern), "CONSTRUCT"), "CONSTRUCT",
    "ASK"))
    AS ?queryType
  )
}
```

**Example Input**:
```ttl
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix iq: <http://symbol.systems/iq/> .

[] rdf:value "SELECT ?x WHERE { ?x rdf:type iq:Agent }" ;
   iq:timeout 5000 .
```

**Example Output**:
```ttl
@prefix ex: <http://example.org/> .
ex:agent-1 rdf:type iq:Agent .
ex:agent-2 rdf:type iq:Agent .
```

**Implementation**: [FactAdapter.java](src/main/java/systems/symbol/mcp/adapters/FactAdapter.java)

---

### Tool: `fact.sparql.update`

**Description**: Execute a SPARQL UPDATE (INSERT, DELETE, WITH, etc.) and return affected triples count.

**Input Schema**:
```ttl
:SparqlUpdateInput a sh:NodeShape ;
  sh:targetNode :input ;
  sh:property [
    sh:path rdf:value ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "SPARQL UPDATE query (INSERT DATA, DELETE WHERE, etc.)"
  ] ;
  sh:property [
    sh:path iq:dryRun ;
    sh:datatype xsd:boolean ;
    sh:description "If true, validate but do not execute (default: false)"
  ] .
```

**Output Schema**:
```ttl
:SparqlUpdateOutput a sh:NodeShape ;
  sh:property [
    sh:path iq:triplesInserted ;
    sh:datatype xsd:integer ;
    sh:description "Number of triples added"
  ] ;
  sh:property [
    sh:path iq:triplesDeleted ;
    sh:datatype xsd:integer ;
    sh:description "Number of triples removed"
  ] ;
  sh:property [
    sh:path iq:affectedGraphs ;
    sh:description "List of named graphs modified"
  ] .
```

**Authorization Query** (SPARQL ASK):
```sparql
ASK WHERE {
  ?caller iq:canUpdate [
    sh:targetClass ?targetGraph
  ] .
  FILTER EXISTS { ?targetGraph a iq:NamedGraph }
}
```

**Rate Limit**: 100 updates/min per actor (stricter than queries)

**Cost Model**: 
- Base cost: 50 units/update
- Per triple: 5 units inserted + 2 units deleted
- Rollback if quota exceeded

**Implementation**: [FactAdapter.java](src/main/java/systems/symbol/mcp/adapters/FactAdapter.java)

---

## Actor Adapter Tools

Exposes agent lifecycle and state machine transitions.

### Tool: `actor.trigger`

**Description**: Trigger an agent's next state transition by posting an intent. Synchronously executes state machine and returns updated agent state.

**Input Schema** (SHACL):
```ttl
:ActorTriggerInput a sh:NodeShape ;
  sh:property [
    sh:path iq:agent ;
    sh:nodeKind sh:IRI ;
    sh:minCount 1 ;
    sh:description "IRI of target agent"
  ] ;
  sh:property [
    sh:path iq:intent ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Intent label or expression (e.g., 'plan', 'think', 'act')"
  ] ;
  sh:property [
    sh:path iq:payload ;
    sh:description "Optional RDF payload (facts, context) to supply to agent"
  ] .
```

**Output Schema**:
```ttl
:ActorTriggerOutput a sh:NodeShape ;
  sh:property [
    sh:path iq:agent ;
    sh:description "Updated agent state (IRI + RDF triples)"
  ] ;
  sh:property [
    sh:path iq:state ;
    sh:nodeKind sh:Literal ;
    sh:description "Current state name (e.g., 'planning', 'executing', 'done')"
  ] ;
  sh:property [
    sh:path iq:workingMemory ;
    sh:description "Agent's working memory (RDF Model)"
  ] ;
  sh:property [
    sh:path iq:nextTransitions ;
    sh:description "List of possible next intents"
  ] .
```

**Authorization Query** (SPARQL ASK):
```sparql
ASK WHERE {
  ?caller iq:canTrigger ?agent .
  ?agent rdf:type iq:Agent ;
         iq:realm ?realm .
  ?caller iq:belongsTo ?realm
}
```

**Rate Limit**: 100 triggers/min per agent

**Cost Model**: Base cost 100 units + LLM tokens if LLM-based reasoning is used

**Example**:
```bash
curl -X POST http://localhost:8080/mcp/tools/actor.trigger \
  -H "Content-Type: application/rdf+xml" \
  -d '
    @prefix iq: <http://symbol.systems/iq/> .
    @prefix ex: <http://example.org/> .
    
    [] iq:agent ex:agent-42 ;
       iq:intent "plan" .
  '
```

**Implementation**: [ActorAdapter.java](src/main/java/systems/symbol/mcp/adapters/ActorAdapter.java)

---

### Tool: `actor.execute`

**Description**: Execute a specific action by an agent. Returns execution result and updated agent state.

**Input Schema**:
```ttl
:ActorExecuteInput a sh:NodeShape ;
  sh:property [
    sh:path iq:agent ;
    sh:nodeKind sh:IRI ;
    sh:minCount 1
  ] ;
  sh:property [
    sh:path iq:action ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Action script (Groovy or SPARQL UPDATE)"
  ] ;
  sh:property [
    sh:path iq:context ;
    sh:description "RDF context (facts available to action)"
  ] .
```

**Output Schema**:
```ttl
:ActorExecuteOutput a sh:NodeShape ;
  sh:property [
    sh:path iq:result ;
    sh:description "Action execution result (RDF Model or text)"
  ] ;
  sh:property [
    sh:path iq:success ;
    sh:datatype xsd:boolean
  ] ;
  sh:property [
    sh:path iq:error ;
    sh:datatype xsd:string ;
    sh:description "Error message if execution failed"
  ] .
```

**Rate Limit**: 50 executions/min (actions are more resource-intensive)

**Cost Model**: Base cost 200 units + custom metrics (e.g., external API calls)

---

### Tool: `actor.status`

**Description**: Query current agent state, working memory, and available transitions.

**Input Schema**:
```ttl
:ActorStatusInput a sh:NodeShape ;
  sh:property [
    sh:path iq:agent ;
    sh:nodeKind sh:IRI ;
    sh:minCount 1
  ] .
```

**Output Schema**: RDF describing agent's complete state (no cost).

**Rate Limit**: 10000 status checks/min (read-only, cheap operation)

---

## Trust Adapter Tools

Identity and credential management.

### Tool: `trust.login`

**Description**: Obtain a JWT access token for a given identity provider (GitHub, Google, custom).

**Input Schema**:
```ttl
:TrustLoginInput a sh:NodeShape ;
  sh:property [
    sh:path iq:realm ;
    sh:nodeKind sh:IRI ;
    sh:minCount 1 ;
    sh:description "Realm IRI"
  ] ;
  sh:property [
    sh:path iq:provider ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Identity provider (e.g., 'github', 'google')"
  ] ;
  sh:property [
    sh:path iq:credential ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Credential (OAuth code, API key, password hash)"
  ] ;
  sh:property [
    sh:path iq:ttl ;
    sh:datatype xsd:integer ;
    sh:description "Token TTL in seconds (default: 3600)"
  ] .
```

**Output Schema**:
```ttl
:TrustLoginOutput a sh:NodeShape ;
  sh:property [
    sh:path iq:token ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "JWT access token"
  ] ;
  sh:property [
    sh:path iq:expiresAt ;
    sh:datatype xsd:dateTime ;
    sh:description "Token expiration time"
  ] ;
  sh:property [
    sh:path iq:actor ;
    sh:nodeKind sh:IRI ;
    sh:description "Authenticated actor IRI"
  ] .
```

**Authorization Query** (SPARQL ASK):
```sparql
ASK WHERE {
  ?provider iq:supportedBy ?realm ;
            rdf:type iq:IdentityProvider .
  OPTIONAL { ?realm iq:requiresApproval ?provider }
}
```

**Rate Limit**: 10 logins/min per provider (prevent brute force)

**Cost Model**: Base cost 0 units (but may incur if external provider call is needed)

**Example**:
```bash
curl -X POST http://localhost:8080/mcp/tools/trust.login \
  -H "Content-Type: application/rdf+xml" \
  -d '
    @prefix iq: <http://symbol.systems/iq/> .
    @prefix ex: <http://example.org/> .
    
    [] iq:realm ex:realm-1 ;
       iq:provider "github" ;
       iq:credential "ghu_ABC123..." .
  '
```

**Implementation**: [TrustAdapter.java](src/main/java/systems/symbol/mcp/adapters/TrustAdapter.java)

---

### Tool: `trust.refresh`

**Description**: Refresh an existing JWT token.

**Input Schema**:
```ttl
:TrustRefreshInput a sh:NodeShape ;
  sh:property [
    sh:path iq:token ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Existing JWT token"
  ] ;
  sh:property [
    sh:path iq:ttl ;
    sh:datatype xsd:integer ;
    sh:description "New TTL in seconds"
  ] .
```

**Output Schema**: New JWT token and expiration.

**Rate Limit**: 100 refreshes/min

---

## LLM Adapter Tools

LLM invocation with fact graph context.

### Tool: `llm.invoke`

**Description**: Invoke an LLM (GPT-4, Claude, Groq, etc.) with RDF context facts, system prompts, and user queries. Streams or returns completion.

**Input Schema** (SHACL):
```ttl
:LlmInvokeInput a sh:NodeShape ;
  sh:property [
    sh:path iq:model ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "LLM model identifier (e.g., 'gpt-4', 'claude-3.5-sonnet')"
  ] ;
  sh:property [
    sh:path iq:systemPrompt ;
    sh:datatype xsd:string ;
    sh:description "System prompt (role, constraints)"
  ] ;
  sh:property [
    sh:path iq:userQuery ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "User query/prompt"
  ] ;
  sh:property [
    sh:path iq:context ;
    sh:description "RDF context facts (automatically formatted for LLM)"
  ] ;
  sh:property [
    sh:path iq:temperature ;
    sh:datatype xsd:float ;
    sh:minInclusive 0 ;
    sh:maxInclusive 1 ;
    sh:description "Sampling temperature (default: 0.7)"
  ] ;
  sh:property [
    sh:path iq:maxTokens ;
    sh:datatype xsd:integer ;
    sh:description "Maximum completion tokens (default: 2048)"
  ] .
```

**Output Schema**:
```ttl
:LlmInvokeOutput a sh:NodeShape ;
  sh:property [
    sh:path rdf:value ;
    sh:datatype xsd:string ;
    sh:description "LLM completion text"
  ] ;
  sh:property [
    sh:path iq:tokensUsed ;
    sh:datatype xsd:integer ;
    sh:description "Total tokens used (prompt + completion)"
  ] ;
  sh:property [
    sh:path iq:cost ;
    sh:datatype xsd:decimal ;
    sh:description "Estimated cost in USD"
  ] .
```

**Authorization Query**:
```sparql
ASK WHERE {
  ?caller iq:canInvokeLlm [
    iq:modelWhitelist ?modelRegex
  ] .
  FILTER REGEX(STR(?modelRegex), ?requestedModel)
}
```

**Rate Limit**: 100 invocations/min (depends on model; faster models can go higher)

**Cost Model**: 
- Per-token pricing (model-dependent, e.g., GPT-4: $0.15/1K input, $0.45/1K output)
- Quota limit: $100/day by default (configurable per realm)

**Example**:
```bash
curl -X POST http://localhost:8080/mcp/tools/llm.invoke \
  -H "Content-Type: application/rdf+xml" \
  -d '
    @prefix iq: <http://symbol.systems/iq/> .
    
    [] iq:model "gpt-4" ;
       iq:systemPrompt "You are a helpful assistant." ;
       iq:userQuery "List the agents in my realm" ;
       iq:context <file:///path/to/context.ttl> ;
       iq:temperature 0.5 .
  '
```

**Implementation**: [LlmAdapter.java](src/main/java/systems/symbol/mcp/adapters/LlmAdapter.java)

---

### Tool: `llm.search`

**Description**: Semantic search over fact graph using LLM embeddings. Returns RDF resources ranked by relevance.

**Input Schema**:
```ttl
:LlmSearchInput a sh:NodeShape ;
  sh:property [
    sh:path iq:query ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:description "Natural language query"
  ] ;
  sh:property [
    sh:path iq:limit ;
    sh:datatype xsd:integer ;
    sh:description "Maximum results (default: 10)"
  ] ;
  sh:property [
    sh:path iq:threshold ;
    sh:datatype xsd:float ;
    sh:description "Similarity threshold 0-1 (default: 0.5)"
  ] .
```

**Output Schema**: RDF resources ranked by score.

**Rate Limit**: 1000 searches/min

**Cost Model**: Base cost 5 units + embedding provider charges

---

## Realm Adapter Tools

Realm inspection and governance.

### Tool: `realm.export`

**Description**: Export realm's complete fact graph and configuration as RDF archive.

**Input Schema**:
```ttl
:RealmExportInput a sh:NodeShape ;
  sh:property [
    sh:path iq:realm ;
    sh:nodeKind sh:IRI ;
    sh:minCount 1
  ] ;
  sh:property [
    sh:path iq:includeSecrets ;
    sh:datatype xsd:boolean ;
    sh:description "If true, export encrypted vault entries (requires high privilege)"
  ] ;
  sh:property [
    sh:path iq:format ;
    sh:datatype xsd:string ;
    sh:description "Export format: 'turtle', 'nquads', 'zip' (default: 'turtle')"
  ] .
```

**Output Schema**: RDF archive (Turtle or ZIP with named graphs).

**Authorization Query**:
```sparql
ASK WHERE {
  ?caller iq:canExport ?realm ;
         iq:role iq:RealmAdmin
}
```

**Rate Limit**: 1 export/hour per realm (expensive operation)

**Cost Model**: Base cost 500 units

---

## Common Audit Trail Format

All tool invocations are logged to the realm's audit graph as RDF triples:

```ttl
@prefix iq: <http://symbol.systems/iq/> .
@prefix prov: <http://www.w3.org/ns/prov#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

_:log rdf:type iq:ToolInvocation ;
  prov:wasAssociatedWith ?caller ;
  prov:startTime "2025-04-25T10:30:45Z"^^xsd:dateTime ;
  prov:endTime "2025-04-25T10:30:47Z"^^xsd:dateTime ;
  iq:toolName "fact.sparql.query" ;
  iq:success true ;
  iq:cost 25 ;
  iq:resultCount 42 ;
  iq:signature "sig_..." ;  # Ed25519 signature for non-repudiation
  prov:wasQuotedFrom ?auditLog .
```

**Signature**: HMAC-SHA256 of (caller || toolName || timestamp || result_hash)

---

## References

- [MCP.md](../MCP.md): Protocol overview
- [iq-run-apis/docs/API_LLM.md](../iq-run-apis/docs/API_LLM.md): REST API examples
- [SPARQL 1.1 Spec](https://www.w3.org/TR/sparql11-query/)
- [SHACL Shapes Spec](https://www.w3.org/TR/shacl/)
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
