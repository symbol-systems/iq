package systems.symbol.cli;

import systems.symbol.io.Display;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import org.eclipse.rdf4j.model.IRI;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "list", description = "List models using a named (SPARQL) query")
public class ListCommand extends AbstractCLICommand{
    @CommandLine.Parameters(index = "0", description = "The name of the query.")
    String query = "";

    public ListCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) throw new CLIException("IQ is not OK");
        if (query==null || query.isEmpty()) {
            showModels();
            return null;
        } else {
            IQ iq = context.newIQBase();
            List<Map<String,Object>> models = Display.models(iq, query);
            if (models==null) {
                context.display("query not found");
                return 1;
            }
            log.info("iq.select.count: " + models.size());
            log.info("----------");
            Display.table(System.out, models);
            iq.close();
            return models;
        }
    }

    public void showModels() {
        IQ iq = context.newIQBase();
        Map<IRI, String> defaults = SPARQLMapper.defaults(iq);
        for(IRI q: defaults.keySet()) {
            System.out.println(q.getLocalName());
        }
        iq.close();
    }
}
