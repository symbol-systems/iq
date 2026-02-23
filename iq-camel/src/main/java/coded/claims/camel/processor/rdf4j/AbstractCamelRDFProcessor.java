package systems.symbol.camel.processor.rdf4j;

import org.apache.camel.Processor;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCamelRDFProcessor implements Processor {
    protected static final Logger log = LoggerFactory.getLogger(AbstractCamelRDFProcessor.class);
    protected Repository repository;

    AbstractCamelRDFProcessor(Repository repository) {
        this.repository=repository;
    }

}
