package systems.symbol.self;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.RDF;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.AbstractIntent;
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
import java.util.Set;

/**
 * An intent that processes current state and updates based on self-reflective LLM content.
 */

public class SelfIntent extends AbstractIntent {
I_Secrets secrets;
Conversation chat;

public SelfIntent(IRI self, Model model, Conversation chat, I_Secrets secrets) {
boot(self, model);
this.secrets = secrets;
this.chat = chat;
}

/**
 * @param activity   The activity/focus of the thought process.
 * @param perspective   The perspective for self reflection.
 * @return The activity/perspective IRIs, if successful.
 * @throws IOExceptionIf an IO exception occurs.
 * @throws APIException   If an API exception occurs.
 */

private Set<IRI> thinks(IRI activity, Resource perspective, Bindings my) throws APIException, IOException, SecretsException {
IRIs done = new IRIs();
if (!perspective.isIRI()) return done;
I_LLM<String> gpt = CommonLLM.gpt(perspective, model, 1024, secrets);
if (gpt==null) {
log.warn("self.missing: {} @ {}", activity, perspective);
return done;
}
log.info("self.thinks: {} @ {}", activity, perspective);

PromptChain ai = new PromptChain();
// my self and state
FactsPrompt prompt = new FactsPrompt(my, model);
prompt.value(activity);
prompt.value(perspective);
ai.add(prompt);

// my context
//FactsPrompt factsPrompt = new FactsPrompt(my, model);
//factsPrompt.labels((IRI)perspective, IQ_NS.KNOWS);
//factsPrompt.labels((IRI)perspective, IQ_NS.TRUSTS);
//factsPrompt.labels((IRI)perspective, IQ_NS.NEEDS);
//ai.add(factsPrompt);

I_Assist<String> memoires = ai.complete(chat);

log.info("self.memoires: {}", memoires);
I_Assist<String> diary = gpt.complete(memoires);
I_LLMessage<String> myself = diary.latest();
log.info("self.reflect: {}", myself.getContent());
model.remove(perspective, org.eclipse.rdf4j.model.vocabulary.RDF.VALUE, null);
model.add(perspective, org.eclipse.rdf4j.model.vocabulary.RDF.VALUE, Values.***REMOVED***(myself.getContent()));
done.add(activity);
done.add((IRI)perspective);
return done;
}

@Override
@RDF(IQ_NS.IQ+"self")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return thinks(actor, state, bindings);
} catch (IOException | APIException e) {
throw new StateException(e.getMessage(), state, e);
} catch (SecretsException e) {
throw new RuntimeException(e);
}
}

}
