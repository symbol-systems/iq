package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.Model;

import java.util.function.Consumer;

public class ResearchHarvester extends AbstractIngestor<FileObject> {
Consumer<FileObject> ingestor;

public ResearchHarvester(Model model, FileObject root) {
}

@Override
public void accept(FileObject url) {
log.info("harvest: {}", url.getURI());
if (ingestor != null)
ingestor.accept(url);

}
}
