package systems.symbol.lake.ingest;

import systems.symbol.vfs.MyVFS;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.junit.jupiter.api.Test;

class ResearchHarvesterTest {

@Test
void testDownload() throws FileSystemException {
Model model = new DynamicModelFactory().createEmptyModel();
try (FileSystemManager vfs = new MyVFS()) {
FileObject library = vfs
.resolveFile("https://export.arxiv.org/oai2?verb=ListRecords&set=cs:AI&metadataPrefix=oai_dc");
System.out.println("test.harvest: " + library.getPublicURIString());
FileObject root = vfs.resolveFile("tested/harvest/");
System.out.println("test.harvest.root: " + root.getPublicURIString());
ResearchHarvester harvester = new ResearchHarvester(model, root);
harvester.accept(library);
}
}
}