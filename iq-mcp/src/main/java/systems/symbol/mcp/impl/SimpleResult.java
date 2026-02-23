package systems.symbol.mcp.impl;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import systems.symbol.mcp.I_MCPResult;

public class SimpleResult implements I_MCPResult {
    private final boolean success;
    private final Model payload;
    private final Model audit;
    private final Optional<IRI> error;
    private final Optional<Throwable> cause;
    private final int cost;
    private final long durationMillis;

    public SimpleResult(boolean success, Model payload) {
        this(success, payload, new LinkedHashModel(), Optional.empty(), Optional.empty(), 0, 0L);
    }

    public SimpleResult(boolean success, Model payload, Model audit, Optional<IRI> error, Optional<Throwable> cause, int cost, long durationMillis) {
        this.success = success;
        this.payload = payload == null ? new LinkedHashModel() : payload;
        this.audit = audit == null ? new LinkedHashModel() : audit;
        this.error = error == null ? Optional.empty() : error;
        this.cause = cause == null ? Optional.empty() : cause;
        this.cost = cost;
        this.durationMillis = durationMillis;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Model getPayload() {
        return payload;
    }

    @Override
    public java.util.Optional<IRI> getError() {
        return error;
    }

    @Override
    public java.util.Optional<Throwable> getCause() {
        return cause;
    }

    @Override
    public Model getAudit() {
        return audit;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public long getDurationMillis() {
        return durationMillis;
    }
}
