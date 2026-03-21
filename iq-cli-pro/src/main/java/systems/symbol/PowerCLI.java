package systems.symbol;

import systems.symbol.cli.*;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
public class PowerCLI extends CLI {
    protected static final Logger log = LoggerFactory.getLogger(PowerCLI.class);

    public static void main(String[] args) {
        PowerCLI cli = new PowerCLI();
        I_Kernel kernel = null;
        CLIContext context = null;
        try {
            File home = cli.getHomeFolder();
            kernel = KernelBuilder.create().withHome(home).build();
            kernel.start();
            context = new CLIContext(kernel);
            CommandLine commands = cli.getCommandLine(context);
            commands.addSubcommand(new AboutCommand(context));

            if (context.isInitialized()) {
                commands.addSubcommand(new ModelsCommand(context, commands));
                commands.addSubcommand(new RunCommand(context, commands));
                commands.addSubcommand(new TriggerCommand(context));
                commands.addSubcommand(new systems.symbol.cli.server.ServerCommand(context));
                commands.addSubcommand(new TrustCommand(context));
                commands.addSubcommand(new BootCommand(context));
            }

            int exitCode = commands.execute(args);
            System.exit(exitCode);
        } catch (IOException e) {
            log.error("iq.cli.pro.io: {} -> {}", e.getMessage(), e.getStackTrace());
            System.exit(2);
        } catch (CLIException e) {
            log.error("iq.cli.pro.iq: {} -> {}", e.getMessage(), e.getStackTrace());
            System.exit(3);
        } catch (Exception e) {
            log.error("iq.cli.pro.fatal: {} -> {}", e.getMessage(), e.getStackTrace());
            System.exit(4);
        } finally {
            if (context != null) context.close();
            else if (kernel != null) {
                try { kernel.stop(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public Number call() throws Exception {
        // System.out.println("help");
        return 0;
    }

    @Override
    public CommandLine getCommandLine(CLIContext context) throws IOException, CLIException {
        CommandLine commands = super.getCommandLine(context);
        if (context.isInitialized()) {
            if (!commands.getSubcommands().containsKey("models")) {
                commands.addSubcommand(new ModelsCommand(context, commands));
            }
            if (!commands.getSubcommands().containsKey("run")) {
                commands.addSubcommand(new RunCommand(context, commands));
            }
            if (!commands.getSubcommands().containsKey("trigger")) {
                commands.addSubcommand(new TriggerCommand(context));
            }
            if (!commands.getSubcommands().containsKey("server")) {
                commands.addSubcommand(new systems.symbol.cli.server.ServerCommand(context));
            }
            if (!commands.getSubcommands().containsKey("trust")) {
                commands.addSubcommand(new TrustCommand(context));
            }
            if (!commands.getSubcommands().containsKey("boot")) {
                commands.addSubcommand(new BootCommand(context));
            }
        }
        return commands;
    }
}
