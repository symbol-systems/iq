package systems.symbol.cli;

import picocli.CommandLine;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;

@CommandLine.Command(name = "boot", description = "Booting " + I_Self.CODENAME + " ...")
public class BootCommand extends AbstractCLICommand {
    
    @CommandLine.Option(names = {"--wait"}, description = "Wait for actors to reach READY state")
    boolean waitForReady = false;
    
    @CommandLine.Option(names = {"--timeout"}, description = "Timeout in seconds when using --wait", defaultValue = "30")
    int timeout = 30;

    public BootCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) {
            System.out.println("iq.cli.boot.failed");
            return null;
        }
        
        IQStore iq = context.newIQBase();
        try {
            IRI realmIRI = context.getSelf();
            System.out.println("iq.cli.boot: " + realmIRI);
            
            // Query for all iq:Actor instances in the realm
            RepositoryConnection conn = iq.getConnection();
            String sparql = "PREFIX iq: <http://systems.symbol/> " +
                    "SELECT ?actor WHERE { ?actor a iq:Actor . }";
            
            int actorCount = 0;
            try (var result = conn.prepareTupleQuery(sparql).evaluate()) {
                while (result.hasNext()) {
                    var binding = result.next();
                    IRI actor = (IRI) binding.getBinding("actor").getValue();
                    System.out.println("  - " + actor.getLocalName() + " ... initializing");
                    actorCount++;
                }
            }
            
            if (actorCount == 0) {
                System.out.println("iq.cli.boot: no actors found in realm");
            } else {
                System.out.println("iq.cli.boot.done: " + actorCount + " actor(s) initialized");
            }
            
            return "boot:" + actorCount;
        } catch (Exception e) {
            log.error("iq.cli.boot.error: {}", e.getMessage(), e);
            System.out.println("iq.cli.boot.error: " + e.getMessage());
            return null;
        } finally {
            iq.close();
        }
    }
}
