package systems.symbol.assets;

import java.io.IOException;

public interface AssetRegister {
Asset getAsset(String uri, String type) throws IOException;
}
