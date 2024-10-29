package systems.symbol.agent;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.vfs2.*;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.fsm.StateException;
import systems.symbol.secrets.SecretsException;

import java.io.*;
import java.util.Map;

/**
 * A simplified string-friendly wrapper for scripting.
 */
public interface I_FacadeAPI {

    public RestAPI api(String url) throws SecretsException;

    public Map<String, Object> json(Response response) throws SecretsException, IOException;

    public Map<String, Object> json(ResponseBody body) throws SecretsException, IOException;

    public FileObject download(String url) throws APIException, IOException, StateException;
}
