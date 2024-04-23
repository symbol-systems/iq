package systems.symbol.persona;
/*
 *  symbol.systems
 *  Copyright (c) 2023-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import javazoom.jl.decoder.JavaLayerException;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface I_Persona {

	InputStream say(String words);

	void play(InputStream mp3) throws JavaLayerException, IOException;
	void speak(String words) throws JavaLayerException, IOException;

	void listen(Consumer<String> listener) throws IOException, LineUnavailableException;
}
