package systems.symbol.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntentMessageTest {

    @Test
    void extract() {

        assert IntentMessage.extract("#QRX:ABC").equals("QRX:ABC");
        assert IntentMessage.extract("[QRX:ABC]").equals("QRX:ABC");
        assert IntentMessage.extract("[#QRX:ABC]").equals("QRX:ABC");
        assert IntentMessage.extract("(#QRX:ABC]").equals("QRX:ABC");
        assert IntentMessage.extract("QRX:ABC]").equals("QRX:ABC");
        assert !IntentMessage.extract("XQRX:ABC").equals("QRX:ABC");
    }
}