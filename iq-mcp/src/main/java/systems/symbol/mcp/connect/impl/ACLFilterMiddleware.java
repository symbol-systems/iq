package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ACLFilterMiddleware — enforces per-principal tool ACL policy (order: 30).
 *
 * <p>Policy triples are read from the {@code mcp:policy} named graph.
 * SPARQL queries are externalized to resource files for maintainability.
 *
 * <p>No-arg constructor for testing (permissive mode — no policy graph).
 * Production constructor accepts a Repository for policy enforcement.
 */
public class ACLFilterMiddleware implements I_MCPPipeline {

    private static final IRI SELF = Values.iri("urn:mcp:pipeline/ACLFilter");

    private final Repository repository;
    private String queryHasEntry;
    private String queryAllowedRole;

    /** Permissive/testing mode — no policy graph. */
    public ACLFilterMiddleware() {
        this.repository = null;
    }

    /** Production mode — loads ACL from the mcp:policy named graph. */
    public ACLFilterMiddleware(Repository repository) {
        this.repository = repository;
        this.queryHasEntry = loadQuery("assets/sparql/mcp-policy-has-entry.rq");
        this.queryAllowedRole = loadQuery("assets/sparql/mcp-policy-allowed-role.rq");
    }

    @Override public IRI getSelf()  { return SELF; }
    @Override public int getOrder() { return 30; }

    @Override
    public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
        if (repository == null) return chain.proceed(ctx); // permissive mode

        String tool = ctx.toolName();
        String principal = ctx.principal();
        List<String> roles = ctx.get(MCPCallContext.KEY_ROLES);

        if (!isAllowed(tool, principal, roles)) {
            throw MCPException.forbidden("access denied");
        }
        ctx.set(MCPCallContext.KEY_AUTHORISED, true);
        return chain.proceed(ctx);
    }

    private boolean isAllowed(String toolName, String principal, List<String> roles) {
        try (RepositoryConnection conn = repository.getConnection()) {
            String query = queryHasEntry.replace("{{toolName}}", toolName);
            boolean hasPolicyEntry = conn.prepareBooleanQuery(query).evaluate();

            if (!hasPolicyEntry) return true; // no restriction defined

            if (roles == null || roles.isEmpty()) return false;
            for (String role : roles) {
                String q = queryAllowedRole
                    .replace("{{toolName}}", toolName)
                    .replace("{{role}}", role);
                boolean allowed = conn.prepareBooleanQuery(q).evaluate();
                if (allowed) return true;
            }
            return false;
        } catch (Exception ex) {
            return true; // fail-open on graph error
        }
    }

    private static String loadQuery(String path) {
        try {
            InputStream in = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(path);
            if (in == null) return "";
            try (in) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        }
    }
}
