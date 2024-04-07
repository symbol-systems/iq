package systems.symbol;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class IQCamelBotTest {

@Test
void testStart() throws IOException {
IQCamelBot iqCamelBot = new IQCamelBot(new File("tmp/iq.test"));
assert iqCamelBot.getRepository()!=null;
}
}