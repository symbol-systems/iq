package systems.symbol.rdf4j.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FakeReturn implements Future<Object> {
Object result;

public FakeReturn() {
}

public void result(Object r) {
this.result=r;
}

public void setResult(Object r) {
this.result=r;
}

@Override
public boolean cancel(boolean mayInterruptIfRunning) {
return false;
}

@Override
public boolean isCancelled() {
return false;
}

@Override
public boolean isDone() {
return true;
}

@Override
public Object get() throws InterruptedException, ExecutionException {
return result;
}

@Override
public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
return result;
}
}