package systems.symbol.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuarkusRuntimeManagerTest {

@Test
void testStatus() {
var manager = new QuarkusRuntimeManager();
assertEquals("OK", manager.status());
}
}
