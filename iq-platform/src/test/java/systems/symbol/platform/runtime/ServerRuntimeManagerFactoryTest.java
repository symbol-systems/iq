package systems.symbol.platform.runtime;

import org.junit.jupiter.api.Test;

import systems.symbol.runtime.RuntimeStatus;
import systems.symbol.runtime.ServerRuntimeManager;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ServerRuntimeManagerFactoryTest {

@Test
void testDefaultManagerExists() {
System.clearProperty("iq.runtime.manager");
ServerRuntimeManager manager = ServerRuntimeManagerFactory.getInstance();
assertNotNull(manager);
assertTrue(manager instanceof ServerRuntimeManager);
}

@Test
void testQuarkusManagerProperty() {
System.setProperty("iq.runtime.manager", "quarkus");
ServerRuntimeManager manager = ServerRuntimeManagerFactory.getInstance();
assertNotNull(manager);
assertTrue(manager instanceof ServerRuntimeManager);

RuntimeStatus status = manager.health("api");
assertNotNull(status);
assertNotNull(status.getDetails());
}
}
