package systems.symbol.camel.processor.rdf4j;

import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.iq.KBMS;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.util.HashMap;
import java.util.Map;

public class GraphQueryProcessor extends AbstractCamelRDFProcessor {
    String query;
    protected boolean inferred = true;
    protected int maxQueryTime = -1;

    public GraphQueryProcessor(Repository repository, String query) {
        super(repository);
        this.query=query;
    }

    public GraphQueryProcessor(Repository repository, boolean inferred, int maxQueryTime, String query) {
        super(repository);
        this.query=query;
        this.inferred=inferred;
        this.maxQueryTime=maxQueryTime;
    }

    public Map getMessageModel(Message in) {
        Map body = in.getBody(Map.class);
        return body == null ? new HashMap(): body;
    }

    public void process(Exchange exchange) throws RDFHandlerException, MalformedQueryException, RepositoryException, QueryResultHandlerException, QueryEvaluationException {
        log.info("iq.camel.rdf.construct.process: "+exchange.getExchangeId());
        RepositoryConnection connection = repository.getConnection();
        IRI iri = connection.getValueFactory().createIRI("urn:" + exchange.getMessage().getMessageId());
        KBMS iq = new KBMS(iri, connection);

        Map model = getMessageModel(exchange.getIn());
        SPARQLMapper SPARQLMapper = new SPARQLMapper(iq);
        String sparql = SPARQLMapper.findQuery(iq.toIRI(this.query));
        GraphQueryResult queryResult = SPARQLMapper.graph(sparql, model);

        Model results = QueryResults.asModel(queryResult);
        exchange.getIn().setHeader("Content-Type", TupleQueryResultFormat.BINARY);
        exchange.getIn().setBody(results);
    }
}
