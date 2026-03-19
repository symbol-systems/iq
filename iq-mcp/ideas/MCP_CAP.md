---
id: mcp:cap/quadra-rdf4j
type: mcp:CapabilityMatrix
version: "1.0"
status: living
updated: "2026-03-19"
audience: [architect, llm, developer]
relates-to: MCP_SELF.md, MCP_ARCHITECTURE.md
---

# MCP Capability Matrix (for QUADRA + RDF4J)

> **What this document is.**  
> A structured matrix of capabilities abstracting how an LLM operates over an RDF4J-backed graph environment (like IQ / QUADRA). It defines what is considered a `Resource` (pure, read-only) versus a `Tool` (stateful, side-effecting, or computationally heavy), grading each primitive for determinism and cost.

---

## 1. Capability Matrix

| Capability Domain | Capability | Description | MCP Type | Inputs | Outputs | Determinism | Cost | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **Schema Awareness** | `list_classes` | Enumerate top-level entity types in a realm | Resource | `realm` | `class[]` | High | Low | Entry point for exploration |
| | `list_properties` | List predicates with usage stats | Resource | `realm` | `property[]` | High | Low | Helps LLM infer schema patterns |
| | `describe_class` | Structural + semantic summary of a class | Resource | `class_uri` | schema object | High | Medium | Include SHACL-like constraints |
| | `graph_shape` | Graph topology for a class (edges, cardinality) | Resource | `class_uri` | shape graph | Medium | Medium | Enables query planning |
| **Entity Access** | `get_entity` | Retrieve canonical representation of entity | Resource | `uri` | entity object | High | Low | Should normalize triples |
| | `get_neighbors` | Traverse graph locally | Resource | `uri`, `depth`, `filters` | subgraph | Medium | Medium | Control explosion carefully |
| | `find_entities` | Query entities by type + constraints | Resource | `type`, `filters`, `limit` | `entity[]` | High | Medium | Core retrieval primitive |
| | `resolve_entity` | Disambiguate entity from text | Resource | `text`, `context` | URI candidates | Medium | Medium | Uses labels + embeddings |
| **Search & Retrieval** | `semantic_search` | Natural language → entity retrieval | Resource | `query`, `scope` | ranked entities | Low | High | Hybrid symbolic/vector |
| | `hybrid_query` | Combine structured + semantic constraints | Resource | IR query | `entity[]` | Medium | High | Key for real use |
| | `contextual_lookup` | Retrieve entities relative to a working set | Resource | context set, `relation` | `entity[]` | Medium | Medium | Enables multi-hop reasoning |
| **Aggregation & Analytics** | `count` | Cardinality of a set | Resource | `type`, `filters` | number | High | Low | Prefer over full retrieval |
| | `group_by` | Distribution across a property | Resource | `type`, `property` | histogram | High | Medium | Useful for summarization |
| | `distribution` | Statistical profile of values | Resource | `property` | stats | High | Medium | Numeric/textual |
| | `summarize_entities` | Compress entity set into summary | Resource | `entity[]` | summary | Low | Medium | LLM-assisted |
| **Provenance & Trust** | `explain_fact` | Justify a triple or relation | Resource | triple | explanation | Medium | Medium | Include inference path |
| | `get_sources` | Source metadata for entity/fact | Resource | `uri` | `sources[]` | High | Low | Regulatory use cases |
| | `confidence_score` | Confidence estimation | Resource | entity/fact | score | Low | Low | Heuristic or learned |
| | `detect_conflicts` | Identify inconsistent facts | Tool | entity set | `conflicts[]` | Medium | High | Needs reasoning |
| **Reasoning & Inference** | `run_inference` | Apply rule sets to graph | Tool | ruleset | inferred triples | Medium | High | Expensive; cache results |
| | `validate_graph` | Check against constraints | Tool | shapes | validation report | High | Medium | SHACL-like |
| | `explain_inference` | Trace derived knowledge | Tool | inferred triple | derivation path | Medium | High | Debugging |
| | `materialize_view` | Persist derived subgraph | Tool | definition | graph fragment | High | High | Optimization layer |
| **Graph Mutation** | `create_entity` | Add new entity | Tool | `type`, properties | `uri` | High | Medium | Guarded |
| | `update_entity` | Modify properties | Tool | `uri`, patch | status | High | Low | Versioning recommended |
| | `link_entities` | Create relationship | Tool | `s`, `p`, `o` | status | High | Low | Validate ontology |
| | `delete_entity` | Remove entity | Tool | `uri` | status | High | Medium | Rarely exposed |
| **Query Composition** | `compose_query` | Intent → structured IR | Tool | natural language | IR object | Low | Medium | Central planner hook |
| | `refine_query` | Iteratively improve IR | Tool | IR, feedback | IR query | Low | Medium | Enables LLM self-correction loops |
| | `explain_query` | Human-readable explanation | Tool | IR query | text | High | Low | Debugging / UX |
| **Cross-Realm Federation** | `federated_query` | Query across multiple realms | Tool | IR query, `realms[]` | merged results | Medium | High | Needs alignment |
| | `align_entities` | Map equivalent entities | Tool | realm A / realm B | `mappings[]` | Low | High | Embeddings + heuristics |
| | `map_ontology` | Schema alignment | Tool | source, target | mapping spec | Low | High | Hard problem |
| | `merge_results` | Deduplicate + unify results | Tool | `datasets[]` | unified set | Medium | Medium | Identity resolution |
| **Context Management** | `create_context` | Define working memory | Tool | seed entities | context ID | High | Low | Session-scoped |
| | `expand_context` | Grow working set | Tool | context ID, `relation` | context object | Medium | Medium | Controlled exploration |
| | `prune_context` | Reduce scope | Tool | context ID | context object | High | Low | Prevent overload |
| | `snapshot_context` | Persist state | Tool | context ID | snapshot | High | Low | Reproducibility across sessions |
| **Observability** | `trace_query` | Execution trace | Tool | query ID | trace log | High | Low | Debugging |
| | `log_tool_usage` | Record decisions | Tool | session | logs | High | Low | Analytics |
| | `estimate_cost` | Predict execution cost | Tool | IR query | cost estimate | Medium | Low | Planner input / guardrail |
| | `detect_failure` | Identify query/tool failure | Tool | execution | error analysis | Medium | Medium | Recovery loop trigger |

---

## 2. Structural Observations

### Resources vs Tools Boundary

- **Resources** = Pure functions over graph state. They are safe to call, idempotent, and act as ambient knowledge lookups.
- **Tools** = Stateful, multi-step, or side-effecting operations. They incur write costs, trigger workflows, or invoke heavyweight computation (like inference or LLM processing).

### Capability Layering

The capabilities stack conceptually as follows. Each higher layer degrades gracefully into the primitives of the layers below it.

```text
 L5: Cross-Realm Federation   (align_entities, federated_query)
 L4: Reasoning & Inference(run_inference, detect_conflicts)
 L3: Query Composition(compose_query, hybrid_query)
 L2: Retrieval & Navigation   (find_entities, get_neighbors, semantic_search)
 L1: Schema Awareness (list_classes, graph_shape)
 L0: Raw Graph (RDF4J)(SPARQL eval context, triple store)
```

### Critical Minimal Cut

If you reduce this matrix down to the **irreducible core** needed for baseline LLM competence without overflowing the context window or creating endless loops, it consists of:

1. `describe_class`
2. `find_entities`
3. `get_entity`
4. `get_neighbors`
5. `count`
6. `compose_query`

Everything else represents acceleration, safety scaling, or observability optimizations.

### The Hidden Keystone Capability: `compose_query`

The most important (and hardest) capability in the entire matrix is **`compose_query` (Natural Language → Intermediate Representation / SPARQL)**.

- **If this is weak**: The entire interaction collapses into brittle, heavily-prompted, multi-step tool usage where the LLM struggles to pull interrelated parts of the graph together.
- **If this is strong**: The system becomes highly **self-adaptive**. The LLM can express complex, succinct, comprehensive, exhaustive, and non-obvious requests idiomatically. The IR acts as a cognitive bridge, absorbing the syntax of the underlying RDF store while letting the LLM reason purely semantically.
