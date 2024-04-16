package systems.symbol.secrets;


import org.eclipse.rdf4j.model.IRI;

import java.util.HashMap;
import java.util.Map;

public class EnvsAsSecrets extends SimpleSecrets {

public String getSecret(String key, IRI agent) throws SecretsException {
String secret = super.getSecret(key);
if (secret!=null) return secret;
return System.getenv(key);
}
}
