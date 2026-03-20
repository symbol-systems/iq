package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Common event topic IRIs for event/hook integration.
 */
public final class KernelTopics {

    private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

    private KernelTopics() {}

    // kernel lifecycle
    public static final IRI KERNEL_START = vf.createIRI("urn:iq:event:kernel:start");
    public static final IRI KERNEL_STOP = vf.createIRI("urn:iq:event:kernel:stop");
    public static final IRI KERNEL_RESTART = vf.createIRI("urn:iq:event:kernel:restart");

    // command/cli lifecycle
    public static final IRI CLI_COMMAND_START = vf.createIRI("urn:iq:event:cli:command:start");
    public static final IRI CLI_COMMAND_COMPLETE = vf.createIRI("urn:iq:event:cli:command:complete");
    public static final IRI CLI_COMMAND_ERROR = vf.createIRI("urn:iq:event:cli:command:error");

    // RDF / repository operations
    public static final IRI RDF_STATEMENT_ADD = vf.createIRI("urn:iq:event:rdf:statement:add");
    public static final IRI RDF_STATEMENT_REMOVE = vf.createIRI("urn:iq:event:rdf:statement:remove");
    public static final IRI RDF_STATEMENT_CLEAR = vf.createIRI("urn:iq:event:rdf:statement:clear");
    public static final IRI RDF_REPOSITORY_COMMIT = vf.createIRI("urn:iq:event:rdf:repository:commit");
    public static final IRI RDF_REPOSITORY_ROLLBACK = vf.createIRI("urn:iq:event:rdf:repository:rollback");
    public static final IRI RDF_REPOSITORY_PREPARE = vf.createIRI("urn:iq:event:rdf:repository:prepare");

    // script execution
    public static final IRI SCRIPT_LOAD = vf.createIRI("urn:iq:event:script:load");
    public static final IRI SCRIPT_COMPILE = vf.createIRI("urn:iq:event:script:compile");
    public static final IRI SCRIPT_EXECUTE_PRE = vf.createIRI("urn:iq:event:script:execute:pre");
    public static final IRI SCRIPT_EXECUTE_POST = vf.createIRI("urn:iq:event:script:execute:post");
    public static final IRI SCRIPT_EXECUTE_ERROR = vf.createIRI("urn:iq:event:script:error");

    // FSM / agent transitions
    public static final IRI FSM_TRANSITION_BEFORE = vf.createIRI("urn:iq:event:fsm:transition:before");
    public static final IRI FSM_TRANSITION_AFTER = vf.createIRI("urn:iq:event:fsm:transition:after");
    public static final IRI FSM_TRANSITION_ERROR = vf.createIRI("urn:iq:event:fsm:transition:error");

    // lake operations
    public static final IRI LAKE_PARTITION_LOAD = vf.createIRI("urn:iq:event:lake:partition:load");
    public static final IRI LAKE_PARTITION_STORE = vf.createIRI("urn:iq:event:lake:partition:store");
    public static final IRI LAKE_PARTITION_DELETE = vf.createIRI("urn:iq:event:lake:partition:delete");
    public static final IRI LAKE_CHECKPOINT = vf.createIRI("urn:iq:event:lake:checkpoint");
}
