package systems.symbol.rdf4j;

import systems.symbol.rdf4j.store.AbstractTripleTest;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.testng.annotations.Test;

import java.util.Set;

public class IRIsTest extends AbstractTripleTest  {

    @Test
    public void testForLabels() {
        IRIs labels = IRIs.forLabels();
        assert labels.contains(SKOS.PREF_LABEL);

    }

    @Test
    public void testForLinks() {
        IRIs inScheme = IRIs.forLinks();
        assert inScheme.contains(SKOS.IN_SCHEME);
    }

    @Test
    public void testISA() {
        assert IRIs.A().contains(RDF.TYPE);
        IRIs isa = IRIs.ISA();
        assert isa.contains(RDFS.SUBCLASSOF);
    }
}