package systems.symbol.secrets;

import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.I_API;
import systems.symbol.agent.tools.RestAPI;

import java.util.HashMap;
import java.util.Map;

public class APISecrets implements I_Secrets {
    private final Logger log = LoggerFactory.getLogger(getClass());
//    IRI agent;
    I_Secrets secrets;
    Map<String, String> grants = new HashMap<>(); // maps URL prefix to secrets

    public APISecrets() {
        this.secrets = new SimpleSecrets();
    }

    public APISecrets(I_Secrets secrets) {
        this.secrets = secrets;
    }

    public void grant(String url, String key) {
        this.grants.put(url, key);
    }

    public String getSecret(String url) throws SecretsException {
        String found = null;
        String name = null;
        for (String urlPrefix : grants.keySet()) {
            if (url.startsWith(urlPrefix) && (found == null || urlPrefix.length() > found.length())) {
                name = grants.get(urlPrefix);
                found = urlPrefix;
            }
            log.info("secret.match: {} -> {} ==> {} -> {} --> {}", found != null ? found.length() : "no", urlPrefix.length(), urlPrefix, url.startsWith(urlPrefix), url);
        }

        log.info("secret.found: {} -> {} in {}", found, name, secrets.getSecret(name));
        if (name == null) return null;
        return secrets.getSecret(name);
    }
    /**
     *     Check the URL matches the Swagger-style pattern (`/v1/example{param1}/{param2}`)
     */
    private boolean isMatch(String pattern, String url) {
        String regexPattern = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return url.matches(regexPattern);
    }


    public I_API<Response> getAPI(String url) throws SecretsException {
        String secret = getSecret(url);
        return new RestAPI( url, secret);
    }
}
