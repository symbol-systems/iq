package systems.symbol.camel.routing;


import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.RoutePolicy;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;

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
// TODO: Check ACL
HttpServletRequest request = (HttpServletRequest)exchange.getIn().getHeader("CamelHttpServletRequest");
if (request!=null) {
if (request.getUserPrincipal()==null) {
exchange.setException(new AuthException("missing HTTP user principal"));
return;
}
exchange.setProperty("iq.user-principal", iq.getConnection().getValueFactory().createIRI(request.getUserPrincipal().getName()));
//log.info("onExchangeBegin.http: "+route.getId()+"->"+request.getUserPrincipal());
} else {
exchange.setProperty("iq.user-principal", iq.getIdentity());
//log.info("onExchangeBegin.null: "+route.getId()+"->"+exchange.getIn().getHeaders());
}

Object user_id = exchange.getProperty("iq.user-principal");
log.info("onExchangeBegin.user: "+user_id);
if (user_id == null) {
exchange.setException(new AuthException("missing user principal"));
return;
}
}

Boolean checkACL(IQ iq) {
return true;
}

@Override
public void onExchangeDone(Route route, Exchange exchange) {
}
}
