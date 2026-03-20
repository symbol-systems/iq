# Executable Predicate Exposure Pattern (IQ Kernel)

## Purpose
This document defines a generic approach for exposing **executable predicates** in the IQ codebase.
It maps RDF predicate-driven behavior (agent/intent/action) to kernel command/event execution so modules can safely plug in predicate-based logic.

## Motivation
`iq` is RDF-first, so predicates are first-class signals. For example:
- `iq:ask` could trigger a question intent
- `iq:execute` could invoke an action pipeline
- `iq:knows` can signal knowledge updates

We want a framework where external systems can register handlers for semantic predicate execution and route unified command objects through kernel command pipeline.

## Core model
### 1. `I_Command` (from `iq-abstract`)
Used to represent a predicate instruction:
- `getActor()` → actor IRI
- `getAction()` → predicate IRI (verb)
- `getTarget()` → object/resource

### 2. `KernelRequest` (IQ Kernel command request)
Extended to carry optional semantic predicate command:
- `getCommand(): I_Command` (optional)

### 3. `KernelTopics` (hook topics)
Added to event taxonomy:
- `urn:iq:event:agent:command:received`
- `urn:iq:event:agent:command:executed`
- `urn:iq:event:agent:command:failed`

### 4. `AbstractKernelCommand` life-cycle publish
- on `execute`: publish received/executed/failed events
- payload includes subject/realm/caller and optional I_Command fields

## Generic predicate execution flow (recommended)
1. **Predicate dispatch table** (in kernel or platform layer)
   - Map RDF predicate IRIs (`iri:foo`) to kernel command class / handler.
   - Example map:
     - `iq:learn` -> `LearnIntentCommand`
     - `iq:forget` -> `ForgetIntentCommand`

2. **Surface ingestion** (CLI / REST / MCP / FSM trigger)
   - Surface translates existing RDF predicate statements into `I_Command`:
     - `new SimpleCommand(actor, predicate, target)`
   - Compose `KernelRequest` with `.command(command)`.

3. **Kernel command execution**
   - `AbstractKernelCommand.execute` emits `AGENT_COMMAND_RECEIVED`.
   - `doExecute()` handles or routes to intent engine.
   - on completion emits `AGENT_COMMAND_EXECUTED` or `AGENT_COMMAND_FAILED`.

## Hook-based predicate extension
Use `I_EventHub` to observe/extend predicate lifecycle with sidecar plugins:
- `subscribe(KernelTopics.AGENT_COMMAND_RECEIVED, sink)`
- `subscribe(KernelTopics.AGENT_COMMAND_EXECUTED, sink)`
- `subscribe(KernelTopics.AGENT_COMMAND_FAILED, sink)`

Plugins can inspect `KernelEvent.getPayload()` to extract command semantics.

## API extension points
### `systems.symbol.kernel.command` additions
- `KernelRequest.Builder.command(I_Command command)`
- `KernelRequest.getCommand()`
- `AbstractKernelCommand` predicate event publishing

### `systems.symbol.kernel.agent` integration
- `I_AgentRegistry` — agent actors by self IRI
- Commands may be routed to concrete agent via registry in command handler.

### `iq-rdf4j` predicate discovery endpoint
- Add SPARQL query utility `UsefulSPARQL.PREDICATES` for predicate listing:
  - `SELECT DISTINCT ?p WHERE { ?s ?p ?o }`
- Use as introspection for dynamic predicate-capable UIs.

## Example implementation (pseudo)
```java
public class PredicateCommandRouter {
    private final Map<IRI, Class<? extends I_KernelCommand<?>>> route;

    public KernelResult<?> route(KernelRequest req) {
        I_Command cmd = req.getCommand();
        if (cmd == null) { return KernelResult.error(...); }

        Class<? extends I_KernelCommand<?>> commandClass = route.get(cmd.getAction());
        if (commandClass == null) { return KernelResult.error(...); }

        I_KernelCommand<?> command = instantiate(commandClass, ctx);
        return command.execute(req);
    }
}
```

## Observability and discovery
Enumerate supported predicate actions with:
- `sparql predicates` command (existing IQ query endpoint)
- in-proc `KernelTopics` event listener and command registry

## CRM-style predicate operations (advanced)
For complex predicate-driven behavior, consider:
- `I_Facade` to expose command bindings
- predicate guard with FSM transitions (`ModelStateMachine`) on `HasCondition` hooks
- auto-registration via `KernelBuilder.withAttribute("predicateRouter", router)`

## Migration checklist
- [x] Add `KernelTopics` agent command events
- [x] Add `KernelRequest.command` field and builder method
- [x] Add command event emission to `AbstractKernelCommand`
- [x] Add tests in `KernelCommandEventTest`
- [x] Add docs path to `iq-kernel/docs/PREDICATES.md`

## Notes
- This design is intentionally low-impact and adheres to current kernel style.
- It is usable in mcps, CLI, and agentic bridges using the same `KernelRequest` model.

