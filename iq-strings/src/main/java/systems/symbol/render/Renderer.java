package systems.symbol.render;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Renderer {

void render(String template, Map<String, Object> attributes, OutputStream out) throws IOException;
}
