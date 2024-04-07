package systems.symbol.agent.apis;

import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.secrets.SecretsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class APIs<T extends I_API> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    String agent = "default";
    I_Secrets secrets;
    Map<String, String> urlToSecretName = new HashMap<>();

    public APIs() {
        this.secrets = new EnvsAsSecrets();
    }

    public APIs(String agent, I_Secrets secrets) {
        this.agent = agent;
        this.secrets = secrets;
    }

    public APIs withAgent(String agent) {
        this.agent = agent;
        return this;
    }

    public APIs withPermission(String urlPrefix, String secretName) {
        this.urlToSecretName.put(urlPrefix, secretName);
        secrets.grant(secretName, agent);
        return this;
    }

    public String findSecret(String url) throws SecretsException {
        String found = null;
        String name = null;
        for (String urlPrefix : urlToSecretName.keySet()) {
            boolean matched = url.startsWith(urlPrefix) && (found == null || found.length() < urlPrefix.length());
            log.info("findSecret.match: {} -> {} ==> {} -> {} --> {}", found!=null?found.length():"no" , urlPrefix.length(), urlPrefix, matched, url);
            if (matched) {
                name = urlToSecretName.get(urlPrefix);
                found = urlPrefix;
            }
        }

        log.info("findSecret.found: {} -> {} in {}", found, name, secrets);
        if (name == null) return null;
        return secrets.getSecret(name, agent);
    }
    /**
     *     Check the URL matches the Swagger-style pattern (`/v1/example{param1}/{param2}`)
     */
    private boolean isMatch(String pattern, String url) {
        String regexPattern = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return url.matches(regexPattern);
    }



    public T get(String url) throws SecretsException {
        String secret = findSecret(url);
        RestAPI api = new RestAPI( url, secret);
        return (T)api;
    }

    public void setSecret(String iqDev, String hello) {
        secrets.setSecret(iqDev, hello);
    }
}
