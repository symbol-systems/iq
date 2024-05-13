package systems.symbol.hub;

import com.echobox.api.linkedin.types.organization.Organization;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

public class LinkedInTest {
    String clientId = "86wnbjyvvassqk";
    String clientSecret = "DKfahhrOv4K6Rv1r";
    String redirectURL = "http://localhost:9080/callback/linkedin";

    // @Test
    public void testLogin() throws GeneralSecurityException, IOException {
        LinkedIn linkedIn = new LinkedIn(clientId, redirectURL);
        String authorizationURL = linkedIn.toAuthorizationURL();
        System.out.println("authorizationURL: "+authorizationURL);
    }

//    @Test
    public void testTestLogin() throws GeneralSecurityException, IOException {
        String code = "AQSVlkFrO_Nx2Wxralh1Fkush2sGJ2pGO-uqfUiufTtaRKfgK4uXIp88RzNcjtytuH-a18JK_Yt46vg8HyeUbWTByQ4pmEvo8Et3jk-aOIWgJFoOJGbnzAZyc-3MmYNGwkKp3krnwOLpifrm1kuq5h1vft3z9bhCjztdQei4vyr2VbFrPF1VnrsOu_xW8FGi-D-YEZu40dGpEExg0wQ";

        LinkedIn linkedIn = new LinkedIn(clientId, redirectURL);
        linkedIn.login(clientSecret, code);
        System.out.println("authorizationURL: "+linkedIn);
        Collection<Organization> organizations = linkedIn.findOrganizationByEmailDomain("symbol.systems");
        System.out.println("organizations: "+organizations);
    }

    @Test
    public void testShare() {
    }

    @Test
    public void testGetOrganization() {
    }
}