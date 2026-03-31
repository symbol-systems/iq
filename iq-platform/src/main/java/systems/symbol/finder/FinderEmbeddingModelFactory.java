package systems.symbol.finder;

import java.lang.reflect.Method;

public class FinderEmbeddingModelFactory {

public static FinderEmbeddingModel defaultModel() {
try {
return new OnnxEmbeddingModel();
} catch (Throwable t) {
return new HashFallbackEmbeddingModel();
}
}

private static class OnnxEmbeddingModel implements FinderEmbeddingModel {
private final Object onnxInstance;
private final Method embedMethod;
private final Method contentMethod;
private final Method vectorMethod;
private final Method textSegmentFromMethod;

OnnxEmbeddingModel() throws Exception {
Class<?> modelClass = Class.forName("systems.symbol.onnx.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel");
Class<?> textSegmentClass = Class.forName("systems.symbol.onnx.data.segment.TextSegment");
Class<?> responseClass = Class.forName("systems.symbol.onnx.model.output.Response");

this.onnxInstance = modelClass.getConstructor().newInstance();
this.embedMethod = modelClass.getMethod("embed", textSegmentClass);
this.textSegmentFromMethod = textSegmentClass.getMethod("from", String.class);
this.contentMethod = responseClass.getMethod("content");
this.vectorMethod = Class.forName("systems.symbol.onnx.data.embedding.Embedding").getMethod("vector");

// check methods available
if (this.onnxInstance == null || this.embedMethod == null || this.contentMethod == null || this.vectorMethod == null) {
throw new IllegalStateException("onnx model adapter could not be constructed");
}
}

@Override
public float[] embed(String text) {
try {
Object textSegment = textSegmentFromMethod.invoke(null, text == null ? "" : text);
Object response = embedMethod.invoke(onnxInstance, textSegment);
Object embedding = contentMethod.invoke(response);
Object vector = vectorMethod.invoke(embedding);
return (float[]) vector;
} catch (Throwable t) {
return new HashFallbackEmbeddingModel().embed(text);
}
}
}

private static class HashFallbackEmbeddingModel implements FinderEmbeddingModel {
private static final int DIM = 384;

@Override
public float[] embed(String text) {
float[] vector = new float[DIM];
if (text == null) {
text = "";
}
String normalized = text.trim().toLowerCase();
String[] words = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
int seed = normalized.hashCode();
for (String word : words) {
int wseed = word.hashCode() ^ seed;
for (int i = 0; i < DIM; i++) {
vector[i] += (float) Math.sin(wseed * 1315423911L + i * 31L) * 0.05f;
}
}
if (words.length == 0) {
vector[0] = 1.0f;
}
float norm = 0.0f;
for (float value : vector) {
norm += value * value;
}
norm = (float) Math.sqrt(norm);
if (norm == 0f) return vector;
for (int i = 0; i < vector.length; i++) {
vector[i] = vector[i] / norm;
}
return vector;
}
}
}
