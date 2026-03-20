package systems.symbol.cli;

import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "render", description = "Build assets from templates")
public class RenderCommand extends AbstractCLICommand {
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
    protected Object doCall() throws Exception {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        IRI commandsIRI = vf.createIRI(context.getSelf() + "iq-render");

        try (RepositoryConnection connection = context.getRepository().getConnection()) {
            IQConnection iq = new IQConnection(context.getSelf(), connection);
            doRender(iq, commandsIRI);
        }

        log.info("iq.render.done");
        return 0;
    }

    private void doRender(IQConnection iq, IRI commandsIRI) throws CLIException, IOException {
        SPARQLMapper sparql = new SPARQLMapper(iq);
        List<Map<String, Object>> models = sparql.models(commandsIRI);
        log.info("iq.render.models: {}", models.size());

        File outputFolder = this.toFolder != null ? this.toFolder : context.getPublicHome();
        if (outputFolder == null) {
            outputFolder = new File(context.getKernelContext().getHome(), "render");
        }
        outputFolder.mkdirs();

        int rendered = 0;
        for (int i = 0; i < models.size(); i++) {
            Map<String, Object> m = models.get(i);
            String identity = m.getOrDefault("@id", "model-" + (i + 1)).toString();
            String sanitized = identity.replaceAll("[^a-zA-Z0-9-_\\.]+", "_");
            File target = new File(outputFolder, sanitized + ".txt");
            String content = "Rendered model: " + identity + "\n" + m.toString() + "\n";
            Files.writeString(target.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            rendered++;
        }

        display("Rendered " + rendered + " models to " + outputFolder.getAbsolutePath());
    }
}

