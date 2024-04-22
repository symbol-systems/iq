/**
 * ScriptCatalog provides methods for managing SPARQL queries and templates.
 * It includes functionality to retrieve SPARQL queries from the knowledge base,
 * apply Handlebars templates, and perform query lookups (scripts are scoped to MIME type and context).
 */
package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.COMMONS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.render.HBSRenderer;

import java.io.IOException;
import java.util.Map;


public class IQScriptCatalog implements I_Contents {
    private static final Logger log = LoggerFactory.getLogger(IQScriptCatalog.class);
    public static IRI HAS_CONTENT = RDF.VALUE;
    public static IRI SPARQL_MIME = IQ.vf.createIRI("urn:"+COMMONS.MIME_SPARQL);
    IQ iq;

    /**
     * Constructs a ScriptCatalog instance.
     *
     * @param iq The IQ instance representing the knowledge base connection.
     */
    public IQScriptCatalog(IQ iq) {
        this.iq = iq;
    }

    public IQScriptCatalog(IRI self, RepositoryConnection connection) {
        this.iq = new IQConnection(self, connection);
    }
    /**
     * Retrieves a SPARQL query based on the provided query path and MIME type.
     *
     * @param queryPath The path or IRI of the SPARQL query.
     * @return The SPARQL query as a string.
     */
    public String getSPARQL(String queryPath) {
        Literal content;
        if (!queryPath.contains(":")) content= getContent(iq.toIRI(queryPath), SPARQL_MIME);
        else content = getContent(IQ.vf.createIRI(queryPath), SPARQL_MIME);
        return content==null?null:content.stringValue();
    }

    /**
     * Retrieves a SPARQL query (template) and injects the bindings.
     *
     * @param queryPath The path or IRI of the SPARQL query (template).
     * @param bindings  The template bindings.
     * @return The SPARQL query with bindings interpolated.
     * @throws IOException If an error occurs during template rendering.
     */
    public String getSPARQL(String queryPath, Map<String, Object> bindings) throws IOException {
        String query = getSPARQL(queryPath);
        return query==null||bindings==null?query:HBSRenderer.template(query, bindings);
    }

    /**
     * Retrieves a SPARQL query based on the provided IRI and MIME type.
     *
     * @param query The IRI representing the SPARQL query.
     * @return The SPARQL query as a string.
     */
    public String getSPARQL(IRI query) {
        Literal content = getContent(query, SPARQL_MIME);
        return content==null?null:content.stringValue();
    }

    /**
     * Retrieves a script based on the provided IRI, MIME type, and context.
     *
     * @param query    The IRI representing the SPARQL query.
     * @param mimetype The MIME type associated with the query.
     * @return The SPARQL query as a string.
     */
    @Override
    public Literal getContent(Resource query, IRI mimetype) {
        return IQScripts.findScript(iq.getConnection(), query, mimetype, iq.getSelf());
    }

}
