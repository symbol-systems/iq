package systems.symbol.io;

public class ConsoleDisplay implements I_Display {
public static final ConsoleDisplay INSTANCE = new ConsoleDisplay();

private ConsoleDisplay() {
}

public static ConsoleDisplay getInstance() {
return INSTANCE;
}

@Override
public void out(String message) {
System.out.println(message == null ? "" : message);
}

@Override
public void err(String message) {
System.err.println(message == null ? "" : message);
}
}
