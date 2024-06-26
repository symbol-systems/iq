package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.LazyAgent;
import systems.symbol.decide.I_Delegate;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.rdf4j.store.SelfModel;
import systems.symbol.secrets.EnvsAsSecrets;

import javax.script.SimpleBindings;

import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class AvatarTest {

    @Test
    void delegate() {
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String openaiApiKey = secrets.getSecret("OPENAI_API_KEY");
        if (openaiApiKey!=null) {
            try {
                GenericGPT llm = new GenericGPT(openaiApiKey, 100);
                IRI self = Values.iri(IQ_NS.TEST);
                DynamicModelFactory dmf = new DynamicModelFactory();
                DynamicModel model = dmf.createEmptyModel();
                Avatar avatar = new Avatar(self, model, model, llm, new SimpleBindings(), secrets);
                Future<I_Delegate<Resource>> delegated = avatar.delegate(new LazyAgent(self, model));

                Resource decided = delegated.get().decide();
                assert decided!=null;
                assert decided.isIRI();
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
                GenericGPT llm = new GenericGPT(openaiApiKey, 100);
                IRI self = Values.iri(IQ_NS.TEST);
                DynamicModelFactory dmf = new DynamicModelFactory();
                DynamicModel model = dmf.createEmptyModel();
                Avatar avatar = new Avatar(self, model, model, llm, new SimpleBindings(), secrets);
                Set<IRI> executed = avatar.execute(self, self, new SimpleBindings());
                assert !executed.isEmpty();
                assert executed.contains(self);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}