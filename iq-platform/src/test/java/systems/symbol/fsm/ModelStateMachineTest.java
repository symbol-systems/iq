package systems.symbol.fsm;

import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static systems.symbol.fsm.SimpleStateMachineTest.*;

public class ModelStateMachineTest {
DynamicModelFactory dmf = new DynamicModelFactory();
public static IRI self = Values.iri(COMMONS.IQ_NS_TEST);
public static IRI useGuardRule = RDF.TYPE;
public static IRI useGuardMatch = Values.iri(COMMONS.IQ_NS_TEST,"SignOff");

public static ModelStateMachine newMSM(Model model, IRI iri) {
ModelStateMachine msm = new ModelStateMachine(model, iri);
return (ModelStateMachine)SimpleStateMachineTest.addFSM(msm);
}

@Test
void initStateModel() throws StateException {
Model model = dmf.createEmptyModel();
ModelStateMachine msm = new ModelStateMachine(model, self);
assert null == msm.getState();
msm.add(ideation, wip);
assert ideation.equals(msm.getState());
msm.add(wip, review);
assert ideation.equals(msm.getState());
}

@Test
void isFinal() throws StateException {
Model model = dmf.createEmptyModel();
ModelStateMachine msm = newMSM(model, self);

assert !msm.isFinal(ideation);
assert msm.isFinal(complete);
}
@Test
void isAllowed() throws StateException {
Model model = dmf.createEmptyModel();
ModelStateMachine msm = newMSM(model, self);

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
Model model = dmf.createEmptyModel();
ModelStateMachine msm = newMSM(model, self);
msm.add(review, complete, useGuardRule, useGuardMatch);

msm.setCurrentState(review);

System.out.println("test.fsm.isGuarded: "+msm.getState()+ " -> "+msm.isAllowedByGuard(self, complete));
// none shall pass
assert ! msm.isAllowedByGuard(self, complete);
try {
msm.transition(complete);
System.out.println("transitioned: "+msm.getState());
assert !complete.equals(msm.getState());
} catch(Exception e) {
assert true; // correctly rejected
}

// fulfil our rule constraints
model.add(self, useGuardRule, useGuardMatch);

assert msm.isAllowedByGuard(self, complete);
msm.transition(complete);
}

@Test
void onTransition() throws StateException {
Model model = dmf.createEmptyModel();
ModelStateMachine msm = newMSM(model, self);

final boolean[] transitioned = {false};
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

final boolean[] veto = {false};
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