package systems.symbol.llm;

import java.util.List;

public interface I_TokenCounter {

/**
 * Count tokens in an arbitrary text string.
 */
int count(String text);

/**
 * Count tokens in a list of LL messages (skip nulls).
 */
default int count(List<I_LLMessage<String>> messages) {
if (messages == null || messages.isEmpty()) {
return 0;
}
int total = 0;
for (I_LLMessage<String> msg : messages) {
if (msg == null || msg.getContent() == null) {
continue;
}
total += count(msg.getContent());
}
return total;
}
}
