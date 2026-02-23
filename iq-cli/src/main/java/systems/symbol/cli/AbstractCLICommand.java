package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractCLICommand implements Callable<Object> {
protected static final Logger log = LoggerFactory.getLogger(AbstractCLICommand.class);

CLIContext context;

public AbstractCLICommand(CLIContext context) throws IOException {
this.context = context;
}

public void display(String msg) {
System.out.println(msg);
}
}
