package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.function.Consumer;

class JsonLdLinkExtractorTest {
    File moat = new File("tested/moat/");
    File testHome = new File("./src/test/resources/json-ld/");

//    @Test
    void testParseInlineHead() throws Exception {
        Model model = testParse(new File(testHome, "inline-head.xhtml"));
        assert !model.isEmpty();
        assert model.size()> 10;
        RDFDump.dump(model, Files.newOutputStream(new File(moat, "inline-head.ttl").toPath()), RDFFormat.TURTLE);
    }

//    @Test
    void testParseInlineBody() throws Exception {
        Model model = testParse(new File(testHome, "inline-body.xhtml"));
        assert !model.isEmpty();
        assert model.size()> 10;
        RDFDump.dump(model, Files.newOutputStream(new File(moat, "inline-body.ttl").toPath()), RDFFormat.TURTLE);
    }
    @Test
    void testParseLinks() throws Exception {
        Model model = testParse(new File(testHome, "links.xhtml"));
        assert !model.isEmpty();
        assert model.size()> 10;
        RDFDump.dump(model, Files.newOutputStream(new File(moat, "links.ttl").toPath()), RDFFormat.TURTLE);
    }

    Model testParse(File from) throws IOException, URISyntaxException {
        Model model = new LinkedHashModel();
        RDFModelIngestor rdfModelIngestor = new RDFModelIngestor(model, RDFFormat.JSONLD);
        FileContentConverter ingestor = new FileContentConverter(new JsonLdLinkExtractor(rdfModelIngestor));
        VFSCrawler crawler = new VFSCrawler(ingestor);
        System.out.println("json-ld.crawl: "+from);

        assert model.isEmpty();
        IRI crawled = crawler.crawl(from.toURI());
        assert null != crawled;
        System.out.println("json-ld.crawled: "+crawled+" -> "+model.size());
        return model;
    }
}