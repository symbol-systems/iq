package systems.symbol.render;

import java.util.Map;

public class PicoTemplate {
private final String template;

public PicoTemplate(String template) {
this.template = template;
}

public String translate(Map<String, Object> headers) {
return template;
}

@Override
public String toString() {
return template;
}
}
