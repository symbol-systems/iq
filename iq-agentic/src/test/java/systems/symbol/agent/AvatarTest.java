package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.llm.Conversation;
import systems.symbol.platform.IQ_NS;
import systems.symbol.secrets.EnvsAsSecrets;

import javax.script.SimpleBindings;

import java.util.Set;
import java.util.concurrent.Future;

class AvatarTest {

    @Test
    void delegateChat() {
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String openaiApiKey = secrets.getSecret("OPENAI_API_KEY");
        if (openaiApiKey!=null) {
            try {
                IRI self = Values.iri(IQ_NS.TEST);
                DynamicModelFactory dmf = new DynamicModelFactory();
                DynamicModel model = dmf.createEmptyModel();
                Agentic<String, Object> agentic = new Agentic<>(() -> self, new SimpleBindings(), new Conversation());
//                I_Decide<Resource> manager = new AgenticDecision(agentic, gpt);
//                Future<I_Delegate<Resource>> delegated = manager.delegate(new LazyAgent(self,model));
//                Resource decided = delegated.get().decide();
//                assert decided!=null;
//                assert decided.isIRI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void execute() {
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String openaiApiKey = secrets.getSecret("OPENAI_API_KEY");
        if (openaiApiKey!=null) {
            try {
                IRI self = Values.iri(IQ_NS.TEST);
                DynamicModelFactory dmf = new DynamicModelFactory();
                DynamicModel model = dmf.createEmptyModel();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}