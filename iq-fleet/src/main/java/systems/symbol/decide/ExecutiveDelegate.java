package systems.symbol.decide;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Thread;
import systems.symbol.platform.I_Self;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashMap;

/*
 * An LLM decision maker that uses a Language Model (LLM) to interpret an actor's intentions on behalf of an agent .
 */
public class ExecutiveDelegate extends SimpleDelegate<Resource> implements I_Self {
    private final I_LLM<String> llm;
    private final IRI agent;
    private final Model model;
    private final Gson gson = new Gson();

    /*
     * Constructor for an ExecutiveDelegate
     * @param llm The LLM used for decision-making.
     * @param agent The agent requesting a decision.
     * @param agent The FSM associated with the decision.
     */
    public ExecutiveDelegate(I_LLM<String> llm, IRI agent, Model model, I_StateMachine<Resource> fsm) {
        super(fsm);
        this.llm = llm;
        this.agent = agent;
        this.model = model;
    }

    /*
     * Make a decision based on the current prompt and historic context.
     * @param history An existing ChatThread for historic context.
     * @param prompt The prompt message to base the decision on.
     * @throws APIException If an API-related error occurs.
     * @throws IOException If an IO-related error occurs.
     * @throws StateException If an error occurs with the state machine.
     */
    public I_Thread<String> decide(I_Thread<String> history, Bindings my) throws APIException, IOException {
        log.info("executive.decide: {} => {}", getSelf(), this.fsm.getState());

        I_Thread<String> prompted = Prompts.decision(history, model, getSelf(), fsm, my);

        I_Thread<String> answer = llm.generate(prompted);
        log.info("executive.answer: {}", answer.latest());
        String content = answer.latest().getContent();
        if ( Validate.isMissing(content) ) return null;

        HashMap<String, Object> reply = gson.fromJson( content, new TypeToken<HashMap<String, Object>>() {}.getType() );
        String intent = (String) reply.get("intent");
        log.info("executive.intent: {} --> {}", reply, intent);

        choice(Values.iri(intent));
        return answer;
    }

    @Override
    public IRI getSelf() {
        return agent;
    }
}
