package systems.symbol.kernel.command;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.I_Command;
import systems.symbol.kernel.KernelBuilder;
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.kernel.event.KernelEvent;
import systems.symbol.kernel.event.KernelTopics;
import systems.symbol.kernel.event.SimpleEventHub;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KernelCommandEventTest {

static class TestCommand extends AbstractKernelCommand<String> {
protected TestCommand(systems.symbol.kernel.KernelContext ctx) {
super(ctx);
}

@Override
protected KernelResult<String> doExecute(KernelRequest request) {
return KernelResult.ok("result");
}
}

static class FailingCommand extends AbstractKernelCommand<String> {
protected FailingCommand(systems.symbol.kernel.KernelContext ctx) { super(ctx); }

@Override
protected KernelResult<String> doExecute(KernelRequest request) {
throw new systems.symbol.kernel.KernelException("cmd.fail", "fail");
}
}

static class SimpleCommand implements I_Command {
private final IRI actor;
private final IRI action;
private final Resource target;

SimpleCommand(IRI actor, IRI action, Resource target) {
this.actor = actor;
this.action = action;
this.target = target;
}

@Override public IRI getActor() { return actor; }
@Override public IRI getAction() { return action; }
@Override public Resource getTarget() { return target; }
}

@Test
void executesCommandPublishesLifecycleEvents() throws Exception {
I_EventHub hub = new SimpleEventHub();
List<String> seen = new ArrayList<>();

hub.subscribe(KernelTopics.AGENT_COMMAND_RECEIVED, event -> seen.add("received"));
hub.subscribe(KernelTopics.AGENT_COMMAND_EXECUTED, event -> seen.add("executed"));
hub.subscribe(KernelTopics.AGENT_COMMAND_FAILED, event -> seen.add("failed"));

var kernel = KernelBuilder.create().withEventHub(hub).build();
kernel.start();

var cmd = new TestCommand(kernel.getContext());
IRI subject = SimpleValueFactory.getInstance().createIRI("urn:subject");
IRI actor = SimpleValueFactory.getInstance().createIRI("urn:actor");
IRI action = SimpleValueFactory.getInstance().createIRI("urn:action");
var commandObj = new SimpleCommand(actor, action, null);

KernelRequest request = KernelRequest.on(subject)
.caller(actor)
.realm(kernel.getContext().getSelf())
.command(commandObj)
.build();

KernelResult<String> result = cmd.execute(request);
assertEquals("result", result.get());
assertEquals(List.of("received", "executed"), seen);

kernel.stop();
}

@Test
void failingCommandPublishesFailedEvent() throws Exception {
I_EventHub hub = new SimpleEventHub();
List<String> seen = new ArrayList<>();
hub.subscribe(KernelTopics.AGENT_COMMAND_FAILED, event -> seen.add("failed"));

var kernel = KernelBuilder.create().withEventHub(hub).build();
kernel.start();

var cmd = new FailingCommand(kernel.getContext());
IRI subject = SimpleValueFactory.getInstance().createIRI("urn:subject");

KernelRequest request = KernelRequest.on(subject).build();

KernelResult<String> result = cmd.execute(request);
assertEquals(true, result.isError());
assertEquals(List.of("failed"), seen);

kernel.stop();
}
}
