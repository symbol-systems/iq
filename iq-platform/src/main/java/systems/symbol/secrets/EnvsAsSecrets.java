package systems.symbol.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvsAsSecrets implements I_Secrets {
private final Logger log = LoggerFactory.getLogger(getClass());
public String getSecret(String key) {
String v = System.getenv(key);
log.debug("env.getSecret: {} == {}", key, v);
return v;
}
}
