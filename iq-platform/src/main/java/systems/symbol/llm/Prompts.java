package systems.symbol.llm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_Agentic;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.util.RDFHelper;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static systems.symbol.agent.MyFacade.PROMPT;

/**
 * Utility class for generating LLM prompts used in neuro-symbolic cognition.
 */
public class Prompts {

    protected static final Logger log = LoggerFactory.getLogger(Prompts.class);
    public static final String INTENT = "intent";
    public static final String CONTENT = "content";
    private static final String SYSTEM_JSON_INTENT = "\nOnly answer in valid JSON: { \""+CONTENT+"\": \"{explanation}\", \""+INTENT+"\": \"{user_intent_IRI}\" }";
    private static final String CHOOSE_INTENT = "\n|available-intent|user instruction|";

    /**
     * Generates a 'decision prompt' for the agent in the given context.
     *
     * @param agent   The agent making the decision.
     * @param context The context in which the decision is being made.
     * @return A chat thread containing the decision prompt.
     * @throws IOException If an I/O error occurs during prompt generation.
     */
    public static I_Assist<String> decision(I_Agent agent, I_Agentic<String> context) throws IOException {
        IRI actor = agent.getSelf();
        Model model = agent.getMemo();
        Bindings my = context.getBindings();
        I_StateMachine<Resource> fsm = agent.getStateMachine();
        Resource current = fsm.getState();
        // actor/state prompt
        I_Assist<String> thread = prompt(actor, current, model, my);
        // intents prompt
        Collection<Resource> transitions = fsm.getTransitions();
        log.info("prompt.transitions: {}", transitions);
        if (!transitions.isEmpty()) {
            StringBuilder intentsPrompt = new StringBuilder(CHOOSE_INTENT);
            transitions.forEach(t -> {
                Literal label = RDFHelper.label(model, t);
                if (label != null) {
                    intentsPrompt.append("\n|").append(t).append(" | ");
                    intentsPrompt.append(label.stringValue()).append(" | ");
                }
            });
            log.debug("prompt.intents: {}", intentsPrompt);
            if (intentsPrompt.length()>CHOOSE_INTENT.length())
                thread.system("<available-choices>"+intentsPrompt+"</available-choices>");
        }
        // json system-prompt
        thread.system(SYSTEM_JSON_INTENT);
        log.debug("prompt.thread: {}", thread);
        // copy non-system
        for(I_LLMessage<String> message: context.getConversation().messages()) {
            if (message.getRole()!=I_LLMessage.RoleType.system) {
                thread.add(message);
            }
        }
        // user-prompt
        String prompt = Prompts.user(thread);
        log.info("prompt.user: {}", prompt);
        my.put(PROMPT, prompt);
        return thread;
    }

    /**
     * Generates a 'conversational prompt' for the given actor and state.
     *
     * @param actor   The actor for whom the prompt is generated.
     * @param current The current state.
     * @param model   The RDF model containing prompt information.
     * @param my      The bindings used for template interpolation.
     * @return A chat thread containing the prompt.
     * @throws IOException If an I/O error occurs during prompt generation.
     */
    public static I_Assist<String> prompt(IRI actor, Resource current, Model model, Bindings my) throws IOException {
        return prompt(new Conversation(), actor,current, model, my);
    }

    /**
     * Generates a 'conversational prompt' for the given actor and state, appending it to an existing chat thread.
     *
     * @param chat    The chat thread to which the prompt is appended.
     * @param actor   The actor for whom the prompt is generated.
     * @param current The current state.
     * @param model   The RDF model containing prompt information.
     * @param my      The bindings used for template interpolation.
     * @return The updated chat thread containing the prompt.
     * @throws IOException If an I/O error occurs during prompt generation.
     */
    public static I_Assist<String> prompt(Conversation chat, IRI actor, Resource current, Model model, Bindings my) throws IOException {
        try {
            RDFDump.dump(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Set<Literal> selfPrompts = RDFHelper.values(model, actor);
        log.info("prompt.actor: {} -> {} -> {}", actor, selfPrompts, current);
        for(Literal p: selfPrompts) {
            String template = HBSRenderer.template(p.stringValue(), my);
            chat.system(template);
        }
        // state prompt
        if (current!=null) {
            Set<Literal> statePrompts = RDFHelper.values(model, current);
            for(Literal p: statePrompts) {
                String template = HBSRenderer.template(p.stringValue(), my);
                chat.system(template);
            }
            log.info("prompt.state: {} -> {}", current, statePrompts);
        }

        return chat;
    }

    /**
     * Extracts the user prompt from a chat thread.
     *
     * @param thread The chat thread.
     * @return The user prompt.
     */
    public static String user(I_Assist<String> thread) {
        return prompt(thread, I_LLMessage.RoleType.user);
    }

    /**
     * Extracts prompts of a specified role type from a chat thread.
     *
     * @param thread The chat thread.
     * @param role   The role type of the prompts to extract.
     * @return The prompts of the specified role type.
     */
    public static String prompt(I_Assist<String> thread, I_LLMessage.RoleType role) {
        StringBuilder prompt = new StringBuilder();
        for(I_LLMessage<String> message: thread.messages()) {
            if (message.getRole()==role) {
                if (prompt.length()>0) prompt.append("\n");
                prompt.append(message.getContent());
            }
        }
        return prompt.toString();
    }

    /**
     * Generates a 'think prompt' for the given actor, state, and intent.
     *
     * @param actor   The actor for whom the prompt is generated.
     * @param state   The current state.
     * @param intent  The intent associated with the prompt.
     * @param model   The RDF model containing prompt information.
     * @param my      The bindings used for template interpolation.
     * @param format  The RDF format to be used.
     * @return A chat thread containing the think prompt.
     * @throws IOException If an I/O error occurs during prompt generation.
     */
    public static I_Assist<String> think(IRI actor, Resource state, String intent, Model model, Bindings my, RDFFormat format) throws IOException {
        I_Assist<String> thread = prompt(actor, state, model, my);
        Map<String, String> namespaces = RDFPrefixer.simple();
        namespaces.put("my", intent);
        thread.system("Reply using valid "+format.getName()+". Use only declared namespaces & please remember to declare: @prefix my: <"+intent+">");
        thread.system(RDFPrefixer.toPrefix(namespaces).toString());
        return thread;
    }
}
