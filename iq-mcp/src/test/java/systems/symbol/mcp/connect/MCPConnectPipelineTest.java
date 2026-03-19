package systems.symbol.mcp.connect;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MCPConnectPipeline — middleware orchestration.
 */
class MCPConnectPipelineTest {

    @Test
    void testPipelineExecution() throws MCPException {
        // Create a simple middleware that marks the context
        I_MCPPipeline middleware1 = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:m1"); }
            @Override public int getOrder() { return 1; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                ctx.set("m1", "executed");
                return chain.proceed(ctx);
            }
        };

        I_MCPPipeline middleware2 = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:m2"); }
            @Override public int getOrder() { return 2; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                ctx.set("m2", "executed");
                return chain.proceed(ctx);
            }
        };

        var pipeline = new MCPConnectPipeline(java.util.List.of(middleware1, middleware2));
        MCPCallContext ctx = new MCPCallContext("tool1", java.util.Map.of());
        
        I_MCPResult result = pipeline.execute(ctx, (c) -> MCPResult.okText("done"));
        
        assertEquals("done", result.getContent());
        assertEquals("executed", ctx.get("m1"));
        assertEquals("executed", ctx.get("m2"));
    }

    @Test
    void testPipelineSortsMiddlewareByOrder() throws MCPException {
        StringBuilder order = new StringBuilder();

        I_MCPPipeline m3 = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:m3"); }
            @Override public int getOrder() { return 30; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                order.append("3");
                return chain.proceed(ctx);
            }
        };

        I_MCPPipeline m1 = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:m1"); }
            @Override public int getOrder() { return 10; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                order.append("1");
                return chain.proceed(ctx);
            }
        };

        I_MCPPipeline m2 = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:m2"); }
            @Override public int getOrder() { return 20; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                order.append("2");
                return chain.proceed(ctx);
            }
        };

        var pipeline = new MCPConnectPipeline(java.util.List.of(m3, m1, m2));
        MCPCallContext ctx = new MCPCallContext("tool1", java.util.Map.of());
        
        pipeline.execute(ctx, (c) -> MCPResult.okText("ok"));
        
        assertEquals("123", order.toString());
    }

    @Test
    void testPipelineShortCircuit() throws MCPException {
        I_MCPPipeline shortCircuit = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:sc"); }
            @Override public int getOrder() { return 10; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                return MCPResult.error(403, "forbidden");
            }
        };

        var pipeline = new MCPConnectPipeline(java.util.List.of(shortCircuit));
        MCPCallContext ctx = new MCPCallContext("tool1", java.util.Map.of());
        
        I_MCPResult result = pipeline.execute(ctx, (c) -> {
            throw new AssertionError("should not reach adaptor");
        });
        
        assertTrue(result.isError());
        assertEquals(403, result.getErrorCode());
    }

    @Test
    void testPipelineExceptionPropagation() throws MCPException {
        I_MCPPipeline failing = new I_MCPPipeline() {
            @Override public IRI getSelf() { return Values.iri("urn:test:fail"); }
            @Override public int getOrder() { return 10; }
            @Override
            public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
                throw MCPException.badRequest("invalid input");
            }
        };

        var pipeline = new MCPConnectPipeline(java.util.List.of(failing));
        MCPCallContext ctx = new MCPCallContext("tool1", java.util.Map.of());
        
        I_MCPResult result = pipeline.execute(ctx, (c) -> MCPResult.okText("ok"));
        
        assertTrue(result.isError());
        assertEquals(400, result.getErrorCode());
    }
}
