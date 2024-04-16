package systems.symbol.cli;

import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "render", description = "Build assets from templates")
public class RenderCommand extends AbstractCLICommand{
@CommandLine.Parameters(index = "0", description = "The IRI of the model to render.")
String subject = "index";
@CommandLine.Option(names = "--to", description = "The folder to render.")
File toFolder = null;
@CommandLine.Option(names = "--index", description = "The name of the index file when rendering a folder.", defaultValue = "index.html")
String indexFile = "index.html";
@CommandLine.Option(names = "--hbs", description = "Use {{ }} rather than [{ }] syntax for template variables.")
boolean useStandardSyntax = false;

public RenderCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) throw new CLIException("IQ not ready");

SimpleValueFactory vf = SimpleValueFactory.getInstance();
IRI commandsIRI = vf.createIRI(context.getSelf()+"queries/iq-render");

try (RepositoryConnection connection = context.getRepository().getConnection()) {
IQConnection iq = new IQConnection(context.getSelf(), connection);
doRender(iq, commandsIRI);
}

log.info("iq.render.done");
return 0;
}

private void doRender(IQConnection iq, IRI commandsIRI) throws CLIException, IOException {
SPARQLMapper sparql = new SPARQLMapper(iq);
List<Map<String,Object>> models = sparql.models(commandsIRI);
log.info("iq.render.models: " + models.size());

//TripleRenderer renderer = new TripleRenderer(iq);
//if (!useStandardSyntax)
//renderer.blockSyntax();
//
//for (Map<String,Object> m : models) {
//log.info("iq.render: {}", Model.getIdentity(m));
//
//Object id = m.get(NS.KEY_AT_ID);
//Object template = m.get("template");
//if (template != null && id != null) {
//IRI ctx = iq.getIdentity();
//IRI modelIRI = iq.toIRI(id.toString());
//
//// localize IRI to Folder / File
//File toFolder = this.toFolder == null ? context.www_docs : this.toFolder;
//File file = Files.toFile(toFolder, ctx, modelIRI);
////log.info("iq.render: {} @ {}", modelIRI, file.getAbsolutePath());
//
//if (file != null && file.isDirectory()) {
//file = new File(file, indexFile);
//log.info("iq.render.index: {}", file.getAbsolutePath());
//}
//// render, if required
//if (file != null && context.isStale(file)) {
//file.getParentFile().mkdirs();
//Map<String, Object> model = TripleFinder.mapOf(iq.getTriples(), new NS(ctx.stringValue()), modelIRI, null, ctx);
//log.info("iq.render.file: {} -> {}", modelIRI, model);
//FileOutputStream fos = new FileOutputStream(file);
//renderer.render(template.toString(), model, fos);
//fos.close();
//} else {
//log.info("iq.render.skip: " + id);
//}
//}
//}
}
}
