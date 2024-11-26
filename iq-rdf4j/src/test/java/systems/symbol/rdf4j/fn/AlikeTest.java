package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class AlikeTest extends CustomFunctionsTest {

Literal bob = Values.***REMOVED***("Bob");
Literal alec = Values.***REMOVED***("Alec");
Literal alice = Values.***REMOVED***("Alice");

public AlikeTest() throws IOException {
}

@Test
public void testEvaluate() {
CustomFunction alike = new Alike();
assert alike.getFunctionName().equals("alike");

SailRepository repo = new SailRepository(new MemoryStore());
try (SailRepositoryConnection conn = repo.getConnection()) {
TripleSource tripleSource = new RepositoryTripleSource(repo.getConnection(), true);
Value text = alike.evaluate(tripleSource, bob, alice);
System.out.println("alike: " + text);
assert text != null;
assert text.isLiteral();
assert ((Literal) text).booleanValue() == false;

text = alike.evaluate(tripleSource, alec, alice);
System.out.println("alike: " + text);
assert text != null;
assert text.isLiteral();
assert ((Literal) text).booleanValue() == true;
}

}
}