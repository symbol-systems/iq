package systems.symbol.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvsAsSecrets implements I_Secrets {
    private final Logger log = LoggerFactory.getLogger(getClass());
    public String getSecret(String key) {

        log.info("getSecret: {} @ {} ", key, System.getenv().keySet());
        if (!System.getenv().containsKey(key)) return null;
        return System.getenv(key);
    }
}
