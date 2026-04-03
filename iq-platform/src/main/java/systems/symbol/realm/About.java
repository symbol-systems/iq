package systems.symbol.realm;

import org.eclipse.rdf4j.model.Model;

/**
 * Legacy entry-point for graph measurements. The real implementation lives in iq-rdf4j.
 */
public class About {

/**
 * Backward-compatible bridge to {@link systems.symbol.rdf.analytics.GraphAnalytics}.
 */
@Deprecated
public static double computePhi(Model model) {
return systems.symbol.rdf.analytics.GraphAnalytics.computePhi(model);
}

/**
 * Backward-compatible bridge to {@link systems.symbol.rdf.analytics.GraphAnalytics}.
 */
@Deprecated
public static double computePhiNormal(Model model) {
return systems.symbol.rdf.analytics.GraphAnalytics.computePhiNormal(model);
}
}
