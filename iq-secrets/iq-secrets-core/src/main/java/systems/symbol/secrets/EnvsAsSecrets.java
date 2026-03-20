package systems.symbol.secrets;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvsAsSecrets implements I_Secrets {
private final Logger log = LoggerFactory.getLogger(getClass());
String prefix = "MY_";

public EnvsAsSecrets(String prefix) {
this.prefix = prefix;
log.debug("EnvsAsSecrets: {}", prefix);
}

public EnvsAsSecrets() {
log.debug("EnvsAsSecrets");
}

// load from .env file if it exists
public EnvsAsSecrets(File file) {
load(file);
}

public String getSecret(String key) {
String v = System.getenv(this.prefix + key);
log.debug("env.getSecret: {}{} == {}", this.prefix, key, v);
return v;
}

public void load(File file) {
log.debug("Loading secrets from file: {}", file.getAbsolutePath());
if (file.exists()) {
log.debug(".env file found, manually loading & parsing environment variables");
try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
reader.lines()
.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
.forEach(line -> {
int idx = line.indexOf('=');
if (idx > 0) {
String key = line.substring(0, idx).trim();
String value = line.substring(idx + 1).trim();
// set as system property (note: this won't affect System.getenv(), but can be used for testing)
System.setProperty(this.prefix + key, value);
log.debug("Loaded secret from .env: {}{} == {}", this.prefix, key, value);
}
});
} catch (Exception e) {
log.error("Error loading .env file: {}", e.getMessage(), e);
}
} else {
log.warn(".env file not found at path: {}", file.getAbsolutePath());
}
}
public Map<String, String> getAllSecrets() {
Map<String, String> all = new HashMap<>();
for (Map.Entry<String, String> env : System.getenv().entrySet()) {
if (env.getKey().startsWith(this.prefix)) {
all.put(env.getKey().substring(this.prefix.length()), env.getValue());
}
}
return Collections.unmodifiableMap(all);
}

}

