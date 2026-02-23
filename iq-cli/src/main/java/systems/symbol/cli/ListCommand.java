package systems.symbol.cli;

import com.github.freva.asciitable.AsciiTable;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.io.Display;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import org.eclipse.rdf4j.model.IRI;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "list", description = "List models using a named (SELECT) query")
public class ListCommand extends AbstractCLICommand{
    @CommandLine.Parameters(index = "0", description = "An IRI for a SELECT query.")
    String query = "";

    public ListCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) throw new CLIException("IQ is not OK");
        if (query==null || query.isEmpty()) {
            showDefaultQueries(context);
            return null;
        }

        try (RepositoryConnection connection = context.getRepository().getConnection()) {
            IQ iq = new IQConnection(context.getSelf(), connection);
            List<Map<String,Object>> models = Display.models(iq, query);
            if (models==null) {
                context.display("query not found: "+query);
            } else {
                System.out.printf("found %s queries\n", models.size());
                Display.table(System.out, models);
            }

        }
        return 0;
    }

    public void showDefaultQueries(CLIContext context) {
        String[] columns = { "name", "IRI" };
        try (RepositoryConnection conn = context.getRepository().getConnection()) {
            IQ iq = new IQConnection(context.getSelf(), conn);
            Map<IRI, String> defaults = SPARQLMapper.defaults(iq);
            List<String[]> rows = new ArrayList<>();
            defaults.forEach( (iri, query) -> {
                String[] row = { iri.getLocalName(), iri.stringValue()};
                rows.add(row);
            });
            System.out.println(AsciiTable.getTable(columns, rows.toArray(String[][]::new)));
            System.out.println();
        }
    }
}
