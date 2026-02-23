package systems.symbol.cli;

import picocli.CommandLine;
import systems.symbol.platform.I_Self;

import java.io.IOException;

@CommandLine.Command(name = "infer", description = "Infer models from this "+ I_Self.CODENAME)
public class InferCommand extends AbstractCLICommand {
    @CommandLine.Parameters(index = "0", description = "The path to the Insert query.")
    String script = "infer/index.sparql";

    public InferCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
//        ValueFactory vf = SimpleValueFactory.getInstance();
//        IRI queryIRI = vf.createIRI(context.getIdentity().stringValue(),script);
//        IQ iq = context.newIQBase();
////        SPARQLInfer script = new SPARQLInfer(iq, queryIRI);
////        IRI done = script.execute(queryIRI);
//        log.info("iq.infer.done: " + done+" => "+queryIRI + " @ " +script);
//        iq.close();
        return 0;
    }
}
