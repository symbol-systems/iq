package systems.symbol.research;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.RDF;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.ingest.RDFModelIngestor;
import systems.symbol.lake.ingest.URLIngestor;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static systems.symbol.platform.Provenance.generated;

public class RDFPage extends AbstractIntent {

    protected Consumer<ContentEntity<String>> processor;
    protected Consumer<ContentEntity<String>> ingestRDF;
    public RDFPage(IRI self, Model model) {
        this(self, model, RDFFormat.TURTLE);
    }

    public RDFPage(IRI self, Model model, RDFFormat format) {
        boot(self, model);
        ingestRDF = new RDFModelIngestor(model, format);
        processor = new URLIngestor(ingestRDF);
        log.info("page.init: {} --> {}", self, format);
    }
    @Override
    @RDF(IQ_NS.IQ+"rdf")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
        Set<IRI> done = new HashSet<>();
        log.info("page.rdf: {} => {} @ {}", getSelf(), state, actor);
        try {
            processor.accept(new ContentEntity<String>((IRI) state, null, null));
            generated(model, actor, getSelf(), state, getSelf());
        } catch (Exception e) {
            log.error("page.error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return done;
    }
}
