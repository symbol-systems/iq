package systems.symbol.platform;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.Pokeable;

import static org.junit.jupiter.api.Assertions.*;

public class PokeTest {

static class GoodConfig {
@Pokeable
public String name;
}

static class BadConfig {
public String name;
}

@Test
public void testPokeInjectsAnnotatedField() throws Exception {
SimpleValueFactory vf = SimpleValueFactory.getInstance();
Resource subject = vf.createIRI("http://example.org/config");
IRI predicate = vf.createIRI("http://example.org/name");
Literal object = vf.createLiteral("gpt-4");

Model model = new LinkedHashModel();
model.add(subject, predicate, object);

GoodConfig config = new GoodConfig();
Poke.poke(subject, model, config);

assertEquals("gpt-4", config.name);
}

@Test
public void testPokeThrowsForUnannotatedField() {
SimpleValueFactory vf = SimpleValueFactory.getInstance();
Resource subject = vf.createIRI("http://example.org/config");
IRI predicate = vf.createIRI("http://example.org/name");
Literal object = vf.createLiteral("gpt-4");

Model model = new LinkedHashModel();
model.add(subject, predicate, object);

BadConfig config = new BadConfig();
assertThrows(Poke.PokeException.class, () -> Poke.poke(subject, model, config));
}
}
