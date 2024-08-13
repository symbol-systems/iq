package systems.symbol.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class APISecrets implements I_Secrets {
    private final Logger log = LoggerFactory.getLogger(getClass());
//    IRI agent;
    I_Secrets secrets;
    Map<String, String> grants = new HashMap<>(); // maps URL prefix to secrets

    public APISecrets(I_Secrets secrets) {
        this.secrets = secrets;
    }

    public void grant(String url, String key) {
        this.grants.put(url, key);
    }

    public String getSecret(String url) {
        if (url == null) return null;
        if (secrets==null) return null;
        String found = null;
        String name = null;
        for (String urlPrefix : grants.keySet()) {
            if (url.startsWith(urlPrefix) && (found == null || urlPrefix.length() > found.length())) {
                name = grants.get(urlPrefix);
                found = urlPrefix;
            }
            log.debug("secret.match: {} -> {} ==> {} -> {} --> {}", found != null ? found.length() : "no", urlPrefix.length(), urlPrefix, url.startsWith(urlPrefix), url);
        }
        if (name == null) {
            log.info("secret.missing: {} -> {}", found, url);
            return null;
        }
        String secret = secrets.getSecret(name);
        log.info("secret.found: {} -> {} => {}", found, name, secret==null?"NONE":secret.substring(0,2));
        return secret;
    }
}
