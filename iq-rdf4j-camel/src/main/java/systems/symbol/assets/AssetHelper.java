package systems.symbol.assets;

import java.util.Map;

public class AssetHelper {
public static Asset getAsset(Asset asset, Map<String, Object> headers) {
if (asset != null) {
return asset;
}
return new Asset("", "application/octet-stream", "");
}
}
