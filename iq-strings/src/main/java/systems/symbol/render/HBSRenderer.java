package systems.symbol.render;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HBSRenderer implements Renderer {
protected static Handlebars hbs = new Handlebars();

public HBSRenderer() {
}

@Override
public void render(String raw_template, Map<String, Object> attributes, OutputStream out) throws IOException {
Template template = hbs.compileInline(raw_template);
template.apply(attributes,new OutputStreamWriter(out, StandardCharsets.UTF_8));
out.flush();
}

public static void writer(String raw_template, Map<String, Object> attributes, Writer out) throws IOException {
Template template = hbs.compileInline(raw_template);
template.apply(attributes, out);
out.flush();
}

public static String template(String raw_template, Map<String, Object> attributes) throws IOException {
StringWriter stringWriter = new StringWriter();
Template template = hbs.compileInline(raw_template);
template.apply(attributes, stringWriter);
return stringWriter.toString();
}
}
