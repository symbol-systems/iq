package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.testng.annotations.Test;

import java.io.IOException;

public class AlikeTest extends CustomFunctionsTest{

Literal bob = vf.createLiteral("Bob");
Literal alec = vf.createLiteral("Alec");
Literal alice = vf.createLiteral("Alice");

public AlikeTest() throws IOException {
}

@Test
public void testEvaluate() {
CustomFunction alike = new Alike();
assert alike.getFunctionName().equals("alike");
Value text = alike.evaluate( tripleSource, bob, alice);
System.out.println("alike: "+text);
assert text != null;
assert text.isLiteral();
assert ((Literal)text).booleanValue() == false;

text = alike.evaluate( tripleSource, alec, alice);
System.out.println("alike: "+text);
assert text != null;
assert text.isLiteral();
assert ((Literal)text).booleanValue() == true;

}
}