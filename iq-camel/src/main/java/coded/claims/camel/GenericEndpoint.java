package systems.symbol.camel;


import org.apache.camel.*;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.DefaultProducer;

public class GenericEndpoint extends DefaultEndpoint {
    Processor processor;
    String uri;

    public GenericEndpoint(String uri, Processor processor) {
        assert processor!=null;
        this.uri = uri;
        this.processor=processor;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new DefaultProducer(this) {
            @Override
            public void process(Exchange exchange) throws Exception {
                if (processor!=null) processor.process(exchange);
            }
        };
    }

    protected String createEndpointUri() {
        return uri;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new DefaultConsumer(this, processor);
    }
}
