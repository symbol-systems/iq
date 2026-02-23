package systems.symbol.camel.processor.nlp;

import systems.symbol.nlp.NLP;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.rdf4j.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NLPProcessor implements Processor {
protected static final Logger log = LoggerFactory.getLogger(NLPProcessor.class);
protected NLP nlp;

public NLPProcessor() throws IOException {
nlp = new NLP();
}

@Override
public void process(Exchange exchange) {
String body = exchange.getIn().getBody(String.class);
log.info("iq.camel.nlp.body: "+body);
Model model = nlp.parse(body);
exchange.getIn().setBody(model);
}
}
