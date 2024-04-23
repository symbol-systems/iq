package systems.symbol.llm.openai;

import org.junit.jupiter.api.Test;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.ChatThread;

import java.io.IOException;

class ChatGPTTest {

    @Test
    void generateLLMResponse() throws APIException, IOException {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        if (openaiApiKey!=null) {
            try {
                ChatGPT ai = new ChatGPT(openaiApiKey, 100);
                ChatThread chat = new ChatThread();
                chat.user( "hello");
                System.out.println("agent.llm.openai.messages: "+chat.messages());
                ai.complete(chat);
                System.out.println("agent.llm.openai: "+chat);
                assert !chat.messages().isEmpty();
            } catch (java.net.UnknownHostException e) {
                System.out.println("agent.llm.openai.offline");
            }
        }
    }
}