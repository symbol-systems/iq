package systems.symbol.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

public class PrincipalRequestWrapper extends HttpServletRequestWrapper  {
    Principal userPrincipal;

    public PrincipalRequestWrapper(HttpServletRequest request, Principal userPrincipal) throws URISyntaxException {
        super(request);
        validate(userPrincipal.getName());
        setUserPrincipal(userPrincipal);
    }

    private void validate(String name) throws URISyntaxException {
        new URI(name);
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    protected void setUserPrincipal(Principal userPrincipal) {
        this.userPrincipal=userPrincipal;
    }
}
