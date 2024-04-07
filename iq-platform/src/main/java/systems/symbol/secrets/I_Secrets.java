package systems.symbol.secrets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface I_Secrets {

public void setSecret(String key, String secret);
public String getSecret(String key, String agent) throws SecretsException;
public void grant(String key, String agent);
public void revoke(String key, String agent);
public boolean granted(String key, String agent);

}
