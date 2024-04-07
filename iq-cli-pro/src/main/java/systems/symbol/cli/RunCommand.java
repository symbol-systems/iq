package systems.symbol.cli;


import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "run", description = "Execute an IQ script")
public class RunCommand extends CompositeCommand {
@CommandLine.Parameters(index = "0", description = "The path of the script / query to run.")
String path = "";

public RunCommand(CLIContext context, CommandLine commands) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (context.isInitialized()) {
if (path !=null && !path.isEmpty()) {
//TripleScript scripts = new TripleScript();
//String code = IOUtils.toString(new FileReader(path));
//TripleScriptContext scriptContext = new TripleScriptContext();
//IQ iq = context.newIQBase();
//scriptContext.setBindings(new IQBindings(iq));
//Object result = scripts.execute(code, Files.getFileExtension(path), scriptContext);
//System.out.println(result);
//iq.close();
}
} else {
System.out.println("iq.trigger.failed");
}
return null;
}}
