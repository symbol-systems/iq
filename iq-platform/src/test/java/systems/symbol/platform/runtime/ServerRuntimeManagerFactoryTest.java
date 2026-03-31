package systems.symbol.platform.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerRuntimeManagerFactoryTest {

    @Test
    void testDefaultManagerIsMemory() {
        System.clearProperty("iq.runtime.manager");
        ServerRuntimeManager manager = ServerRuntimeManagerFactory.getInstance();
        assertNotNull(manager);
        assertTrue(manager instanceof InMemoryServerRuntimeManager);
    }

    @Test
    void testQuarkusManagerSelectedByProperty() {
        System.setProperty("iq.runtime.manager", "quarkus");
        // Use a fast command for test reliability
        System.setProperty("iq.runtime.command", "sleep 1");

        ServerRuntimeManager manager = ServerRuntimeManagerFactory.getInstance();
        assertNotNull(manager);
        assertTrue(manager instanceof QuarkusRuntimeManager);

        boolean started = manager.start("api");
        assertTrue(started, "Quarkus manager should start command");

        RuntimeStatus status = manager.health("api");
        assertNotNull(status);
        assertTrue(status.getRuntimeType().contains("api"));

        boolean stopped = manager.stop("api");
        assertTrue(stopped, "Quarkus manager should stop command");
    }
}