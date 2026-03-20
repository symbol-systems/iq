# Intents and Commands: Complementary Execution Models

## Overview

The IQ ecosystem supports **two orthogonal execution models**:

1. **Intents** (RDF-semantic, stateful, in `iq-platform`)
2. **Commands** (predicate-based, stateless, in `iq-kernel`)

Both can coexist and interoperate within the same kernel context.

---

## Intents: Semantic Template Execution

### Purpose
Intents represent **semantic actions** driven by RDF metadata. They are stateful, template-driven, and discovered via runtime reflection on `@RDF` annotations.

### Key Characteristics
- **RDF-Driven**: Each intent is identified by an RDF IRI (e.g., `IQ_NS.IQ + "think"`)
- **Annotated Discovery**: Methods marked with `@RDF(IRI_VALUE)` are auto-registered by `ExecutiveIntent`
- **Stateful**: Hold references to `Model`, `self` (IRI), and other domain-specific state
- **Semantic**: Template-based processing (Handlebars, SPARQL, etc.)

### Existing Implementations
| Intent | Annotation | Purpose |
|--------|-----------|---------|
| `Think` | `@RDF(IQ_NS.IQ + "think")` | Render templates via LLM, merge RDF results |
| `Learn` | `@RDF(IQ_NS.IQ + "learn")` | Add facts to model |
| `Forget` | `@RDF(IQ_NS.IQ + "forget")` | Remove facts from model |
| `ExecutiveIntent` | `@RDF(IQ_NS.IQ + "do")` | Dispatch to registered intents by predicate |
| `Construct` | `@RDF(IQ_NS.IQ + "construct")` | Execute SPARQL CONSTRUCT queries |
| `Find` | `@RDF(IQ_NS.IQ + "find")` | Query and discover resources |

### Execution Flow
```
IntentAgent.onTransition() 
  → ExecutiveIntent.execute(actor, state, bindings)
→ ExecutiveIntent.executeIntent(predicate, object)
  → Look up intent by predicate IRI
  → I_Intent.execute(actor, object, bindings)
```

### Reflection-Based Registration (ExecutiveIntent.add)
```java
public IRI add(I_Intent intent) {
for (Method method : intent.getClass().getDeclaredMethods()) {
if (method.isAnnotationPresent(RDF.class)) {
RDF annotation = method.getAnnotation(RDF.class);
IRI methodIRI = Values.iri(annotation.value());
this.intents.put(methodIRI, intent);  // ← Maps predicate IRI to intent instance
}
}
}
```

**No interference from kernel changes**: The `@RDF` annotation is pure Java metadata. Our kernel does not scan, modify, or interfere with intent discovery.

---

## Commands: Predicate-Based Execution

### Purpose
Commands represent **ordered actions** with explicit actor/action/target predicates. They are stateless and wired through the kernel request pipeline.

### Key Characteristics
- **Predicate-Based**: Identified by RDF predicate (e.g., `iq:create`, `iq:update`)
- **Kernel-Integrated**: Execute via `KernelRequest.withCommand()`
- **Event-Tracked**: Publish `AGENT_COMMAND_RECEIVED`, `AGENT_COMMAND_EXECUTED`, `AGENT_COMMAND_FAILED`
- **Optional**: Can be omitted from requests entirely

### Implementation
```java
public interface I_Command extends I_Agent {
// Defines actor, action, target as RDF IRIs
}

public class KernelRequest {
private final I_Command command;  // ← Optional

public KernelRequest withCommand(I_Command cmd) {
return new KernelRequest(/* ... */, cmd);
}
}

public abstract class AbstractKernelCommand {
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
eventHub.publish(AGENT_COMMAND_RECEIVED, toJson(command));
try {
Set<IRI> result = doExecute(actor, state, bindings);
eventHub.publish(AGENT_COMMAND_EXECUTED, toJson(command));
return result;
} catch (Exception e) {
eventHub.publish(AGENT_COMMAND_FAILED, toJson(command));
throw e;
}
}
}
```

---

## Coexistence and Interoperability

### Why They Don't Conflict

| Aspect | Intents | Commands |
|--------|---------|----------|
| **Discovery** | Reflection on `@RDF` annotations | Explicit wiring via `KernelRequest` |
| **State** | Stateful (Model, IRI self) | Stateless (parameters only) |
| **Execution Context** | IntentAgent → ModelStateMachine | KernelRequest → AbstractKernelCommand |
| **Event Model** | None (legacy, no events) | Full lifecycle events via I_EventHub |
| **RDF Representation** | Semantic triple (predicate IRI) | I_Command metadata (actor/action/target) |

### Interop Patterns

**Pattern 1: Intent invokes a Command**
```java
public class MyIntent extends AbstractIntent {
@RDF(IQ_NS.IQ + "myIntentAction")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
// Wrap work in a command for observability
I_Command cmd = new SimpleCommand(actor, IQ_NS.IQ + "action", state);
KernelRequest req = KernelRequest.builder()
.command(cmd)
.build();
// Execute command through kernel
return kernel.execute(req);
}
}
```

**Pattern 2: Command invokes an Intent**
```java
public class MyCommand extends AbstractKernelCommand {
protected Set<IRI> doExecute(IRI actor, Resource state, Bindings bindings) {
I_Intent intent = executive.getIntents().get(targetPredicate);
if (intent != null) {
return intent.execute(actor, state, bindings);  // ← Invoke existing intent
}
throw new StateException("Intent not found: " + targetPredicate);
}
}
```

**Pattern 3: Hook-Based Observability of Intents**
```java
public class ObservableExecutive extends ExecutiveIntent {
@Override
protected void executeIntent(IRIs done, IRI actor, IRI predicate, Resource target, Bindings bindings) {
I_EventHub hub = kernel.getEventHub();
hub.publish(KernelTopics.INTENT_EXECUTING, toJson(predicate));
try {
super.executeIntent(done, actor, predicate, target, bindings);
hub.publish(KernelTopics.INTENT_EXECUTED, toJson(predicate));
} catch (Exception e) {
hub.publish(KernelTopics.INTENT_FAILED, toJson(predicate));
throw e;
}
}
}
```

---

## Invariants: What Remains Unchanged

### Intents
✅ `AbstractIntent` class signature is **unchanged**
✅ `I_Intent.execute(actor, state, bindings)` contract is **unchanged**
✅ `@RDF` annotation semantics are **unchanged**
✅ `ExecutiveIntent.add()` reflection mechanism is **unchanged**
✅ State machine ↔ intent dispatch pipeline is **unchanged**
✅ Think, Learn, Forget, etc. continue to work **as-is**

### Commands
✅ `I_Command` interface contract (actor/action/target) is **well-defined**
✅ `KernelRequest` is a new transport vessel (no impact on existing code)
✅ Event publishing is **optional** via I_EventHub (SafeEventHub, NoopEventHub wrappers)
✅ Kernel execution pipeline supports **both with and without** commands

---

## Example: Existing Use of Think Intent

The `Think` intent from `iq-platform/src/main/java/systems/symbol/intent/Think.java`:

```java
public class Think extends AbstractIntent {

@Override
@RDF(IQ_NS.IQ + "think")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return thinks(actor, state, bindings);
} catch (IOException | APIException e) {
throw new StateException(e.getMessage(), state, e);
}
}

protected Set<IRI> thinks(IRI actor, Resource state, Literal template, Bindings my, Model model)
throws IOException, APIException {
// Render Handlebars template
String remodelled = HBSRenderer.template(template.stringValue(), bindings);

// Send to LLM for inference
I_Assist<String> thought = new Conversation();
thought.user(remodelled);
llm.complete(thought);

// Parse RDF results and merge into model
Model parsed = Rio.parse(new StringReader(rdf), intent, format);
model.addAll(parsed);
return Models.subjectIRIs(parsed);
}
}
```

### Compatibility Check
| Item | Status | Notes |
|------|--------|-------|
| Extends `AbstractIntent` | ✅ **OK** | No kernel dependency, class unchanged |
| `@RDF` annotation | ✅ **OK** | Runtime metadata, discovery unchanged |
| `execute()` signature | ✅ **OK** | (IRI, Resource, Bindings) unchanged |
| Throws `StateException` | ✅ **OK** | Error handling unchanged |
| Calls `model.addAll()` | ✅ **OK** | RDF operations emit events if hooked (optional, backward compatible) |
| Returns `Set<IRI>` | ✅ **OK** | Contract unchanged |

**Verdict**: Think intent continues to work **without modification**.

---

## Migration Checklist: Intent System

When adding command-driven execution to your application:

- [ ] Intents remain semantic and template-driven (no changes required)
- [ ] Commands are orthogonal and opt-in (existing code unaffected)
- [ ] If you want intent execution to publish events, subclass `ExecutiveIntent` and override `executeIntent()`
- [ ] If an intent should be discoverable as a command, wrap it with an `I_Command` adapter
- [ ] Intent reflection (ExecutiveIntent.add) still finds all `@RDF` annotations
- [ ] State machine transitions still trigger intents via `IntentAgent.onTransition()`
- [ ] Event hub is **optional** (defaults to NoopEventHub if not configured)

---

## Summary

The intent system and command system are **complementary**:

- **Intents** = What semantic action should happen (RDF metadata + template)
- **Commands** = How to order, track, and observe the execution (predicate ordering + events)

Both can coexist in the same kernel. Intents provide the **semantic meaning**, commands provide the **operational observability**. Choose intents for domain logic (learn/forget/think), choose commands for system actions (create/update/delete).
