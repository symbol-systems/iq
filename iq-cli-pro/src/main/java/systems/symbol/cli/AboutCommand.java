package systems.symbol.cli;

import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.sparql.SPARQLMapper;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Map;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "about", description = "About this "+CODENAME)
public class AboutCommand extends AbstractCLICommand {

public AboutCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() {
if (context.isInitialized()) {
IQ iq = context.newIQBase();
try (RepositoryConnection conn = iq.getConnection()) {
System.out.println("----------");
System.out.println("iq.contents: ");
conn.getContextIDs().forEach(System.out::println);

System.out.println("iq.namespaces: ");
System.out.println("----------");
for (Namespace ns : conn.getNamespaces()) {
System.out.println(ns);
}

System.out.println("iq.queries: ");
log.info("----------");
showModels();
}
} else {
System.out.println("iq.about.not-loaded");
}
return null;
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
