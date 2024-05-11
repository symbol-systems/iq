package systems.symbol.decide;
public interface I_Guard<T> {
    boolean allows(T actor, T intent);
}
