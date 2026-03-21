package systems.symbol.onnx.data.segment;

public class TextSegment {
    private final String text;

    private TextSegment(String text) {
        this.text = text;
    }

    public static TextSegment from(String text) {
        return new TextSegment(text == null ? "" : text);
    }

    public String text() {
        return text;
    }
}