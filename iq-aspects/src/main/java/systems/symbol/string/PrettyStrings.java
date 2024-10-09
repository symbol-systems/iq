package systems.symbol.string;

import javax.script.Bindings;
import java.util.Collection;
import java.util.Map;

public class PrettyStrings {

    public static String pretty(Object object) {
        StringBuilder sb = new StringBuilder();
        pretty(sb, object, 0);
        return sb.toString();
    }

    private static void pretty(StringBuilder sb, Object object, int indent) {
        if (object == null) {
            sb.append("null");
        } else if (object instanceof Bindings || object instanceof Map<?, ?>) {
            prettyMap(sb, object, indent);
        } else if (object instanceof Collection<?>) {
            prettyCollection(sb, (Collection<?>) object, indent);
        } else {
            sb.append(object.toString());
        }
    }

    private static void prettyMap(StringBuilder sb, Object object, int indent) {
        sb.append("{\n");
        Map<?, ?> map = (object instanceof Bindings) ? (Bindings) object : (Map<?, ?>) object;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            appendIndented(sb, entry.getKey() + ": ", indent);
            pretty(sb, entry.getValue(), indent + 2);
            sb.append("\n");
        }
        appendIndentation(sb, indent);
        sb.append("}");
    }

    private static void prettyCollection(StringBuilder sb, Collection<?> collection, int indent) {
        sb.append("[\n");
        int size = collection.size();
        int i = 0;
        for (Object item : collection) {
            appendIndented(sb, "", indent);
            pretty(sb, item, indent + 2);
            if (i++ < size - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n");
        appendIndentation(sb, indent);
        sb.append("]");
    }

    private static void appendIndented(StringBuilder sb, String text, int indent) {
        sb.append(" ".repeat(indent)).append(text);
    }

    private static void appendIndentation(StringBuilder sb, int indent) {
        sb.append(" ".repeat(indent));
    }

}
