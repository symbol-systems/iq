package systems.symbol.agent;

import systems.symbol.intent.JSR233;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * An agent capable of executing scripts as intents.
 * Extends the IntentAgent class.
 */
public class ScriptAgent extends IntentAgent {

    /**
     * Constructs a new ScriptAgent with the provided RDF4J model and self identity.
     * Uses a JSR233 intent for script execution.
     *
     * @param model The RDF4J model associated with the agent.
     * @param self  The self identity of the agent.
     */
    public ScriptAgent(@NotNull Model model, @NotNull IRI self) {
        super(new JSR233(model, self), model, self);
    }

}
