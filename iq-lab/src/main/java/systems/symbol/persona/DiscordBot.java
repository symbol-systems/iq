package systems.symbol.persona;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class DiscordBot extends ListenerAdapter {
private final Model model;
private final Map<String, IRI> namespaces;
private final Map<String, Resource> userSubjects;
private final String defaultNamespace;

public DiscordBot(Model model, String defaultNamespace) {
this.model = model;
this.namespaces = new HashMap<>();
for (Namespace ns : model.getNamespaces()) {
this.namespaces.put(ns.getPrefix(), Values.iri(ns.getName()));
}
this.defaultNamespace = defaultNamespace;
this.userSubjects = new HashMap<>();
}

public static void main(String[] args) throws InterruptedException {
String botToken = System.getenv("MY_DISCORD_BOT_TOKEN");
System.out.println("token: "+botToken);
if (botToken==null) return;

Model model = new LinkedHashModel();
String qrx = "https://QRX.agency/";
String cmd = "qrx";

IRI root = Values.iri(qrx);
IRI knows = Values.iri(qrx, "knows");
IRI name = Values.iri(qrx, "name");
IRI age = Values.iri(qrx, "age");
IRI person1 = Values.iri(qrx, "Person1");
IRI person2 = Values.iri(qrx, "Person2");

model.add(root, knows, person1);
model.add(person1, name, Values.***REMOVED***("Alice"));
model.add(person1, age, Values.***REMOVED***("30"));
model.add(person2, name, Values.***REMOVED***("Bob"));
model.add(person2, age, Values.***REMOVED***("25"));

model.setNamespace(cmd, qrx);
DiscordBot bot = new DiscordBot(model, qrx);

JDA jda = JDABuilder.createLight(botToken, EnumSet.noneOf(GatewayIntent.class))
.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,GatewayIntent.GUILD_PRESENCES,GatewayIntent.DIRECT_MESSAGES)
.addEventListeners(bot)
.build();

CommandListUpdateAction commands = jda.updateCommands();
CommandListUpdateAction command = commands.addCommands(
Commands.slash(cmd, cmd)
.addOption(STRING, "command", qrx, true)
);
System.out.println("listening: "+command);
commands.queue(
success -> System.out.println("ok"),
failure -> System.out.println("oops: " + failure.getMessage())
);
CountDownLatch latch = new CountDownLatch(1);
Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
System.out.println("waiting ...");
latch.await();

System.out.println("stopped");
}

@Override
public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
String command = event.getName();
System.out.println("command: "+command);
if (namespaces.containsKey(command)) {
handleRdfCommand(event, command);
} else if (event.getName().equals(defaultNamespace)) {
String rdfCommand = event.getOption("command").getAsString();
handleRdfCommand(event, rdfCommand);
}
}

private void handleRdfCommand(SlashCommandInteractionEvent event, String input) {
String userId = event.getUser().getId();
String[] parts = input.split("\\s+", 3);

if (parts.length == 1) {
Resource subject = resolveSubject(parts[0]);
userSubjects.put(userId, subject);
printPredicates(event.getHook(), subject);
} else if (parts.length == 2) {
IRI subject = (IRI) userSubjects.getOrDefault(userId, resolveSubject(parts[0]));
IRI predicate = resolvePredicate(parts[1]);
userSubjects.put(userId, subject);
printObjects(event.getHook(), subject, predicate);
} else if (parts.length == 3) {
IRI subject = resolveSubject(parts[0]);
IRI predicate = resolvePredicate(parts[1]);
Value object = resolveObject(parts[2]);
model.add(subject, predicate, object);
String reply= subject.getLocalName() + " " + predicate.getLocalName() + " " + (object instanceof IRI ? ((IRI) object).getLocalName() : object.stringValue());
event.reply(reply ).setEphemeral(true).queue();
}
}

private IRI resolveSubject(String input) {
if (namespaces.containsKey(input)) {
return namespaces.get(input);
} else if (input.contains(":")) {
return resolveIri(input);
} else {
return Values.iri(defaultNamespace, input);
}
}

private IRI resolvePredicate(String input) {
return resolveIri(input);
}

private Value resolveObject(String input) {
if (input.startsWith("\"") && input.endsWith("\"")) {
return Values.***REMOVED***(input.substring(1, input.length() - 1));
} else if (input.contains(":")) {
return resolveIri(input);
} else {
return Values.iri(defaultNamespace, input);
}
}

private IRI resolveIri(String input) {
if (input.contains(":")) {
String[] parts = input.split(":");
IRI namespace = namespaces.get(parts[0]);
if (namespace != null) {
return Values.iri(namespace.stringValue(), parts[1]);
}
}
return Values.iri(defaultNamespace, input);
}

private void printPredicates(InteractionHook hook, Resource subject) {
Set<IRI> predicates = new HashSet<>();
for (Statement stmt : model.filter(subject, null, null)) {
if (stmt.getPredicate() != null) {
predicates.add(stmt.getPredicate());
}
}

StringBuilder response = new StringBuilder();
for (IRI predicate : predicates) {
response.append(predicate.getLocalName()).append("\n");
}
hook.sendMessage(response.toString().trim()).queue();
}

private void printObjects(InteractionHook hook, Resource subject, IRI predicate) {
Set<Value> objects = new HashSet<>();
for (Statement stmt : model.filter(subject, predicate, null)) {
objects.add(stmt.getObject());
}

StringBuilder response = new StringBuilder();
for (Value object : objects) {
response.append(object instanceof IRI ? ((IRI) object).getLocalName() : object.stringValue()).append("\n");
}
hook.sendMessage(response.toString().trim()).queue();
}
}
