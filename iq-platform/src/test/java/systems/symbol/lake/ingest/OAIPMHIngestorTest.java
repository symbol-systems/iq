package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;

class OAIPMHIngestorTest {
    File testHome = new File("./src/test/resources/oai/");
    File moat = new File("tested/moat/");

    @Test
    void processSingleOAIRecord() throws Exception {
        File file = new File(testHome, "oai2-single-record.xml");
        assert file.exists() && file.isFile();
        Model model = new LinkedHashModel();
        OAIPMHIngestor ingestor = new OAIPMHIngestor(model);
        VFSCrawler crawler = new VFSCrawler(ingestor);
        System.out.println("oai.crawl: "+testHome);
        assert model.isEmpty();
        IRI crawled = crawler.crawl(file.toURI());
        assert !model.isEmpty();
        System.out.println("oai.crawled: "+crawled+" x "+model.size());
        assert model.size() > 15;
        RDFDump.dump(model, System.out, RDFFormat.TURTLE);
        RDFDump.dump(model, new FileOutputStream(new File(moat, "oai2-single-record.ttl")), RDFFormat.TURTLE);
    }
}