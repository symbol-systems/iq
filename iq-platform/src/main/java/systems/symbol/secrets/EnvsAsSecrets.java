package systems.symbol.secrets;


import java.util.HashMap;
import java.util.Map;

public class EnvsAsSecrets extends SimpleSecrets {

public String getSecret(String key, String agent) throws SecretsException {
String secret = super.getSecret(key, agent);
if (secret!=null) return secret;
return System.getenv(key);
}
}
