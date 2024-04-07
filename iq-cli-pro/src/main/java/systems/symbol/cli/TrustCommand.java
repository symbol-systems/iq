package systems.symbol.cli;

import picocli.CommandLine;

import java.io.IOException;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "trust", description = "Export this "+CODENAME+" to another.")
public class TrustCommand extends AbstractCLICommand{
@CommandLine.Parameters(index = "0", description = "The identity of the agent you want to trust", defaultValue = "me")
String identity = "me";

public TrustCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (context.isInitialized()) {
System.out.println("iq.trusts: " + identity);
if (identity.equalsIgnoreCase("me")) {
trustSelf();
} else {
trustRemote(identity);
}
}
return null;
}

private void trustRemote(String identity) {
}

private void trustSelf() {

}
}
