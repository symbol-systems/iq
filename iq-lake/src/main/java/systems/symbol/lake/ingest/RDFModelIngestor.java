package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.openjdk.nashorn.internal.scripts.JD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
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
defaults();
}

public RDFModelIngestor(Model model, RDFFormat fallback) {
this.model = model;
this.fallback = fallback;
defaults();
}

public void defaults() {
config.set(JSONLDSettings.WHITELIST, Set.of("http://www.w3.org/ns/anno.jsonld",
"http://www.w3.org/ns/activitystreams.jsonld", "http://www.w3.org/ns/ldp.jsonld",
"http://www.w3.org/ns/oa.jsonld", "http://www.w3.org/ns/hydra/context.jsonld",
"http://schema.org/", "https://w3id.org/security/v1", "https://w3c.github.io/json-ld-rc/context.jsonld",
"https://www.w3.org/2018/credentials/v1", "https://health-lifesci.schema.org/",
"https://auto.schema.org/", "https://bib.schema.org/", "http://xmlns.com/foaf/spec/index.jsonld",
"https://pending.schema.org/", "https://schema.org/", "https://schema.org/docs/jsonldcontext.jsonld",
"https://schema.org/version/latest/schemaorg-current-https.jsonld",
"https://schema.org/version/latest/schemaorg-all-http.jsonld",
"https://schema.org/version/latest/schemaorg-all-https.jsonld",
"https://schema.org/version/latest/schemaorg-current-http.jsonld",
"https://schema.org/version/latest/schemaorg-all.jsonld",
"https://schema.org/version/latest/schemaorg-current.jsonld",
"https://project-open-data.cio.gov/v1.1/schema/catalog.jsonld",
"https://geojson.org/geojson-ld/geojson-context.jsonld", "https://www.w3.org/2019/wot/td/v1"));
}

public void parse(ContentEntity<String> rdf) throws IOException {

RDFFormat format = Rio.getParserFormatForMIMEType(rdf.getContentType()).orElse(fallback);
log.debug("rdf.model.parse: {} -> {} ==> {} ==> {}", rdf.getSelf(), rdf.getContent(), rdf.getContentType(),
format);

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
if (!matcher.find())
return msg;
return matcher.group(2);
}

@Override
public void accept(ContentEntity<String> content) {
try {
parse(content);
next(content);
} catch (IOException e) {
log.error("parse.failed: {} -> {}", content.getSelf(), content.getContent(), e);
throw new RuntimeException(e);
}
}
}
