package systems.symbol.finder;

import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;

public interface I_Found<T> extends I_Delegate<T> {
    double score();
}
