# SPARQL — useful queries and patterns for IQ

This page collects common and useful SPARQL examples.

1) Find all scripts (store, template strings)
- Purpose: discover scripts stored as literal content.
- Query:
  SELECT DISTINCT ?this ?script
  WHERE { ?this <http://www.w3.org/2011/content#chars> ?script. }

2) Count instances by type
- Purpose: quick health check / inventory.
- Query:
  SELECT ?type (COUNT(?s) as ?count)
  WHERE { ?s a ?type . } GROUP BY ?type

3) Hydrate labels & definitions (given a set of IRIs)
- Purpose: build a small lookup to render UI items or debugging output.
- Query:
  SELECT DISTINCT ?this ?label ?definition ?score
  WHERE {
    ?this <http://www.w3.org/2004/02/skos/core#prefLabel> ?label.
    OPTIONAL { ?this <http://www.w3.org/2004/02/skos/core#definition> ?definition. }
    VALUES (?this ?score) { {{{these}}} }
  }
- Note: `{{{these}}}` is a template substitution used by the runtime to pass a list of IRIs + scores.

4) List Concept Schemes
- Purpose: list vocabularies and their labels.
- Query:
  SELECT DISTINCT ?conceptScheme ?label
  WHERE {
    ?conceptScheme a <http://www.w3.org/2004/02/skos/core#ConceptScheme> .
    OPTIONAL { ?conceptScheme <http://www.w3.org/2004/02/skos/core#prefLabel> ?label . }
  }

5) Subject-centric dump
- Purpose: debug everything about a subject for inspection.
- Query:
  CONSTRUCT { ?s ?p ?o } WHERE { BIND(<IRI> AS ?s) ?s ?p ?o }

6) Find Agents and their workflows
- Purpose: enumerate agents and current states used by runtime.
- Example Query (instantiate with actual prefixes):
  PREFIX iq: <iq:>
  SELECT ?agent ?workflow ?state
  WHERE {
    ?agent a iq:Agent ; iq:workflow ?workflow .
    ?workflow iq:initial ?state .
  }

7) Discover LLM named maps and secrets
- Purpose: enumerate configured LLM named maps and their model names.
- Example (TTL layout is often `llm:*`):
  SELECT ?map ?prop ?value
  WHERE {
    ?map a ?type .
    FILTER(REGEX(STR(?map), "llm", "i"))
    OPTIONAL { ?map ?prop ?value }
  }
- Note: actual property names used by TTLs include `iq:url`, `iq:modelName`, `iq:secret`.

8) Trust/Identity checks (realm-based)
- Purpose: find trust relationships or authorizations.
- Example: `SELECT ?p ?o WHERE { BIND(<{{my.self}}> AS ?s) }` (used as a template in `identify.sparql`)

9) Useful authoring tips
- Keep template substitution small and tidy (runtime commonly passes `VALUES` blocks).
- For interactive debugging in dev mode, use CONSTRUCT or DESCRIBE to get structured graph output.
- When results are large, use LIMIT/OFFSET and order by date or score if available.
