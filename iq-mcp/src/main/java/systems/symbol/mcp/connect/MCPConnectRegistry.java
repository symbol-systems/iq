package systems.symbol.mcp.connect;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * MCPConnectRegistry — loads and instantiates middleware from the RDF
 * {@code mcp:pipeline} named graph at application startup.
 *
 * <p>Expected triples in the graph (Turtle snippet):
 * <pre>
 *   &lt;urn:mcp:pipeline/AuthGuard&gt; a mcp:Middleware ;
 *     mcp:order 10 ;
 *     mcp:enabled true ;
 *     mcp:middlewareClass "systems.symbol.mcp.connect.impl.AuthGuardMiddleware" .
 * </pre>
 *
 * <p>If no RDF configuration is found the registry falls back to the
 * {@link #defaultPipeline()} which includes every built-in middleware in
 * canonical order.
 */
public class MCPConnectRegistry {

    private static final Logger log = LoggerFactory.getLogger(MCPConnectRegistry.class);

    private final Repository repository;

    public MCPConnectRegistry(Repository repository) {
        this.repository = repository;
    }

    /**
     * Load middleware from the {@code mcp:pipeline} named graph.
     * Falls back to {@link #defaultPipeline()} if the graph is empty or missing.
     */
    public List<I_MCPPipeline> loadAll() {
        List<I_MCPPipeline> result = new ArrayList<>();
        try (RepositoryConnection conn = repository.getConnection()) {
            String sparql = """
                    PREFIX mcp: <urn:mcp:>
                    SELECT ?m ?class ?order ?jwtSecret ?jwtIssuer ?jwtAudience ?jwksUri WHERE {
                        GRAPH <urn:mcp:pipeline> {
                            ?m a mcp:Middleware ;
                               mcp:middlewareClass ?class ;
                               mcp:order ?order ;
                               mcp:enabled true .
                            OPTIONAL { ?m mcp:jwtSecret ?jwtSecret }
                            OPTIONAL { ?m mcp:jwtIssuer ?jwtIssuer }
                            OPTIONAL { ?m mcp:jwtAudience ?jwtAudience }
                            OPTIONAL { ?m mcp:jwksUri ?jwksUri }
                        }
                    } ORDER BY ?order
                    """;
            conn.prepareTupleQuery(sparql).evaluate().stream().forEach(bs -> {
                String className = bs.getValue("class").stringValue();
                try {
                    Class<?> clazz = Class.forName(className);
                    Map<String, Object> config = new java.util.HashMap<>();
                    if (bs.hasBinding("jwtSecret"))  config.put("jwtSecret",  bs.getValue("jwtSecret").stringValue());
                    if (bs.hasBinding("jwtIssuer"))  config.put("jwtIssuer",  bs.getValue("jwtIssuer").stringValue());
                    if (bs.hasBinding("jwtAudience")) config.put("jwtAudience", bs.getValue("jwtAudience").stringValue());
                    if (bs.hasBinding("jwksUri"))    config.put("jwksUri",    bs.getValue("jwksUri").stringValue());

                    Constructor<?> ctor;
                    I_MCPPipeline mw;
                    // Support constructor (Repository, Map) -> (Map) -> (Repository) -> ()
                    try {
                        ctor = clazz.getConstructor(Repository.class, Map.class);
                        mw = (I_MCPPipeline) ctor.newInstance(repository, config);
                    } catch (NoSuchMethodException ex1) {
                        try {
                            ctor = clazz.getConstructor(Map.class);
                            mw = (I_MCPPipeline) ctor.newInstance(config);
                        } catch (NoSuchMethodException ex2) {
                            try {
                                ctor = clazz.getConstructor(Repository.class);
                                mw = (I_MCPPipeline) ctor.newInstance(repository);
                            } catch (NoSuchMethodException ex3) {
                                ctor = clazz.getConstructor();
                                mw = (I_MCPPipeline) ctor.newInstance();
                            }
                        }
                    }

                    result.add(mw);
                    log.info("[MCPConnect] loaded middleware: {} order={}", className,
                             bs.getValue("order").stringValue());
                } catch (Exception ex) {
                    log.warn("[MCPConnect] failed to load middleware class {}: {}", className, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("[MCPConnect] could not query mcp:pipeline graph: {}", ex.getMessage());
        }
        if (result.isEmpty()) {
            log.info("[MCPConnect] no RDF config found — using default pipeline");
            return defaultPipeline();
        }
        return result;
    }

    /**
     * Canonical default pipeline — all built-in middleware in declared order.
     * Used when no {@code mcp:pipeline} graph is configured.
     */
    public static List<I_MCPPipeline> defaultPipeline() {
        return List.of(
            new systems.symbol.mcp.connect.impl.AuthGuardMiddleware(),
            new systems.symbol.mcp.connect.impl.ACLFilterMiddleware(),
            new systems.symbol.mcp.connect.impl.QuotaGuardMiddleware(),
            new systems.symbol.mcp.connect.impl.SparqlSafetyMiddleware(),
            new systems.symbol.mcp.connect.impl.AuditWriterMiddleware()
        );
    }
}
