package systems.symbol.skill;

import java.util.Map;

public interface I_SkillBuilder {

    public Map<String, Object> getBindings();
    public I_Skill build() throws SkillException;
}
