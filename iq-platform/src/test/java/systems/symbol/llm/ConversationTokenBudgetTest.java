package systems.symbol.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConversationTokenBudgetTest {

@Test
void withTokenBudgetRespectsSoftLimit() {
Conversation conversation = new Conversation();
conversation.system("system prompt");
conversation.user("Hello world");
conversation.assistant("Hi there");
conversation.user("Please explain the process in one simple paragraph.");

I_TokenCounter counter = new WordTokenCounter();
int maxTokens = 20;
Conversation trimmed = Conversation.withTokenBudget(conversation, counter, maxTokens);

// should always include system message
assertEquals("system", trimmed.messages().get(0).getRole().toString());
// we expect at least one user message included; ensure <= 0.8*maxTokens
assertEquals(true, trimmed.tokensUsed(counter) <= maxTokens * 0.8);
assertEquals(true, trimmed.messages().size() >= 2);
}
}
