package systems.symbol.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCPException — typed exception with HTTP-style error codes.
 */
class MCPExceptionTest {

@Test
void testExceptionCode() {
MCPException ex = new MCPException(400, "bad request");
assertEquals(400, ex.getCode());
assertEquals("bad request", ex.getMessage());
}

@Test
void testExceptionWithCause() {
Exception cause = new RuntimeException("original");
MCPException ex = new MCPException(500, "wrapped", cause);

assertEquals(500, ex.getCode());
assertEquals("wrapped", ex.getMessage());
assertEquals(cause, ex.getCause());
}

@Test
void testFactories() {
assertTrue(MCPException.badRequest("bad input").getCode() == 400);
assertTrue(MCPException.unauthorized("no auth").getCode() == 401);
assertTrue(MCPException.forbidden("denied").getCode() == 403);
assertTrue(MCPException.notFound("missing").getCode() == 404);
assertTrue(MCPException.quotaExceeded("limit").getCode() == 429);
assertTrue(MCPException.internal("error", null).getCode() == 500);
assertTrue(MCPException.serviceUnavailable("down").getCode() == 503);
}

@Test
void testToResult() {
MCPException ex = MCPException.badRequest("invalid query");
I_MCPResult result = ex.toResult();

assertTrue(result.isError());
assertEquals(400, result.getErrorCode());
}
}
