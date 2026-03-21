package systems.symbol.cli;

import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "trigger", description = "Trigger an event to invoke a set of actions")
public class TriggerCommand extends AbstractCLICommand {
    public TriggerCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (context.isInitialized()) {
            log.info("iq.cli.trigger.start: {}", context.getSelf());
            // future: dispatch event through Camel/AgentService
            log.info("iq.cli.trigger.done: event fired");
            return "triggered";
        } else {
            System.out.println("iq.trigger.failed");
            throw new CLIException("IQ not initialized");
        }
    }
}
