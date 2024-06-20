package systems.symbol;

import javazoom.jl.decoder.JavaLayerException;

import org.testng.annotations.Test;
import systems.symbol.persona.Persona;

import java.io.IOException;

public class PersonaTest {
    @Test
    void testSpeech() throws JavaLayerException, IOException {
        String key = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (key==null||key.isEmpty()) return;
        Persona persona = new Persona();
        persona.play( persona.say("looking good") );

    }
}
