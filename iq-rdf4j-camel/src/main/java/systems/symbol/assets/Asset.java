package systems.symbol.assets;

public class Asset {
private final String uri;
private final String contentType;
private final String data;

public Asset(String uri, String contentType) {
this(uri, contentType, "");
}

public Asset(String uri, String contentType, String data) {
this.uri = uri;
this.contentType = contentType;
this.data = data;
}

public String getIdentity() {
return uri;
}

public String getContentType() {
return contentType;
}

@Override
public String toString() {
return data == null ? "" : data;
}
}
