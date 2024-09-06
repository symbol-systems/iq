package systems.symbol.self;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.RDF;
import systems.symbol.agent.Avatar;
import systems.symbol.agent.I_Selfie;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.intent.I_Intent;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.gpt.CommonLLM;
import systems.symbol.platform.IQ_NS;
import systems.symbol.prompt.*;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class SelfIntent extends AbstractIntent {
    Conversation chat;
    I_Selfie selfie;

    public SelfIntent(I_Selfie selfie, Conversation chat) {
        this.selfie = selfie;
        this.chat = chat;
    }

    @Override
    @RDF(IQ_NS.IQ+"self")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        Set<IRI> executed = selfie.execute(actor, state, bindings);
        if (executed.isEmpty()) return executed;
        I_LLMessage<String> myself = chat.latest();
        Resource intent = selfie.intent();
        if (intent==null) return executed;
        if (intent.isIRI()) executed.add((IRI) intent);
        Resource now = selfie.getStateMachine().transition(intent);
        if (!now.isIRI()) return executed;
        model.remove(now, org.eclipse.rdf4j.model.vocabulary.RDF.VALUE, null);
        model.add(now, org.eclipse.rdf4j.model.vocabulary.RDF.VALUE, Values.literal(myself.getContent()));
        executed.add((IRI)now);
        return executed;
    }
}
