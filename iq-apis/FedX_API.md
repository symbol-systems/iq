# FedX REST API Documentation

## Overview

The FedX REST API provides federated SPARQL query execution across an IQ cluster. It implements the SPARQL Protocol for RDF Query Language and enables distributed query processing with automatic optimization.

**Base URL:** `http://<host>:8080/sparql/federated`

**Protocol:** HTTP/REST

**Response Format:** JSON (SPARQL JSON Results Format)

---

## Endpoints

### 1. Query Execution

#### GET /query

Execute SPARQL SELECT or ASK queries via HTTP GET.

**Parameters:**
- `query` (required, URL-encoded): SPARQL SELECT or ASK query
- `timeout` (optional, integer): Query timeout in seconds (default: 30, max: 300)

**Example:**
```bash
curl -G "http://localhost:8080/sparql/federated/query" \
  --data-urlencode 'query=SELECT ?subject WHERE { ?subject ?p ?object } LIMIT 10'
```

**Response (200 OK):**
```json
{
  "head": {
"vars": ["subject"]
  },
  "results": {
"bindings": [
  {
"subject": {
  "type": "uri",
  "value": "http://example.org/resource/1"
}
  }
]
  }
}
```

**Error Responses:**
- **400 Bad Request**: Query validation failed
  ```json
  {
"error": "Query exceeded maximum size (1048576 chars)"
  }
  ```
- **500 Internal Server Error**: Query execution failed
  ```json
  {
"error": "RDF4J repository error message"
  }
  ```

---

#### POST /query

Execute SPARQL queries via HTTP POST with form-encoded body.

**Content-Type:** `application/x-www-form-urlencoded`

**Form Parameters:**
- `query` (required): SPARQL SELECT, ASK, or CONSTRUCT query
- `timeout` (optional, integer): Query timeout in seconds

**Example:**
```bash
curl -X POST "http://localhost:8080/sparql/federated/query" \
  -d "query=SELECT ?s WHERE { ?s rdf:type ?type } LIMIT 20" \
  -d "timeout=60"
```

**Response:** Same as GET endpoint

---

### 2. Health Check

#### GET /health

Check FedX topology health and available endpoints.

**Parameters:** None

**Example:**
```bash
curl http://localhost:8080/sparql/federated/health
```

**Response (200 OK):**
```json
{
  "status": "healthy",
  "endpoints": 3
}
```

**Response (503 Service Unavailable):**
```json
{
  "status": "unhealthy",
  "reason": "No healthy cluster nodes available"
}
```

---

## Query Validation

All queries are validated before execution with the following rules:

### Size Limits
- **Minimum:** 10 characters
- **Maximum:** 1 MB (1,048,576 bytes)

### Timeout Limits
- **Minimum:** 1 second
- **Maximum:** 300 seconds
- **Default:** 30 seconds (if not specified)

### Supported Query Types
- `SELECT` - Retrieve variable bindings
- `ASK` - Check existence of solutions
- `CONSTRUCT` - Generate RDF triples (Phase 3C implementation)

### Blocked Operations (for security)
- `INSERT DATA` - Write operations not supported
- `DELETE DATA` - Write operations not supported
- `LOAD` - Data loading not supported
- `CLEAR` - Graph clearing not supported
- `DROP` - Graph dropping not supported

---

## Query Examples

### Simple Pattern Matching
```sparql
SELECT ?s ?p ?o
WHERE {
  ?s ?p ?o
}
LIMIT 10
```

### With FILTER
```sparql
SELECT ?subject ?name
WHERE {
  ?subject foaf:name ?name .
  ?subject rdf:type foaf:Person .
  FILTER (strlen(?name) > 5)
}
```

### UNION (Parallelized)
```sparql
SELECT ?entity
WHERE {
  {
?entity rdf:type ex:Class1
  }
  UNION
  {
?entity rdf:type ex:Class2
  }
}
```

### OPTIONAL (Parallelized)
```sparql
SELECT ?s ?label
WHERE {
  ?s rdf:type rdf:Resource .
  OPTIONAL {
?s rdfs:label ?label .
  }
}
LIMIT 100
```

---

## Distributed Query Optimization

The FedX API automatically analyzes queries and selects optimal execution strategies:

### Execution Strategies

1. **BROADCAST** - Simple queries sent to all healthy nodes
   - Best for: Low complexity, high parallelism
   - Example: `SELECT ?s WHERE { ?s ?p ?o }`

2. **UNION_DISTRIBUTE** - UNION branches executed in parallel
   - Best for: Queries with multiple independent branches
   - Example: `{ ?s rdf:type A } UNION { ?s rdf:type B }`

3. **OPTIONAL_DISTRIBUTE** - Optional branches executed in parallel
   - Best for: Queries with optional graph patterns
   - Example: Graph patterns with OPTIONAL blocks

4. **FEDERATED** - FedX automatic federation strategy
   - Best for: Complex queries across distributed data
   - Handles: Join decomposition, remote execution, result aggregation

5. **SEQUENTIAL** - Single-node execution
   - Used for: Queries too complex for parallelization

---

## Performance Optimization Tips

### 1. Use LIMIT Clauses
Reduce network overhead by limiting result sizes:
```sparql
SELECT ?s ?p ?o
WHERE { ?s ?p ?o }
LIMIT 10000  -- Better than unlimited
```

### 2. Add FILTER Conditions Early
Push filtering down to remote endpoints:
```sparql
SELECT ?s
WHERE {
  ?s rdf:type ex:Person .
  FILTER (?s > "2000-01-01"^^xsd:date)  -- Early filtering
}
```

### 3. Avoid Complex Nested JOINs
Simple star or tree patterns are more efficient:
```sparql
-- Good: simple joins
SELECT ?s ?p ?o
WHERE {
  ?s rdf:type ex:Resource .
  ?s ex:hasName ?name .
  ?s ex:hasValue ?value .
}

-- Avoid: complex nested joins
SELECT ?a ?b ?c
WHERE {
  ?a ex:p1 ?x .
  ?x ex:p2 ?y .
  ?y ex:p3 ?z .
  ?z ex:p4 ?b .
  ?b ex:p5 ?c .
}
```

### 4. Set Appropriate Timeouts
Balance completeness vs. response time:
```bash
# Short timeout for quick results
curl -G "http://localhost:8080/sparql/federated/query" \
  --data-urlencode 'query=...' \
  --data-urlencode 'timeout=10'  # 10 seconds

# Long timeout for complex queries
curl -G "http://localhost:8080/sparql/federated/query" \
  --data-urlencode 'query=...' \
  --data-urlencode 'timeout=120'  # 2 minutes
```

---

## Error Handling

### Validation Errors (400)

Returned when query validation fails:

| Error | Cause | Solution |
|-------|-------|----------|
| `Query is empty` | Empty query string | Provide a non-empty query |
| `Query is too small` | Query < 10 characters | Expand query structure |
| `Query exceeded maximum size` | Query > 1 MB | Simplify query or use pagination |
| `Query does not start with valid SPARQL operation` | Invalid operation | Use SELECT, ASK, CONSTRUCT, or DESCRIBE |
| `Query contains write or data manipulation operations` | INSERT, DELETE, LOAD, CLEAR, DROP | Use read-only operations only |
| `Timeout must be at least 1 second` | Timeout < 1 | Increase timeout value |
| `Timeout exceeds maximum allowed (300 seconds)` | Timeout > 300 | Reduce timeout or simplify query |

### Server Errors (500)

Returned when query execution fails:

```json
{
  "error": "Detailed error message from RDF4J"
}
```

Common causes:
- RDF repository connection failure
- Query evaluation failure on remote endpoints
- Memory exhaustion (large result sets)
- Timeout during remote execution

---

## Client Libraries

### JavaScript (Node.js / Fetch API)

```javascript
const query = `
  SELECT ?s ?p ?o
  WHERE { ?s ?p ?o }
  LIMIT 10
`;

const response = await fetch('http://localhost:8080/sparql/federated/query', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: `query=${encodeURIComponent(query)}&timeout=30`
});

const results = await response.json();
console.log(results);
```

### Python

```python
import requests
import json

query = """
  SELECT ?s ?p ?o
  WHERE { ?s ?p ?o }
  LIMIT 10
"""

response = requests.post(
  'http://localhost:8080/sparql/federated/query',
  data={'query': query, 'timeout': '30'}
)

results = response.json()
print(json.dumps(results, indent=2))
```

### Java

```java
String query = "SELECT ?s WHERE { ?s ?p ?o }";
String baseUrl = "http://localhost:8080/sparql/federated/query";

CloseableHttpClient client = HttpClients.createDefault();
HttpPost post = new HttpPost(baseUrl);

List<NameValuePair> params = new ArrayList<>();
params.add(new BasicNameValuePair("query", query));
params.add(new BasicNameValuePair("timeout", "30"));

post.setEntity(new UrlEncodedFormEntity(params));

try (CloseableHttpResponse response = client.execute(post)) {
HttpEntity entity = response.getEntity();
String result = EntityUtils.toString(entity);
// Parse JSON results
}
```

### cURL

```bash
# Simple GET request
curl -G "http://localhost:8080/sparql/federated/query" \
  --data-urlencode 'query=SELECT * WHERE { ?s ?p ?o } LIMIT 10'

# POST with timeout
curl -X POST "http://localhost:8080/sparql/federated/query" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "query=$(urlencode 'SELECT * WHERE { ?s ?p ?o } LIMIT 10')" \
  -d "timeout=60"

# With authentication (if configured)
curl -u user:pass -G "http://localhost:8080/sparql/federated/query" \
  --data-urlencode 'query=SELECT * WHERE { ?s ?p ?o }'
```

---

## Configuration

FedX configuration can be set via environment variables or `.iq/federated-sparql.properties`:

```properties
# Maximum query size in bytes
fedx.query.max.size=1048576

# Query timeout limits (seconds)
fedx.query.timeout.default=30
fedx.query.timeout.max=300
fedx.query.timeout.min=1

# Cluster node discovery
fedx.cluster.refresh.interval=30000  # milliseconds

# Remote SPARQL client timeout
fedx.remote.timeout=60000  # milliseconds
fedx.remote.max.connections=10
```

---

## Monitoring

### Metrics

Track API usage via:
- Query execution time
- Result set size
- Cluster node utilization
- Cache hit rate (when caching is enabled)

### Logging

Enable debug logging for FedX:

```log4j.properties
log4j.logger.systems.symbol.controller.rdf.FedXResource=DEBUG
log4j.logger.systems.symbol.rdf4j.fedx=DEBUG
```

---

## Standards Compliance

- **SPARQL Protocol:** W3C SPARQL Protocol for RDF
- **SPARQL JSON:** W3C SPARQL 1.1 JSON Results Format
- **RDF4J API:** Eclipse RDF4J Repository API
- **HTTP REST:** REST architectural principles

---

## Versioning

- **API Version:** 1.0
- **SPARQL Version:** 1.1
- **RDF4J Version:** 4.0+

---

## Contact & Support

For issues, feature requests, or questions:
- **GitHub:** https://github.com/symbol-systems/iq
- **Docs:** https://docs.symbol.systems/iq
- **Support:** support@smarttrust.ai
