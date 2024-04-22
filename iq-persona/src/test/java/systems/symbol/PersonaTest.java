package systems.symbol;

import javazoom.jl.decoder.JavaLayerException;
import org.junit.jupiter.api.Test;
import systems.symbol.persona.Persona;

public class PersonaTest {
@Test
void testSpeech() throws JavaLayerException {
Persona persona = new Persona();
persona.play( persona.say("looking good") );
}
}
