package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CamelCaseTest extends CustomFunctionsTest {

    public CamelCaseTest() throws IOException {
    }

    @Test
    public void testEvaluate() {
        CustomFunction fn = new CamelCase();
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        System.out.println("fn.camel: " + fn.getFunctionName());
        Value hello_world = fn.evaluate(vf, vf.createLiteral("Hello World"));
        assert hello_world.stringValue().equals("helloWorld");
    }
}