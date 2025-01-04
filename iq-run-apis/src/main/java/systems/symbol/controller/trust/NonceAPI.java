package systems.symbol.controller.trust;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.secrets.SecretsHelper;
import systems.symbol.string.Validate;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;

@Path("trust/nonce")
public class NonceAPI extends RealmAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
RealmPlatform realms;

@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
log.warn("nonce.preflight: {} @ {}", path, info.getBaseUri());
return new DataResponse(null).build();
}

@Override
public boolean entitled(DecodedJWT jwt, IRI agent) {
return true;
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("{realm}")
public Response nonce(@PathParam("realm") String realmName, @Context UriInfo info, SimpleBindings payload)
throws IOException, OopsException, SecretsException {
log.info("trust.nonce: {} -> {}", realmName, info.getBaseUri());
if (Validate.isMissing(realmName))
return new OopsResponse("ux.trust.nonce.realm", Response.Status.BAD_REQUEST).build();
if (payload == null || !payload.containsKey("address"))
return new OopsResponse("ux.trust.nonce.payload", Response.Status.BAD_REQUEST).build();

I_Realm realm = realms.getRealm(realmName);
if (realm == null)
return new OopsResponse("ux.trust.nonce.realm", Response.Status.NOT_FOUND).build();
Bindings bindings = new SimpleBindings();
String nonce = SecretsHelper.totp(realmName, 10, "HmacSHA1", 1);
String nonced = IdentityHelper.password(realm.getSelf().stringValue(), nonce);

bindings.put("nonced", nonced);
bindings.put("nonce", nonce);
log.info("trust.nonced: {} -> {}", realm.getSelf().stringValue(), nonced);
SimpleResponse response = new SimpleResponse(bindings);
return response.build();
}

}
