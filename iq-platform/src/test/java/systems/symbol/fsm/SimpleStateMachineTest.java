package systems.symbol.fsm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.ns.COMMONS;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleStateMachineTest {
    public static IRI ideation = Values.iri(COMMONS.IQ_NS_TEST,"ideation");
    public static IRI wip = Values.iri(COMMONS.IQ_NS_TEST, "work-in-progress");
    public static IRI review = Values.iri(COMMONS.IQ_NS_TEST,"in-review");
    public static IRI revision = Values.iri(COMMONS.IQ_NS_TEST,"revision");
    public static IRI complete = Values.iri(COMMONS.IQ_NS_TEST,"complete");

    public static I_StateMachine<Resource> newFSM() {
        return addFSM(new SimpleStateMachine<>());
    }


    public static I_StateMachine<Resource> addFSM(I_StateMachine<Resource> fsm) {
        fsm.add(ideation, wip);
        fsm.add(wip, review);
        fsm.add(review, revision);
        fsm.add(revision, ideation);
        fsm.add(revision, wip);
        fsm.add(review, complete);
        return fsm;
    }
    
    @Test
    void testAddState() {
    }
}