package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.llm.ChatThread;
import systems.symbol.rdf4j.util.RDFHelper;

import java.util.Collection;
import java.util.Set;

public class Prompts {
    protected static final Logger log = LoggerFactory.getLogger(Prompts.class);
    private static final String SYSTEM_JSON_INTENT = "\nOnly answer in valid JSON: { \"response\": \"{succinct_response}\", \"explanation\": \"{explanation}\", \"intent\": \"{intent_uri}\" }";
    private static final String CHOOSE_INTENT = "\n|available-intent|user instruction|";

    public static ChatThread decision(ChatThread thread, Model model, IRI self, I_StateMachine<Resource> fsm) {
        Set<Literal> selfPrompts = RDFHelper.values(model, self);
        log.info("self: {} -> {}", self, selfPrompts);
        for(Literal p: selfPrompts) {
            thread.system(p.stringValue());
        }
        Resource current = fsm.getState();
        Set<Literal> statePrompts = RDFHelper.values(model, current);
        log.info("state: {} -> {}", current, statePrompts);
        for(Literal p: statePrompts) {
            thread.system(p.stringValue());
        }

        Collection<Resource> transitions = fsm.getTransitions();
        log.info("transitions: {}", transitions);
        if (!transitions.isEmpty()) {
            StringBuilder intentsPrompt = new StringBuilder(CHOOSE_INTENT);
            transitions.forEach(t -> {
                Literal label = RDFHelper.label(model, t);
                if (label != null) {
                    intentsPrompt.append("\n|").append(t).append(" | ");
                    intentsPrompt.append(label.stringValue()).append(" | ");
                }
            });
            log.info("intents: {}", intentsPrompt);
            if (intentsPrompt.length()>CHOOSE_INTENT.length())
                thread.system("<available-choices>"+intentsPrompt+"</available-choices>");
        }
        thread.system(SYSTEM_JSON_INTENT);
        log.info("thread: {}", thread);

        return thread;
    }
}
