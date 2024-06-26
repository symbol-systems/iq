package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.ScriptAgent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Avatar;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.secrets.SecretsException;

import javax.script.SimpleBindings;

import java.io.File;
import java.io.IOException;

class AvatarBuilderTest {
String modelName = "llama3-8b-8192";
IRI grqq = Values.iri(IQ_NS.TEST,"Groq_Chat_llama3_8b_8192");
IRI hello = Values.iri(IQ_NS.TEST,"hello");
IRI self = Values.iri(IQ_NS.TEST);

@Test
void configure() throws IOException {
BootstrapRepository repo = new BootstrapRepository(new File("src/test/resources/avatar/"), IQ_NS.TEST);

AvatarBuilder builder = new AvatarBuilder(self, 1000, new SimpleBindings(), null);

try (RepositoryConnection connection = repo.getConnection()) {
builder.setGround(connection.getStatements(null,null,null));
I_LLMConfig llm = builder.configure();
assert !builder.getGround().isEmpty();
assert llm!=null;
System.out.println("avatar.llm: " + llm.getName()+" @ "+llm.getURL());
assert llm.getName()!=null;
assert modelName.equals(llm.getName());
assert llm.getURL()!=null;
assert llm.getURL().startsWith("https://");
}
}

@Test
void avatar() throws IOException, SecretsException {
BootstrapRepository repo = new BootstrapRepository(new File("src/test/resources/avatar/"), IQ_NS.TEST);
AvatarBuilder builder = new AvatarBuilder(grqq, 1000, new SimpleBindings(), new EnvsAsSecrets());

try (RepositoryConnection connection = repo.getConnection()) {
builder.setGround(connection.getStatements(null,null,null));
Avatar avatar = builder.build();
assert avatar != null;
System.out.println("avatar.groq: " + avatar.getSelf());
avatar.execute(self, hello, new SimpleBindings());
} catch (StateException e) {
throw new RuntimeException(e);
}
}

@Test
void agent() throws Exception {
IRI self = Values.iri(IQ_NS.TEST);
BootstrapRepository repo = new BootstrapRepository(new File("src/test/resources/avatar/"), IQ_NS.TEST);
AvatarBuilder builder = new AvatarBuilder(self, 1000, new SimpleBindings(), new EnvsAsSecrets());

try (RepositoryConnection connection = repo.getConnection()) {
builder.setGround(connection.getStatements(null,null,null));
assert builder.getGround().size()>5;
Avatar avatar = builder.build();
assert avatar != null;
ScriptAgent agent = new ScriptAgent(self, avatar.getGround(), avatar.getGround());
Resource state = agent.getStateMachine().getState();
System.out.println("avatar.agent.state: " + self + " @ " + state);
assert null != state;;
assert hello.equals(state);
assert !agent.getStateMachine().getTransitions().isEmpty();
System.out.println("avatar.agent.choices: " + agent.getStateMachine().getTransitions());
Resource decided = avatar.next(agent);
System.out.println("avatar.agent.delegate: " + state + " @ " + decided);
System.out.println("avatar.agent.decided: " + state + " @ " + decided);
assert decided.stringValue().equals("urn:iq:test:greetings") || decided.stringValue().equals("urn:iq:test:welcome");
System.out.println("avatar.agent.thoughts: " + agent.getStateMachine().getState() + " -> " + avatar.getThoughts().size());
RDFDump.dump(avatar.getThoughts(), System.out, RDFFormat.TURTLE);
} catch (StateException e) {
throw new RuntimeException(e);
}
}
}