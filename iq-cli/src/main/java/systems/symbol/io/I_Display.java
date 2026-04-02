package systems.symbol.io;

public interface I_Display {
void out(String message);
void err(String message);
default void outf(String format, Object... args) {
out(String.format(format, args));
}
default void errf(String format, Object... args) {
err(String.format(format, args));
}
}
