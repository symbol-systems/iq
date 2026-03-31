# /domains — Knowledge domain definitions

This folder contains RDF (Turtle) files that define the structure of your knowledge domain.

## What goes here

Define entities and relationships:

```turtle
# Person entity
ex:person-1
  a ex:Person ;
  ex:name "Alice" ;
  ex:email "alice@example.com" ;
  ex:role "manager" .

# Order entity
ex:order-123
  a ex:Order ;
  ex:customer ex:customer-1 ;
  ex:amount 15000 ;
  ex:status "pending" .
```

## Files included

- `customers.ttl` — Customer entities, revenue, tier, status
- `orders.ttl` — Orders, amounts, status, delivery info

## How to use

1. **Copy and adapt** these examples to your domain
2. **Add new entities** specific to your business
3. **Define relationships** between entities
4. **Load into IQ** using the CLI or API

## Example: Add a new domain

Create `products.ttl`:

```turtle
@prefix ex: <http://example.com/> .

ex:product-1
  a ex:Product ;
  ex:name "Widget" ;
  ex:price 99.99 ;
  ex:inventory 500 .
```

Then load it:

```bash
iq> import-file domains/products.ttl
```

## Key concepts

**Entity:** A thing with properties (customer, order, product)  
**Property:** An attribute (name, email, status)  
**Relationship:** A link between entities (order → customer)

## Format: Turtle (TTL)

Simple, human-readable RDF format:

```turtle
@prefix ex: <http://example.com/> .

# Entity definition
ex:thing
  ex:property "value" ;           # property
  ex:relatesto ex:otherthing .    # relationship
```

See [Turtle spec](https://www.w3.org/TR/turtle/) for syntax details.

## Tips

- **Use namespaces:** `@prefix` keeps things organized
- **Add types:** Every entity should have `a :Type`
- **Use meaningful URIs:** `ex:customer-123` is better than `ex:obj1`
- **Document relationships:** Make it clear why things are linked

---

**Next:** Write SPARQL queries to ask questions about your domain. See [../queries/README.md](../queries/README.md)
