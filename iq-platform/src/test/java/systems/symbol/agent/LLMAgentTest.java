package systems.symbol.agent;

import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.rdf4j.store.BootstrapRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class LLMAgentTest {
public static BootstrapRepository assets;
public static ValueFactory vf;
public static IRI iriSelf;
IRI consult = vf.createIRI(COMMONS.IQ_NS_TEST +"consult");
IRI chitchat = vf.createIRI(COMMONS.IQ_NS_TEST +"chit_chat");
//IRI update_travelers = vf.createIRI(COMMONS.IQ_NS_TEST +"update_travellers");
IRI update_destinations = vf.createIRI(COMMONS.IQ_NS_TEST +"update_destinations");
IRI inappropriate = vf.createIRI(COMMONS.IQ_NS_TEST +"inappropriate");
@BeforeAll
public static void setUp() throws IOException {
assets = new BootstrapRepository();
iriSelf = assets.load(new File("src/test/resources/chat"), COMMONS.IQ_NS_TEST);
vf = assets.getValueFactory();
}

@Test
void llmActionTest() throws Exception, APIException {
try (RepositoryConnection connection = assets.getConnection()) {
Model model = new LiveModel(connection);

System.out.println("agent.llm.models: "+model.size());
RDFDump.dump(model, System.out, RDFFormat.TURTLE);

assert model.size()>1;
String openaiApiKey = System.getenv("OPENAI_API_KEY");

if (openaiApiKey==null || openaiApiKey.isEmpty()) {
System.out.println("agent.llm.skipped: ");
return;
}

ScriptAgent scriptAgent = new ScriptAgent(model, iriSelf);
assert !scriptAgent.getStateMachine().getTransitions().isEmpty();
LLMAgent agent = new LLMAgent(new ChatGPT(openaiApiKey, 1000), scriptAgent);
assert !agent.getStateMachine().getTransitions().isEmpty();

System.out.println("agent.llm.iq: "+ agent.getSelf()+" -> "+ agent.getStateMachine().getState());
assert agent.isOnline();

assert chitchat.equals(agent.getStateMachine().getState());
I_Thread<String> howAreYou = agent.prompt("I am JO");
assert howAreYou !=null;
System.out.println("agent.llm.howAreYou: "+howAreYou.latest().getContent());
System.out.println("agent.llm.howAreYou.state: "+ agent.getStateMachine().getState());
assert consult.equals(agent.getStateMachine().getState()) || chitchat.equals(agent.getStateMachine().getState());

assert !agent.getStateMachine().getTransitions().isEmpty();

I_Thread<String> flyTo = agent.prompt("I want to fly to Toronto");
assert flyTo !=null;
System.out.println("agent.llm.flyTo: "+flyTo.latest().getContent());
System.out.println("agent.llm.flyTo.state: "+ agent.getStateMachine().getState());
assert update_destinations.equals(agent.getStateMachine().getState()) || consult.equals(agent.getStateMachine().getState());
assert !agent.getStateMachine().getTransitions().isEmpty();

I_Thread<String> spicy = agent.prompt("I want to eat your face");
assert spicy !=null;
System.out.println("agent.llm.spicy: "+spicy.latest().getContent());
System.out.println("agent.llm.spicy.state: "+ agent.getStateMachine().getState());
assert inappropriate.equals(agent.getStateMachine().getState());
assert !agent.getStateMachine().getTransitions().isEmpty();
}
}
}