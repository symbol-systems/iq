package systems.symbol.llm.gpt;

import org.junit.jupiter.api.Test;
import systems.symbol.tools.APIException;
import systems.symbol.llm.Conversation;

import java.io.IOException;

class GenericGPTTest {

    @Test
    void generateLLMResponse() throws APIException, IOException {
        String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
        if (OPENAI_API_KEY != null) {
            try {
                GenericGPT ai = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));
                Conversation chat = new Conversation();
                chat.user("hello");
                System.out.println("gpt.llm.openai.messages: " + chat.messages());
                ai.complete(chat);
                System.out.println("gpt.llm.openai: " + chat);
                assert !chat.messages().isEmpty();
            } catch (java.net.UnknownHostException e) {
                System.out.println("gpt.llm.openai.offline");
            }
        }
    }
}