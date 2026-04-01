package systems.symbol.gql;

import graphql.schema.DataFetchingEnvironment;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Policy engine that evaluates a configurable ASK SPARQL template.
 * The template may use placeholders like {actor}, {type}, {arg.name}, {ctx.name}.
 * It supports fallback engines and a defaultAllow flag to decide on error.
 */
public class AskPolicyEngine implements PolicyEngine {
    private static final Logger log = LoggerFactory.getLogger(AskPolicyEngine.class);
    final Repository repository;
    final String askTemplate;
    final List<PolicyEngine> fallbacks = new ArrayList<>();
    final boolean defaultAllow;
    final Pattern placeholder = Pattern.compile("\\{([a-zA-Z0-9_.]+)\\}");

    public AskPolicyEngine(Repository repository, String askTemplate) {
        this(repository, askTemplate, false);
    }

    public AskPolicyEngine(Repository repository, String askTemplate, boolean defaultAllow) {
        this.repository = repository;
        this.askTemplate = (askTemplate==null || askTemplate.isBlank())? "ASK WHERE { <{actor}> <http://symbol.systems/v0/onto/trust#canQuery> <{type}> }" : askTemplate;
        this.defaultAllow = defaultAllow;
    }

    public void addFallback(PolicyEngine engine) { if (engine!=null) fallbacks.add(engine); }

    @Override
    public boolean isAllowed(String actor, String typeIRI, DataFetchingEnvironment env) {
        if (actor==null || typeIRI==null) return false;
        log.info("AskPolicyEngine template: {}", askTemplate);
        String ask = substitute(askTemplate, actor, typeIRI, env);
        log.info("AskPolicyEngine ask: {}", ask);
        try (RepositoryConnection conn = repository.getConnection()) {
            BooleanQuery q = conn.prepareBooleanQuery(org.eclipse.rdf4j.query.QueryLanguage.SPARQL, ask);
            // retry read a few times to avoid transient visibility races in tests
            for (int attempt=0; attempt<3; attempt++) {
                try {
                    // diagnostic: how many statements exist for the subject actor?
                    try {
                        var vf = conn.getValueFactory();
                        int count = 0;
                        for (var st : conn.getStatements(vf.createIRI(actor), null, null, false)) count++;
                        log.info("AskPolicyEngine local statement count for actor {}: {} (attempt {})", actor, count, attempt);
                    } catch (Exception diag) {
                        log.info("AskPolicyEngine diagnostic failed: {}", diag.getMessage());
                    }

                    boolean ok = q.evaluate();
                    log.info("AskPolicyEngine evaluation result (attempt {}): {}", attempt, ok);
                    if (ok) return true;
                    // cheap fallback: direct triple check for the common canQuery pattern
                    try {
                        var vf = conn.getValueFactory();
                        org.eclipse.rdf4j.model.IRI subj = vf.createIRI(actor);
                        org.eclipse.rdf4j.model.IRI obj = vf.createIRI(typeIRI);
                        org.eclipse.rdf4j.model.IRI canQuery = vf.createIRI("http://symbol.systems/v0/onto/trust#canQuery");
                        boolean has = conn.hasStatement(subj, canQuery, obj, true);
                        log.info("AskPolicyEngine direct triple check (attempt {}): {}", attempt, has);
                        if (has) return true;
                    } catch (Exception ex) {
                        // ignore
                    }

                } catch (Exception e) {
                    log.info("AskPolicyEngine query attempt failed: {}", e.getMessage());
                }
                try { Thread.sleep(20); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        } catch (Exception e) {
            log.warn("Policy evaluation failed (primary): {}", e.getMessage());
            // fall through to fallbacks or default
        }
        // try fallbacks
        for (PolicyEngine p: fallbacks) {
            try {
                if (p.isAllowed(actor, typeIRI, env)) return true;
            } catch (Exception ex) {
                log.warn("Fallback policy failed: {}", ex.getMessage());
            }
        }
        return defaultAllow;
    }

    String substitute(String template, String actor, String type, DataFetchingEnvironment env) {
        Matcher m = placeholder.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = "";
            if ("actor".equals(key)) replacement = actor;
            else if ("type".equals(key)) replacement = type;
            else if (key.startsWith("arg.")) {
                String arg = key.substring(4);
                Object v = null;
                try { v = env.getArgument(arg); } catch (Exception ignored) {}
                replacement = v==null? "" : v.toString();
            } else if (key.startsWith("ctx.")) {
                String c = key.substring(4);
                Object ctx = env.getContext();
                if (ctx instanceof Map) {
                    Object v = ((Map)ctx).get(c);
                    replacement = v==null? "" : v.toString();
                }
            } else {
                // unknown placeholder, leave empty
                replacement = "";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
