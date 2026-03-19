package systems.symbol.kernel;

/**
 * Thrown when a required secret cannot be resolved.
 * Replaces {@code SecretsException} for cross-surface use.
 */
public class KernelSecretException extends KernelException {

private final String secretName;

public KernelSecretException(String secretName) {
super("kernel.secret.missing", "Secret not found: " + secretName);
this.secretName = secretName;
}

public KernelSecretException(String secretName, String message) {
super("kernel.secret", message);
this.secretName = secretName;
}

public KernelSecretException(String secretName, Throwable cause) {
super("kernel.secret.missing", "Secret not found: " + secretName, cause);
this.secretName = secretName;
}

/** The key that could not be resolved. */
public String getSecretName() {
return secretName;
}
}
