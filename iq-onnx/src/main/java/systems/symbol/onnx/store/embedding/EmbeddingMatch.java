package systems.symbol.onnx.store.embedding;

public class EmbeddingMatch<T> {
    private final T segment;
    private final double score;

    public EmbeddingMatch(T segment, double score) {
        this.segment = segment;
        this.score = score;
    }

    public T segment() {
        return segment;
    }

    public double score() {
        return score;
    }
}