package systems.symbol.kernel.policy;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.pipeline.KernelCallContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyInputTest {

@Test
public void fromKernelCallContext_buildsCorrectly() {
KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_PRINCIPAL, "alice");
IRI realm = SimpleValueFactory.getInstance().createIRI("urn:iq:realm:public");
ctx.set(KernelCallContext.KEY_REALM, realm);
ctx.set(KernelCallContext.KEY_ROLES, List.of("admin", "user"));
ctx.set("kernel.scopes", List.of("chat.read", "sparql.select"));

PolicyInput input = PolicyInput.from(ctx, PolicyVocab.ACTION_READ, PolicyVocab.resource("api"));

assertNotNull(input);
assertEquals(PolicyVocab.principal("alice"), input.principal());
assertEquals(realm, input.realm());
assertEquals(PolicyVocab.ACTION_READ, input.action());
assertEquals(PolicyVocab.resource("api"), input.resource());
assertTrue(input.roles().contains(PolicyVocab.role("admin")));
assertTrue(input.scopes().contains(PolicyVocab.scope("chat.read")));
}

@Test
public void fromKernelCallContext_missingPrincipal_throws() {
KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_REALM, SimpleValueFactory.getInstance().createIRI("urn:iq:realm:public"));

assertThrows(IllegalArgumentException.class,
() -> PolicyInput.from(ctx, PolicyVocab.ACTION_READ, PolicyVocab.resource("api")));
}
}
