
package systems.symbol.connect.googleapps;

import java.util.concurrent.CompletableFuture;

import systems.symbol.connect.core.I_Connector;
import systems.symbol.connect.core.I_ConnectorKernel;

public final class GoogleAppsKernel implements I_ConnectorKernel {

    private final I_Connector connector;

    public GoogleAppsKernel(I_Connector connector) {
        this.connector = connector;
    }

    @Override
    public I_Connector getConnector() {
        return connector;
    }

    @Override
    public CompletableFuture<Void> start() {
        connector.start();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        connector.stop();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> refresh() {
        connector.refresh();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public systems.symbol.connect.core.I_ConnectorDescriptor getDescriptor() {
        if (connector instanceof systems.symbol.connect.core.I_ConnectorDescriptor) {
            return (systems.symbol.connect.core.I_ConnectorDescriptor) connector;
        }
        throw new IllegalStateException("Connector must implement I_ConnectorDescriptor");
    }
}
