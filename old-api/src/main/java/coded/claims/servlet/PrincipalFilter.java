package systems.symbol.servlet;

import org.apache.http.auth.BasicUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.UUID;

@Component
@Order(1)
public class PrincipalFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(PrincipalFilter.class);
    public static final String ANON = "urn:auth:anonymous";

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("iq.filter.principal.init: "+filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse _response, FilterChain filters) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            filters.doFilter(request, _response);
            return;
        }
        HttpServletResponse response = (HttpServletResponse) _response;
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            String authorization = httpServletRequest.getHeader("Authorization");
            // first, try JWT
            if (authorization!=null && !authorization.isEmpty()) {
                useJWT(authorization, httpServletRequest, response, filters);
            } else if (request.getScheme().equals("https")) {
                // then X509
                useX509(httpServletRequest, response, filters);
            } else {
                useAnon(httpServletRequest, response, filters);
            }
        } catch (URISyntaxException e) {
            log.error("Principal has invalid URI", e);
            response.setStatus(400); // invalid parameters
            response.addHeader("X-IQ-ERROR", e.getLocalizedMessage());
        }
    }

    private void useAnon(HttpServletRequest httpServletRequest, ServletResponse response, FilterChain filters) throws ServletException, IOException, URISyntaxException {
        String id = ANON+"#"+fingerprint(httpServletRequest);
        BasicUserPrincipal userPrincipal = new BasicUserPrincipal(id);
        log.info("iq.filter.anonymous: "+userPrincipal);
        PrincipalRequestWrapper principalRequest = new PrincipalRequestWrapper(httpServletRequest, userPrincipal);
        filters.doFilter(principalRequest,response);
    }

    private void useX509(HttpServletRequest request, ServletResponse response, FilterChain filters) throws ServletException, IOException, URISyntaxException {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null && certs.length >0) {
            Principal subjectDN = certs[0].getSubjectDN();
            log.info("iq.filter.x509: "+subjectDN);
            // TODO: validate subjectDN as IRI
            PrincipalRequestWrapper principalRequest = new PrincipalRequestWrapper(request, subjectDN);
            filters.doFilter(principalRequest, response);
        }
    }

    private void useJWT(String authorization, HttpServletRequest httpServletRequest, ServletResponse response, FilterChain filters) {
        log.info("iq.filter.jwt: "+authorization);
    }

    private String fingerprint(HttpServletRequest request) {
        StringBuilder entropy = new StringBuilder();
        log.info("iq.filter.fingerprint: "+ request.getAuthType()+" -> "+request.getHeaderNames());

        entropy.append(UUID.randomUUID());
        return entropy.toString();
    }

    public void destroy() {}
}
