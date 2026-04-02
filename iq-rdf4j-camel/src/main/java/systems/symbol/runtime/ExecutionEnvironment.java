package systems.symbol.runtime;

import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.assets.AssetRegister;
import java.util.Map;

public class ExecutionEnvironment {
private final Repository repository;
private final AssetRegister assetRegister;
private final String identity;
private final Map<String, Object> config;

public ExecutionEnvironment(Repository repository, AssetRegister assetRegister, String identity, Map<String, Object> config) {
this.repository = repository;
this.assetRegister = assetRegister;
this.identity = identity;
this.config = config;
}

public Repository getRepository() {
return repository;
}

public AssetRegister getAssetRegister() {
return assetRegister;
}

public String getIdentity() {
return identity;
}

public Map<String, Object> getConfig() {
return config;
}
}
