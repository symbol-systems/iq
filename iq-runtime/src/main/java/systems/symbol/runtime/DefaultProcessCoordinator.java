package systems.symbol.runtime;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of ProcessCoordinator using in-memory task tracking.
 */
public class DefaultProcessCoordinator implements ProcessCoordinator {
private static final Logger log = LoggerFactory.getLogger(DefaultProcessCoordinator.class);

private final ConcurrentMap<IRI, TaskState> tasks = new ConcurrentHashMap<>();

private static class TaskState {
IRI taskId;
volatile boolean running = false;
Object result;

TaskState(IRI taskId) {
this.taskId = taskId;
}
}

@Override
public void submit(IRI taskId) throws Exception {
log.debug("process.submit: {}", taskId);
TaskState task = new TaskState(taskId);
task.running = true;
tasks.put(taskId, task);
}

@Override
public void await(IRI taskId, long timeoutMs) throws Exception {
log.debug("process.await: {} ({}ms)", taskId, timeoutMs);
TaskState task = tasks.get(taskId);
if (task == null) {
throw new IllegalArgumentException("Task not found: " + taskId);
}

long deadline = System.currentTimeMillis() + timeoutMs;
while (task.running && System.currentTimeMillis() < deadline) {
Thread.sleep(100);
}

if (task.running) {
throw new RuntimeException("Task timeout: " + taskId);
}
}

@Override
public boolean isRunning(IRI taskId) {
TaskState task = tasks.get(taskId);
return task != null && task.running;
}

@Override
public void cancel(IRI taskId) throws Exception {
log.debug("process.cancel: {}", taskId);
TaskState task = tasks.get(taskId);
if (task != null) {
task.running = false;
}
}

@Override
public Object getResult(IRI taskId) {
TaskState task = tasks.get(taskId);
return task != null ? task.result : null;
}
}
