package systems.symbol.cli;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import picocli.CommandLine;
import systems.symbol.platform.I_Self;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "about", description = "About this "+ I_Self.CODENAME)
public class AboutCommand extends AbstractCLICommand {

public AboutCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() {
if (context.isInitialized()) {
displayNamespaces(context);
displaySelf(context);
} else {
System.out.println("IQ not initialized");
}
return null;
}


public static void displayNamespaces(CLIContext context) {
Column[] columns = {
new Column().header("prefix").headerAlign(HorizontalAlign.RIGHT).with(Namespace::getPrefix),
new Column().header("namespace").headerAlign(HorizontalAlign.LEFT).with(Namespace::getName)
};
try (RepositoryConnection conn = context.getRepository().getConnection()) {
List<String[]> rows = new ArrayList<>();
conn.getNamespaces().forEach(ns -> {
String[] row = { ns.getPrefix(), ns.getName()};
rows.add(row);
});
System.out.println(AsciiTable.getTable(columns, rows.toArray(String[][]::new)));
System.out.println();
}
}

public static void displaySelf(CLIContext context) {
try (RepositoryConnection conn = context.getRepository().getConnection()) {
String[] columns = { "workspace", "identity", "size" };
Object[] row = { context.workspace.getCurrentRepositoryName(), context.getSelf(), conn.size() };
System.out.println(AsciiTable.getTable(columns, new Object[][] { row }));
System.out.println();
}
System.out.printf("%s folder: %s\n", CODENAME, context.home.getAbsolutePath());
;

}

}
