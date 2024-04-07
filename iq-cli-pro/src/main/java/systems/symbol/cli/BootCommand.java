package systems.symbol.cli;

import systems.symbol.rdf4j.iq.IQ;
import picocli.CommandLine;

import java.io.IOException;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "boot", description = "Booting "+CODENAME+" ...")
public class BootCommand extends AbstractCLICommand{

    public BootCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (context.isInitialized()) {
            IQ iq = context.newIQBase();
//            Actions actions = new Wisdom(iq);
//            System.out.println("iq.cli.boot: " + context.getIdentity());
//            IRI done = actions.execute(context.getIdentity());
            iq.close();
//            System.out.println("iq.cli.boot.done: " + done);
        } else {
            System.out.println("iq.cli.boot.failed");
        }
        return null;
    }}
