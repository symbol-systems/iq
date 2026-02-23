package systems.symbol.fsm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import systems.symbol.platform.IQ_NS;

import static systems.symbol.fsm.SimpleStateMachineTest.*;
import static systems.symbol.platform.IQ_NS.TO;
import static systems.symbol.platform.IQ_NS.hasInitialState;

public class ModelStateMachineTest {
static DynamicModelFactory dmf = new DynamicModelFactory();
public static IRI self = Values.iri(IQ_NS.TEST);
public static IRI useGuardRule = RDF.TYPE;
public static IRI useGuardMatch = Values.iri(IQ_NS.TEST, "SignOff");

public static ModelStateMachine newMSM(IRI iri) throws StateException {
ModelStateMachine msm = new ModelStateMachine(iri, newTestModel(iri));
return (ModelStateMachine) SimpleStateMachineTest.addFSM(msm);
}

public static Model newTestModel(IRI self) {
Model model = dmf.createEmptyModel();
model.add(ideation, TO, wip);
model.add(wip, TO, review);
model.add(review, TO, revision);
model.add(revision, TO, ideation);
model.add(revision, TO, wip);
model.add(review, TO, complete);
model.add(self, hasInitialState, ideation);
return model;
}

@Test
void initStateModel() throws StateException {
Model model = newTestModel(self);
ModelStateMachine msm = new ModelStateMachine(self, model);
assert null != msm.getState();
assert ideation.equals(msm.getState());
}

@Test
void isFinal() throws StateException {
Model model = newTestModel(self);
ModelStateMachine msm = new ModelStateMachine(self, model);

assert !msm.isFinal(ideation);
assert msm.isFinal(complete);
}

@Test
void isAllowed() throws StateException {
ModelStateMachine msm = newMSM(self);

assert msm.getState().equals(ideation);
assert msm.isAllowed(wip);
assert msm.isAllowed(ideation); // can always transition to self

msm.transition(wip);
assert wip.equals(msm.getState());
msm.transition(review);
assert review.equals(msm.getState());

assert msm.isAllowed(complete);
assert msm.isAllowed(revision);
}

@Test
void isGuarded() throws StateException {
ModelStateMachine msm = newMSM(self);
msm.add(review, complete, useGuardRule, useGuardMatch);

msm.setCurrentState(review);

System.out.println("test.fsm.isGuarded: " + msm.getState() + " -> " + msm.isAllowedByGuard(self, complete));
// none shall pass
assert !msm.isAllowedByGuard(self, complete);
try {
msm.transition(complete);
System.out.println("transitioned: " + msm.getState());
assert !complete.equals(msm.getState());
} catch (Exception e) {
assert true; // correctly rejected
}

// fulfil our rule constraints
msm.getGround().add(self, useGuardRule, useGuardMatch);

assert msm.isAllowedByGuard(self, complete);
msm.transition(complete);
}

@Test
void onTransition() throws StateException {
ModelStateMachine msm = newMSM(self);

final boolean[] transitioned = { false };
msm.listen(new I_StateListener<Resource>() {
@Override
public boolean onTransition(Resource from, Resource to) {
transitioned[0] = true;
return true;
}
});
assert !transitioned[0];
msm.transition(wip);
assert transitioned[0];
assert wip.equals(msm.getState());

// test veto

final boolean[] veto = { false };
msm.listen(new I_StateListener<Resource>() {
@Override
public boolean onTransition(Resource from, Resource to) {
veto[0] = true;
return false;
}
});
assert !veto[0];
msm.transition(review);
assert veto[0];
// veto should leave us unaffected
assert wip.equals(msm.getState());
}

}