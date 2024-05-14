package systems.symbol.platform;

import junit.framework.TestCase;

import java.io.IOException;

import static systems.symbol.COMMONS.IQ;

public class I_SelfTest extends TestCase {

public void testVersion() throws IOException {

System.out.println("* * * * "  + IQ + " - v" +I_Self.version());
}
}