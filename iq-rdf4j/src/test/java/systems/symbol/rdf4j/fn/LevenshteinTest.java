package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class LevenshteinTest extends CustomFunctionsTest {

    Literal bob = Values.literal("Bob");
    Literal brad = Values.literal("Brad");
    Literal alec = Values.literal("Alec");
    Literal alice = Values.literal("Alice");

public LevenshteinTest() throws IOException {
}

@Test
public void testEvaluate() {
SailRepository repo = new SailRepository(new MemoryStore());
try (SailRepositoryConnection conn = repo.getConnection()) {
TripleSource tripleSource = new RepositoryTripleSource(repo.getConnection(), true);
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
}