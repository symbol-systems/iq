package systems.symbol.cli;

import picocli.CommandLine;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;

@CommandLine.Command(name = "boot", description = "Booting " + I_Self.CODENAME + " ...")
public class BootCommand extends AbstractCLICommand {

    public BootCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (context.isInitialized()) {
            IQStore iq = context.newIQBase();
            // Actions actions = new Wisdom(iq);
            // System.out.println("iq.cli.boot: " + context.getIdentity());
            // IRI done = actions.execute(context.getIdentity());
            iq.close();
            // System.out.println("iq.cli.boot.done: " + done);
        } else {
            System.out.println("iq.cli.boot.failed");
        }
        return null;
    }
}
