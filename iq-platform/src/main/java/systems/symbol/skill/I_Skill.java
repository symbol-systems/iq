package systems.symbol.skill;

public interface I_Skill<T> {

    public T perform() throws SkillException;
}
