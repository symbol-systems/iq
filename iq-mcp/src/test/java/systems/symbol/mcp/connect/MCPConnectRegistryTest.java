package systems.symbol.mcp.connect;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPResult;
import systems.symbol.mcp.connect.MCPChain;
import systems.symbol.mcp.connect.impl.AuthGuardMiddleware;

import static org.junit.jupiter.api.Assertions.*;

class MCPConnectRegistryTest {

    @Test
    void testMiddlewareConfigFromRdfRunsWithConfiguredJwtSecret() throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        repo.init();

        try (RepositoryConnection conn = repo.getConnection()) {
            var vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
            IRI graph = Values.iri("urn:mcp:pipeline");
            IRI middleware = Values.iri("urn:mcp:pipeline/AuthGuard");

            conn.add(middleware, vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    Values.iri("urn:mcp:Middleware"), graph);
            conn.add(middleware, Values.iri("urn:mcp:middlewareClass"),
                    vf.createLiteral(AuthGuardMiddleware.class.getName()), graph);
            conn.add(middleware, Values.iri("urn:mcp:order"), vf.createLiteral(10), graph);
            conn.add(middleware, Values.iri("urn:mcp:enabled"), vf.createLiteral(true), graph);

            // Provide JWT config via RDF so the middleware is built with a real validator
            conn.add(middleware, Values.iri("urn:mcp:jwtSecret"), vf.createLiteral("super-secret-key"), graph);
            conn.add(middleware, Values.iri("urn:mcp:jwtIssuer"), vf.createLiteral("https://example.com"), graph);
            conn.add(middleware, Values.iri("urn:mcp:jwtAudience"), vf.createLiteral("mcp-client"), graph);
        }

        MCPConnectRegistry registry = new MCPConnectRegistry(repo);
        var middleware = registry.loadAll();

        AuthGuardMiddleware auth = middleware.stream()
                .filter(m -> m instanceof AuthGuardMiddleware)
                .map(m -> (AuthGuardMiddleware) m)
                .findFirst()
                .orElseThrow(() -> new AssertionError("AuthGuardMiddleware not loaded"));

        String jwt = com.auth0.jwt.JWT.create()
                .withIssuer("https://example.com")
                .withAudience("mcp-client")
                .withSubject("alice")
                .withExpiresAt(java.util.Date.from(java.time.Instant.now().plusSeconds(60)))
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("super-secret-key"));

        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + jwt);

        I_MCPResult res = auth.process(ctx, (MCPChain) c -> MCPResult.okText("ok"));
        assertFalse(res.isError());
        assertEquals("alice", ctx.principal());
    }
}
