package systems.symbol.camel.processor.fsm;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.jeasy.states.api.FiniteStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FSMAgentProcessor implements Processor {
    protected static final Logger log = LoggerFactory.getLogger(FSMAgentProcessor.class);
    FiniteStateMachine fsm;

    public FSMAgentProcessor(FiniteStateMachine fsm) {
        this.fsm = fsm;
    }

    public void process(Exchange exchange) throws RDFHandlerException, MalformedQueryException, RepositoryException, IOException, QueryResultHandlerException, QueryEvaluationException {
        Message in = exchange.getIn();
        log.info("iq.camel.fsm.process: "+ in.getMessageId()+" -> "+in.getBody());
        in.setBody("ok");
    }
}
