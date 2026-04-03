package systems.symbol.scripting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoopScriptEngineTest {

@Test
void testExecuteReturnsCode() {
var engine = new NoopScriptEngine();
assertEquals("1+1", engine.execute("1+1"));
}
}
