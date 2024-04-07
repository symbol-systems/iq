package systems.symbol.agent;

import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of an agent that executes intents within a symbolic system.
 */
public class IntentAgent extends AbstractAgent {
    private final I_Intent intent;

    /**
     * Constructs a new IntentAgent with the provided intent, RDF4J model, and self identity.
     *
     * @param intent The intent to be executed by the agent.
     * @param model  The RDF4J model associated with the agent.
     * @param self   The self identity of the agent.
     */
    public IntentAgent(@NotNull I_Intent intent, @NotNull Model model, @NotNull IRI self) {
        super(model, self);
        this.intent = intent;
    }

    /**
     * Handles transitions within the symbolic system.
     *
     * @param from The resource representing the source state of the transition.
     * @param to   The resource representing the target state of the transition.
     * @return true if the transition is handled successfully, false otherwise.
     */
    @Override
    public boolean onTransition(Resource from, Resource to) {
        try {
            log.debug("onTransition: {}", to);
            execute(getIdentity(), to);
            return true;
        } catch (StateException e) {
            return false;
        }
    }

    /**
     * Executes an intent based on the provided subject and object.
     *
     * @param subject The subject of the execution.
     * @param object  The object of the execution.
     * @return A set of IRIs resulting from the execution.
     * @throws StateException If an error occurs during execution.
     */
    @Override
    public Set<IRI> execute(IRI subject, Resource object) throws StateException {
        log.debug("execute: {} -> {}", subject, object);
        if (object instanceof IRI)
            return intent.execute(subject, object);
        Set<IRI> iris = new HashSet<>();
        Collection<Statement> found = RDFCollections.getCollection(model, object, new HashSet<>());
        for (Statement statement : found) {
            Value v = statement.getObject();
            if (v instanceof Resource) {
                iris.addAll(execute(subject, (Resource) v));
            }
        }
        return iris;
    }
}
