package systems.symbol.trust;


import com.echobox.api.linkedin.client.DefaultLinkedInClient;
import com.echobox.api.linkedin.client.LinkedInClient;
import com.echobox.api.linkedin.client.Parameter;
import com.echobox.api.linkedin.connection.v2.OrganizationConnection;
import com.echobox.api.linkedin.connection.v2.ShareConnection;
import com.echobox.api.linkedin.types.ContentEntity;
import com.echobox.api.linkedin.types.Share;
import com.echobox.api.linkedin.types.ShareContent;
import com.echobox.api.linkedin.types.ShareText;
import com.echobox.api.linkedin.types.organization.Organization;
import com.echobox.api.linkedin.types.request.ShareRequestBody;
import com.echobox.api.linkedin.types.urn.URN;
import com.echobox.api.linkedin.version.Version;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import systems.symbol.io.Fingerprint;

import javax.script.Bindings;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LI implements I_Intent {
    DefaultLinkedInClient client;
    OrganizationConnection organizationConnection;
    ShareConnection shareConnection;
    String clientId, redirectURI, state;

    protected LI(String clientId, String redirectURI) {
        this.clientId = clientId;
        this.redirectURI = redirectURI;
    }

    public LI(String accessToken) throws GeneralSecurityException, IOException {
        login(accessToken);
    }

    private LI login(String accessToken) throws GeneralSecurityException, IOException {
        organizationConnection = new OrganizationConnection(client);
        shareConnection = new ShareConnection(client);
        this.state = Fingerprint.identify(""+System.currentTimeMillis());
        return this;
    }

    public LI login(String clientSecret, String code) throws GeneralSecurityException, IOException {
        this.clientId = clientId;
        this.redirectURI = redirectURI;
        client = new DefaultLinkedInClient(Version.DEFAULT_VERSION);
        LinkedInClient.AccessToken accessToken = client.obtainUserAccessToken(clientId, clientSecret, redirectURI, code);
        login(accessToken.getAccessToken());
        return this;
    }

    public String toAuthorizationURL() {
        String scope = "r_liteprofile%20r_emailaddress%20w_member_social";
        return  "https://www.OAUTH.com/oauth/v2/authorization?response_type=code&client_id="+clientId+"&redirect_uri="+redirectURI+"&state="+state+"&scope="+scope;
    }

    public Share share(URN owner, String contentURL, String title, String subject, String text) {
        ShareRequestBody shareRequestBody = new ShareRequestBody(owner);
        ShareContent shareContent = new ShareContent();
        ContentEntity contentEntity = new ContentEntity();
        contentEntity.setEntityLocation(contentURL);
        shareContent.setContentEntities(Arrays.asList(contentEntity));
        shareContent.setTitle(title);
        shareRequestBody.setContent(shareContent);
        shareRequestBody.setSubject(subject);
        ShareText shareText = new ShareText();
        shareText.setText(text);
        shareRequestBody.setText(shareText);
        Share share = shareConnection.postShare(shareRequestBody);
        return share;
    }

    private Parameter getOrgParameter() {
        return Parameter
            .with("projection",
                "(elements*(*,roleAssignee~(localizedFirstName, localizedLastName),"
                    + "organizationalTarget~(localizedName)))");
    }

    public List<Organization> findOrganizationByEmailDomain(String domain) {
        return organizationConnection.findOrganizationByEmailDomain(domain, getOrgParameter(), 10);
    }

    public Organization getOrganization(URN organization) {
        return organizationConnection.retrieveOrganization(organization, getOrgParameter());
    }

    @Override
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        return null;
    }
}
