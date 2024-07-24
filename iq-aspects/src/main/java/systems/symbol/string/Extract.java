package systems.symbol.string;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extract {
    static String EXTRACT_REGEX = "([a-zA-Z0-9:/]+)";
    static Pattern EXTRACT_SNIPPET = Pattern.compile("```(\\w+\n)\\s*(.*?)\n```", Pattern.DOTALL);

    public static String extractIntent(String s) {
        Pattern pattern = Pattern.compile(EXTRACT_REGEX);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find() && matcher.group(1).indexOf(":")>0) {
            return matcher.group(1);
        }
        return "";
    }

    public static String hackItToWork(String msg) {
        Matcher matcher = EXTRACT_SNIPPET.matcher(msg);
        if (!matcher.find()) return msg;
        return matcher.group(2);
    }

}
