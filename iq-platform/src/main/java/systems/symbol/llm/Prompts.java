package systems.symbol.llm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.rdf4j.util.RDFHelper;

import java.util.Collection;

public class Prompts {
    protected static final Logger log = LoggerFactory.getLogger(Prompts.class);
    private static final String SYSTEM_JSON_INTENT = "\nAlways answer in JSON: { \"response\": \"{response}\", \"intent\": \"{intent_uri}\" }";
    private static final String CHOOSE_INTENT = "\nOnly choose your next action from: <intents>\n|intent|user instruction|";

    public static ChatThread decision(ChatThread thread, Model model, IRI self, I_StateMachine<Resource> fsm) {
        Literal systemPrompt = RDFHelper.label(model, self);
//        log.debug("prompt.system: {} -> {}", self, systemPrompt);
        assert systemPrompt != null;
        thread.system(systemPrompt.stringValue() + SYSTEM_JSON_INTENT);

        Resource current = fsm.getState();
        Literal statePrompt = RDFHelper.label(model, current);

        StringBuilder prompt = new StringBuilder();
//        log.info("prompt.state: {}", statePrompt);
        if (statePrompt != null) {
            prompt.append("<current_state>");
            prompt.append(statePrompt);
            prompt.append("</current_state>");
        }

        Collection<Resource> transitions = fsm.getTransitions();
        if (!transitions.isEmpty()) {
            prompt.append(CHOOSE_INTENT);
            transitions.forEach(t -> {
                Literal label = RDFHelper.label(model, t);
                if (label != null) {
                    prompt.append("\n|").append(t).append(" | ");
                    prompt.append(label.stringValue()).append(" | ");
                }
            });
            prompt.append("</intents>");
        }
        log.info("prompt.done: {}", prompt);
        thread.system(prompt.toString());
        return thread;
    }
}
