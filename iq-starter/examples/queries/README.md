# /queries — SPARQL query examples

This folder contains ready-to-run SPARQL queries for your knowledge graph.

## What goes here

Questions you want to ask about your data:

```sparql
# "What are our top customers by revenue?"
SELECT ?customer ?name ?revenue
WHERE {
  ?customer a :Customer ;
    :name ?name ;
    :revenue ?revenue .
}
ORDER BY DESC(?revenue)
LIMIT 10
```

## Files included

- `00-hello.sparql` — "Hello world" (list first 5 facts)
- `customers.sparql` — Find all customers
- `orders-by-customer.sparql` — (template for linking queries)

## How to use

```bash
# Run a query
./bin/demo-query queries/customers.sparql

# Output: Table of results

# Or use the CLI
iq> query queries/customers.sparql
```

## Writing your own queries

Start with a simple SELECT:

```sparql
PREFIX ex: <http://example.com/>

SELECT ?customer ?name
WHERE {
  ?customer a ex:Customer ;
    ex:name ?name .
}
```

Common patterns:

**Find all entities of a type:**
```sparql
SELECT ?item WHERE {
  ?item a :Customer .
}
```

**Filter by value:**
```sparql
SELECT ?name WHERE {
  ?customer :name ?name ;
    :revenue ?r .
  FILTER (?r > 1000000)
}
```

**Join across entities:**
```sparql
SELECT ?customer ?order
WHERE {
  ?order :customer ?customer .
  ?customer :status "active" .
}
```

**Aggregation:**
```sparql
SELECT ?customer (COUNT(?order) as ?order_count)
WHERE {
  ?order :customer ?customer .
}
GROUP BY ?customer
```

## Tips

- **Use prefixes:** Keep queries readable
- **Test incrementally:** Start with a simple pattern, add filters gradually
- **Check syntax:** IQ will tell you if something's wrong
- **Use comments:** `# This is a comment` helps future-you

## More SPARQL

- [W3C SPARQL Spec](https://www.w3.org/TR/sparql11-query/)
- [Tutorials](https://www.w3.org/2001/sw/DataAccess/)

---

**Next:** Build workflows that use these queries. See [../agents/README.md](../agents/README.md)
