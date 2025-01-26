package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.RDF;
import systems.symbol.agent.Facades;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An intent implementation that executes scripts using SPARQL.
 *
 * The Select intent executes SPARQL queries stored in the IQScriptCatalog and
 * returns the results
 * as a named list of maps stored in the bindings object.
 *
 * It embodies an agent's capability to retrieve structured/tabular data from
 * arbitrary RDF graphs.
 *
 * This intent provides a versatile mechanism for querying RDF, allowing agents
 * to dynamically
 * retrieve information based on their internal state, context, through
 * interpolated queries.
 *
 * @author Symbol Systems
 * @see systems.symbol.intent.I_Intent
 * @see systems.symbol.rdf4j.sparql.IQScriptCatalog
 * @see systems.symbol.rdf4j.sparql.SPARQLMapper
 */
public class Select implements I_Intent, I_Self {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final IQScriptCatalog catalog;
    private final IQStore iq;

    /**
     * Constructs a new SPARQL intent with the provided Connection and self
     * identity.
     *
     * @param self The self identity of the agent.
     * @param conn The RepositoryConnection of the agent.
     */
    public Select(IRI self, RepositoryConnection conn) {
        this.iq = new IQConnection(self, conn);
        this.catalog = new IQScriptCatalog(iq);

    }

    /**
     * Creates a new bindings object for script execution.
     * Executes the SPARQL query based on the provided actor and resource.
     *
     * @param actor The actor of the execution.
     * @param state The resource containing the script.
     * @param my    Bindings used in query interpolation.
     * @return A set of IRIs indicating the completion of execution.
     */
    @Override
    @RDF(IQ_NS.IQ + "select")
    public Set<IRI> execute(IRI actor, Resource state, Bindings my) throws StateException {
        Set<IRI> done = new HashSet<>();
        try {
            Bindings bindings = Facades.rebind(actor, state, my);
            String sparql = catalog.getSPARQL(state.stringValue(), bindings);
            log.info("sparql.select: {}", sparql);
            if (sparql == null || sparql.isEmpty())
                return done;
            TupleQuery prepared = iq.getConnection().prepareTupleQuery(sparql);
            List<Map<String, Object>> results = SPARQLMapper.toMaps(prepared.evaluate());
            Facades.results(bindings, results);
            log.info("sparql.results: {}", results.size());
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
