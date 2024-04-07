package systems.symbol.intent;

import systems.symbol.fsm.StateException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

class PerformerTest {

    ValueFactory vf = SimpleValueFactory.getInstance();
    IRI nop = vf.createIRI("urn:systems.symbol:v0:agent:task.perform:nop");
    @Test
    void testLazyNOP() throws StateException {
        DynamicModelFactory dmf = new DynamicModelFactory();
        DynamicModel model = dmf.createEmptyModel();
        Performer performer = new Performer(model, nop);
        IRI ok = performer.add(new NOP());
        System.out.println("agent.cac.performer.nop: "+ok);
        assert null != ok;
        Set<IRI> lazy = performer.execute(null, nop, null);
        assert null != lazy;
        System.out.println("agent.cac.performer.lazy: 0 == "+lazy.size());
        assert lazy.isEmpty();
    }
}