package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.COMMONS;
import systems.symbol.fsm.StateException;

import java.util.Set;

class ExecutiveTest {

    ValueFactory vf = SimpleValueFactory.getInstance();
    IRI nop = vf.createIRI(COMMONS.IQ_NS, "nop");
    @Test
    void testLazyNOP() throws StateException {
        DynamicModelFactory dmf = new DynamicModelFactory();
        DynamicModel model = dmf.createEmptyModel();
        Executive executive = new Executive(nop, model);
        IRI ok = executive.add(new NOP());
        System.out.println("nop.ok: "+ok);
        assert null != ok;
        Set<IRI> lazy = executive.execute(null, nop, null);
        assert null != lazy;
        System.out.println("nop.lazy: 0 == "+lazy.size());
        assert lazy.isEmpty();
    }
}