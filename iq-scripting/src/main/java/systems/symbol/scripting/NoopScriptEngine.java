package systems.symbol.scripting;

public class NoopScriptEngine implements I_ScriptEngine {

@Override
public String execute(String code) {
return code;
}
}
