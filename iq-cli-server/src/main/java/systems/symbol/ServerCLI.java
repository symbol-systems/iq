package systems.symbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.cli.CLIContext;
import systems.symbol.cli.CLIException;
import systems.symbol.cli.server.ServerCommand;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;

@CommandLine.Command(name = "iq-server", description = "IQ Server lifecycle operations")
public class ServerCLI implements Runnable {

    protected static final Logger log = LoggerFactory.getLogger(ServerCLI.class);

    @CommandLine.Option(names = "--home", required = false, description = "Workspace home directory")
    File home = null;

    public static void main(String[] args) {
        RuntimeBanner.print();
        ServerCLI cli = new ServerCLI();
        I_Kernel kernel = null;
        CLIContext context = null;
        try {
            File home = cli.getHomeFolder();
            log.info("iq-server.home: {}", home.getAbsolutePath());
            kernel = KernelBuilder.create().withHome(home).build();
            kernel.start();
            context = new CLIContext(kernel);
            CommandLine cmd = new CommandLine(cli);
            cmd.addSubcommand("server", new ServerCommand(context));

            int exitCode = cmd.execute(args);
            System.exit(exitCode);
        } catch (Exception e) {
            log.error("iq-server.fatal: {}", e.getMessage(), e);
            System.exit(4);
        } finally {
            if (context != null) {
                context.close();
            } else if (kernel != null) {
                try { kernel.stop(); } catch (Exception ignored) {}
            }
        }
    }

    public File getHomeFolder() {
        if (home != null) {
            return home;
        }
        String HOME_FOLDER = System.getenv("IQ_HOME");
        if (HOME_FOLDER == null || HOME_FOLDER.isEmpty()) {
            HOME_FOLDER = ".iq";
        }
        return new File(HOME_FOLDER);
    }

    @Override
    public void run() {
        System.out.println("Use `iq-server server --help` for commands.");
    }
}
