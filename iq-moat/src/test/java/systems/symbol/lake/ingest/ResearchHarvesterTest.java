package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFS;
import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.ns.COMMONS;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchHarvesterTest {

    @Test
    void testDownload() throws FileSystemException {
        Model model = new DynamicModelFactory().createEmptyModel();
        VFS vfs = new VFS();
        FileObject library = vfs.resolveFile("https://export.arxiv.org/oai2?verb=ListRecords&set=cs:AI&metadataPrefix=oai_dc");
        System.out.println("test.harvest: "+library.getPublicURIString());
        FileObject root = vfs.resolveFile("tested/harvest/");
        System.out.println("test.harvest.root: "+root.getPublicURIString());
        ResearchHarvester harvester = new ResearchHarvester(model, vfs, root);
        harvester.accept(library);
    }
}