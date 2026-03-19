package systems.symbol.kernel;

/**
 * Thrown when the kernel fails to start: realm init, VFS failure, bad config TTL.
 * Replaces {@code PlatformException} and the boot-path uses of {@code CLIException}.
 */
public class KernelBootException extends KernelException {

public KernelBootException(String message) {
super("kernel.boot", message);
}

public KernelBootException(String code, String message) {
super(code, message);
}

public KernelBootException(String message, Throwable cause) {
super("kernel.boot", message, cause);
}

public KernelBootException(String code, String message, Throwable cause) {
super(code, message, cause);
}
}
