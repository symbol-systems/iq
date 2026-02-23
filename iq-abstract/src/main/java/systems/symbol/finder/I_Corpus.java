package systems.symbol.finder;

public interface I_Corpus<T> {

    I_Search<I_Found<T>> byConcept(T concept);
}
