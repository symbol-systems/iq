# IQ Kernel Abstract Model

This document captures the abstract capabilities for `iq-kernel` and how it integrates the agent-centric abstractions defined in `iq-abstract`.

## Goal
Provide a canonical place in `iq-kernel` for defining and wiring in generic platform/agent abstractions with minimal coupling.

- keep `iq-kernel` runtime-agnostic and composable
- support commands, events, state machines, and repository hooks
- use `iq-abstract` types (`I_Agent`, `I_Command`, `I_Facade`) as base contract for higher-level modules

## Core interfaces in `iq-kernel`

From existing code:

- `I_Kernel` (lifecycle: `start` / `stop`, self IRI)
- `KernelContext` (`I_Self`, `I_Secrets`, home path, version, attributes)
- `I_EventHub` / `I_EventSink` / `KernelEvent`
- `I_AccessPolicy` (pipeline authorization)

## Extended abstract capabilities from `iq-abstract`

### `I_Agent`
Defined in `iq-abstract/src/main/java/systems/symbol/agent/I_Agent.java`.

- extends `I_Self`, `I_StartStop`
- exposes:
  - `Model getThoughts()` (working memory)
  - `I_StateMachine<Resource> getStateMachine()`

`iq-kernel` should own the lifecycle orchestration and the agent registry whereas specific agent implementations can live in `iq-agentic`.

### `I_Command`
Defined in `iq-abstract/src/main/java/systems/symbol/agent/I_Command.java`.

- `IRI getActor()`
- `IRI getAction()`
- `Resource getTarget()`

`iq-kernel` command pipeline can execute or dispatch `I_Command` messages via `KernelEvent` or `KernelCommand` adapters.

### `I_Facade`
Defined in `iq-abstract/src/main/java/systems/symbol/agent/I_Facade.java`.

- `Bindings getBindings()`

Used for integration with script engines, bridge points to `javax.script` execution contexts, and payload bindings for event/message transport.

## Suggested kernel extensions

### `iq-kernel` + `I_Agent`
- Add a small agent registry interface:
  - `I_AgentRegistry` with methods `register(I_Agent)`, `lookup(IRI)`, `list()`, `unregister(IRI)`.
- Expose from `KernelContext` (either typed getter `getAgentRegistry()` or `get("agentRegistry")`).
- Provide default simple implementation in `iq-kernel`.

### `IQ command/event/agent convergence`
- New model for agent-based command handling:
  - `KernelCommand` may accept `I_Command` payload.
  - Emit events:
- `urn:iq:event:agent:command:received`
- `urn:iq:event:agent:command:executed`
- `urn:iq:event:agent:command:failed`
- Combine with `I_EventHub` for low-friction extension points.

### `I_Facade` and scripting
- `iq-kernel` should define a small script API wrapper:
  - `I_KernelFacade extends I_Facade` with helper methods for kernel context, event hub, security.
- Shell commands, CLI commands, and controllers can choose this abstraction for uniform behavior.

## Existing patterns to reuse

- `KernelBuilder.withAttribute(name,value)` (use keys `"eventHub"`, `"agentRegistry"`, `"commandFacade"`)
- `SimpleEventHub` for default, plus optional `NoopEventHub`.
- Surface adapters (CLI/MCP/REST/Camel/Vertx) can instantiate their own `I_Agent` and register events using kernel event topics.

## Calling out rich hooks

`iq-kernel/HOOKS.md` (already added) should remain the source of truth for event names. `ABSTRACT.md` references this file for the agent layer and should be placed as a companion doc.

## Open iteration items (next commits)

1. Add `I_AgentRegistry` and wire into `KernelContext`.
2. Add `KernelAgent` adapter in `iq-agentic` implementing `I_Agent`.
3. Add command/event mapping for `I_Command` in kernel command pipeline.
4. Add integration tests in `iq-kernel` module for the kernel + agent ecosystem.
