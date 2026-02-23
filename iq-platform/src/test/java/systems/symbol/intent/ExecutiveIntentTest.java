package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import java.util.Set;

class ExecutiveIntentTest {

ValueFactory vf = SimpleValueFactory.getInstance();
IRI nop = vf.createIRI(IQ_NS.IQ, "nop");
@Test
void testLazyNOP() throws StateException {
DynamicModelFactory dmf = new DynamicModelFactory();
DynamicModel model = dmf.createEmptyModel();
ExecutiveIntent executiveIntent = new ExecutiveIntent(nop, model);
IRI ok = executiveIntent.add(new NOP());
System.out.println("nop.ok: "+ok);
assert null != ok;
Set<IRI> lazy = executiveIntent.execute(null, nop, null);
assert null != lazy;
System.out.println("nop.lazy: 0 == "+lazy.size());
assert lazy.isEmpty();
}
}