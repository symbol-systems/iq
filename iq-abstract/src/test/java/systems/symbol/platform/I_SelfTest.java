package systems.symbol.platform;

import junit.framework.TestCase;

import java.io.IOException;

import static systems.symbol.COMMONS.CODENAME;

public class I_SelfTest extends TestCase {

    public void testVersion() throws IOException {

        System.out.println("* * * * "  + CODENAME + " - v" +I_Self.version());
    }
}