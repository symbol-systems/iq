package systems.symbol.cli;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class TestCLIExecutor {

    private File home;
    private I_Kernel kernel;
    private CLIContext context;

    @BeforeEach
    public void setup() throws Exception {
        home = Files.createTempDirectory("iq-cli-executor").toFile();
        home.deleteOnExit();

        kernel = KernelBuilder.create().withHome(home).build();
        kernel.start();
        context = new CLIContext(kernel);
        assertNotNull(context);
    }

    @AfterEach
    public void teardown() {
        if (context != null) {
            context.close();
        }
        if (kernel != null) {
            kernel.stop();
        }
    }

    @Test
    public void inferCommandExecutesSparqlInsert() throws Exception {
        File script = new File(home, "infer/index.sparql");
        script.getParentFile().mkdirs();
        Files.writeString(script.toPath(), "INSERT DATA { <urn:test> <urn:p> \"x\" }", StandardCharsets.UTF_8);

        InferCommand inferCommand = new InferCommand(context);
        Object result = inferCommand.call();
        assertEquals(0, result);

        try (RepositoryConnection conn = context.getRepository().getConnection()) {
            boolean exists = conn.prepareBooleanQuery("ASK { <urn:test> <urn:p> \"x\" }").evaluate();
            assertTrue(exists, "Triple inserted by infer command should exist");
        }
    }

    @Test
    public void renderCommandDoesNotFailWhenNoModels() throws Exception {
        RenderCommand renderCommand = new RenderCommand(context);
        Object result = renderCommand.call();
        assertEquals(0, result);
    }

    @Test
    public void agentCommandListAndTriggerStubWorks() throws Exception {
        AgentCommand agentCommand = new AgentCommand(context);
        Object result = agentCommand.call();
        assertEquals(0, result);

        AgentCommand triggerCommand = new AgentCommand(context);
        triggerCommand.actor = "dummy";
        triggerCommand.intent = "dummy";
        triggerCommand.trigger = true;
        result = triggerCommand.call();
        assertEquals(0, result);
    }
}
