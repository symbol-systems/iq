package systems.symbol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JSR233Test {


@Test
public void allEngines() {
ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
System.out.println("ScriptEngines: " + scriptEngineManager.getEngineFactories().size());
scriptEngineManager.getEngineFactories().forEach( scriptEngineFactory -> {
System.out.println("\t"+scriptEngineFactory.getLanguageName()+" v"+scriptEngineFactory.getLanguageVersion()+" -> "+ scriptEngineFactory.getExtensions());
});
assert scriptEngineManager.getEngineFactories().size()>1;
}

@Test
public void testGroovyEngineInstalled() {
ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
ScriptEngine groovyEngine = scriptEngineManager.getEngineByExtension("groovy");
assertNotNull(groovyEngine, "Groovy not installed");
}

@Test
public void testJavaScriptEngineInstalled() {
ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
ScriptEngine jsEngine = scriptEngineManager.getEngineByExtension("js");
assertNotNull(jsEngine, "JavaScript not installed");
}
}
