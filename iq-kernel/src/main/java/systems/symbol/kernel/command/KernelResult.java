package systems.symbol.kernel.command;

import systems.symbol.kernel.KernelException;

import java.util.Optional;

/**
 * Discriminated union result from a kernel command.
 *
 * <p>Either wraps a successful value or a {@link KernelException}.
 * Surface adapters pattern-match on {@link #isSuccess()} to translate:
 * <ul>
 *   <li>REST — {@code 200 OK} body or {@code 4xx/5xx} HTTP response</li>
 *   <li>MCP  — {@code MCPResult} content or {@code MCPResult.error(...)}</li>
 *   <li>CLI  — stdout output or stderr + non-zero exit</li>
 *   <li>Camel — exchange body or {@code exchange.setException(...)}</li>
 * </ul>
 *
 * @param <T> the success value type
 */
public final class KernelResult<T> {

private final T  value;
private final KernelException error;

private KernelResult(T value, KernelException error) {
this.value = value;
this.error = error;
}

/** Creates a successful result wrapping {@code value}. */
public static <T> KernelResult<T> ok(T value) {
return new KernelResult<>(value, null);
}

/** Creates an error result from a {@link KernelException}. */
public static <T> KernelResult<T> error(KernelException ex) {
return new KernelResult<>(null, ex);
}

/** Creates an empty successful result (fire-and-forget commands). */
public static <T> KernelResult<T> ok() {
return new KernelResult<>(null, null);
}

public boolean isSuccess() { return error == null; }
public boolean isError()   { return error != null; }

/**
 * Returns the value, or throws the wrapped exception if this is an error.
 *
 * @throws KernelException if this result represents a failure
 */
public T get() {
if (error != null) throw error;
return value;
}

public Optional<T>value() { return Optional.ofNullable(value); }
public Optional<KernelException> failure() { return Optional.ofNullable(error); }

@Override
public String toString() {
return isSuccess()
   ? "KernelResult[ok=" + value + "]"
   : "KernelResult[error=" + error + "]";
}
}
