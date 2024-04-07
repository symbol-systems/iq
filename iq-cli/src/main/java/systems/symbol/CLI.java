package systems.symbol;

import systems.symbol.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.cli.ImportCommand;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static systems.symbol.cli.CLIContext.CODENAME;


/*
 *  systems.symbol - see license
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
@CommandLine.Command(name = "iq", description = "IQ: applied knowledge")
public class CLI implements Callable<Number>  {
	protected static final Logger log = LoggerFactory.getLogger(CLI.class);
	@CommandLine.Option(names = "--home", required = false, description = "Location of workspace files ")
	File home = null;

	public CLI() {
	}

	public CommandLine getCommandLine(CLIContext context) throws IOException, CLIException {
		try {
			CommandLine cli = new CommandLine(this);

			cli.addSubcommand(new InitCommand(context));
			if (context.isInitialized()) {
				cli.addSubcommand(new ListCommand(context));
				cli.addSubcommand(new ScriptCommand(context));
				cli.addSubcommand(new SPARQLCommand(context));
				cli.addSubcommand(new RenderCommand(context));
				cli.addSubcommand(new InferCommand(context));

				cli.addSubcommand(new ImportCommand(context));
				cli.addSubcommand(new ExportCommand(context));
				cli.addSubcommand(new BackupCommand(context));
				cli.addSubcommand(new RecoverCommand(context));
			} else {
				context.display("run `iq init`");
			}

			cli.setAbbreviatedOptionsAllowed(true);
			cli.setAbbreviatedSubcommandsAllowed(true);
			cli.setCaseInsensitiveEnumValuesAllowed(true);
			cli.setExpandAtFiles(true);
			cli.setInterpolateVariables(false);
			return cli;
		} catch (Exception e) {
			log.error("iq.cli.error: {} -> {}", e, e.getStackTrace());
		}
		context.close();
		return null;
	}

	public File getHomeFolder() {
		if (home!=null) return home;
		String HOME_FOLDER = System.getenv("IQ_HOME");
		if (HOME_FOLDER == null || HOME_FOLDER.isEmpty()) HOME_FOLDER= "."+ CODENAME;
		return new File(HOME_FOLDER);
	}



	public static void main(String[] args) {
		CLI cli = new CLI();
		try {
			File home = cli.getHomeFolder();
			log.info("iq.cli.home: {}", home.getAbsolutePath());
			CLIContext context = new CLIContext(home);
			CommandLine commands = cli.getCommandLine(context);
			if (commands==null) {
				System.exit(1);
			}
			int exitCode = commands.execute(args);
			System.exit(exitCode);
		} catch (IOException e) {
			log.error("iq.cli.io.error: {} -> {}", e, e.getStackTrace());
		} catch (CLIException e) {
throw new RuntimeException(e);
}
}

	@Override
	public Number call() throws Exception {
		return 0;
	}
}
