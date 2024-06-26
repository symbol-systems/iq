package systems.symbol.agent;

import systems.symbol.fsm.StateException;
import systems.symbol.intent.JSR233;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 * Agent that executes JSR233 scripts when a state transition occurs.
 */
public class ScriptAgent extends IntentAgent {
    /**
     * Constructs a new ScriptAgent with the provided RDF4J model and self identity.
     *
     * @param model The RDF4J model associated with the agent.
     * @param self  The self identity of the agent.
     */
    public ScriptAgent(@NotNull IRI self, @NotNull Model model, Bindings bindings) throws StateException {
        super(self, model, new JSR233(self, model), bindings);
    }

    public ScriptAgent(@NotNull IRI self, @NotNull Model model) throws StateException {
        super(self, model, new JSR233(self, model), new SimpleBindings());
    }

    public ScriptAgent(@NotNull IRI self, @NotNull Model ground, @NotNull Model thoughts) throws StateException {
        super(self, ground, new JSR233(self, thoughts), new SimpleBindings());
    }
}
