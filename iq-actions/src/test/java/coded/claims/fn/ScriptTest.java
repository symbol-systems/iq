package systems.symbol.fn;

import systems.symbol.iq.AbstractIQTest;
import systems.symbol.rdf4j.util.FakeReturn;
import systems.symbol.rdf4j.util.SupportedScripts;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.testng.annotations.Test;

import java.util.Collection;

public class ScriptTest extends AbstractIQTest {

    @Test
    public void testScriptEngines() {
        Collection<String> supported = SupportedScripts.getMimeTypes();
        System.out.println("iq.fn.script.mimeTypes: " + supported);
        assert supported.contains("application/x-groovy");
    }
    @Test
    public void testGroovyScriptLiteral() {
        Script script = new Script();

        Literal groovy = vf.createLiteral("return \"Hello ${args[1]}\"", "groovy");
        Value text = script.evaluate(triples, groovy, vf.createLiteral("World"));
        assert text != null;
        assert text.stringValue().equals("Hello World");
    }
}