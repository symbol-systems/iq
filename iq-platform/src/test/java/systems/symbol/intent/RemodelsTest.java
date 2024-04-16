package systems.symbol.intent;

import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.agent.MyFacade;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.io.RDFDump;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RemodelsTest extends AbstractIntentTest {
DynamicModelFactory dmf = new DynamicModelFactory();

public static List<Map<String,Object>> fakeResults() {
List<Map<String, Object>> results = new ArrayList<>();
Bindings row = new SimpleBindings();
row.put("id", "urn:iq:test:hello.world");
row.put("label", "Hello World");
results.add(row);
return results;
}

@Test
void testRemodel() throws Exception {
try (RepositoryConnection connection = bootstrap.getConnection()) {
Model model = dmf.createEmptyModel();
System.out.println("remodel.dump");

RDFDump.dump(connection, System.out, RDFFormat.TURTLE, self);

Remodel remodel = new Remodel(connection, model, self);

IRI template = vf.createIRI(self.stringValue(), "hbs/render_model");
System.out.println("remodel.template: "+template);

SimpleBindings my = new SimpleBindings();
my.put(MyFacade.RESULTS, fakeResults());

Set<IRI> done = remodel.execute(self, template, my);
assert done != null;
System.out.println("remodel.done: "+done+" x"+done.size()+"/"+remodel.model.size());
assert done.size()==2;
assert remodel.getModel().size() == 4;
}
}
}