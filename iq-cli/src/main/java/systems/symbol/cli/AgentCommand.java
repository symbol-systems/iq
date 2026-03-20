package systems.symbol.cli;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.io.Display;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "agent", description = "Agent transition management: list and trigger agent transitions")
public class AgentCommand extends AbstractCLICommand {

    @CommandLine.Option(names = "--list", description = "List available agent transitions")
    boolean list = false;

    @CommandLine.Option(names = "--actor", description = "Restrict by actor name")
    String actor;

    @CommandLine.Option(names = "--intent", description = "Restrict by intent name")
    String intent;

    @CommandLine.Option(names = "--agent", description = "Restrict by agent IRI")
    String agent;

    @CommandLine.Option(names = "--trigger", description = "Trigger a matched agent transition")
    boolean trigger = false;

    public AgentCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) {
            display("Workspace is not initialized. run `iq init` first.");
            return 1;
        }

        if (list || !trigger) {
            listTransitions();
        }

        if (trigger) {
            if ((actor == null || actor.isBlank()) || (intent == null || intent.isBlank())) {
                display("--trigger requires --actor and --intent");
                return 1;
            }
            triggerTransition(actor, intent);
        }

        return 0;
    }

    private void listTransitions() {
        try (RepositoryConnection conn = context.getRepository().getConnection()) {
            IQStore iq = new IQConnection(context.getSelf(), conn);
            SPARQLMapper mapper = new SPARQLMapper(iq);

            String q = "PREFIX iq: <iq:>\n" +
                    "SELECT ?agent ?actorName ?intent WHERE {\n" +
                    "  GRAPH <iq:agents> {\n" +
                    "    ?agent a iq:Agent ;\n" +
                    "           iq:name ?actorName ;\n" +
                    "           iq:to ?transition .\n" +
                    "    ?transition iq:intent ?intent .\n" +
                    "  }\n" +
                    "}";

            List<Map<String, Object>> transitions = mapper.query(q, null);
            if (agent != null && !agent.isBlank()) {
                transitions.removeIf(m -> !agent.equals(m.get("agent")));
            }
            if (actor != null && !actor.isBlank()) {
                transitions.removeIf(m -> !actor.equals(m.get("actorName")));
            }
            if (intent != null && !intent.isBlank()) {
                transitions.removeIf(m -> !intent.equals(m.get("intent")));
            }

            if (transitions.isEmpty()) {
                display("No agent transitions found.");
            } else {
                Display.display(transitions);
            }

        } catch (Exception ex) {
            display("Failed to list transitions: " + ex.getMessage());
        }
    }

    private void triggerTransition(String actor, String intent) {
        // Placeholder: real engine integration requires IntentAPI / agent execution path.
        display("Trigger requested: actor=" + actor + " intent=" + intent + ".");
        display("This is a stub in iq-cli; integrate with IntentAPI or iq-mcp DynamicAgentBridge for real runs.");
    }
}
