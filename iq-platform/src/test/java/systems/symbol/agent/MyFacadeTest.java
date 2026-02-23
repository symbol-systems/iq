package systems.symbol.agent;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.util.UsefulSPARQL;

public class MyFacadeTest {
IRI self = Values.iri(IQ_NS.TEST);

@Test
public void testFacade() {
SimpleBindings params = new SimpleBindings();
params.put("self", "oops");
Bindings my = Facades.rebind(self, params);
assert my.containsKey("my");
assert my.get("my") instanceof Bindings;
Bindings test = (Bindings) my.get("my");
assert test.containsKey("self");
assert test.get("self") != null;
assert test.get("self").toString().equals(self.stringValue());

String sparql = Facades.template(UsefulSPARQL.SELF, my);
assert sparql != null;
assert !sparql.contains("oops");
assert sparql.contains(self.stringValue());
assert sparql.contains("BIND(<" + IQ_NS.TEST + ">");
System.out.println("facade.tested: " + sparql);
}
}
