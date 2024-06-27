package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import systems.symbol.llm.*;
import systems.symbol.llm.gpt.CommonLLM;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Self;
import systems.symbol.prompt.AgentPrompt;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;
import java.io.IOException;
import java.util.*;

public class Avatar implements I_Self, I_Intent {
    private static final Logger log = LoggerFactory.getLogger(Avatar.class);
    I_Agent agent;
    I_Assist<String> chat;
    Model facts;
    I_Secrets secrets;

    public Avatar(I_Agent agent, I_Assist<String> chat, Model facts, I_Secrets secrets) {
        this.agent = agent;
        this.chat = chat;
        this.facts = facts;
        this.secrets = secrets;
    }

    @Override
    @systems.symbol.RDF(IQ_NS.IQ + "a")
    public Set<IRI> execute(IRI actor, Resource assistant, Bindings bindings) throws StateException {
        Set<IRI> done = new HashSet<>();
        try {
            I_LLM<String> gpt = CommonLLM.gpt(assistant, facts, 2048, secrets);
            if (gpt==null) return done;
            processLLM(actor, assistant, gpt, bindings);
        } catch (SecretsException | APIException | IOException e) {
            throw new RuntimeException(e);
        }
        done.add(actor);
        return done;
    }

    private void processLLM(IRI actor, Resource assistant, I_LLM<String> gpt, Bindings bindings) throws SecretsException, IOException, APIException, StateException {
        log.info("avatar.llm: {} @ {}", actor, agent.getStateMachine().getState());

        AgentPrompt prompts = new AgentPrompt();
        Conversation ai = new Conversation();

        String prompt = prompts.prompt(actor, agent.getStateMachine().getState(), this.facts);
        ai.system(prompt);
        ai.user(chat.latest().getContent());

        String wrapper = prompts.prompt(assistant, this.facts);
        String choices = prompts.choices(facts, agent.getStateMachine().getTransitions());

        if (wrapper.isEmpty()) {
            chat.assistant(choices);
            log.info("avatar.choices: {} -> {}", assistant, choices);
        } else {
            bindings.put("choices", choices);
            ai.system( prompts.prompt(wrapper, bindings) );
            log.info("avatar.wrapper: {} == {} -> {}", assistant, wrapper, choices);
        }
        I_Assist<String> complete = gpt.complete(ai);
        processIntent(agent, chat ,complete);
        log.info("avatar.complete: {} == {}\n -> {}", actor, agent.getStateMachine().getState(), chat);
    }

    private void processIntent(I_Agent agent, I_Assist<String> chat, I_Assist<String> ai) throws StateException {
//        log.info("avatar.intent: {} == {}", actor, agent.getStateMachine().getState());
        if (ai.latest() instanceof IntentMessage) {
            IntentMessage intent = (IntentMessage) ai.latest();
            I_StateMachine<Resource> fsm = agent.getStateMachine();
            if (fsm.getTransitions().contains(intent.getSelf())) {
                if (intent.getSelf().equals(fsm.getState())) {
                    log.info("avatar.nop: {}\n-> {}", fsm.getState(), intent.getContent());
                    chat.assistant(intent.getContent());
                } else {
                    chat.add(intent);
                    fsm.transition(intent.getSelf());
                    log.info("avatar.intent: {} => {} -> {} \n-> {}", chat.messages().size(), fsm.getState(), intent.getIntent(), intent.getContent());
                }
            }
        } else {
            String reply = ai.latest().getContent();
            log.info("avatar.reply: {} => {}", chat.messages().size(), reply);
            chat.assistant(reply);
        }
    }


    @Override
    public IRI getSelf() {
        return agent.getSelf();
    }
}
