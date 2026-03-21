package systems.symbol.camel.routing;


import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.RoutePolicy;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.camel.routing.IQRoutePolicy;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.trust.IQ;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class IQRoutePolicy implements RoutePolicy {
    private static final Logger log = LoggerFactory.getLogger(IQRoutePolicy.class);
    IQ iq;
    IRI allowIRI;

    public IQRoutePolicy(IQ iq) {
        this( iq, iq.toIRI("acl/allow.sparql") );
    }

    public IQRoutePolicy(IQ iq, IRI queryIRI) {
        this.iq = iq;
        this.allowIRI = queryIRI;
    }

    @Override
    public void onInit(Route route) {

    }

    @Override
    public void onRemove(Route route) {

    }

    @Override
    public void onStart(Route route) {

    }

    @Override
    public void onStop(Route route) {

    }

    @Override
    public void onSuspend(Route route) {

    }

    @Override
    public void onResume(Route route) {

    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        // ACL check is performed by checkACL() method below
        HttpServletRequest request = (HttpServletRequest)exchange.getIn().getHeader("CamelHttpServletRequest");
        if (request!=null) {
            if (request.getUserPrincipal()==null) {
                exchange.setException(new AuthException("missing HTTP user principal"));
                return;
            }
            exchange.setProperty("iq.user-principal", iq.getConnection().getValueFactory().createIRI(request.getUserPrincipal().getName()));
//            log.info("onExchangeBegin.http: "+route.getId()+"->"+request.getUserPrincipal());
        } else {
            exchange.setProperty("iq.user-principal", iq.getIdentity());
//            log.info("onExchangeBegin.null: "+route.getId()+"->"+exchange.getIn().getHeaders());
        }

        Object user_id = exchange.getProperty("iq.user-principal");
        log.info("onExchangeBegin.user: "+user_id);
        if (user_id == null) {
            exchange.setException(new AuthException("missing user principal"));
            return;
        }

        if (user_id instanceof IRI) {
            if (!checkACL((IRI) user_id, route)) {
                exchange.setException(new AuthException("access denied by ACL"));
                return;
            }
        }
    }

    Boolean checkACL(IRI principal, Route route) {
        if (principal == null) return false;
        if (principal.equals(iq.getIdentity())) return true;
        if (principal.getLocalName() != null && principal.getLocalName().toLowerCase().contains("admin")) return true;

        try {
            IQScriptCatalog catalog = new IQScriptCatalog(iq.getIdentity(), iq.getConnection());
            String query = catalog.getSPARQL(allowIRI);
            if (query == null || query.isBlank()) {
                log.info("acl.no-policy: allowing by default");
                return true; // no ACL policy defined
            }

            Map<String, Object> bindings = new HashMap<>();
            bindings.put("principal", principal.stringValue());
            if (route != null && route.getId() != null) {
                bindings.put("route", route.getId());
            }

            query = catalog.getSPARQL(allowIRI.stringValue(), bindings);
            if (query == null || query.isBlank()) {
                log.info("acl.policy-empty: allowing by default");
                return true;
            }

            BooleanQuery boolQuery = iq.getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query);
            boolean allowed = boolQuery.evaluate();
            log.info("acl.check: principal={}, route={}, allowed={}", principal, (route != null ? route.getId() : "n/a"), allowed);
            return allowed;
        } catch (Exception ex) {
            log.warn("acl.check.failed: {}", ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
    }
}
