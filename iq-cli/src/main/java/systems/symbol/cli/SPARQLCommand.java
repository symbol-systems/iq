package systems.symbol.cli;

import systems.symbol.io.Display;
import systems.symbol.io.StreamCopy;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "sparql", description = "Run a local SPARQL query")
public class SPARQLCommand extends AbstractCLICommand{
@CommandLine.Parameters(index = "0", arity = "0..1", description = "The name of the query file (local asset) or inline SPARQL text")
String query = "";

public SPARQLCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
File assets = context.getAssetsHome();
if (query == null || query.isBlank()) {
showQueries(assets);
return 1;
}

String sparql;
File qFile = new File(query);
File localQueryFile = new File(assets, query + ".sparql");
if (qFile.exists()) {
sparql = StreamCopy.toString(qFile);
display("executing query file: " + qFile.getAbsolutePath());
} else if (localQueryFile.exists()) {
sparql = StreamCopy.toString(localQueryFile);
display("executing local asset query: " + localQueryFile.getAbsolutePath());
} else if (looksLikeSparql(query)) {
sparql = query;
display("executing inline query");
} else {
showQueries(assets);
return 1;
}

IQStore iq = context.newIQBase();
SPARQLMapper mapper = new SPARQLMapper(iq);
if (mapper.isSelect(sparql) || sparql.toUpperCase().contains("ASK")) {
List<java.util.Map<String, Object>> selected = mapper.query(sparql, null);
Display.display(selected);
} else {
var graphResult = mapper.graph(sparql, null);
if (graphResult == null) {
display("Unsupported SPARQL form. Use SELECT, ASK, CONSTRUCT.");
} else {
while (graphResult.hasNext()) {
display(graphResult.next().toString());
}
}
}
iq.close();
return 0;
}

private boolean looksLikeSparql(String q) {
String upper = q.toUpperCase().trim();
return upper.startsWith("SELECT") || upper.startsWith("ASK") || upper.startsWith("CONSTRUCT") || upper.startsWith("DESCRIBE") || upper.startsWith("PREFIX");
}

void showQueries(File queryHome) {
File[] files = queryHome.listFiles();
if (files != null && files.length>0) {
display("local sparql queries:");
for (File file : files) {
if (file.getName().contains(".sparql"))
display(file.getName());
}
} else {
display("no queries in "+queryHome.getAbsolutePath());
}
}
}
