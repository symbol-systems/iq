package systems.symbol.connect.core;

/**
 * A tiny reusable scanner contract for connector composition.
 *
 * @param <C> scanner context type
 */
@FunctionalInterface
public interface ConnectorScanner<C> {

void scan(C context) throws Exception;
}