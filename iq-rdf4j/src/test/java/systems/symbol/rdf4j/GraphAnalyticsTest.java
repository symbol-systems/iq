package systems.symbol.rdf4j;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf.analytics.GraphAnalytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphAnalyticsTest {

@Test
public void testComputePhiWithSingleTriple() {
Model model = new LinkedHashModel();
var vf = SimpleValueFactory.getInstance();
model.add(vf.createIRI("http://example.org/s1"), vf.createIRI("http://example.org/p1"), vf.createLiteral("o1"));

double phi = GraphAnalytics.computePhi(model);
double phiNormal = GraphAnalytics.computePhiNormal(model);

assertEquals(0.0, phi, 1e-9, "Phi should be zero for single triple graph (no partitions or entropy variance)");
assertEquals(0.0, phiNormal, 1e-9, "Normalized Phi should be zero for single triple graph");
}

@Test
public void testComputePhiWithDisconnectedComponents() {
Model model = new TreeModel();
var vf = SimpleValueFactory.getInstance();

model.add(vf.createIRI("http://example.org/a"), vf.createIRI("http://example.org/p"), vf.createLiteral("x"));
model.add(vf.createIRI("http://example.org/b"), vf.createIRI("http://example.org/p"), vf.createLiteral("y"));

double phi = GraphAnalytics.computePhi(model);
double phiNormal = GraphAnalytics.computePhiNormal(model);

assertTrue(phi >= 0.0, "Phi should be non-negative for disconnected graph");
assertTrue(phiNormal >= 0.0, "Normalized Phi should be non-negative for disconnected graph");
}
}
