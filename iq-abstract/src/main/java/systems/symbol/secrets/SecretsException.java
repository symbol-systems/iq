package systems.symbol.secrets;

public class SecretsException extends Exception {
    public SecretsException(String s) {
        super(s);
    }

    public SecretsException(String s, Throwable cause) {
        super(s, cause);
    }

    public SecretsException(Throwable cause) {
        super(cause);
    }
}
