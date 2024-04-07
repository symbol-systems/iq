package systems.symbol.llm.openai;

import systems.symbol.agent.apis.APIException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_Thread;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ChatGPTTest {

    @Test
    void generateLLMResponse() throws APIException, IOException {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        if (openaiApiKey!=null) {
            try {
                ChatGPT ai = new ChatGPT(openaiApiKey, 100);
                ChatThread chat = new ChatThread();
                chat.add("me", "user", "hello");
                System.out.println("agent.llm.openai.messages: "+chat.messages());
                I_Thread<String> generated = ai.generate(chat);
                assert null != generated;
                System.out.println("agent.llm.openai: "+generated);
                assert !generated.messages().isEmpty();
            } catch (java.net.UnknownHostException e) {
                System.out.println("agent.llm.openai.offline");
            }
        }
    }
}