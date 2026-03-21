package systems.symbol.cli;

import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "recover", description = "Reload from backup (after deleting KBMS)")
public class RecoverCommand extends AbstractCLICommand{

    public RecoverCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) throw new CLIException("IQ not ready");
        context.recover();
        log.info("iq.cli.recover.done");
        return "recovered";
    }}
