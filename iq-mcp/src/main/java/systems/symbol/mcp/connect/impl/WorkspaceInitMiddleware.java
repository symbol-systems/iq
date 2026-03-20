package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

/**
 * WorkspaceInitMiddleware — checks that a realm is set in the request context (order: 5).
 *
 * <p>Used to prevent execution of MCP tools before kernel/realm selection is complete.
 */
public class WorkspaceInitMiddleware implements I_MCPPipeline {

private static final IRI SELF = Values.iri("urn:mcp:pipeline/WorkspaceInit");

@Override
public IRI getSelf() {
return SELF;
}

@Override
public int getOrder() {
return 5;
}

@Override
public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
IRI realm = ctx.realm();
if (realm == null) {
throw MCPException.badRequest("kernel.workspace.not.initialised");
}
return chain.proceed(ctx);
}
}
