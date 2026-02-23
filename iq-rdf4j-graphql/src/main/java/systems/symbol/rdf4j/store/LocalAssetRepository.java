package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Minimal LocalAssetRepository stub used by tests: wraps an in-memory Sail repository and
 * exposes a simple load(File, base) API that loads all RDF files found under the folder.
 */
public class LocalAssetRepository extends SailRepository implements Repository {
    public LocalAssetRepository() {
        super(new MemoryStore());
    }

    public IRI load(File assetsFolder, String base) throws java.io.IOException {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) return SimpleValueFactory.getInstance().createIRI(base+"/assets/"+UUID.randomUUID());
        File[] files = assetsFolder.listFiles();
        if (files==null || files.length==0) return SimpleValueFactory.getInstance().createIRI(base+"/assets/"+UUID.randomUUID());
        String ctx = base+"/assets/"+UUID.randomUUID();
        for (File f: files) {
            if (f.isFile()) {
                try (InputStream in = new FileInputStream(f)) {
                    RDFFormat fmt = Rio.getParserFormatForFileName(f.getName()).orElse(RDFFormat.TURTLE);
                    this.init();
                    try (var conn = this.getConnection()) {
                        conn.add(in, ctx, fmt);
                    }
                }
            }
        }
        return SimpleValueFactory.getInstance().createIRI(ctx);
    }
}