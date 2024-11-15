package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Value;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HBSTest extends CustomFunctionsTest {

public HBSTest() throws IOException {
super();
}

@Test
void testEvaluateTemplate() {
CustomFunction hbs = new HBS();
Value text = hbs.evaluate(tripleSource, vf.createLiteral("Hello {{this.1}}"), vf.createLiteral("World"));
assert text != null;
assert text.stringValue().equals("Hello World");
}
}
