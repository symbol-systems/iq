---
@context:
  schema: "http://schema.org/"
  dct: "http://purl.org/dc/terms/"
  iq: "urn:iq:"
@type: "TechArticle"
title: "Kernel pipeline (ontologist guide)"
audience: ["ontologist"]
---

# Kernel pipeline configuration (ontologist view)

The pipeline configuration is stored as RDF in the `mcp:pipeline` named graph.
Each middleware component is represented as a resource with a small set of
properties that control how it is instantiated and ordered.

## What the graph describes

A middleware entry describes:

- **What code to run** (`mcp:middlewareClass`).
- **What order to run it in** (`mcp:order`). Lower numbers run earlier.
- **Whether it is enabled** (`mcp:enabled`).

That means you can change request processing behavior by editing RDF alone;
for example, disabling audit or inserting a new guard.

## Example entry (bulletized)

A middleware entry looks like a tiny record:

```turtle
<urn:mcp:pipeline/AuthGuard>
  a mcp:Middleware ;
  mcp:order 10 ;
  mcp:enabled true ;
  mcp:middlewareClass "systems.symbol.mcp.connect.impl.AuthGuardMiddleware" .
```

This says:

1. There is a middleware called `urn:mcp:pipeline/AuthGuard`.
2. It is an instance of `mcp:Middleware`.
3. It runs first (order 10).
4. It is enabled.
5. The JVM class to instantiate is `AuthGuardMiddleware`.

## Common middleware roles

| Role | Property | Purpose |
|------|----------|---------|
| Authentication | `mcp:middlewareClass` → `AuthGuardMiddleware` | Validate credentials and populate the caller identity.
| Authorization | `mcp:middlewareClass` → `ACLFilterMiddleware` | Enforce which tools a user is allowed to run.
| Quota | `mcp:middlewareClass` → `QuotaGuardMiddleware` | Rate-limit usage to protect the system.
| Audit | `mcp:middlewareClass` → `AuditWriterMiddleware` | Record request outcomes in the audit graph.

## How to extend

1. Add a new IRI (e.g. `<urn:mcp:pipeline/MyGuard>`).
2. Add triples for `mcp:order`, `mcp:enabled`, and `mcp:middlewareClass`.
3. Implement the corresponding Java middleware (see the developer guide).
4. Reload/restart the system to pick up the new configuration.
