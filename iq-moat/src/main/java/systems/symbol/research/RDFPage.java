package systems.symbol.research;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.RDF;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.ingest.*;
import systems.symbol.COMMONS;

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
log.info("init: {} --> {}", self, format);
}
@Override
@RDF(COMMONS.IQ_NS+"rdf")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
Set<IRI> done = new HashSet<>();
log.info("rdf: {} @ {}", state, actor);
try {
generated(model, actor, getSelf(), state, getSelf());
processor.accept(new ContentEntity<String>((IRI) state, null));
} catch (Exception e) {
throw new RuntimeException(e);
}
return done;
}
}
