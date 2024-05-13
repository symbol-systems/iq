package systems.symbol.fsm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.platform.IQ_NS;

/**
            +---------+       +-----+
     ----->| ideation | -->  | wip |
     |      +---------+      +-----+
     |        |               |
     |        |               |
     |        v               |
     |     +-------+          |
     |     | review|          |
     |     +-------+          |
     |        |               |
     |        |               |
     +--------|               |
              v               |
     +---------+              |
     | revision|    <---------+
     +---------+
 */
public class SimpleStateMachineTest {
    public static IRI ideation = Values.iri(IQ_NS.TEST,"ideation");
    public static IRI wip = Values.iri(IQ_NS.TEST, "work-in-progress");
    public static IRI review = Values.iri(IQ_NS.TEST,"in-review");
    public static IRI revision = Values.iri(IQ_NS.TEST,"revision");
    public static IRI complete = Values.iri(IQ_NS.TEST,"complete");

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