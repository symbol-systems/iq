# IQ Kernel Hooks + Event-Driven Triggers

**Table of Contents**
- [Overview](#overview)
- [Architecture](#architecture)
- [Event Reference](#event-reference-quick-lookup)
- [Implementation Guide](#implementation-guide)
- [Observability & Debugging](#observability--debugging)
- [Configuration & Lifecycle](#configuration--lifecycle)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#implementation-roadmap)

---

## Overview

### Objective
Add first-class, minimal-footprint event/hook support across IQ layers so runtime operations in:
- RDF4J models and repositories
- Script execution (e.g., `iq-run-apis`, `iq-platform` script engines)
- FSMs (e.g., `iq-agentic` / actor transitions)
- Lake and storage operations
- CLI / MCP / REST surfaces

...can register and consume event-driven hooks with consistent semantics.

### Key Benefits
- **Decoupled monitoring**: Observe any operation without modifying core logic
- **Audit trails**: Track data mutations and state transitions
- **Extensibility**: Scripts, agents, and connectors can react to system events
- **Observability**: Centralized visibility into system behavior for debugging and metrics
- **Testing**: Verify interactions without coupling to implementations

---

## Architecture

### Core Baseline (Kernel Event Foundation)

The `iq-kernel` already provides a minimal, proven event abstraction:

| Component | Package | Purpose |
|-----------|---------|---------|
| `I_EventHub` | `systems.symbol.kernel.event` | Publish/subscribe broker; decouples producers from consumers |
| `KernelEvent` | `systems.symbol.kernel.event` | Immutable event envelope with `topic` (IRI), `source`, `timestamp`, `payload`, `contentType`, `id` |
| `I_EventSink` | `systems.symbol.kernel.event` | Consumer interface; `accept(KernelEvent)` functional method |
| `SimpleEventHub` | `systems.symbol.kernel.event` | In-process, synchronous hub for tests and CLI |
| `KernelBuilder` | `systems.symbol.kernel` | Integration point: `withAttribute("eventHub", hub)` for pluggable hub injection |

**Why this baseline?**
- Minimal footprint: single dependency on RDF4J IRIs + no external frameworks
- Proven: used in existing kernel lifecycle and test harnesses
- Extensible: surface layers can inject Camel, Vert.x, or custom implementations

### Layered Hook System

```
┌─────────────────────────────────────────────────────────────┐
│ Application / CLI / REST (iq-run-apis, iq-cli-pro)          │
│  └─ Subscribe to events: kernel start, command pre/post     │
├─────────────────────────────────────────────────────────────┤
│ Domain Layers (iq-platform, iq-rdf4j, iq-lake, iq-agentic)  │
│  └─ Emit domain events: RDF changes, FSM transitions,       │
│    script execution, data operations                         │
├─────────────────────────────────────────────────────────────┤
│ Kernel Core (iq-kernel)                                      │
│  └─ I_EventHub, KernelEvent, I_EventSink (transport)         │
└─────────────────────────────────────────────────────────────┘
```

Each domain layer wraps APIs with **transparent event emission**:
- `ObservedRepositoryConnection` (wraps `RepositoryConnection` for RDF4J)
- `ObservedScriptExecutor` (wraps script lifecycle)
- `ObservedStateMachine` (wraps FSM transitions)

---

## Event Reference: Quick Lookup

All events follow the naming pattern: `urn:iq:event:<domain>:<operation>[:<stage>]`

### Kernel Core Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:kernel:start` | (none) | `null` or context JSON | Kernel initialization complete |
| `urn:iq:event:kernel:stop` | (none) | `null` or shutdown reason | Kernel shutdown initiated |
| `urn:iq:event:kernel:restart` | (none) | `null` | Kernel restarts |

### Repository & RDF Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:rdf:statement:add` | (none) | Statement IRI/Model snippet | Triple added to repository |
| `urn:iq:event:rdf:statement:remove` | (none) | Statement IRI | Triple removed |
| `urn:iq:event:rdf:statement:clear` | (none) | Context IRI or null | Repository/context cleared |
| `urn:iq:event:rdf:repository:commit` | (none) | Transaction ID + row count | Transaction committed |
| `urn:iq:event:rdf:repository:rollback` | (none) | Transaction ID + reason | Transaction rolled back |
| `urn:iq:event:rdf:repository:prepare` | (none) | Transaction ID | 2-phase commit prepare phase |

### Script Execution Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:script:load` | (none) | Script IRI + metadata | Script loaded from asset |
| `urn:iq:event:script:compile` | (none) | Language (SPARQL/Groovy) | Script compiled/prepared |
| `urn:iq:event:script:execute` | `pre` | Parameters, user, actor | Before execution |
| `urn:iq:event:script:execute` | `post` | Result, duration (ms) | After successful execution |
| `urn:iq:event:script:error` | (none) | Exception + stack trace | Script execution fails |

### FSM / Agent Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:fsm:transition:before` | (none) | From/to state, event, actor | Before state transition |
| `urn:iq:event:fsm:transition:after` | (none) | New state, bindings changed | After state transition completes |
| `urn:iq:event:fsm:transition:error` | (none) | Error reason, attempted state | Transition guard fails |

### Lake / Batch Operations Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:lake:load` | (none) | Path, schema, row count | Partition loaded into memory |
| `urn:iq:event:lake:store` | (none) | Path, rows written | Partition persisted |
| `urn:iq:event:lake:checkpoint` | (none) | Checkpoint ID, timestamp | Checkpoint created |

### CLI / Command Events
| Topic | Stage | Payload | When Fired |
|-------|-------|---------|-----------|
| `urn:iq:event:cli:command:start` | (none) | Command name, args, user | CLI command begins |
| `urn:iq:event:cli:command:complete` | (none) | Exit code, duration | CLI command exits successfully |
| `urn:iq:event:cli:command:error` | (none) | Exception, exit code | CLI command fails |

**Payload Content Types:**
- `application/json` — structured metadata (duration, row counts, IRIs)
- `text/plain` — error messages, trace info
- `application/x-sparql-results+xml` — SPARQL result sets
- `application/rdf+xml` or `text/turtle` — RDF snippets (avoid full models)

---

## Implementation Guide

### 1. Subscribing to Events (Consumer Pattern)

**Simple listener** — listen to RDF statement additions:

```java
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.kernel.event.KernelEvent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

I_EventHub eventHub = context.getEventHub(); // from KernelContext or injected
IRI topic = SimpleValueFactory.getInstance()
    .createIRI("urn:iq:event:rdf:statement:add");

eventHub.subscribe(topic, event -> {
    System.out.println("Statement added at " + event.getTimestamp());
    Object payload = event.getPayload();
    // Handle payload based on contentType
    String contentType = event.getContentType();
});
```

**Subscribe before executing operations:**

```java
// Setup listener
eventHub.subscribe(
    SimpleValueFactory.getInstance().createIRI("urn:iq:event:script:execute"),
    event -> logExecutionMetrics(event)
);

// Now execute — listener will fire
executor.execute(scriptName, bindings);

// Cleanup when done
eventHub.unsubscribe(
    SimpleValueFactory.getInstance().createIRI("urn:iq:event:script:execute"),
    listener
);
```

### 2. Publishing Events (Producer Pattern)

**Emit events from domain layer code:**

```java
import systems.symbol.kernel.event.KernelEvent;

KernelEvent event = KernelEvent.builder()
    .topic(SimpleValueFactory.getInstance()
        .createIRI("urn:iq:event:rdf:statement:add"))
    .source(repoIRI)
    .contentType("application/json")
    .payload("{\"statement\": \"" + stmt.toString() + "\"}")
    .build();

eventHub.publish(event);
```

**Use helper class for consistency:**

```java
// Proposed: systems.symbol.kernel.event.KernelTopics (constants)
public class KernelTopics {
    public static final IRI RDF_STATEMENT_ADD = 
        SimpleValueFactory.getInstance()
            .createIRI("urn:iq:event:rdf:statement:add");
    public static final IRI SCRIPT_EXECUTE_PRE = 
        SimpleValueFactory.getInstance()
            .createIRI("urn:iq:event:script:execute:pre");
    // ... more constants
}

// Usage
eventHub.publish(KernelEvent.builder()
    .topic(KernelTopics.RDF_STATEMENT_ADD)
    .source(mySource)
    .payload(myData)
    .build());
```

### 3. Wrapper Pattern (Non-Invasive Instrumentation)

**Example: `KernelRepositoryConnection` for RDF4J events**

```java
public class KernelRepositoryConnection extends RepositoryConnectionWrapper {
    private final I_EventHub eventHub;

    public KernelRepositoryConnection(RepositoryConnection delegate, I_EventHub hub) {
        super(delegate, getRepository(delegate));
        this.eventHub = hub != null ? hub : new NoopEventHub();
    }

    @Override
    public void add(Statement st, Resource... contexts) throws RepositoryException {
        try {
            eventHub.publish(KernelEvent.builder()
                .topic(KernelTopics.RDF_STATEMENT_ADD)
                .source(getCurrentRepositoryName())
                .payload(st)
                .contentType("application/rdf+xml")
                .build());
        } catch (Exception e) {
            // Log but do not propagate — hooks must not break core flow
            logger.warn("Event publication failed for RDF add", e);
        }
        
        // Proceed with actual add
        super.add(st, contexts);
    }

    @Override
    public void commit() throws RepositoryException {
        try {
            eventHub.publish(KernelEvent.builder()
                .topic(KernelTopics.RDF_REPOSITORY_COMMIT)
                .payload("{\"transactionId\": \"" + getTransactionId() + "\"}")
                .contentType("application/json")
                .build());
        } catch (Exception e) {
            logger.warn("Event publication failed for commit", e);
        }
        
        super.commit();
    }
}
```

**Factory to inject wrappers transparently:**

```java
public class ObservedRepositoryFactory {
    public static RepositoryConnection wrap(
            RepositoryConnection conn, 
            I_EventHub eventHub) {
        if (eventHub == null || eventHub instanceof NoopEventHub) {
            return conn; // Bypass decoration if disabled
        }
        return new ObservedRepositoryConnection(conn, eventHub);
    }
}

// In connection acquisition code:
RepositoryConnection conn = repository.getConnection();
conn = ObservedRepositoryFactory.wrap(conn, eventHub);
```

---

## Observability & Debugging

### Tracing Hook Execution

**Enable hook tracing via logging:**

```java
// In ObservedRepositoryConnection or any hook producer
SLF4J Logger logger = LoggerFactory.getLogger("iq.hooks.trace");

if (logger.isDebugEnabled()) {
    logger.debug("Publishing hook: topic={}, source={}, type={}", 
        event.getTopic(), event.getSource(), event.getContentType());
}

if (logger.isTraceEnabled()) {
    logger.trace("Hook payload: {}", event.getPayload());
}
```

**Logback Configuration** (`logback.xml`):

```xml
<!-- Trace hook execution -->
<logger name="iq.hooks.trace" level="DEBUG" />

<!-- Separate audit log for mutations -->
<appender name="AUDIT" class="ch.qos.logback.core.FileAppender">
    <file>logs/hooks-audit.log</file>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<logger name="iq.hooks.audit" level="INFO" additivity="false">
    <appender-ref ref="AUDIT" />
</logger>
```

### Inspection Hook

**Debug hook: collect all events for inspection:**

```java
/**
 * For testing/debugging: collects all events into a thread-local list.
 */
public class InspectionHub implements I_EventHub, I_EventSink {
    private static final ThreadLocal<List<KernelEvent>> events = 
        ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void subscribe(IRI topic, I_EventSink sink) {
        // For inspection, just add this hub as sink internally
    }

    @Override
    public void publish(KernelEvent event) {
        events.get().add(event);
    }

    public static List<KernelEvent> getEvents() {
        return events.get();
    }

    public static void clear() {
        events.get().clear();
    }
}

// Usage in tests
@Test
void testRepositoryEmitsAddEvent() {
    InspectionHub hub = new InspectionHub();
    ObservedRepositoryConnection conn = 
        new ObservedRepositoryConnection(repo.getConnection(), hub);
    
    conn.add(testStatement);
    
    List<KernelEvent> events = InspectionHub.getEvents();
    assertTrue(events.stream()
        .anyMatch(e -> e.getTopic().stringValue()
            .contains("statement:add")));
}
```

### Query Hook Payloads

**Common pattern: decode hook payload by content type:**

```java
public class EventPayloadDecoder {
    public static Object decode(KernelEvent event) {
        String contentType = event.getContentType();
        Object raw = event.getPayload();

        if ("application/json".equals(contentType)) {
            return new JsonObject(raw.toString());
        } else if ("text/plain".equals(contentType)) {
            return raw.toString();
        } else if ("application/rdf+xml".equals(contentType)) 
            || "text/turtle".equals(contentType)) {
            // Parse as RDF Model
            return Rio.parse(new StringReader(raw.toString()), "", 
                RDFFormat.forMIMEType(contentType).get());
        }
        return raw;
    }
}

// Usage in listener
hub.subscribe(topic, event -> {
    Object payload = EventPayloadDecoder.decode(event);
    // Now payload is strongly typed (JsonObject, String, Model, etc.)
});
```

---

## Configuration & Lifecycle

### Kernel Builder Integration

**Wire event hub at startup:**

```java
// In application initialization (iq-run-apis, iq-cli, etc.)

I_EventHub eventHub;
if (isDevelopment()) {
    eventHub = new SimpleEventHub(); // Synchronous in-process
} else {
    eventHub = new VertxEventHub(vertx); // Async via Vert.x bus
}

KernelBuilder builder = KernelBuilder.create()
    .withAttribute("eventHub", eventHub)
    .withAttribute("hookEnabled", true);

KernelContext context = builder.build().start();
```

**Access event hub from context:**

```java
// Anywhere that has KernelContext
I_EventHub eventHub = (I_EventHub) context
    .getAttribute("eventHub")
    .orElse(new SimpleEventHub());
```

### Configuration Keys

Add to `application.properties` or equivalents:

```properties
# Enable/disable hooks globally (default: true)
iq.hooks.enabled=true

# Enable by namespace (fine-grained control)
iq.hooks.rdf.enabled=true
iq.hooks.script.enabled=true
iq.hooks.fsm.enabled=false
iq.hooks.cli.enabled=true

# Event buffering (for batch operations)
iq.hooks.buffer.enabled=false
iq.hooks.buffer.size=1000
iq.hooks.buffer.flush_interval_ms=5000

# Disable heavy-payload events in production
iq.hooks.rdf.exclude_payloads=true
```

**Implement config check in hooks:**

```java
public class ObservedRepositoryConnection extends RepositoryConnectionWrapper {
    private final I_EventHub eventHub;
    private final boolean enabled;

    public ObservedRepositoryConnection(RepositoryConnection delegate, 
            I_EventHub hub, boolean enabled) {
        super(delegate, getRepository(delegate));
        this.eventHub = hub;
        this.enabled = enabled;
    }

    @Override
    public void add(Statement st, Resource... contexts) throws RepositoryException {
        if (enabled) {
            try {
                eventHub.publish(/* ... */);
            } catch (Exception e) {
                logger.warn("Hook failed", e);
            }
        }
        super.add(st, contexts);
    }
}
```

### Lifecycle: Startup and Shutdown

**Startup sequence:**

```
1. KernelBuilder.create()
2. withAttribute("eventHub", instance)
3. builder.build()
4. context.start() — fires urn:iq:event:kernel:start
5. Subscribe to hooks (listeners register themselves)
6. Execute business logic (hooks emit events)
```

**Shutdown sequence:**

```
1. context.stop() — fires urn:iq:event:kernel:stop
2. Unsubscribe all hooks (listeners clean up)
3. Flush any buffered events
4. Close event hub resources
```

---

## Best Practices

### 1. **Never Block Core Flow**

Hooks must be fail-safe. Event emission failures must **never** crash application logic:

```java
// ✅ CORRECT: Catch and log, continue
try {
    eventHub.publish(event);
} catch (Exception e) {
    logger.warn("Hook failed; continuing", e);
}

// ❌ WRONG: Propagates exception, breaks core logic
eventHub.publish(event); // If this throws, transaction dies
```

### 2. **Lightweight Payloads**

Avoid sending large models or result sets in payloads:

```java
// ❌ WRONG: Full Model in payload
Model largeGraph = // ... 1MB of triples
KernelEvent event = KernelEvent.builder()
    .payload(largeGraph)
    .build();

// ✅ CORRECT: Minimal metadata + reference
String payload = "{"
    + "\"graph_iri\": \"" + graphIRI.stringValue() + "\", "
    + "\"triple_count\": " + largeGraph.size() + ", "
    + "\"generated_at\": \"" + Instant.now() + "\""
    + "}";
KernelEvent event = KernelEvent.builder()
    .payload(payload)
    .contentType("application/json")
    .build();
```

### 3. **Emit Pre- and Post- Events Strategically**

```java
// Pre-event: allows listener to intercept/veto if needed
eventHub.publish(KernelEvent.builder()
    .topic(KernelTopics.SCRIPT_EXECUTE_PRE)
    .payload("{\"script\": \"" + name + "\", \"user\": \"" + user + "\"}")
    .build());

// Execute script
long startMs = System.currentTimeMillis();
ScriptResult result = executor.execute(script, bindings);
long durationMs = System.currentTimeMillis() - startMs;

// Post-event: allows listener to react to outcome
eventHub.publish(KernelEvent.builder()
    .topic(KernelTopics.SCRIPT_EXECUTE_POST)
    .payload("{\"result\": \"" + result + "\", \"duration_ms\": " + durationMs + "}")
    .contentType("application/json")
    .build());
```

### 4. **Use Constants for Topic IRIs**

```java
// ✅ GOOD: Centralized, no typos
import static systems.symbol.kernel.event.KernelTopics.*;

eventHub.publish(KernelEvent.builder()
    .topic(RDF_STATEMENT_ADD)
    .build());

// ❌ AVOID: String literals, prone to typos
eventHub.publish(KernelEvent.builder()
    .topic(vf.createIRI("urn:iq:event:rdf:statement:add"))
    .build());
```

### 5. **Test Hook Behavior Independently**

```java
@Test
void testHookDelivery() {
    InspectionHub hub = new InspectionHub();
    
    // Setup
    List<KernelEvent> received = new ArrayList<>();
    hub.subscribe(KernelTopics.RDF_STATEMENT_ADD, e -> received.add(e));
    
    // Trigger
    ObservedRepositoryConnection conn = new ObservedRepositoryConnection(
        repo.getConnection(), hub);
    conn.add(testStatement);
    
    // Verify
    assertEquals(1, received.size());
    assertEquals(KernelTopics.RDF_STATEMENT_ADD, received.get(0).getTopic());
}
```

### 6. **Document Hook Contracts in README**

```markdown
### Hooks Emitted by [Module]

#### `urn:iq:event:rdf:statement:add`
- **When**: Triple added to repository
- **Payload**: Statement IRI (JSON)
- **ContentType**: `application/json`
- **Reliability**: Synchronous, guaranteed delivery
- **Idempotency**: No; may fire multiple times for same statement

#### `urn:iq:event:script:execute:pre`
- **When**: Before script execution
- **Payload**: Script metadata (name, user, parameters)
- **Reliability**: Synchronous
- **Veto**: Listener may throw to block execution
```

---

## Troubleshooting

### Problem: Events Not Received

**Check 1: Subscription registered?**
```java
// Add debug logging
hub.subscribe(topic, event -> {
    logger.info("EVENT RECEIVED: {}", event.getTopic());
    handler.accept(event);
});
```

**Check 2: Event hub is not no-op?**
```java
if (eventHub instanceof NoopEventHub) {
    logger.warn("Event hub is no-op; hooks are disabled");
}
```

**Check 3: Topic IRI exactly matches?**
```java
// Print what's being published
logger.info("Publishing to topic: {}", event.getTopic().stringValue());

// Compare with subscription
IRI subTopic = SimpleValueFactory.getInstance()
    .createIRI("urn:iq:event:rdf:statement:add");
logger.info("Subscribed to: {}", subTopic.stringValue());

assert event.getTopic().equals(subTopic);
```

### Problem: Hooks Impact Performance

**Solution 1: Disable by namespace**
```properties
iq.hooks.rdf.enabled=false
iq.hooks.lake.enabled=false
iq.hooks.script.enabled=true  # Keep only critical ones
```

**Solution 2: Filter at producer side**
```java
if (config.isHookEnabled("rdf.statement.add")) {
    eventHub.publish(event);
}
```

**Solution 3: Buffer & batch events**
```java
public class BufferingEventHub implements I_EventHub {
    private static final int BATCH_SIZE = 1000;
    private final Queue<KernelEvent> buffer = new ConcurrentLinkedQueue<>();
    
    @Override
    public void publish(KernelEvent event) {
        buffer.offer(event);
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    private void flush() {
        List<KernelEvent> batch = new ArrayList<>();
        while (!buffer.isEmpty()) {
            batch.add(buffer.poll());
        }
        // Send batch to subscribers in single operation
        batch.forEach(this::deliverToSubscribers);
    }
}
```

### Problem: Hook Listener Throws Exception

**Expected behavior:**
```
Core flow is NOT interrupted. Exception is logged, execution continues.
```

**If core flow is interrupted:**
1. Check that try/catch wraps eventHub.publish()
2. Add failure container test: see [SimpleEventHub](./src/main/java/systems/symbol/kernel/event/SimpleEventHub.java) implementation

---

## Implementation Roadmap

### Phase 1: Core (Q1)
- [ ] Define `KernelTopics` constants in `iq-kernel/event` package
- [ ] Document hook contract in this HOOKS.md
- [ ] Add hook configuration keys to `application.properties.example`
- [ ] Ensure `KernelBuilder.withAttribute("eventHub", hub)` works

### Phase 2: RDF4J Layer (Q2)
- [ ] Implement `ObservedRepositoryConnection` in `iq-rdf4j`
- [ ] Wrap connection creation in `iq-platform` via injection
- [ ] Add unit tests for RDF statement add/remove/commit events
- [ ] Document RDF hooks in module README

### Phase 3: Script Execution (Q2)
- [ ] Add `ObservedScriptExecutor` in `iq-platform`
- [ ] Emit script load/compile/execute/error events
- [ ] Integrate with existing catalog + executor classes
- [ ] Add smoke tests for script event delivery

### Phase 4: FSM / Agents (Q3)
- [ ] Instrument `iq-agentic` FSM transition code
- [ ] Emit before/after/error events for state changes
- [ ] Document FSM hook semantics

### Phase 5: Lake Operations (Q3)
- [ ] Instrument partition load/store in `iq-lake`
- [ ] Emit checkpoint events
- [ ] Document lake hook contracts

### Phase 6: CLI / REST / MCP (Q3)
- [ ] Expose event hub in `KernelContext`
- [ ] Emit command start/complete/error in CLI  
- [ ] Wire event hub in REST/MCP surfaces
- [ ] Document CLI/REST hook topics

### Phase 7: Testing & Docs (Q4)
- [ ] Create comprehensive hook test suite per module
- [ ] Add InspectionHub utility + examples
- [ ] Write troubleshooting guide
- [ ] Create "hooks quickstart" guide for extension authors

---

## Summary

**For Developers Implementing Hooks:**
1. Use the [Event Reference](#event-reference-quick-lookup) table to find the right topic
2. Follow the [Wrapper Pattern](#3-wrapper-pattern-non-invasive-instrumentation) to keep core logic untouched
3. Use [Code Examples](#implementation-guide) as templates
4. Always wrap event publication in try/catch and **never break core flow**
5. Keep payloads minimal; use content-type hints for deserialization

**For Developers Consuming Hooks:**
1. Subscribe at startup via `eventHub.subscribe(topic, sink)`
2. Decode payload using [EventPayloadDecoder](#query-hook-payloads)
3. Unsubscribe at shutdown to avoid memory leaks
4. See [Observability](#observability--debugging) for debugging hooks

**For Operators/SREs:**
1. Enable/disable hooks via [configuration keys](#configuration--lifecycle)
2. Use event tracing and audit logs for observability
3. Monitor hook listener performance; disable heavy listeners in production
4. See [Troubleshooting](#troubleshooting) for common issues
