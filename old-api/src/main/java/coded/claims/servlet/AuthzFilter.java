package systems.symbol.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

import static systems.symbol.servlet.PrincipalFilter.ANON;

@Component
@Order(2)
public class AuthzFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AuthzFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("iq.filter.auth.init: "+filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filters) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if (httpRequest.getServletPath().equals("/")) {
                log.info("iq.filter.auth.index");
                // home/index is always allowed
                filters.doFilter(httpRequest, response);
            } else if (httpRequest.getServletPath().startsWith("/public/")) {
                log.info("iq.filter.auth.public");
                // public is always allowed
                filters.doFilter(httpRequest, response);
            } else {
                Principal principal = httpRequest.getUserPrincipal();
                // reject quickly if anon user
                if (null == principal || principal.getName().startsWith(ANON)) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    log.info("iq.filter.auth.anon: "+ principal);
                    httpResponse.setStatus(401); // lacks valid authentication credentials
//TODO: remove
                    filters.doFilter(httpRequest,response);
                    return;
                }
                // TODO
                // verify identity
                // check roles
                // continue request processing
                log.info("iq.filter.auth.user: "+ principal);
                filters.doFilter(httpRequest,response);
            }
        }
    }

    public void destroy() {}
}
