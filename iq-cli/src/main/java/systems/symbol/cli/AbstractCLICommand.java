package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;
import systems.symbol.kernel.command.KernelRequest;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractCLICommand extends systems.symbol.kernel.command.AbstractKernelCommand<Object>
        implements Callable<Object> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractCLICommand.class);

    protected final CLIContext context;

    public AbstractCLICommand(CLIContext context) throws IOException {
        super(context.getKernelContext());
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

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) {
            throw new CLIException("IQ workspace not initialized");
        }

        KernelRequest request = kernelRequest(context.getSelf().stringValue()).build();
        systems.symbol.kernel.command.KernelResult<Object> result = execute(request);
        return result.isSuccess() ? result.value().orElse(null) : result.failure().get();
    }

    @Override
    protected systems.symbol.kernel.command.KernelResult<Object> doExecute(KernelRequest request) {
        try {
            Object output = doCall();
            return systems.symbol.kernel.command.KernelResult.ok(output);
        } catch (systems.symbol.kernel.KernelException ke) {
            throw ke;
        } catch (Exception e) {
            throw new systems.symbol.kernel.KernelCommandException("cli.command.failed", e.getMessage(), e);
        }
    }

    protected abstract Object doCall() throws Exception;
}
