package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.linux.Stat;
import systems.symbol.agent.Agentic;
import systems.symbol.agent.LazyAgent;
import systems.symbol.finder.IndexHelper;
import systems.symbol.finder.TextFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.Conversation;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.EnvsAsSecrets;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static systems.symbol.platform.IQ_NS.TEST;

class SearchDecisionTest {

@Test
void decide() throws IOException, StateException {
BootstrapRepository assets = new BootstrapRepository();
IRI self = assets.load(new File("src/test/resources/avatar"), TEST);
assert TEST.equals( self.stringValue() );
TextFinder finder = new TextFinder();
try(RepositoryConnection connection = assets.getConnection()) {
assert connection.size()> 100;
long indexed = IndexHelper.index(finder, connection.getStatements(null, RDFS.LABEL, null));
System.out.println("search.indexed: "+indexed);
assert indexed>0;
Conversation chat = new Conversation();
chat.user("what time is it");
System.out.println("search.question: "+chat.latest());
SearchDecision decider = new SearchDecision(finder, new Agentic<>(chat));
Resource decided = decider.decide();
System.out.println("search.decided: "+decided);
assert decided!=null;
}
}

@Test
void delegate() throws IOException, StateException, ExecutionException, InterruptedException {
BootstrapRepository assets = new BootstrapRepository();
IRI self = assets.load(new File("src/test/resources/avatar"), TEST);
assert TEST.equals( self.stringValue() );
TextFinder finder = new TextFinder();
try(RepositoryConnection connection = assets.getConnection()) {
assert connection.size()> 100;
long indexed = IndexHelper.index(finder, connection.getStatements(null, RDFS.LABEL, null));
System.out.println("search.delegate.indexed: "+indexed);
assert indexed>0;
Conversation chat = new Conversation();
chat.user("what time is it");
System.out.println("search.delegate.question: "+chat.latest());
SearchDecision decider = new SearchDecision(finder, new Agentic<>(chat));
Future<I_Delegate<Resource>> manager = decider.delegate(new LazyAgent(self, new LiveModel(connection)));
assert manager!=null;
Resource decided = manager.get().decide();
System.out.println("search.delegate.decided: "+ decided);
assert decided!=null;
}
}

@Test
void workflow() throws IOException, StateException, ExecutionException, InterruptedException {
BootstrapRepository assets = new BootstrapRepository();
IRI self = assets.load(new File("src/test/resources/avatar"), TEST);
assert TEST.equals( self.stringValue() );
TextFinder finder = new TextFinder();
try(RepositoryConnection connection = assets.getConnection()) {
assert connection.size()> 100;
long indexed = IndexHelper.index(finder, connection.getStatements(null, RDFS.LABEL, null));
System.out.println("search.workflow.indexed: "+indexed);
assert indexed>0;
Conversation chat = new Conversation();
chat.user("search the web");
System.out.println("search.workflow.question: "+chat.latest());
SearchDecision decider = new SearchDecision(finder, new Agentic<>(chat));
Future<I_Delegate<Resource>> manager = decider.delegate(new LazyAgent(self, new LiveModel(connection)));
assert manager!=null;
Resource decided = manager.get().decide();
System.out.println("search.workflow.decided: "+ decided);
assert decided!=null;
}
}
}