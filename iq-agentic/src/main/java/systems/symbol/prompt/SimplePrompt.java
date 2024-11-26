package systems.symbol.prompt;

import systems.symbol.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.TextMessage;

import javax.script.Bindings;
import java.io.IOException;

public class SimplePrompt extends AbstractPrompt<String> {
    StringBuilder msg = new StringBuilder();
    I_LLMessage.RoleType role;

    public SimplePrompt(String msg, Bindings my) {
        this(I_LLMessage.RoleType.system, msg, my);
    }

    public SimplePrompt(I_LLMessage.RoleType role, String msg, Bindings my) {
        super(my);
        this.role = role;
        this.msg.append(msg);
    }

    public void append(String m) {
        this.msg.append(m);
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        String txt = bind(msg.toString());
        // log.info("prompt.complete: {}", txt);
        chat.add(new TextMessage(role, txt));
        return chat;
    }
}
