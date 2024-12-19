package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.fsm.I_StateListener;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.ModelStateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import systems.symbol.platform.I_Bootstrap;

import javax.script.Bindings;
import java.util.Set;

/**
 * A generic agent that can perform actions
 * The agent state is maintained by an RDF4J Model.
 * Skills are finite state machines which represent sets of next-best actions.
 */
public abstract class AbstractAgent implements I_Agent, I_Bootstrap, I_Intent, I_StateListener<Resource> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected I_StateMachine<Resource> fsm;
    protected Model thoughts;
    protected IRI self;

    /**
     * Parameterized constructor allowing initialization with a pre-existing RDF4J
     * model.
     * 
     * @param thoughts The RDF4J model to be associated with the agent.
     */
    public AbstractAgent(IRI self, @NotNull Model thoughts) throws StateException {
        this.self = self;
        this.thoughts = thoughts;
        boot(self, thoughts);
    }

    @Override
    public void boot(IRI self, Model model) throws StateException {
        if (fsm == null) {
            ModelStateMachine fsm = new ModelStateMachine(self, model);
            setFSM(fsm);
            log.debug("agent.boot: {} @ {}", getSelf(), fsm.getState());
        } else if (fsm instanceof ModelStateMachine) {
            ((ModelStateMachine) fsm).initialize(self, model, model);
            log.info("agent.reboot: {} @ {}", getSelf(), fsm.getState());
        }
    }

    @Override
    public void start() throws Exception {
        getStateMachine().transition(getStateMachine().getState());
        log.debug("agent.started: {} @ {}", getSelf(), getStateMachine().getState());
    }

    @Override
    public void stop() {
        log.info("agent.stopped: {} @ {}", getSelf(), getStateMachine().getState());
    }

    /**
     * Retrieves the state model associated with the agent.
     * 
     * @return The RDF4J model.
     */
    @Override
    public Model getThoughts() {
        return thoughts;
    }

    /**
     * initiating a state machine associated with a given workflow resource.
     * 
     * @return The state machine associated with the workflow (currently set to
     *         null).
     */
    @Override
    public I_StateMachine<Resource> getStateMachine() {
        return fsm;
    }

    /**
     * build actionable workflow sets by associating a workflow with a state
     * machine.
     * The state machines represent the behaviour of the agent
     *
     * @param fsm The state machine associated with the workflow.
     */
    protected void setFSM(@NotNull I_StateMachine<Resource> fsm) {
        this.fsm = fsm;
        fsm.listen((from, to) -> {
            log.info("agent.onIntent: {} ==> {} @ {}", from, to, getSelf());
            try {
                return onTransition(from, to);
            } catch (Exception e) {
                log.warn("agent.intent.failed: {} @ {} ==> ", getSelf(), fsm.getState(), e);
                return false;
            }
        });
        log.debug("agent.listen: {} @ {}", fsm.getState(), getSelf());
    }

    /**
     * 
     * @param from previous state
     * @param to   current state (unless veto-ed)
     * @return will veto/revert the transition if false
     */
    public abstract boolean onTransition(Resource from, Resource to) throws StateException;

    /**
     * Executes a transition in the specified workflow to reach the desired state.
     *
     * This method executes a workflow represented as a finite state machine.
     * to the specified target state. If the transition is successful, the workflow
     * is marked as done, and the set of completed workflows is returned.
     *
     * @param actor The IRI representing the actor/subject of the action.
     * @param state The IRI representing the target state we've transitioned to.
     * @return The set of IRIs of workflows AND states that have been successful.
     * @throws StateException If there is an issue with the state transition.
     */
    @Override
    public abstract Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException;

    public Resource decide(Resource state) throws StateException {
        return getStateMachine().transition(state);
    }

    @Override
    public IRI getSelf() {
        return self;
    }
}
