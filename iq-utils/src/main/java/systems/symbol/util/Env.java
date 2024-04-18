package systems.symbol.util;

public class Env {

    public static String get(String name, String fallback) {
        String env = System.getenv(name);
        return env==null?fallback:env;
    }
}
