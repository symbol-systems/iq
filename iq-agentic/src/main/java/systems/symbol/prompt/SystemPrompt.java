package systems.symbol.prompt;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.io.IOException;

public class SystemPrompt extends AbstractPrompt<String> {
    String msg;

    public SystemPrompt(String msg, Bindings my) {
        super(my);
        this.msg = msg;
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        chat.system(msg);
        return chat;
    }
}
