package systems.symbol;

import systems.symbol.cli.*;
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
        try {
            File home = cli.getHomeFolder();
            CLIContext context = new CLIContext(home);
            CommandLine commands = cli.getCommandLine(context);
            commands.addSubcommand(new AboutCommand(context));

            if (context.isInitialized()) {
                // commands.addSubcommand(new ModelsCommand(home, commands));
                // commands.addSubcommand(new RunCommand(home, commands));
                // commands.addSubcommand(new TriggerCommand(home, commands));
                commands.addSubcommand(new TrustCommand(context));
                commands.addSubcommand(new BootCommand(context));
            }

            int exitCode = commands.execute(args);
            context.close();
            System.exit(exitCode);
        } catch (IOException e) {
            log.error("iq.cli.pro.io: {} -> {}", e.getMessage(), e.getStackTrace());
        } catch (CLIException e) {
            log.error("iq.cli.pro.iq: {} -> {}", e.getMessage(), e.getStackTrace());
        } catch (Exception e) {
            log.error("iq.cli.pro.fatal: {} -> {}", e.getMessage(), e.getStackTrace());
        }
    }

    @Override
    public Number call() throws Exception {
        // System.out.println("help");
        return 0;
    }
}
