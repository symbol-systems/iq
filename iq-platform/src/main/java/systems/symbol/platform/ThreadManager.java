package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@code ThreadManager} class manages multiple I_StartStop instances identified by IRIs.
 * It implements the {@code I_StartStop} interface to manage the orderly starting and stopping of tasks.
 */
public class ThreadManager implements I_StartStop {
protected static final Logger log = LoggerFactory.getLogger(ThreadManager.class);

private final ConcurrentMap<IRI, Thread> threads = new ConcurrentHashMap<>();
private final ConcurrentMap<IRI, I_StartStop> tasks = new ConcurrentHashMap<>();

/**
 * Adds an {@code I_StartStop} instance identified by an IRI to the manager.
 *
 * @param iri   the IRI identifying the {@code I_StartStop} instance.
 * @param task  the {@code I_StartStop} instance to be managed.
 * @return the thread managing the task.
 */
public synchronized Thread add(IRI iri, I_StartStop task) {
if (tasks.containsKey(iri)) {
return threads.get(iri);
}

Runnable runnable = () -> {
try {
log.info("threads.run: {}", iri);
task.start();
task.stop();
log.info("threads.done: {}", iri);
} catch (Exception e) {
log.error("threads.fatal: {} -> {}", iri, e.getMessage());
}
};

Thread thread = new Thread(runnable);
thread.setName(iri.stringValue());
threads.put(iri, thread);
tasks.put(iri, task);
log.info("threads.add: {}", iri);
return thread;
}

/**
 * Starts a specific agent.
 *
 * @param agent  the IRI identifying the task.
 */
public synchronized Thread start(IRI agent) {
Thread thread = threads.get(agent);
if (thread == null) return null;
log.info("threads.start: {} -> {}", agent, thread.isAlive());
if (!thread.isAlive()) thread.start();
return thread;
}

@Override
public synchronized void start() throws Exception {
log.info("threads.starting: x{}", threads.keySet());
for (IRI iri : tasks.keySet()) {
Thread thread = threads.get(iri);
if (thread != null && !thread.isAlive()) {
thread.start();
}
}
}

@Override
public synchronized void stop() {
for (IRI iri : tasks.keySet()) {
I_StartStop startStop = tasks.get(iri);
if (startStop != null) {
startStop.stop();
}

Thread thread = threads.get(iri);
if (thread != null) {
try {
thread.join();
} catch (InterruptedException e) {
log.error("threads.oops: {} -> {}", iri, e.getMessage());
}
}
}
}
}
