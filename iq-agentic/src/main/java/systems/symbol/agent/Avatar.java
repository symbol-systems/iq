package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.tools.APIException;
import systems.symbol.decide.I_Delegate;
import systems.symbol.finder.I_Found;
import systems.symbol.finder.I_Search;
import systems.symbol.finder.SearchMatrix;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;
import systems.symbol.llm.gpt.GPTResponse.Usage;
import systems.symbol.llm.gpt.GPTWrapper;
import systems.symbol.llm.gpt.LLMFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.prompt.*;
import systems.symbol.secrets.I_Secrets;

import javax.script.Bindings;

import java.io.IOException;
import java.util.*;

/**
 * The `Avatar` class represents an intelligent agent capable of interacting
 * with a large language model (LLM) to process and execute user intents.
 * This class acts as a mediator between the agent's internal
 * decision-making, the prompts and responses of the LLM.
 *
 * <ul>
 * <li>Manages agent's state machine and ensuring that state transitions
 * are performed according to the message intent.</li>
 * <li>Generating and processing prompts through the LLM, allowing the agent to
 * provide intelligent, context-aware responses.</li>
 * <li>Updating the conversation flow (`chat`) based on the LLM's output and
 * the agent's internal decision-making logic.</li>
 * <li>Heuristically determining the correct state transition based on user
 * intents, ensuring that the agent's behavior aligns with the user's
 * expectations.</li>
 * </ul>
 *
 * The intent flow in the Avatar class involves recognizing a user’s intent
 * through chat interactions, processing it using a LLM,
 * and then determining and executing the appropriate state transition within
 * the agent’s state machine.
 * The flow is designed to be modular, with clear separation of concerns between
 * intent recognition, state management, and interaction handling.
 */

public class Avatar implements I_Avatar {
    private static final Logger log = LoggerFactory.getLogger(Avatar.class);
    I_Agent agent;
    I_Assist<String> chat;
    Model facts;
    I_Secrets secrets;
    int contextLength = 2048;
    private List<Usage> usage = new ArrayList<>();
    I_Delegate<Resource> manager;

    public Avatar(I_Agent agent, I_Assist<String> chat, Model facts, I_Secrets secrets) {
        this.agent = agent;
        this.chat = chat;
        this.facts = facts;
        this.secrets = secrets;
    }

    public Avatar(I_Delegate<Resource> manager, I_Agent agent, I_Assist<String> chat, Model facts, I_Secrets secrets) {
        this.manager = manager;
        this.agent = agent;
        this.chat = chat;
        this.facts = facts;
        this.secrets = secrets;
    }

    // Executes the agent's action given a state, LLM , and bindings
    @Override
    @systems.symbol.RDF(IQ_NS.IQ + "ai")
    public Set<IRI> execute(IRI state, Resource llm, Bindings bindings) throws StateException {
        try {
            return llm(state, llm, bindings);
        } catch (APIException | Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Executes the agent's current/default state

    public Set<IRI> execute(Bindings bindings) {
        try {
            return llm(getSelf(), getStateMachine().getState(), bindings);
        } catch (APIException | Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Handles the flow between agent and the LLM, then updating state
    public Set<IRI> llm(IRI action, Resource llm, Bindings bindings) throws APIException, Exception {
        Set<IRI> done = new HashSet<>();
        GPTWrapper gpt = LLMFactory.llm(llm, facts, contextLength, secrets);
        if (gpt == null) {
            log.error("*** OOPS.llm.configure: {} @ {} *** ", action, llm);
            return done;
        }
        if (!generate(action, llm, gpt, bindings)) {
            log.warn("*** OOPS.llm.prompt: {} @ {} *** ", action, llm);
            return done;
        }
        usage.addAll(gpt.getUsage());
        done.add(action);
        // done.add(decision);
        return done;
    }

    private boolean generate(IRI action, Resource llm, I_LLM<String> gpt, Bindings bindings)
            throws IOException, APIException, StateException {
        AvatarPrompt prompts = new AvatarPrompt(agent, facts, bindings);
        prompts.avatar(llm);
        prompts.assistant();
        I_Assist<String> prompted = prompts.complete(chat);
        log.info("avatar.generate: {}", action);
        I_Assist<String> answer = gpt.complete(prompted);
        answered(agent, chat, answer);
        return true;
    }

    // Updates the chat interface with the LLM's response and intent processing
    private void answered(I_Agent agent, I_Assist<String> chat, I_Assist<String> answer) throws StateException {
        I_LLMessage<String> latest = answer.latest();
        if (!latest.getRole().equals(I_LLMessage.RoleType.assistant)) {
            log.info("avatar.not-reply {}", agent.getSelf());
            return;
        }
        if (!(latest instanceof IntentMessage)) {
            String reply = latest.getContent();
            log.info("avatar.reply: #{} => {}", answer.messages().size(), answer.messages());
            chat.assistant(reply);
            return;
        }
        IntentMessage intent = (IntentMessage) latest;
        chat.add(intent);
        log.info("avatar.intent: {} => {} @ {}", intent.getIntent(), agent.getSelf(),
                agent.getStateMachine().getState());
    }

    // Returns the current identity of the agent
    @Override
    public IRI getSelf() {
        return agent.getSelf();
    }

    // Retrieves the current thoughts or internal state of the agent
    @Override
    public Model getThoughts() {
        return agent.getThoughts();
    }

    // Accesses the state machine of the agent
    @Override
    public I_StateMachine<Resource> getStateMachine() {
        return agent.getStateMachine();
    }

    public List<Usage> getUsage() {
        return usage;
    }

    // Starts the agent
    @Override
    public void start() throws Exception {
        agent.start();
        if (manager != null) {
            Resource delegated = manager.intent();
            log.info("avatar.delegated: {}", delegated);
            agent.getStateMachine().transition(delegated);
        }
        log.info("avatar.started: {} @ {}", agent.getSelf(), agent.getStateMachine().getState());
    }

    // Stops the agent's operation
    @Override
    public void stop() {
        agent.stop();
    }

    @Override
    public Resource intent() throws StateException {
        return manager.intent();
    }

}
