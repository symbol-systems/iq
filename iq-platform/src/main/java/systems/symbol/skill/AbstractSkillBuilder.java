package systems.symbol.skill;

import systems.symbol.platform.Platform;

import java.util.Map;

/**
 * Builder class for constructing a QuerySkill instance.
 */
public abstract class AbstractSkillBuilder<T> implements I_SkillBuilder {
    protected final Platform platform;
    Map<String, Object> bindings;
    int cost = 0;

    public AbstractSkillBuilder(Platform platform) {
        this.platform = platform;
    }

    public void incur(int expense) {
        this.cost+=Math.max(expense,0);
    }

    @Override
    public Map<String, Object> getBindings() {
        return bindings;
    }

    public AbstractSkillBuilder<T> withBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
        return this;
    }
}
