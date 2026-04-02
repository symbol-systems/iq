package systems.symbol.assets;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import java.io.IOException;

public class SesameAssetRegister implements AssetRegister {
private final RepositoryConnection connection;

public SesameAssetRegister(RepositoryConnection connection) {
this.connection = connection;
}

@Override
public Asset getAsset(String uri, String type) throws IOException {
return new Asset(uri, type == null ? "application/octet-stream" : type, "");
}
}
