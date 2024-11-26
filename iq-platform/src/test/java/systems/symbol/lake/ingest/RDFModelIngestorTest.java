package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.rdf4j.io.RDFDump;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.Consumer;

class RDFModelIngestorTest {
File moat = new File("tested/moat/");
File from = new File("./src/test/resources/assets/fsm.ttl");

@Test
void testFileIntoModel() throws Exception {
assert from.exists() && from.isFile();
Model model = new LinkedHashModel();

Consumer<FileObject> ingestor = new FileContentConverter(new RDFModelIngestor(model, RDFFormat.TURTLE));
VFSCrawler crawler = new VFSCrawler(ingestor);
System.out.println("model.crawl: " + from);

assert model.isEmpty();
IRI crawled = crawler.crawl(from.toURI());
assert crawled != null;

assert !model.isEmpty();
assert model.size() > 10;
System.out.println("model.crawled: " + model.size());
RDFDump.dump(model, new FileOutputStream(new File(moat, "fsm-model.ttl")), RDFFormat.TURTLE);
}
}