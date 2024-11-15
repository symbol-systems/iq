package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class LevenshteinTest extends CustomFunctionsTest {

Literal bob = vf.createLiteral("Bob");
Literal brad = vf.createLiteral("Brad");
Literal alec = vf.createLiteral("Alec");
Literal alice = vf.createLiteral("Alice");

public LevenshteinTest() throws IOException {
}

@Test
public void testEvaluate() {
CustomFunction fn = new Levenshtein();
Value text = fn.evaluate(tripleSource, bob, alice);
// System.out.println("levenshtein.alice: "+text);
assert text != null;
assert text.isLiteral();
assert ((Literal) text).doubleValue() == 0.0;

text = fn.evaluate(tripleSource, alec, alice);
// System.out.println("levenshtein.alec: "+text);
assert text != null;
assert text.isLiteral();
assert ((Literal) text).doubleValue() == 0.6;

text = fn.evaluate(tripleSource, bob, brad);
// System.out.println("levenshtein.brad: "+text);
assert text != null;
assert text.isLiteral();
assert ((Literal) text).doubleValue() == 0.25;

}
}