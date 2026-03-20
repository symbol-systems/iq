package systems.symbol.cli;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import picocli.CommandLine;
import systems.symbol.platform.I_Self;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@CommandLine.Command(name = "infer", description = "Infer models from this " + I_Self.CODENAME)
public class InferCommand extends AbstractCLICommand {
    @CommandLine.Parameters(index = "0", description = "The path to the Insert query.")
    String script = "infer/index.sparql";

    public InferCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    protected Object doCall() throws Exception {
        File queryFile = new File(context.getKernelContext().getHome(), script);
        if (!queryFile.exists() || !queryFile.isFile()) {
            display("Infer script not found: " + queryFile.getAbsolutePath());
            return 1;
        }

        String sparql = Files.readString(queryFile.toPath(), StandardCharsets.UTF_8).trim();
        if (sparql.isEmpty()) {
            display("Infer script is empty: " + queryFile.getAbsolutePath());
            return 1;
        }

        try (RepositoryConnection conn = context.getRepository().getConnection()) {
            conn.prepareUpdate(QueryLanguage.SPARQL, sparql).execute();
            display("Infer script executed: " + queryFile.getName());
        }

        log.info("iq.infer.done: {}", script);
        return 0;
    }
}


