package systems.symbol.llm;

import org.junit.jupiter.api.Test;

import static systems.symbol.string.Extract.extractIntent;

class IntentMessageTest {

    @Test
    void extract() {

        assert extractIntent("#QRX:ABC").equals("QRX:ABC");
        assert extractIntent("[QRX:ABC]").equals("QRX:ABC");
        assert extractIntent("[#QRX:ABC]").equals("QRX:ABC");
        assert extractIntent("(#QRX:ABC]").equals("QRX:ABC");
        assert extractIntent("QRX:ABC]").equals("QRX:ABC");
        assert !extractIntent("XQRX:ABC").equals("QRX:ABC");
    }
}