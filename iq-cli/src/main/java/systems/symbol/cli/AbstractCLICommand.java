package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;
import systems.symbol.kernel.command.KernelRequest;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractCLICommand implements Callable<Object> {
    protected static final Logger log = LoggerFactory.getLogger(AbstractCLICommand.class);

    CLIContext context;

    public AbstractCLICommand(CLIContext context) throws IOException {
        this.context = context;
    }

    public KernelContext getKernelContext() {
        return context.getKernelContext();
    }

    public KernelRequest.Builder kernelRequest(String subject) {
        return KernelRequest.on(context.getSelf())
                .realm(context.getSelf())
                .param("subject", subject)
                .param("caller", context.getSelf());
    }

    public void display(String msg) {
        System.out.println(msg);
    }
}
