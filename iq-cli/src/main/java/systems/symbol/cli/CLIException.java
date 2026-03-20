package systems.symbol.cli;

import systems.symbol.kernel.KernelCommandException;

public class CLIException extends KernelCommandException {

    static final long serialVersionUID = 123456789;

    public CLIException(String msg) {
        super("cli.error", msg);
    }

    public CLIException(String msg, Throwable cause) {
        super("cli.error", msg, cause);
    }
}
