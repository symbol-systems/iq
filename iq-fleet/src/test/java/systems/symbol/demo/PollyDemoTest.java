package systems.symbol.demo;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.JavaSoundAudioDeviceFactory;
import javazoom.jl.player.Player;
import javazoom.jl.player.jlp;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;
import systems.symbol.rdf4j.io.IOCopier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PollyDemoTest {
    String AWS_KEY = System.getenv("AWS_ACCESS_KEY_ID");

//    @Test
    void saySample() throws IOException, JavaLayerException, InterruptedException {
        String[] args = {"src/test/resources/audio/test.mp3"};
        jlp player = jlp.createInstance(args);
        player.setAudioDevice(FactoryRegistry.systemRegistry().createAudioDevice(JavaSoundAudioDeviceFactory.class));

        player.play();
        System.out.println("audio.tested");
    }


    @Test
    void sayHello() throws IOException, JavaLayerException {
        if (AWS_KEY==null) return;
        String speak = IOCopier.load(new File("src/test/resources/fleet/script/talk.md"));
        PollyClient polly = PollyClient.builder()
//                .region(Region.AP_SOUTHEAST_2)
                .region(Region.US_EAST_1)
                .build();
        // long-form (Danielle), neural (Joanna/Niamh), standard
        DescribeVoicesRequest describeVoiceRequest = DescribeVoicesRequest.builder()
                .engine("neural")
                .build();

        DescribeVoicesResponse describeVoicesResult = polly.describeVoices(describeVoiceRequest);
//        describeVoicesResult.voices().forEach( voice -> {
//            System.out.println("voice: "+voice);
//        });

        Voice voice = describeVoicesResult.voices().stream()
                .filter(v -> v.name().equalsIgnoreCase("Joanna"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Voice not found"));
//        if (!voice.isPresent()) return;
        System.out.println("voice.chosen: "+voice);
        InputStream stream = synthesize(polly, speak, voice, OutputFormat.MP3);

        Player player = new Player(stream);
        player.play();
        System.out.println("audio.tested");
    }

    public static InputStream synthesize(PollyClient polly, String text, Voice voice, OutputFormat format)
            throws IOException {
        SynthesizeSpeechRequest synthReq = SynthesizeSpeechRequest.builder()
                .text(text)
                .voiceId(voice.id())
                .outputFormat(format)
                .build();

        return polly.synthesizeSpeech(synthReq);
    }
}
