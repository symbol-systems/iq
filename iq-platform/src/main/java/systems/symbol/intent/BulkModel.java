package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import systems.symbol.model.HasIdentity;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class BulkModel extends RenderAbstract implements I_Intent, HasIdentity {
protected RDFFormat format = RDFFormat.TURTLE;
protected ParserConfig settings = new ParserConfig();
protected ParseErrorLogger errors = new ParseErrorLogger();

public BulkModel(IQ iq, Model model, IRI self) throws IOException {
init(iq, model, self);
}

protected void process(IRI entity, String blended) throws IOException {
log.info("perform.reformat: {} => {}", entity, format);
InputStream in = IOUtils.toInputStream(blended, StandardCharsets.UTF_8);
RDFParser parser = Rio.createParser(format, vf);
parser.setParserConfig(settings);
parser.setParseErrorListener(errors);
parser.setRDFHandler(new ContextStatementCollector(model, vf, getIdentity()));
parser.parse(in, entity.stringValue());
}

@Override
@RDF(COMMONS.IQ_NS+"bulk-model")
public Set<IRI> execute(IRI subject, Resource object) {
try {
return perform(subject, object);
} catch (IOException e) {
return null;
}
}

public IRI getIdentity() {
return SimpleValueFactory.getInstance().createIRI("urn:systems.symbol:v0:intent:bulk-facts");
}
}
