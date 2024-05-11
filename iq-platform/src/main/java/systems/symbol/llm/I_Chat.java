package systems.symbol.llm;


import java.util.List;

public interface I_Chat<T> {
public I_Chat<T> add(I_LLMessage<T> msg);
public List<I_LLMessage<T>> messages();
I_LLMessage<T> latest();

void system(T s);
void assistant(T s);
void user(T s);
}
