package systems.symbol.connect.azure;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import systems.symbol.connect.core.I_Connector;
import systems.symbol.connect.core.I_ConnectorKernel;
import systems.symbol.connect.core.I_ConnectorDescriptor;

public final class AzureConnectorKernel implements I_ConnectorKernel {

private final AzureConnector connector;
private final Duration pollInterval;
private final ScheduledExecutorService executor;
private final AtomicBoolean started = new AtomicBoolean(false);
private ScheduledFuture<?> scheduledTask;

public AzureConnectorKernel(AzureConnector connector, Duration pollInterval) {
this.connector = connector;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "azure-connector-kernel"));
}

@Override
public CompletableFuture<Void> start() {
if (started.compareAndSet(false, true)) {
scheduledTask = executor.scheduleAtFixedRate(() -> {
try {
connector.refresh();
} catch (Exception e) {
// already recorded by connector internal status handling
}
}, 0L, pollInterval.toMillis(), TimeUnit.MILLISECONDS);
}
return CompletableFuture.completedFuture(null);
}

@Override
public CompletableFuture<Void> stop() {
if (started.get() && scheduledTask != null) {
scheduledTask.cancel(false);
}
executor.shutdownNow();
started.set(false);
return CompletableFuture.completedFuture(null);
}

@Override
public CompletableFuture<Void> refresh() {
try {
connector.refresh();
return CompletableFuture.completedFuture(null);
} catch (Exception e) {
CompletableFuture<Void> failed = new CompletableFuture<>();
failed.completeExceptionally(e);
return failed;
}
}

@Override
public I_Connector getConnector() {
return connector;
}

@Override
public I_ConnectorDescriptor getDescriptor() {
return connector;
}
}
