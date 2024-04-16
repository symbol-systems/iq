package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.MyFacade;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.model.I_Self;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.sparql.ScriptCatalog;

import javax.script.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An intent implementation that executes scripts using SPARQL (Java Scripting API).
 * Extends the AbstractIntent class.
 */
public class Select implements I_Intent, I_Self {
    private final ScriptCatalog catalog;
    private final IQ iq;

    /**
     * Constructs a new SPARQL intent with the provided Connection and self identity.
     *
     * @param self  The self identity of the agent.
     */
    public Select(RepositoryConnection conn, IRI self) {
        this.iq = new IQConnection(self, conn);
        this.catalog = new ScriptCatalog(iq);

    }

    /**
     * Creates a new bindings object for script execution.
     * Executes the SPARQL query based on the provided actor and resource.
     *
     * @param actor   The actor of the execution.
     * @param state  The resource containing the script.
     * @param my        Bindings used in query interpolation.
     * @return A set of IRIs indicating the completion of execution.
     */
    @Override
    @RDF(COMMONS.IQ_NS + "select")
    public Set<IRI> execute(IRI actor, Resource state, Bindings my) throws StateException {
        Set<IRI> done = new HashSet<>();
        try {
            Bindings bindings = MyFacade.rebind(actor, state, my);
            String sparql = catalog.getSPARQL(state.stringValue(), bindings);
            if (sparql==null||sparql.isEmpty()) return null;
            TupleQuery prepared = iq.getConnection().prepareTupleQuery(sparql);
            List<Map<String, Object>> results = SPARQLMapper.toMaps(prepared.evaluate());
            MyFacade.results(bindings, results);
            done.add((IRI) state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return done;
    }

    @Override
    public IRI getSelf() {
        return iq.getSelf();
    }
}
