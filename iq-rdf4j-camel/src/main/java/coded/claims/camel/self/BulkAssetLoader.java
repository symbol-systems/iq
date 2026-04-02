package systems.symbol.camel.self;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class BulkAssetLoader {
private static final Logger log = LoggerFactory.getLogger(BulkAssetLoader.class);

private final String identity;
private final RepositoryConnection connection;
private boolean deployScripts = false;
private boolean deployRDF = true;

public BulkAssetLoader(String identity, RepositoryConnection connection) {
this.identity = identity;
this.connection = connection;
}

public void setDeployScripts(boolean deployScripts) {
this.deployScripts = deployScripts;
}

public void setDeployRDF(boolean deployRDF) {
this.deployRDF = deployRDF;
}

public void deploy(File folder, File file) throws IOException {
if (file == null || !file.exists()) {
log.warn("Cannot deploy file, not found: {}", file);
return;
}
// Minimal no-op placeholder: actual bulk loader behavior is upstream/optional.
log.info("[BulkAssetLoader] pretend deploy file: {} (scripts={}, rdf={})", file.getAbsolutePath(), deployScripts, deployRDF);
}

public void deploy(URL uri) throws IOException {
if (uri == null) {
log.warn("Cannot deploy URL, uri is null");
return;
}
log.info("[BulkAssetLoader] pretend deploy resource: {} (scripts={}, rdf={})", uri, deployScripts, deployRDF);
}
}
