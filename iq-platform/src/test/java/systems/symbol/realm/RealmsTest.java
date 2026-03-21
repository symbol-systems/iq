package systems.symbol.realm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.IRIs;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RealmsTest {

@Test
public void directTrustShouldBeDetected() {
Model model = new DynamicModelFactory().createEmptyModel();
IRI alice = Values.iri("iq:alice");
IRI bob = Values.iri("iq:bob");

model.add(alice, IQ_NS.TRUSTS, bob);

assertTrue(Realms.trusts(model, alice, bob));
assertFalse(Realms.trusts(model, bob, alice));
}

@Test
public void transitiveTrustShouldBeIncludedWhenRecurseTrue() {
Model model = new DynamicModelFactory().createEmptyModel();
IRI alice = Values.iri("iq:alice");
IRI bob = Values.iri("iq:bob");
IRI charlie = Values.iri("iq:charlie");

model.add(alice, IQ_NS.TRUSTS, bob);
model.add(bob, IQ_NS.TRUSTS, charlie);

Set<String> trusted = new HashSet<>();
Realms.trusts(model, alice, new IRIs(), true).forEach(i -> trusted.add(i.stringValue()));

assertTrue(trusted.contains(bob.stringValue()), "should include direct trust target");
assertTrue(trusted.contains(charlie.stringValue()), "should include transitive trust target");
}
}
