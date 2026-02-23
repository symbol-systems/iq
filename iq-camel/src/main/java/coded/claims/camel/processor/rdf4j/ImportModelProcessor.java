package systems.symbol.camel.processor.rdf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ImportModelProcessor implements Processor {
    protected static final Logger log = LoggerFactory.getLogger(ImportModelProcessor.class);
    Repository repository;

    public ImportModelProcessor(Repository repository) {
        this.repository = repository;
    }

    public void process(Exchange exchange) throws RDFHandlerException, MalformedQueryException, RepositoryException, IOException, QueryResultHandlerException, QueryEvaluationException {
        Message in = exchange.getIn();
        Model model = in.getBody(Model.class);
        log.info("camel.rdf4j.import: "+ +model.size());
        RepositoryConnection connection = repository.getConnection();
        connection.add(model);
        connection.close();
        in.setBody(null);
    }
}
