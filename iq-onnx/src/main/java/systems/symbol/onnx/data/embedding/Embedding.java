package systems.symbol.onnx.data.embedding;

public class Embedding {
private final float[] vector;

public Embedding(float[] vector) {
if (vector == null) {
throw new IllegalArgumentException("vector cannot be null");
}
this.vector = vector;
}

public float[] vector() {
return vector;
}

public static Embedding of(float[] vector) {
return new Embedding(vector);
}
}