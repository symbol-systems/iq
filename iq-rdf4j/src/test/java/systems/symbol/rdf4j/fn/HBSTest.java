package systems.symbol.rdf4j.fn;

import org.apache.maven.model.Repository;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HBSTest extends CustomFunctionsTest {

public HBSTest() throws IOException {
super();
}

@Test
void testEvaluateTemplate() {
CustomFunction hbs = new HBS();
SimpleValueFactory vf = SimpleValueFactory.getInstance();
SailRepository repo = new SailRepository(new MemoryStore());
try (SailRepositoryConnection conn = repo.getConnection()) {
TripleSource tripleSource = new RepositoryTripleSource(repo.getConnection(), true);
Value text = hbs.evaluate(tripleSource, vf.createLiteral("Hello {{this.1}}"), vf.createLiteral("World"));
assert text != null;
assert text.stringValue().equals("Hello World");

}
}
}
