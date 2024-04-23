package systems.symbol.persona;
/*
 *  symbol.systems
 *  Copyright (c) 2023-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.vosk.Model;
import org.vosk.Recognizer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class Persona implements I_Persona {
	public Persona()  {
	}

	public void speak(String words) throws JavaLayerException, IOException {
		play(say(words));
	}

	public InputStream say(String words) {
		PollyClient polly = PollyClient.builder()
//.region(Region.AP_SOUTHEAST_2)
				.region(Region.US_EAST_1)
				.build();
		// long-form (Danielle), neural (Joanna/Niamh), standard
		DescribeVoicesRequest describeVoiceRequest = DescribeVoicesRequest.builder()
				.engine("neural")
				.build();

		DescribeVoicesResponse describeVoicesResult = polly.describeVoices(describeVoiceRequest);
//describeVoicesResult.voices().forEach( voice -> {
//System.out.println("voice: "+voice);
//});

		Voice voice = describeVoicesResult.voices().stream()
				.filter(v -> v.name().equalsIgnoreCase("Joanna"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Voice not found"));
//if (!voice.isPresent()) return;
		System.out.println("voice.chosen: "+voice);
		return synthesize(polly, words, voice, OutputFormat.MP3);

	}

	public void play(InputStream mp3) throws JavaLayerException, IOException {
		System.out.println("audio.played: "+mp3);
		if (mp3==null) return;
		Player player = new Player(mp3);
		player.play();
	}

	public static InputStream synthesize(PollyClient polly, String text, Voice voice, OutputFormat format) {
		SynthesizeSpeechRequest synthReq = SynthesizeSpeechRequest.builder()
				.text(text)
				.voiceId(voice.id())
				.outputFormat(format)
				.build();
		return polly.synthesizeSpeech(synthReq);
	}

	public void listen(Consumer<String> listener) throws IOException, LineUnavailableException {
		System.out.println("testRecognize: "+new File("model").getAbsolutePath());
		Model model = new Model("model/vosk-model-en-us-0.22-lgraph"); //
		Recognizer recognizer = new Recognizer(model, 16000);
		AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			recognizer.close();
			System.out.println("Microphone not supported!");
			return;
		}

		System.out.println("listening ...");
		TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
		microphone.open(format);
		microphone.start();

		// Buffer size for audio data
		int bufferSize = 8096;
		byte[] buffer = new byte[bufferSize];

		// Recognition loop
		String said = "";

		while (!said.contains("stop")) {
			// Read audio data from the microphone
//System.out.print(".");
			int bytesRead = microphone.read(buffer, 0, buffer.length);
			if (bytesRead <= 0) {
				break;
			}
			// Feed audio data to the recognizer
//System.out.print(": ");
			boolean accepted = recognizer.acceptWaveForm(buffer, bytesRead);

			if (accepted) {
				said = recognizer.getFinalResult();
				System.out.println(said);
				listener.accept( said );
			}
//else
//System.out.println("?: " + recognizer.getPartialResult());
		}
		System.out.println("not listening.");
		// Close the microphone and recognizer
		microphone.stop();
		microphone.close();
		recognizer.close();
	}
}
