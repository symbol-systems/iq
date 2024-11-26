package systems.symbol.rdf4j.util;

import com.google.common.io.Files;
import systems.symbol.COMMONS;
import systems.symbol.rdf4j.io.AssetMimeTypes;
import org.eclipse.rdf4j.model.IRI;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.*;

public class SupportedScripts {
private final Set<String> supportedMimeTypes = new HashSet<>();

public boolean supports(String mime) {
return supportedMimeTypes.contains(mime);
}

public Set<String> getTypes() {
return supportedMimeTypes;
}

public boolean supports(IRI mime) {
return supportedMimeTypes.contains(mime.stringValue());
}

public SupportedScripts() {
}

public void supportSPARQL() {
supportedMimeTypes.add(COMMONS.MIME_SPARQL);
}

public static Map<String, String> getScriptMimeTypes() {
Map<String, String> mimeTypes = new HashMap<>(AssetMimeTypes.types);
ScriptEngineManager sem = new ScriptEngineManager();
List<ScriptEngineFactory> scriptEngineFactories = sem.getEngineFactories();
for (ScriptEngineFactory sef : scriptEngineFactories) {
List<String> mimes = sef.getMimeTypes();
if (!mimes.isEmpty()) {
List<String> extensions = sef.getExtensions();
for (String extn : extensions) {
mimeTypes.put(extn, mimes.get(0));
}
}
}
return mimeTypes;
}

public static String toMimeType(String file) {
String ext = Files.getFileExtension(file);
return getScriptMimeTypes().get(ext);
}

public static String toMimeType(IRI datatype) {
if (datatype == null)
return null;
String value = datatype.stringValue();
if (value.contains("/") && value.startsWith("urn:"))
return value.substring(4);
return null;
}

public static Collection<String> getMimeTypes() {
List<String> mimes = new ArrayList<>();
ScriptEngineManager sem = new ScriptEngineManager();
List<ScriptEngineFactory> scriptEngineFactories = sem.getEngineFactories();
for (ScriptEngineFactory sef : scriptEngineFactories) {
List<String> engine_mimes = sef.getMimeTypes();
mimes.addAll(engine_mimes);

}
return mimes;
}
}
