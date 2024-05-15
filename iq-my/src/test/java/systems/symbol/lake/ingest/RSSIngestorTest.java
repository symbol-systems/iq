package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

class RSSIngestorTest {
File testHome = new File("./src/test/resources/rss/");

@Test
void parseLocalRSS() throws Exception {
Model model = testParse(testHome);
assert !model.isEmpty();
assert model.size()> 10;
RDFDump.dump(model, System.out, RDFFormat.TURTLE);
ValueFactory vf = SimpleValueFactory.getInstance();
IRI inferredConcept = vf.createIRI("https://arstechnica.com#Space");
Literal inferredLabel = vf.createLiteral("Space");
assert model.getStatements(inferredConcept, SKOS.PREF_LABEL, inferredLabel).iterator().hasNext();
}

Model testParse(File from) throws IOException, URISyntaxException {
Model model = new LinkedHashModel();
FileContentConverter ingestor = new FileContentConverter(new RSSIngestor(model));
VFSCrawler crawler = new VFSCrawler(ingestor);
System.out.println("rss.crawl: "+from);

assert model.isEmpty();
IRI crawled = crawler.crawl(from.toURI());
assert null !=crawled;
System.out.println("rss.crawled: "+from+" -> "+crawled);
//assert from.toURI().toString().equals( crawled.stringValue());
return model;
}
}