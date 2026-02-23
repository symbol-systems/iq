package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static systems.symbol.COMMONS.IQ;

/**
 *
 */
public class SelfTest  {

    @Test
    public void testVersion() throws IOException {

        System.out.println("* * version:"  + IQ + " - v" +I_Self.version());

//        assert I_Self.version() != null;
//        assert I_Self.version().startsWith("0.77.");
    }

    @Test
    public void testGetSelf() {

        String my_iq = System.getenv("MY_IQ");
        assert my_iq == null || my_iq.isEmpty();
        assert I_Self.name().equals("iq");
        assert I_Self.self().getSelf().stringValue().equals("iq:");
    }

    @Test
    public void testTrust() {
        IRI correct = Values.iri("iq:hello");
        IRI wrong = Values.iri("my:oops");
        assert I_Self.trust(correct);
        assert !I_Self.trust(wrong);
    }
}