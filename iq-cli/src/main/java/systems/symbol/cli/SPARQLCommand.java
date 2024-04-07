package systems.symbol.cli;

import systems.symbol.io.Display;
import systems.symbol.io.StreamCopy;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "sparql", description = "Run a local SPARQL query")
public class SPARQLCommand extends AbstractCLICommand{
    @CommandLine.Parameters(index = "0", description = "The name of the query.")
    String query = "";

    public SPARQLCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        File assets = context.getAssetsHome();
        File queryFile = new File(assets, query+".sparql");
        if (query==null || query.isEmpty() || !queryFile.exists()) {
            showQueries(context.getAssetsHome());
            return 1;
        }
        System.out.println("executing query: "+queryFile.getAbsolutePath());
        String query = StreamCopy.toString(queryFile);
        System.out.println("query results: "+query);
        IQ iq = context.newIQBase();
        SPARQLMapper sparqlMapper = new SPARQLMapper(iq);
        List<java.util.Map<String, Object>> selected = sparqlMapper.query(query, null);
        Display.display(selected);
        iq.close();
        return 0;
    }

    void showQueries(File queryHome) {
        File[] files = queryHome.listFiles();
        if (files != null && files.length>0) {
            System.out.println("local sparql queries:");
            for (File file : files) {
                if (file.getName().contains(".sparql"))
                    System.out.println(file.getName());
            }
        } else {
            System.out.println("no queries in "+queryHome.getAbsolutePath());
        }
    }
}
