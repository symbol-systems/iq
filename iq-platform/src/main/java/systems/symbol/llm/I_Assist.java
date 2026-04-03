package systems.symbol.llm;

import java.util.List;

public interface I_Assist<T> {
public I_Assist<T> add(I_LLMessage<T> msg);

public List<I_LLMessage<T>> messages();

I_LLMessage<T> latest();

public void add(I_Assist<T> chat, int window);

void system(T s);

void assistant(T s);

void user(T s);

default int tokensUsed(I_TokenCounter counter) {
if (counter == null) {
return 0;
}
int total = 0;
for (I_LLMessage<T> msg : messages()) {
if (msg == null || msg.getContent() == null) continue;
total += counter.count(msg.getContent().toString());
}
return total;
}
}
