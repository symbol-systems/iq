package systems.symbol.cli;

import picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import systems.symbol.rdf4j.store.IQStore;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * CLI Command: Manage realms (multi-tenancy).
 * 
 * Usage:
 *   iq realm list # List all realms
 *   iq realm show <iri>   # Show realm details
 *   iq realm create <name># Create new realm
 *   iq realm delete <iri> # Delete realm
 *   iq realm config show  # Show config
 */
@CommandLine.Command(name = "realm", description = "Manage realms and multi-tenancy")
public class RealmCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(RealmCommand.class);

@CommandLine.Parameters(index = "0", arity = "0..1", description = "action: list | show | create | delete | config")
String action = "list";

@CommandLine.Parameters(index = "1", arity = "0..1", description = "Realm IRI or name")
String param = null;

public RealmCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
displayError("Error: IQ context not initialized");
return null;
}

try {
switch (action.toLowerCase()) {
case "list":
return listRealms();
case "show":
return showRealm();
case "create":
return createRealm();
case "delete":
return deleteRealm();
case "config":
return configRealm();
default:
displayError("Unknown action: " + action);
return null;
}
} catch (Exception e) {
log.error("Error: {}", e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
}
}

private Object listRealms() throws Exception {
var store = context.getRepository();
if (store == null) {
displayError("Repository not initialized");
return null;
}

try (var conn = store.getConnection()) {
display("Realms");
display("-".repeat(70));

// Query all realms with their labels and creation timestamps
String sparql = """
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iq: <urn:iq:>

SELECT ?realm ?label ?created ?status WHERE {
?realm a iq:Realm .
OPTIONAL { ?realm rdfs:label ?label }
OPTIONAL { ?realm iq:createdAt ?created }
OPTIONAL { ?realm iq:status ?status }
}
ORDER BY ?label
LIMIT 1000
""";

var query = conn.prepareTupleQuery(sparql);
int count = 0;

try (TupleQueryResult result = query.evaluate()) {
while (result.hasNext()) {
BindingSet binding = result.next();

String realm = binding.getValue("realm").stringValue();
String label = binding.getValue("label") != null ?
binding.getValue("label").stringValue() :
realm.substring(realm.lastIndexOf("/") + 1);
String status = binding.getValue("status") != null ?
binding.getValue("status").stringValue() :
"active";

displayf("  ✓ %-30s [%s]%n", label, status);
count++;
}
}

display();
displayf("Total: %d realm(s)%n", count);
log.info("realm.list: {} realms found", count);
return "realms:listed:" + count;
} catch (Exception e) {
log.error("Error listing realms", e);
displayError("Error: " + e.getMessage());
return null;
}
}

private Object showRealm() throws Exception {
if (param == null) {
displayError("Realm IRI or name required");
return null;
}

var store = context.getRepository();
if (store == null) {
displayError("Repository not initialized");
return null;
}

try (var conn = store.getConnection()) {
String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"PREFIX iq: <urn:iq:>\n" +
"\n" +
"SELECT ?realm ?label ?owner ?created ?configPath WHERE {\n" +
"  VALUES ?realm { <" + param + "> }\n" +
"  ?realm a iq:Realm .\n" +
"  OPTIONAL { ?realm rdfs:label ?label }\n" +
"  OPTIONAL { ?realm iq:owner ?owner }\n" +
"  OPTIONAL { ?realm iq:createdAt ?created }\n" +
"  OPTIONAL { ?realm iq:configPath ?configPath }\n" +
"}";

var query = conn.prepareTupleQuery(sparql);

try (TupleQueryResult result = query.evaluate()) {
if (!result.hasNext()) {
displayError("Realm not found: " + param);
return null;
}

BindingSet binding = result.next();
String label = binding.getValue("label") != null ?
binding.getValue("label").stringValue() : "N/A";
String owner = binding.getValue("owner") != null ?
binding.getValue("owner").stringValue() : "system";
String created = binding.getValue("created") != null ?
binding.getValue("created").stringValue() : "N/A";

display("Realm Details");
display("-".repeat(70));
displayf("  IRI:  %s%n", param);
displayf("  Label:%s%n", label);
displayf("  Owner:%s%n", owner);
displayf("  Created:  %s%n", created);
displayf("  Status:   ACTIVE%n");

log.info("realm.show: {}", param);
return "realm:shown:" + param;
}
} catch (Exception e) {
log.error("Error showing realm", e);
displayError("Error: " + e.getMessage());
return null;
}
}

private Object createRealm() throws Exception {
if (param == null) {
displayError("Realm name required");
return null;
}

// Validate realm name (alphanumeric + hyphens)
if (!param.matches("^[a-z0-9-]+$")) {
displayError("Realm name must contain only lowercase letters, numbers, and hyphens");
return null;
}

var store = context.getRepository();
if (store == null) {
displayError("Repository not initialized");
return null;
}

try (var conn = store.getConnection()) {
String realmIRI = "urn:iq:realm:" + param;

// Check if realm already exists
String checkQuery = "PREFIX iq: <urn:iq:>\n" +
"ASK {\n" +
"  <" + realmIRI + "> a iq:Realm\n" +
"}";

var ask = conn.prepareBooleanQuery(checkQuery);
if (ask.evaluate()) {
displayError("Realm already exists: " + param);
return null;
}

// Create realm
display("Creating realm: " + param);

String insertQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"PREFIX iq: <urn:iq:>\n" +
"\n" +
"INSERT DATA {\n" +
"  <" + realmIRI + "> \n" +
"a iq:Realm ;\n" +
"rdfs:label \"" + param + "\" ;\n" +
"iq:createdAt \"" + System.currentTimeMillis() + "\"^^<http://www.w3.org/2001/XMLSchema#long> ;\n" +
"iq:status \"active\" .\n" +
"}";

var update = conn.prepareUpdate(insertQuery);
update.execute();

display("  ✓ Realm created successfully");
displayf("  IRI: %s%n", realmIRI);

log.info("realm.create: {} as {}", param, realmIRI);
return "realm:created:" + realmIRI;
} catch (Exception e) {
log.error("Error creating realm", e);
displayError("Error: " + e.getMessage());
return null;
}
}

private Object deleteRealm() throws Exception {
if (param == null) {
displayError("Realm IRI required");
return null;
}

var store = context.getRepository();
if (store == null) {
displayError("Repository not initialized");
return null;
}

try (var conn = store.getConnection()) {
// Verify realm exists
String checkQuery = "PREFIX iq: <urn:iq:>\n" +
"ASK {\n" +
"  <" + param + "> a iq:Realm\n" +
"}";

var ask = conn.prepareBooleanQuery(checkQuery);
if (!ask.evaluate()) {
displayError("Realm not found: " + param);
return null;
}

display("Deleting realm: " + param);

// Delete all triples for this realm
String deleteQuery = "PREFIX iq: <urn:iq:>\n" +
"DELETE {\n" +
"  <" + param + "> ?p ?o\n" +
"}\n" +
"WHERE {\n" +
"  <" + param + "> ?p ?o\n" +
"}";

var update = conn.prepareUpdate(deleteQuery);
update.execute();

display("  ✓ Realm deleted successfully");
log.info("realm.delete: {}", param);
return "realm:deleted:" + param;
} catch (Exception e) {
log.error("Error deleting realm", e);
displayError("Error: " + e.getMessage());
return null;
}
}

private Object configRealm() throws Exception {
display("Realm Configuration");
display("-".repeat(70));
display("  Backend:  RDF-backed (RDF4J with persistent store)");
display("  Storage:  " + (context.getRepository() != null ? "INITIALIZED" : "NOT INITIALIZED"));
display("  Isolation:Realm-based (SPARQL FILTER on caller realm)");
display("  Auth: JWT tokens in ~/.iq/tokens/<realm>.jwt");
display("  Audit:mcp:audit named graph");
display("  Quota:Enforced per principal per hour");
display();
log.info("realm.config.show");
return "config:shown";
}
}
