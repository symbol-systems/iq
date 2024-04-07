package systems.symbol.rdf4j.util;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.io.AssetMimeTypes;
import org.eclipse.rdf4j.model.IRI;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.*;

public class SupportedScripts {
private final Set<String> supportedMimeTypes = new HashSet();
private AssetMimeTypes mimeTypes;

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
init();
}

private void init() {
mimeTypes = getExtension2MimeType();
}

public void supportSPARQL() {
supportedMimeTypes.add(COMMONS.MIME_SPARQL);
}

public String getMimeType(String extn) {

return mimeTypes.toMimeType(extn);
}

public static Set<String> getScriptTypes() {
Set<String> supportedMimeTypes = new HashSet<>();
ScriptEngineManager sem = new ScriptEngineManager();
List<ScriptEngineFactory> scriptEngineFactories = sem.getEngineFactories();
for (int i=0;i<scriptEngineFactories.size();i++) {
ScriptEngineFactory sef = scriptEngineFactories.get(i);

List<String> mimes = sef.getMimeTypes();
for (int m = 0; m < mimes.size(); m++) {
Object mime = mimes.get(m);
if (mime != null) {
supportedMimeTypes.add(NS.MIME_TYPE + mime);
supportedMimeTypes.add(mime.toString());
}
}
}
return supportedMimeTypes;
}

protected AssetMimeTypes getExtension2MimeType() {
AssetMimeTypes mimeTypes = new AssetMimeTypes();
ScriptEngineManager sem = new ScriptEngineManager();
List<ScriptEngineFactory> scriptEngineFactories = sem.getEngineFactories();
for (int i=0;i<scriptEngineFactories.size();i++) {
ScriptEngineFactory sef = scriptEngineFactories.get(i);

List mimes = sef.getMimeTypes();
for(int m=0;m<mimes.size();m++) {
Object mime = mimes.get(m);
if (mime != null) {
supportedMimeTypes.add(NS.MIME_TYPE+mime);
supportedMimeTypes.add(mime.toString());
}
}

if (mimes!=null && mimes.size()>0) {
List<String> extensions = sef.getExtensions();
for(int e=0; e<extensions.size();e++) {
String extn = extensions.get(e);
mimeTypes.put(extn, NS.MIME_TYPE+mimes.get(0).toString());
}
}
}
return mimeTypes;
}

public static Collection<String> getMimeTypes() {
List<String> mimes = new ArrayList<>();
ScriptEngineManager sem = new ScriptEngineManager();
List<ScriptEngineFactory> scriptEngineFactories = sem.getEngineFactories();
for (int i = 0; i < scriptEngineFactories.size(); i++) {
ScriptEngineFactory sef = scriptEngineFactories.get(i);
List<String> engine_mimes = sef.getMimeTypes();
mimes.addAll(engine_mimes);

}
return mimes;
}


public static ScriptEngine getScriptEngine(String scriptEngineType) {
ScriptEngineManager sem = new ScriptEngineManager();
if (scriptEngineType.startsWith(COMMONS.MIME_TYPE)) {
scriptEngineType = scriptEngineType.substring(COMMONS.MIME_TYPE.length());
}
ScriptEngine scriptEngine = sem.getEngineByName(scriptEngineType);
if (scriptEngine==null) {
scriptEngine = sem.getEngineByMimeType(scriptEngineType);
}
if (scriptEngine==null) {
scriptEngine = sem.getEngineByExtension(scriptEngineType);
}
return scriptEngine;
}
}
