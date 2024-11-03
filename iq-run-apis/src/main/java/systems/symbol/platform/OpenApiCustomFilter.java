package systems.symbol.platform;

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.OASFilter;

import java.util.Map;
import java.util.ResourceBundle;

// @ApplicationScoped
public class OpenApiCustomFilter implements OASFilter {
private static final ResourceBundle messages = ResourceBundle.getBundle("messages");

@Override
public void filterOpenAPI(OpenAPI openAPI) {
Map<String, PathItem> paths = openAPI.getPaths().getPathItems();
paths.forEach((path, pathItem) -> {
processOperation(pathItem.getGET());
processOperation(pathItem.getPOST());
processOperation(pathItem.getPUT());
processOperation(pathItem.getDELETE());
processOperation(pathItem.getPATCH());
processOperation(pathItem.getOPTIONS());
processOperation(pathItem.getHEAD());
});
}

private void processOperation(Operation operation) {
if (operation.getSummary() != null && operation.getSummary().startsWith("api.")) {
operation.setSummary(resolvePlaceholder(operation.getSummary()));
}
if (operation.getDescription() != null && operation.getDescription().startsWith("api.")) {
operation.setDescription(resolvePlaceholder(operation.getDescription()));
}
}

private String resolvePlaceholder(String key) {
return messages.containsKey(key) ? messages.getString(key) : key;
}
}
