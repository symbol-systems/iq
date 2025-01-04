package systems.symbol.tools;

import okhttp3.*;
import okio.BufferedSink;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MultipartRequestBuilder {

public static MultipartBody.Builder multipart(InputStream in, String fileName, Map<String, Object> queryParams)
throws IOException {
MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM);

// Add form fields
for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
multipartBody.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
}

// Add file part
if (in != null && fileName != null) {
// Create a RequestBody for the file
RequestBody fileBody = new RequestBody() {
@Override
public MediaType contentType() {
return MediaType.parse("application/octet-stream");
}

@Override
public void writeTo(BufferedSink sink) throws IOException {
byte[] buffer = new byte[8192];
int bytesRead;
while ((bytesRead = in.read(buffer)) != -1) {
sink.write(buffer, 0, bytesRead);
}
}

};

multipartBody.addFormDataPart("file", fileName, fileBody);
}

return multipartBody;
}

}
