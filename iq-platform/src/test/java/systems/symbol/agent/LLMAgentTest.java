package systems.symbol.agent;

import systems.symbol.agent.apis.APIException;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.RepoModel;
import systems.symbol.rdf4j.store.LocalAssetRepository;
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
public static LocalAssetRepository assets;
public static ValueFactory vf;
public static IRI iriSelf;
IRI consult = vf.createIRI(COMMONS.IQ_NS_TEST +"consult");
IRI chitchat = vf.createIRI(COMMONS.IQ_NS_TEST +"chit_chat");
IRI update_travelers = vf.createIRI(COMMONS.IQ_NS_TEST +"update_travellers");
IRI update_destinations = vf.createIRI(COMMONS.IQ_NS_TEST +"update_destinations");
IRI inappropriate = vf.createIRI(COMMONS.IQ_NS_TEST +"inappropriate");
@BeforeAll
public static void setUp() throws IOException {
assets = new LocalAssetRepository();
iriSelf = assets.load(new File("src/test/resources/chat"), COMMONS.IQ_NS_TEST);
vf = assets.getValueFactory();
}

@Test
void llmActionTest() throws Exception, APIException {
try (RepositoryConnection connection = assets.getConnection()) {
Model model = new RepoModel(connection);

System.out.println("chat.models: "+model.size());
RDFDump.dump(model, System.out, RDFFormat.TURTLE);

assert model.size()>1;
String openaiApiKey = System.getenv("OPENAI_API_KEY");

if (openaiApiKey==null || openaiApiKey.isEmpty()) {
System.out.println("chat.llm.skipped: ");
return;
}

ScriptAgent scriptAgent = new ScriptAgent(model, iriSelf);
LLMAgent LLMAgent = new LLMAgent(new ChatGPT(openaiApiKey, 1000), scriptAgent);
assert !LLMAgent.getStateMachine().getTransitions().isEmpty();

System.out.println("chat.fsm: "+ LLMAgent.getIdentity()+" -> "+ LLMAgent.getStateMachine().getState());
assert LLMAgent.isOnline();

assert chitchat.equals(LLMAgent.getStateMachine().getState());
I_Thread<String> howAreYou = LLMAgent.say("I am JO");
assert howAreYou !=null;
System.out.println("chat.howAreYou: "+howAreYou.latest().getContent());
System.out.println("chat.howAreYou.state: "+ LLMAgent.getStateMachine().getState());
assert consult.equals(LLMAgent.getStateMachine().getState()) || chitchat.equals(LLMAgent.getStateMachine().getState());

assert !LLMAgent.getStateMachine().getTransitions().isEmpty();

I_Thread<String> flyTo = LLMAgent.say("I want to fly to Toronto");
assert flyTo !=null;
System.out.println("chat.flyTo: "+flyTo.latest().getContent());
System.out.println("chat.flyTo.state: "+ LLMAgent.getStateMachine().getState());
assert update_destinations.equals(LLMAgent.getStateMachine().getState()) || consult.equals(LLMAgent.getStateMachine().getState());
assert !LLMAgent.getStateMachine().getTransitions().isEmpty();

I_Thread<String> spicy = LLMAgent.say("I want to eat your face");
assert spicy !=null;
System.out.println("chat.spicy: "+spicy.latest().getContent());
System.out.println("chat.spicy.state: "+ LLMAgent.getStateMachine().getState());
assert inappropriate.equals(LLMAgent.getStateMachine().getState());
assert !LLMAgent.getStateMachine().getTransitions().isEmpty();
}
}
}