package systems.symbol.fsm;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

public class ModelStateMachineTest {
    DynamicModelFactory dmf = new DynamicModelFactory();
    static ValueFactory vf = SimpleValueFactory.getInstance();
    public static IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);
    public static IRI ideation = vf.createIRI(COMMONS.IQ_NS_TEST,"ideation");
    public static IRI wip = vf.createIRI(COMMONS.IQ_NS_TEST, "work-in-progress");
    public static IRI review = vf.createIRI(COMMONS.IQ_NS_TEST,"in-review");
    public static IRI revision = vf.createIRI(COMMONS.IQ_NS_TEST,"revision");
    public static IRI complete = vf.createIRI(COMMONS.IQ_NS_TEST,"complete");
    public static IRI test_script = vf.createIRI(COMMONS.IQ_NS_TEST,"test_script");
    public static IRI useGuardRule = RDF.TYPE;
    public static IRI useGuardMatch = vf.createIRI(COMMONS.IQ_NS_TEST,"SignOff");


    public static ModelStateMachine newMSM(Model model, IRI iri) {
        ModelStateMachine msm = new ModelStateMachine(model, iri);
        msm.add(ideation, wip);
        msm.add(wip, review);
        msm.add(review, revision);
        msm.add(revision, ideation);
        msm.add(revision, wip);
        msm.add(review, complete);
        return msm;
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
    void dumpStates() throws Exception {
        Model model = dmf.createEmptyModel();
        ModelStateMachine msm = newMSM(model, self);
        assert msm.isAllowedByGuard(self, complete);
        assert ideation.equals(msm.getState());

        //        System.out.println("test.fsm.dump: "+msm.getState()+ " -> "+msm.isAllowedByGuard(self, complete));
//        System.out.println("\n");
//        RDFDump.dump(model, System.out, RDFFormat.TURTLE);
//        System.out.println("\n");
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