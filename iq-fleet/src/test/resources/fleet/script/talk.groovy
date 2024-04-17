package fleet.script

import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.DescribeVoicesRequest
import software.amazon.awssdk.services.polly.model.DescribeVoicesResponse
import software.amazon.awssdk.services.polly.model.OutputFormat
import software.amazon.awssdk.services.polly.model.PollyRequest
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse
import software.amazon.awssdk.services.polly.model.Voice;

PollyClient polly = PollyClient.builder()
.region(Region.US_WEST_2)
.build();

DescribeVoicesRequest describeVoiceRequest = DescribeVoicesRequest.builder()
.engine("standard")
.build();

DescribeVoicesResponse describeVoicesResult = polly.describeVoices(describeVoiceRequest);
Voice voice = describeVoicesResult.voices().stream()
.filter(v -> v.name().equals("Joanna"))
.findFirst()
.orElseThrow(() -> new RuntimeException("Voice not found"));

SynthesizeSpeechRequest synthReq = SynthesizeSpeechRequest.builder()
.text("hello")
.voiceId(voice.id())
.outputFormat(OutputFormat.MP3)
.build();

ResponseInputStream<SynthesizeSpeechResponse> synthRes = polly.synthesizeSpeech(synthReq);
