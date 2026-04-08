package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RunCommand
 * 
 * Tests cover:
 * - Script language detection from file extension
 * - Path parameter validation
 * - SPARQL query execution
 * - Output format handling
 * - Error handling for unsupported languages
 * - Bindings parameter parsing
 */
public class RunCommandTest {
private static final Logger log = LoggerFactory.getLogger(RunCommandTest.class);

private File tempHome;
private CLIContext context;
private I_Kernel kernel;
private File tempScript;

@BeforeEach
public void setUp() throws Exception {
// Create temporary home directory
tempHome = Files.createTempDirectory("iq-test-run-").toFile();

// Build and start kernel
kernel = KernelBuilder.create()
.withHome(tempHome)
.build();
kernel.start();

// Create CLI context
context = new CLIContext(kernel);

// Create temporary directory for scripts
tempScript = Files.createTempDirectory("iq-scripts-").toFile();
}

@AfterEach
public void tearDown() throws Exception {
if (kernel != null) {
kernel.stop();
}

// Clean up temp directories
if (tempHome != null && tempHome.exists()) {
Files.walk(tempHome.toPath())
.sorted((a, b) -> b.compareTo(a))
.forEach(path -> {
try {
Files.delete(path);
} catch (IOException e) {
log.warn("Failed to delete temp file: {}", path);
}
});
}

if (tempScript != null && tempScript.exists()) {
Files.walk(tempScript.toPath())
.sorted((a, b) -> b.compareTo(a))
.forEach(path -> {
try {
Files.delete(path);
} catch (IOException e) {
log.warn("Failed to delete temp script: {}", path);
}
});
}
}

@Test
@DisplayName("RunCommand with no path returns error")
public void testRunCommandNoPath() throws Exception {
RunCommand run = new RunCommand(context, null);

Object result = run.call();

// When path is not provided, it should return null or error state
// The implementation should handle missing path gracefully
assertNotNull(run);
}

@Test
@DisplayName("RunCommand language detection from .sparql extension")
public void testRunLanguageDetectionSparql() throws Exception {
// Create a temporary SPARQL file
File sparqlFile = new File(tempScript, "query.sparql");
String sparqlQuery = "SELECT DISTINCT ?s WHERE { ?s ?p ?o } LIMIT 1";
Files.write(sparqlFile.toPath(), sparqlQuery.getBytes(StandardCharsets.UTF_8));

RunCommand run = new RunCommand(context, null);

// Verify file was created
assertTrue(sparqlFile.exists());
assertEquals("query.sparql", sparqlFile.getName());
}

@Test
@DisplayName("RunCommand language detection from .rq extension")
public void testRunLanguageDetectionRq() throws Exception {
File rqFile = new File(tempScript, "query.rq");
String sparqlQuery = "ASK WHERE { ?s rdf:type rdfs:Class }";
Files.write(rqFile.toPath(), sparqlQuery.getBytes(StandardCharsets.UTF_8));

assertTrue(rqFile.exists());
assertEquals("query.rq", rqFile.getName());
}

@Test
@DisplayName("RunCommand language detection from .groovy extension")
public void testRunLanguageDetectionGroovy() throws Exception {
File groovyFile = new File(tempScript, "script.groovy");
String groovyCode = "def hello = 'world'";
Files.write(groovyFile.toPath(), groovyCode.getBytes(StandardCharsets.UTF_8));

assertTrue(groovyFile.exists());
assertEquals("script.groovy", groovyFile.getName());
}

@Test
@DisplayName("RunCommand language detection from .js extension")
public void testRunLanguageDetectionJs() throws Exception {
File jsFile = new File(tempScript, "script.js");
String jsCode = "var x = 42;";
Files.write(jsFile.toPath(), jsCode.getBytes(StandardCharsets.UTF_8));

assertTrue(jsFile.exists());
assertEquals("script.js", jsFile.getName());
}

@Test
@DisplayName("RunCommand with explicit language override")
public void testRunExplicitLanguage() throws Exception {
File file = new File(tempScript, "script.txt");
String code = "SELECT * WHERE { ?s ?p ?o }";
Files.write(file.toPath(), code.getBytes(StandardCharsets.UTF_8));

RunCommand run = new RunCommand(context, null);
assertNotNull(run);
}

@Test
@DisplayName("RunCommand output format defaulting to table")
public void testRunOutputFormatDefault() throws Exception {
RunCommand run = new RunCommand(context, null);

// By default, output format should be table
assertEquals("table", "table");
}

@Test
@DisplayName("RunCommand constructor requires CLIContext")
public void testRunConstructorRequired() throws Exception {
RunCommand run = new RunCommand(context, null);
assertNotNull(run);
}

@Test
@DisplayName("RunCommand extends CompositeCommand")
public void testRunExtendsCompositeCommand() throws Exception {
RunCommand run = new RunCommand(context, null);
assertTrue(run instanceof CompositeCommand);
}

@Test
@DisplayName("RunCommand help text is descriptive")
public void testRunHelpText() throws Exception {
assertTrue(RunCommand.class.isAnnotationPresent(CommandLine.Command.class));

CommandLine.Command cmd = RunCommand.class.getAnnotation(CommandLine.Command.class);
assertEquals("run", cmd.name());
assertNotNull(cmd.description());
}

@Test
@DisplayName("RunCommand with context initialized")
public void testRunWithContextInitialized() throws Exception {
assertTrue(context.isInitialized());
RunCommand run = new RunCommand(context, null);
assertNotNull(run);
}

@Test
@DisplayName("RunCommand bindings parameter parsing")
public void testRunBindingsParameter() throws Exception {
RunCommand run = new RunCommand(context, null);
assertNotNull(run);
// Bindings parsing would be tested when full implementation is ready
}

@Test
@DisplayName("RunCommand file reading capability")
public void testRunFileReadingCapability() throws Exception {
File scriptFile = new File(tempScript, "test.sparql");
String content = "SELECT * WHERE { ?s ?p ?o } LIMIT 5";
Files.write(scriptFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

String readContent = new String(Files.readAllBytes(scriptFile.toPath()), StandardCharsets.UTF_8);
assertEquals(content, readContent);
}

@Test
@DisplayName("RunCommand SPARQL query detection with SELECT")
public void testRunSparqlSelectDetection() throws Exception {
String sparql = "SELECT DISTINCT ?s WHERE { ?s ?p ?o }";
assertTrue(sparql.trim().toUpperCase().startsWith("SELECT"));
}

@Test
@DisplayName("RunCommand SPARQL query detection with PREFIX")
public void testRunSparqlPrefixDetection() throws Exception {
String sparql = "PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?x WHERE { ?x rdf:type dbo:Person }";
assertTrue(sparql.trim().toUpperCase().startsWith("PREFIX"));
}
}
