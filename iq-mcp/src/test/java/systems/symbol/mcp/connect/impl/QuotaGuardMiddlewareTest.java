package systems.symbol.mcp.connect.impl;

import org.junit.jupiter.api.Test;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import systems.symbol.mcp.connect.MCPChain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuotaGuardMiddleware — rate limiting.
 */
class QuotaGuardMiddlewareTest {

@Test
void testQuotaGuardAllowsWithinLimit() throws MCPException {
QuotaGuardMiddleware quota = new QuotaGuardMiddleware(100, 10);
MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
ctx.set(MCPCallContext.KEY_PRINCIPAL, "alice");

MCPChain chain = c -> MCPResult.okText("ok");
I_MCPResult result = quota.process(ctx, chain);

assertFalse(result.isError());
assertEquals(1, (int) ctx.get(MCPCallContext.KEY_QUOTA_USED));
}

@Test
void testQuotaGuardRejectsExceeded() throws MCPException {
QuotaGuardMiddleware quota = new QuotaGuardMiddleware(2, 10);  // limit of 2
MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
ctx.set(MCPCallContext.KEY_PRINCIPAL, "alice");

MCPChain chain = c -> MCPResult.okText("ok");

// First call should succeed
quota.process(ctx, chain);

// Second call should succeed
quota.process(ctx, chain);

// Third call should fail
MCPException ex = assertThrows(MCPException.class, () -> {
quota.process(ctx, chain);
});

assertEquals(429, ex.getCode());
}

@Test
void testQuotaGuardDisposesPerWindow() throws MCPException, InterruptedException {
QuotaGuardMiddleware quota = new QuotaGuardMiddleware(1, 10);
MCPCallContext ctx1 = new MCPCallContext("sparql.query", java.util.Map.of());
ctx1.set(MCPCallContext.KEY_PRINCIPAL, "alice");

MCPChain chain = c -> MCPResult.okText("ok");

// First call succeeds
quota.process(ctx1, chain);

// Second call fails (same window)
assertThrows(MCPException.class, () -> {
MCPCallContext ctx2 = new MCPCallContext("sparql.query", java.util.Map.of());
ctx2.set(MCPCallContext.KEY_PRINCIPAL, "alice");
quota.process(ctx2, chain);
});
}

@Test
void testQuotaGuardStricterLimitForWrite() throws MCPException {
QuotaGuardMiddleware quota = new QuotaGuardMiddleware(100, 2);  // write limit 2

MCPChain chain = c -> MCPResult.okText("ok");

// First write call succeeds
MCPCallContext ctx1 = new MCPCallContext("sparql.update", java.util.Map.of());
ctx1.set(MCPCallContext.KEY_PRINCIPAL, "alice");
quota.process(ctx1, chain);

// Second write call succeeds
MCPCallContext ctx2 = new MCPCallContext("sparql.update", java.util.Map.of());
ctx2.set(MCPCallContext.KEY_PRINCIPAL, "alice");
quota.process(ctx2, chain);

// Third write call fails (write limit is 2)
MCPCallContext ctx3 = new MCPCallContext("sparql.update", java.util.Map.of());
ctx3.set(MCPCallContext.KEY_PRINCIPAL, "alice");
assertThrows(MCPException.class, () -> quota.process(ctx3, chain));
}
}
