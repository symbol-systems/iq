package systems.symbol.secrets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface I_Secrets {
   String getSecret(String key) throws SecretsException;
}
