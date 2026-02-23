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
}
