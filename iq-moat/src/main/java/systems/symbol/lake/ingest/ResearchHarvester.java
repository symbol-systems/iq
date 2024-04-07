package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFS;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.Model;

import java.util.function.Consumer;

public class ResearchHarvester extends AbstractIngestor<FileObject> {
    Consumer ingestor;
    VFS vfs;

    public ResearchHarvester(Model model, VFS vfs, FileObject root) {
        this.vfs = vfs;
//        ingestor = new FileCloneIngestor(new OAIPMHIngestor(model), root);
    }
    @Override
    public void accept(FileObject url) {
        log.info("harvest: {}", url.getURI());
        if (ingestor!=null) ingestor.accept(url);

    }
}
