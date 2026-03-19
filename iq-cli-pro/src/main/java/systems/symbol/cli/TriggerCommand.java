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
// TODO: Wire-up Apache Camel
} else {
System.out.println("iq.trigger.failed");
}
return null;
}
}
