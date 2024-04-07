package systems.symbol.agent;

import systems.symbol.agent.apis.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Thread;
import systems.symbol.rdf4j.util.RDFHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * An agent that interacts with a Language Learning Model (LLM).
 */
public class LLMAgent implements I_Agent {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final I_LLM<String> llm;
    private final I_Agent agent;

    /**
     * Constructs a new LLMAgent with the provided Language Learning Model (LLM) and base agent.
     *
     * @param llm   The Language Learning Model (LLM) to interact with.
     * @param agent The base agent for the LLMAgent.
     */
    public LLMAgent(I_LLM<String> llm, I_Agent agent) {
        this.llm = llm;
        this.agent = agent;
    }

    /**
     * Checks if the LLMAgent is online.
     *
     * @return true if the LLMAgent is online, false otherwise.
     */
    public boolean isOnline() {
        return this.llm != null && !this.getModel().isEmpty();
    }

    /**
     * Sends a message to the LLMAgent and receives a response.
     *
     * @param message The message to send to the LLMAgent.
     * @return An I_Thread containing the response from the LLMAgent.
     * @throws APIException   If an error occurs during API interaction.
     * @throws IOException    If an I/O error occurs.
     * @throws StateException If an error occurs during state transition.
     */
    public I_Thread<String> say(String message) throws APIException, IOException, StateException {
        ChatThread thread = new ChatThread();
        Literal systemPrompt = RDFHelper.label(getModel(), getIdentity());
        log.info("say.system: {} -> {}", getIdentity(), systemPrompt);
        assert systemPrompt != null;
        thread.system(systemPrompt.stringValue() + "\nAlways answer in JSON: { \"response\": \"{response}\", \"action\": \"{action_uri}\" }");

        I_StateMachine<Resource> fsm = getStateMachine();
        Resource current = fsm.getState();
        Literal currentPrompt = RDFHelper.label(getModel(), current);

        StringBuilder options = new StringBuilder();
        log.info("say.current: {}", currentPrompt);
        if (currentPrompt != null) {
            options.append("<current_action>");
            options.append(currentPrompt);
            options.append("</current_action>");
        }

        Collection<Resource> transitions = fsm.getTransitions();
        if (!transitions.isEmpty()) {
            options.append("\nOnly choose your next action from: <action_table>|allowed_action|user intention|");
            transitions.forEach(t -> {
                Literal label = RDFHelper.label(getModel(), t);
                if (label != null) {
                    options.append("\n|").append(t).append(" | ");
                    options.append(label.stringValue()).append(" | ");
                }
            });
            options.append("</action_table>");
        }
        log.info("say.ai: {}", options);
        thread.system(options.toString());
        log.info("say.human: {}", message);
        thread.user("<user_message>" + message + "</user_message>");
        log.info("say.thread: {}", thread);
        I_Thread<String> answer = llm.generate(thread);
        Gson gson = new Gson();
        String content = answer.latest().getContent();
        if (content != null) {
            HashMap<String, Object> reply = gson.fromJson(content, new TypeToken<HashMap<String, Object>>() {
            }.getType());
            String action = (String) reply.get("action");
            log.info("say.reply: {} --> {}", reply, action);
            if (action != null) {
                fsm.transition(Values.iri(action));
            }

        }
        return answer;
    }

    @Override
    public void setModel(Model model) {
        agent.setModel(model);
    }

    @Override
    public Model getModel() {
        return agent.getModel();
    }

    @Override
    public I_StateMachine<Resource> getStateMachine() {
        return agent.getStateMachine();
    }

    @Override
    public void learn(I_StateMachine<Resource> fsm) {
        agent.learn(fsm);
    }

    @Override
    public IRI getIdentity() {
        return agent.getIdentity();
    }

    @Override
    public Resource decide(Resource state) throws StateException {
        return getStateMachine().transition(state);
    }
}
