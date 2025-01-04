package systems.symbol.controller.ux;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.gson.Gson;

import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.llm.Conversation;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.tools.RestAPI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.script.SimpleBindings;
import javax.script.Bindings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Path("/ux/vox")
@Tag(name = "api.ux.vox.name", description = "api.ux.vox.description")
public class VoxAPI extends GuardedAPI {

String url = "https://api.groq.com/openai/v1/audio/transcriptions";

@ConfigProperty(name = "iq.realm.vox.maxLength", defaultValue = "10240000") // 10 MB default
int maxLength;

@GET
@Operation(summary = "api.ux.vox.get.summary", description = "api.ux.vox.get.description")
@Path("{realm}")
public Response getSpeech(
@PathParam("realm") String _realm,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
log.info("ux.vox: {} --> {}", _realm, uriInfo.getQueryParameters().keySet());
return new OopsResponse("ux.vox.ignore", Response.Status.BAD_REQUEST).build();
}

@POST
@Path("{realm}")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Operation(summary = "api.ux.vox.post.summary", description = "api.ux.vox.post.description")
public Response transcribe(
@PathParam("realm") String _realm,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth,
@FormParam("file") InputStream fileInputStream) throws IOException, SecretsException {

if (fileInputStream == null) {
return new OopsResponse("api.vox.files.missing", Response.Status.BAD_REQUEST).build();
}
if (Validate.isNonAlphanumeric(_realm)) {
return new OopsResponse("ux.vox.realm", Response.Status.BAD_REQUEST).build();
}
if (!Validate.isBearer(auth)) {
return new OopsResponse("ux.vox.unauthorized", Response.Status.UNAUTHORIZED).build();
}

I_Realm realm = platform.getRealm(_realm);
APISecrets apiSecrets = new APISecrets(realm.getSecrets());
apiSecrets.grant(url, "MY_GROQ_API_KEY");
String secret = apiSecrets.getSecret(url);
log.info("ux.vox.secret: {}", secret);
if (Validate.isMissing(secret)) {
return new OopsResponse("ux.vox.key-missing", Response.Status.UNAUTHORIZED).build();
}
RestAPI api = new RestAPI(url);
api.header("Authorization", "Bearer " + secret);
Bindings config = new SimpleBindings();
config.put("model", "distil-whisper-large-v3-en");
config.put("temperature", "0.1");
config.put("prompt", "transcribe and fix typos");
config.put("response_format", "json");
log.info("ux.vox.transcribe: {} --> {}", _realm, fileInputStream.available());
okhttp3.Response transcribed = api.multipart(fileInputStream, "audio.webm", config);
String text = transcribed.body().string();
log.info("ux.vox.transcribed: {}", text);

SimpleBindings json = new Gson().fromJson(text, SimpleBindings.class);
if (!transcribed.isSuccessful()) {
return new OopsResponse("ux.vox.transcribe", Response.Status.BAD_GATEWAY).build();
}
Conversation msgs = new Conversation();
msgs.user(json.getOrDefault("text", "").toString());

return new SimpleResponse(msgs).build();
}
}
