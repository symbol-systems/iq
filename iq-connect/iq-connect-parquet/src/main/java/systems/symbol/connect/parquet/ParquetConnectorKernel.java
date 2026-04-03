
package systems.symbol.connect.parquet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import systems.symbol.connect.core.I_Connector;
import systems.symbol.connect.core.I_ConnectorDescriptor;
import systems.symbol.connect.core.I_ConnectorKernel;

public final class ParquetConnectorKernel implements I_ConnectorKernel {

private final ParquetConnector connector;
private final ScheduledExecutorService executor;
private final AtomicBoolean started = new AtomicBoolean(false);
private ScheduledFuture<?> scheduledTask;

public ParquetConnectorKernel(ParquetConnector connector) {
this.connector = connector;
this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "parquet-connector-kernel"));
}

@Override
public CompletableFuture<Void> start() {
if (started.compareAndSet(false, true)) {
scheduledTask = executor.scheduleAtFixedRate(() -> {
try { connector.refresh(); } catch (Exception ignored) { }
}, 0L, 60000L, TimeUnit.MILLISECONDS);
}
return CompletableFuture.completedFuture(null);
}

@Override
public CompletableFuture<Void> stop() {
if (scheduledTask != null) scheduledTask.cancel(false);
executor.shutdownNow();
started.set(false);
return CompletableFuture.completedFuture(null);
}

@Override
public CompletableFuture<Void> refresh() {
try { connector.refresh(); return CompletableFuture.completedFuture(null); }
catch (Exception e) { CompletableFuture<Void> failed = new CompletableFuture<>(); failed.completeExceptionally(e); return failed; }
}

@Override
public I_Connector getConnector() { return connector; }

@Override
public I_ConnectorDescriptor getDescriptor() { return connector; }
}
