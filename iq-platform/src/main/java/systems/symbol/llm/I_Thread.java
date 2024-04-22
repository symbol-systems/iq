package systems.symbol.llm;


import java.util.List;

public interface I_Thread<T> {
    public I_Thread<T> add(I_LLMessage<T> msg);
    public List<I_LLMessage<T>> messages();
    I_LLMessage<T> latest();

    void system(T s);
    void assistant(T s);
    void user(T s);
}
