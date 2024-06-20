package systems.symbol.agent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
//import org.apache.ivy.util.CopyProgressEvent;
//import org.apache.ivy.util.CopyProgressListener;
//import org.apache.ivy.util.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.agent.tools.TrustedAPIs;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.io.IOCopier;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simplified string-friendly wrapper for scripting an agent, model and state machine.
 */
public class IQFacade {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Model model;
    private final IRI self;
    private FileSystemManager vfs;
    I_Secrets secrets;
    Gson gson = new Gson();

    /**
     * Constructs a new ScriptFacade with the provided RDF4J model and self identity,
     * creating a LazyAgent as the base agent.
     *
     * @param model The RDF4J model associated with the facade.
     * @param self  The self identity of the facade.
     */

    public IQFacade(@NotNull IRI self, @NotNull Model model, I_Secrets secrets) throws SecretsException {
        this.self = self;
        this.model = model;
        this.secrets = TrustedAPIs.trusted(model, self, secrets);
    }

    protected void enableVFS() throws FileSystemException {
        this.vfs = VFS.getManager();
        log.info("api.vfs: {} ", vfs);
    }
    public RestAPI api(String url) throws SecretsException {
        String secret = secrets==null?null:secrets.getSecret(url);
        log.info("api.secret: {} -> {}", url, secret);
        return new RestAPI(url, secrets);
    }

    public Map<String,Object> json(Response response) throws SecretsException, IOException {
        ResponseBody body = response.body();
        return body==null?null:json(body);
    }

    public Map<String,Object> json(ResponseBody body) throws SecretsException, IOException {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(body.string(), type);
    }

    /**
     * Sets an `rdf:value`.
     *
     * @param value The value of the property.
     * @return The ScriptFacade instance.
     */
    public IQFacade value(Object value) {
        model.add(self, RDF.VALUE, Values.literal(value), this.self);
        return this;
    }

    public Literal value() {
        return get(RDF.VALUE).get(0);
    }

    /**
     * Sets a property in the model.
     *
     * @param key   The key of the property.
     * @param value The value of the property.
     * @return The ScriptFacade instance.
     */
    public IQFacade set(String key, Object value) {
        model.add(self, toIRI(key), Values.literal(value), this.self);
        return this;
    }

    /**
     * Expand a prefixed string key to an IRI or appends a simple key to self IRI.
     *
     * @param key The string key to convert.
     * @return The IRI representation of the key.
     */
    protected IRI toIRI(String key) {
        return NS.toIRI(model, self, key);
    }

    /**
     * Retrieves literals associated with a property in the RDF4J model.
     *
     * @param key The key of the property.
     * @return A list of literals associated with the property.
     */
    public List<Literal> get(String key) {
        return get(toIRI(key));
    }

    protected List<Literal> get(IRI predicate) {
        List<Literal> literals = new ArrayList<>();
        Iterable<Statement> statements = model.getStatements(self, predicate, null, this.self);
        for (Statement statement : statements) {
            if (statement.getObject() instanceof Literal) {
                literals.add((Literal) statement.getObject());
            }
        }
        return literals;
    }

    public FileObject download(String url) throws APIException, IOException, StateException {
        RestAPI api = new RestAPI(url, secrets);
        Response response = api.get();
        assert response.body() != null;
        InputStream in = response.body().byteStream();
        return save(in);
    }
    public FileObject save(InputStream in) throws StateException {
        if (this.vfs==null) throw new StateException("save.disabled", self);
        try {
            File file = new File("tested/saved.png");
            FileObject fileObject = vfs.resolveFile(file.toURI().toASCIIString());
            log.info("copy.save: {}", fileObject);
            fileObject.setWritable(true, true);
            OutputStream out = fileObject.getContent().getOutputStream();
            IOCopier.copy(in, out);

            return fileObject;
        } catch (IOException e) {
            throw new StateException(e.getMessage(), self, e);
        }
    }
}
