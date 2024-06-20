package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

public class RDFModelIngestor extends AbstractIngestor<ContentEntity<String>> {
private static final Logger log = LoggerFactory.getLogger(RDFModelIngestor.class);
private final ParserConfig config = new ParserConfig();
RDFFormat fallback;
Model model;
Pattern extractBody = Pattern.compile("```(\\w+\n)\\s*(.*?)\n```", Pattern.DOTALL);

public RDFModelIngestor(Model model, RDFFormat fallback, Consumer<ContentEntity<String>> next) {
super(next);
this.model = model;
this.fallback = fallback;
}

public RDFModelIngestor(Model model, RDFFormat fallback) {
this.model = model;
this.fallback = fallback;
}

public void parse(ContentEntity<String> rdf) throws IOException {

RDFFormat format = Rio.getParserFormatForMIMEType(rdf.getContentType()).orElse(fallback);
log.debug("rdf.model.parse: {} -> {} ==> {} ==> {}", rdf.getSelf(), rdf.getContent(), rdf.getContentType(), format);

RDFParser rdfParser = Rio.createParser(format);
rdfParser.setParserConfig(config);
rdfParser.setRDFHandler(new StatementCollector(model));

String content = hackItToWork(rdf.getContent());
StringReader reader = new StringReader(content);
rdfParser.parse(reader, rdf.getSelf().stringValue());
}

// remove ```format``` from payload
String hackItToWork(String msg) {
Matcher matcher = extractBody.matcher(msg);
if (!matcher.find()) return msg;
return matcher.group(2);
}

@Override
public void accept(ContentEntity<String> content) {
try {
parse(content);
next(content);
} catch (IOException e) {
log.error("parse.failed: {} -> {}",content.getSelf(), content.getContent(), e);
throw new RuntimeException(e);
}
}
}
