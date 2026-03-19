---
@context:
  schema: "http://schema.org/"
  dct: "http://purl.org/dc/terms/"
  iq: "urn:iq:"
@type: "TechArticle"
title: "Kernel pipeline"
audience: ["developer"]
---

# Kernel Pipeline (developer view)

This document explains the core abstractions used to build an extensible request pipeline in IQ, and how the MCP surface adapts those abstractions.

## Core concepts (kernel layer)

### `KernelCallContext`
- A mutable per-request envelope that carries cross-cutting state: `traceId`, `realm`, `principal`, `authorised`, and an extensible attribute map.
- Implemented in `iq-kernel/src/main/java/systems/symbol/kernel/pipeline/KernelCallContext.java`.

### `I_Middleware<CTX>`
- Interface for pipeline steps: `void process(CTX ctx, I_Chain<CTX> chain)`.
- Implementations can short-circuit by not calling `chain.proceed(ctx)`.
- Ordered by `getOrder()`.

### `I_Chain<CTX>`
- Continuation representing "the rest of the pipeline". Calling `proceed(ctx)` advances to the next middleware.

### `I_Pipeline<CTX>`
- A builder that composes a list of `I_Middleware<CTX>` into a single `I_Chain<CTX>`.
- Kernel provides a default implementation via `I_Pipeline.of(List)`.

### `I_AccessPolicy`
- A lightweight guard interface used by *some* surfaces (e.g. Camel route policy).

## MCP surface adaptation (iq-mcp)

The MCP surface implements a higher-level pipeline that returns `I_MCPResult` objects (HTTP-like results). To bridge this with the kernel pipeline, the MCP layer introduces:

### `MCPCallContext`
- Extends `KernelCallContext` with MCP-specific fields: tool name, raw input map, etc.
- Located at `iq-mcp/src/main/java/systems/symbol/mcp/MCPCallContext.java`.

### `I_MCPPipeline`
- Extends `I_Middleware<MCPCallContext>`.
- Adds a result-bearing overload `I_MCPResult process(MCPCallContext, MCPChain)`.
- Provides a `default void process(KernelCallContext, I_Chain)` bridge so kernel pipeline can call it directly.
- Located at `iq-mcp/src/main/java/systems/symbol/mcp/connect/I_MCPPipeline.java`.

### `MCPChain`
- A result-bearing continuation (`I_MCPResult proceed(MCPCallContext)`).
- Includes a static adapter `MCPChain.from(I_Chain<MCPCallContext>)` to wrap the kernel chain into a result-bearing form.
- Located at `iq-mcp/src/main/java/systems/symbol/mcp/connect/MCPChain.java`.

### `MCPConnectPipeline`
- Implements `I_Pipeline<MCPCallContext>` (kernel contract) and also exposes an `execute(ctx, MCPChain)` helper that wires pre/post middleware around the MCP adapter.
- Located at `iq-mcp/src/main/java/systems/symbol/mcp/connect/MCPConnectPipeline.java`.

## Writing middleware (example)

A minimal middleware looks like this:

```java
public class MyMiddleware implements I_MCPPipeline {

@Override
public IRI getSelf() {
return Values.iri("urn:mcp:pipeline/MyMiddleware");
}

@Override
public int getOrder() {
return 25;
}

@Override
public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
// Example: abort if some condition is not met
if (!checkSomething(ctx)) {
throw MCPException.forbidden("Not allowed");
}

// Continue the pipeline
return chain.proceed(ctx);
}
}
```

The kernel-side bridge ensures that when `MCPConnectPipeline` is built as an `I_Pipeline<MCPCallContext>`, the same middleware can be treated as a generic kernel middleware.

## Where configuration lives

The pipeline middleware list is usually configured in the `mcp:pipeline` named graph (see `docs/kernel-pipeline/ontologist.md`). At startup, `MCPConnectRegistry` reads that graph and instantiates middleware by class name.

- `iq-mcp/src/main/java/systems/symbol/mcp/connect/MCPConnectRegistry.java`

## Non-obvious points (gotchas)

- **Order matters**: middleware ordering is controlled solely by `mcp:order` and the `getOrder()` value. Changing the order can change semantics (e.g., audit should run after the adapter to capture state).
- **Result handling**: `I_MCPPipeline` implementations return an `I_MCPResult`, but the kernel pipeline expects `void`. The bridge writes the result into `ctx.set("mcp.result", result)` so downstream layers (including `AuditWriterMiddleware`) can access it.
- **Exceptions are mapped**: `MCPException` → `KernelException` (and back) via the bridge in `I_MCPPipeline` and `MCPChain.from()`.
- **Reuse outside MCP**: using `I_Pipeline.of()` with `I_Middleware<KernelCallContext>` is the simplest way to test middleware in isolation, without involving the MCP stack.

## Recommended next steps (developer)

1. If you add middleware, add a matching TTL entry in `mcp:pipeline` so it can be toggled/ordered at runtime.
2. If you need a new kernel-level field, add it to `KernelCallContext` and expose a typed accessor.
3. If you need to adapt another surface (CLI, REST, Camel) to the same pipeline model, follow the MCP example: create a surface-specific `*CallContext` extending `KernelCallContext`, then implement a bridge similar to `I_MCPPipeline`.
