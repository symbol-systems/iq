package systems.symbol.util;

import java.util.Map;

public class IRITemplate {
private final String template;
private final Map<String, Object> config;

public IRITemplate(String template, Map<String, Object> config) {
this.template = template;
this.config = config;
}

public static boolean isTemplated(String uri) {
return uri != null && uri.contains("{") && uri.contains("}");
}

@Override
public String toString() {
return template;
}
}
