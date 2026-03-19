package systems.symbol.kernel.pipeline;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.KernelAuthException;
import systems.symbol.kernel.KernelException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pipeline / middleware chain.
 */
class PipelineTest {

/* ── helpers ─────────────────────────────────────────────────────────── */

/** Simple call log we assert against. */
private final List<String> log = new ArrayList<>();

private I_Middleware<KernelCallContext> logging(String name) {
return new I_Middleware<>() {
@Override public int getOrder() { return 0; }
@Override
public void process(KernelCallContext ctx, I_Chain<KernelCallContext> chain)
throws KernelException {
log.add(name + ":before");
chain.proceed(ctx);
log.add(name + ":after");
}
};
}

private I_Middleware<KernelCallContext> blocking(String name) {
return new I_Middleware<>() {
@Override public int getOrder() { return 0; }
@Override
public void process(KernelCallContext ctx, I_Chain<KernelCallContext> chain) {
log.add(name + ":blocked");
// does NOT call chain.proceed()
}
};
}

private I_Middleware<KernelCallContext> throwing(String name) {
return new I_Middleware<>() {
@Override public int getOrder() { return 0; }
@Override
public void process(KernelCallContext ctx, I_Chain<KernelCallContext> chain)
throws KernelException {
log.add(name + ":throw");
throw new KernelAuthException(KernelAuthException.Kind.UNAUTHORIZED,
"test-unauthorised");
}
};
}

/* ── tests ────────────────────────────────────────────────────────────── */

@Test
void emptyPipelineIsNoop() throws KernelException {
I_Chain<KernelCallContext> chain = I_Pipeline.of(List.of());
KernelCallContext ctx = new KernelCallContext();
// Should not throw
chain.proceed(ctx);
assertTrue(log.isEmpty());
}

@Test
void singleMiddlewareIsCalledAndChainProceedsFinalNoop() throws KernelException {
I_Chain<KernelCallContext> chain = I_Pipeline.of(List.of(logging("A")));
chain.proceed(new KernelCallContext());
assertEquals(List.of("A:before", "A:after"), log);
}

@Test
void multipleMiddlewareCalledInOrder() throws KernelException {
var m1 = logging("first");
var m2 = logging("second");
var m3 = logging("third");
I_Chain<KernelCallContext> chain = I_Pipeline.of(List.of(m1, m2, m3));
chain.proceed(new KernelCallContext());
// Expect nested: first-before, second-before, third-before, third-after, second-after, first-after
assertEquals(List.of(
"first:before", "second:before", "third:before",
"third:after", "second:after", "first:after"
), log);
}

@Test
void blockingMiddlewareStopsChain() throws KernelException {
var chain = I_Pipeline.of(List.of(logging("A"), blocking("B"), logging("C")));
chain.proceed(new KernelCallContext());
// C should never be called; A gets after because B returned normally
assertEquals(List.of("A:before", "B:blocked", "A:after"), log);
}

@Test
void throwingMiddlewarePropagatesException() {
var chain = I_Pipeline.of(List.of(logging("A"), throwing("auth"), logging("C")));
KernelException ex = assertThrows(KernelException.class,
() -> chain.proceed(new KernelCallContext()));
assertTrue(ex instanceof KernelAuthException);
// A:after should not be reached either
assertEquals(List.of("A:before", "auth:throw"), log);
}

@Test
void allowAllPolicyPermitsAll() {
I_AccessPolicy policy = I_AccessPolicy.allowAll();
assertTrue(policy.allows(new KernelCallContext()));
}

@Test
void denyAllPolicyDeniesAll() {
I_AccessPolicy policy = I_AccessPolicy.denyAll();
assertFalse(policy.allows(new KernelCallContext()));
}

@Test
void callContextTraceIdIsStable() {
KernelCallContext ctx = new KernelCallContext();
String id = ctx.traceId();
assertNotNull(id);
assertEquals(id, ctx.traceId());
}

@Test
void callContextAttributeRoundtrip() {
KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_PRINCIPAL, "alice");
assertEquals("alice", ctx.principal());
}

@Test
void authorisedFlagDefaultsFalse() {
KernelCallContext ctx = new KernelCallContext();
assertFalse(ctx.isAuthorised());
}

@Test
void authorisedFlagCanBeSetTrue() {
KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_AUTHORISED, true);
assertTrue(ctx.isAuthorised());
}
}
