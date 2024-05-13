package systems.symbol.decide;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FutureDecision<T> implements Future<I_Delegate<T>> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    boolean isCancelled = false;
    long timeoutSeconds = 30;
    T decision;

    public FutureDecision() {}

    public FutureDecision(T decision) {
        this.decision = decision;
    }
    @Override
    public boolean cancel(boolean b) {
        return isCancelled = b || isCancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return decision!=null;
    }

    public void decide(T decision) {
        this.decision = decision;
    }
    @Override
    public I_Delegate<T> get() throws InterruptedException, ExecutionException {
        try {
            return get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new InterruptedException(e.getMessage());
        }
    }

    @Override
    public I_Delegate<T> get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return () -> decision;
    }
}
