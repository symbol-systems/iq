package systems.symbol.iq.exec;

import systems.symbol.assets.Asset;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Scripting {
public Future<Object> execute(Asset asset, Map<String, Object> headers) {
return CompletableFuture.completedFuture(asset != null ? asset.toString() : null);
}
}
