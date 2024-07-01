package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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

import javax.script.Bindings;
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
    public Set<IRI> execute(IRI state, Resource llm, Bindings bindings) throws StateException {
        try {
            return _execute(state, llm, bindings);
        } catch (APIException | Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<IRI> _execute(IRI actor, Resource assistant, Bindings bindings) throws APIException, Exception {
        Set<IRI> done = new HashSet<>();
        I_LLM<String> gpt = CommonLLM.gpt(assistant, facts, 2048, secrets);
        if (gpt==null) return done;
        processLLM(actor, assistant, gpt, agent, bindings);
        done.add(actor);
        return done;
    }

    private void processLLM(IRI actor, Resource assistant, I_LLM<String> gpt, I_Agent agent, Bindings bindings) throws Exception, APIException {
        Resource state = agent.getStateMachine().getState();
        log.info("avatar.LLM: {} @ {} as {}", actor, state, assistant);
        Bindings my = MyFacade.rebind(agent.getSelf(), bindings);
        Optional<Literal> name = Models.getPropertyLiteral(facts, actor, RDFS.LABEL);
        name.ifPresent(literal -> my.put("name", literal.stringValue()));
        log.info("avatar.name: {} @ {} x {}", name.isPresent()?name.get():"??", my.keySet(), facts.size());

        AgentPrompt prompts = new AgentPrompt();
        Conversation ai = new Conversation();

        String prompt = prompts.prompt(actor, state, this.facts);
        ai.system(prompts.bind(prompt,my));
//        prompts.copy(ai, chat);
        ai.user(chat.latest().getContent());

        String wrapper = prompts.prompt(assistant, this.facts);
        String choices = prompts.choices(facts, agent.getStateMachine().getTransitions());
        my.put("choices", prompts.bind(choices, my));

        if (wrapper.isEmpty()) {
            chat.assistant(choices);
        } else {
            ai.system(prompts.bind(wrapper, my));
        }
        I_Assist<String> complete = gpt.complete(ai);
        log.debug("avatar.gpt: {} == {}", actor,  ai);
        updateChat(agent, chat ,complete);
        log.debug("avatar.done: {} == {}", actor,  chat);
    }

    private void updateChat(I_Agent agent, I_Assist<String> chat, I_Assist<String> ai) throws StateException {
        if ( !(ai.latest() instanceof IntentMessage)) {
            String reply = ai.latest().getContent();
            log.info("avatar.reply: {} => {}", chat.messages().size(), reply);
            chat.assistant(reply);
            return;
        }
        IntentMessage intent = (IntentMessage) ai.latest();
        I_StateMachine<Resource> fsm = agent.getStateMachine();
        if (!fsm.getTransitions().contains(intent.getSelf())) {
            log.info("avatar.confused: {} => {}", intent.getIntent(), intent.getContent());
            chat.assistant(intent.getContent());
            return;
        }
        I_LLMessage<String> latest = chat.latest();
        if (intent.getSelf().equals(fsm.getState())) {
            log.info("avatar.same: {}\n-> {}", fsm.getState(), intent.getContent());
            chat.assistant(intent.getContent());
        } else if (latest.getRole() == I_LLMessage.RoleType.user) {
            chat.messages().removeLast();
            chat.add(new IntentMessage(intent.getIntent(), I_LLMessage.RoleType.assistant, latest.getContent()));
            log.info("avatar.user: {} => {}", fsm.getState(), chat.latest());
        } else {
            chat.add(intent);
        }
    }


    @Override
    public IRI getSelf() {
        return agent.getSelf();
    }
}
