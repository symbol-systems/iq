package systems.symbol.platform;

import systems.symbol.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.jupiter.api.Test;

class SubjectiveModelTest {

@Test
void addSubjectiveFacts() {
ValueFactory vf = SimpleValueFactory.getInstance();
IRI self = vf.createIRI(IQ_NS.TEST);
Model baseModel = new DynamicModelFactory().createEmptyModel();
Model selfModel = new SubjectiveModel(baseModel, self);

IRI eg0 = vf.createIRI(self.stringValue(), "example");
boolean eg0_ok = selfModel.add(eg0, SKOS.PREF_LABEL, vf.createLiteral("hello"));
assert eg0_ok;
assert baseModel.contains(eg0, null, null, self);
assert selfModel.contains(eg0, null, null, self);

IRI eg1 = vf.createIRI("http://example.com");
boolean eg1_ok = selfModel.add(eg1, SKOS.PREF_LABEL, vf.createLiteral("ignored"));
assert !eg1_ok;

assert !selfModel.contains(eg1, null, null);
assert !baseModel.contains(eg1, null, null);
}
}