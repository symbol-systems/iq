package systems.symbol.agent;

import org.junit.jupiter.api.Test;
import systems.symbol.secrets.EnvsAsSecrets;

class AvatarTest {

    @Test
    void delegateChat() {
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String OPENAI_API_KEY = secrets.getSecret("OPENAI_API_KEY");
        if (OPENAI_API_KEY != null) {
            try {
                // IRI self = Values.iri(IQ_NS.TEST);
                // DynamicModelFactory dmf = new DynamicModelFactory();
                // DynamicModel model = dmf.createEmptyModel();
                // Agentic<String, Object> agentic = new Agentic<>(() -> self, new
                // SimpleBindings(), new Conversation());
                // I_Decide<Resource> manager = new AgenticDecision(agentic, gpt);
                // Future<I_Delegate<Resource>> delegated = manager.delegate(new
                // LazyAgent(self,model));
                // Resource decided = delegated.get().decide();
                // assert decided!=null;
                // assert decided.isIRI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}