package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import systems.symbol.vfs.MyVFS;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

class JsonLdLinkExtractorTest {
    private static final Logger log = LoggerFactory.getLogger(JsonLdLinkExtractor.class);
    File moat = new File("tested/moat/");
    File testHome = new File("./src/test/resources/json-ld/");

    // @Test
    void testParseInlineHead() throws Exception {
        Model model = testParse(new File(testHome, "inline-head.xhtml"));
        assert !model.isEmpty();
        assert model.size() > 10;
        RDFDump.dump(model, Files.newOutputStream(new File(moat, "inline-head.ttl").toPath()), RDFFormat.TURTLE);
    }

    // @Test
    public void testParseInlineBody() throws Exception {
        Model model = testParse(new File(testHome, "inline-body.xhtml"));
        assert !model.isEmpty();
        assert model.size() > 10;
        Path file = new File(moat, "inline-body.ttl").toPath();
        log.info("json-ld.dump.inline: {} triples @ {}", model.size(), file);
        RDFDump.dump(model, Files.newOutputStream(file), RDFFormat.TURTLE);
    }

    @Test
    public void testParseLinks() throws Exception {
        Model model = testParse(new File(testHome, "links.xhtml"));
        assert !model.isEmpty();
        assert model.size() > 10;
        Path file = new File(moat, "links.ttl").toPath();
        RDFDump.dump(model, Files.newOutputStream(file), RDFFormat.TURTLE);
        log.info("json-ld.dump.links: {} triples @ {}", model.size(), file);
    }

    public Model testParse(File from) throws Exception {
        Model model = new LinkedHashModel();
        RDFModelIngestor rdfModelIngestor = new RDFModelIngestor(model, RDFFormat.JSONLD);
        log.info("json-ld.ingest: " + model.size() + " @ " + from);

        try (FileSystemManager vfs = new MyVFS()) {
            FileContentConverter ingest = new FileContentConverter(new JsonLdLinkExtractor(vfs, rdfModelIngestor));
            System.out.printf("json-ld.ingest: %s -> {}", from, Arrays.toString(vfs.getSchemes()));

            log.info("json-ld.ingested: {} -> {}", from, Arrays.toString(vfs.getSchemes()));
            FileObject fileObject = vfs.toFileObject(from);
            ingest.accept(fileObject);
            log.info("json-ld.crawled: {} @ {}", model.size(), fileObject);

            // RDFDump.dump(model);
            assert model.size() > 20;
            return model;
        }
    }
}