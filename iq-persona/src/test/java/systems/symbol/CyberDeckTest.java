package systems.symbol;

import de.rcblum.stream.deck.StreamDeckController;
import de.rcblum.stream.deck.device.IStreamDeck;
import de.rcblum.stream.deck.device.StreamDeck;
import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import de.rcblum.stream.deck.items.FolderItem;
import de.rcblum.stream.deck.items.StreamItem;
import de.rcblum.stream.deck.items.URIItem;
import de.rcblum.stream.deck.util.IconHelper;
import de.rcblum.stream.deck.util.SDImage;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CyberDeckTest {


    static IStreamDeck sd = null;

//    @BeforeAll
    public static void init() throws IOException {
        List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
        for(HidDeviceInfo hid: devList) {
            System.out.println("hid: "+hid.getDeviceId()+", v: "+hid.getVendorId()+", p: "+hid.getProductId()+" -> "+hid.getPath());
            if (hid.getVendorId()==4057 && hid.getProductId()==109) {
                HidDevice hidDevice = PureJavaHidApi.openDevice(hid);
                sd = new StreamDeck(hidDevice, 99, 15);
            }
        }
        System.out.println("deck: "+sd);
    }

    public SDImage setButton(int button, String path) throws IOException {
        SDImage iconData = IconHelper.loadImage(path);
        sd.drawImage(button,  iconData);
        System.out.println("button: "+button+" @ "+path);
        return iconData;
    }
//    @Test
    public void testShowButtons() throws IOException, URISyntaxException, InterruptedException {
        if (sd==null) return;

        System.out.println("buttons: "+(new File(".")).getAbsolutePath());
        StreamItem[] items = new StreamItem[15];
        StreamItem item = new URIItem(setButton(7, "src/test/resources/buttons/button0.png"), new URI("https://symbol.systems/"));
        items[7] = item;
        item.setText("Demo");
        item.setTextPosition(StreamItem.TEXT_POS_TOP);
        FolderItem root = new FolderItem(null, null, items);
        sd.reset();
        StreamDeckController sdc = new StreamDeckController(sd, root);
        assert sdc.getStreamDeck().isHardware();
        Thread.sleep(10000);

    }

//    @Test
    public void testReceiveButtons() {
        if (sd==null) return;
        sd.reset();
        sd.setBrightness(100);
        System.out.println("waiting ...");
        sd.addKeyListener(new StreamKeyListener() {
            @Override
            public void onKeyEvent(KeyEvent keyEvent) {
                System.out.println("key: "+keyEvent.getKeyId());
            }
        });
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

//    @AfterAll
    public static void deinit() {
        if (sd==null) return;
        System.out.println("------------------------ DEINIT -------------------------");
        sd.stop();
        sd.waitForCompletion();
        sd = null;
    }

    static class ButtonListener implements StreamKeyListener {
        ConcurrentLinkedQueue<KeyEvent> event = new ConcurrentLinkedQueue<>();

        @Override
        public void onKeyEvent(KeyEvent event) {
            this.event.add(event);
        }

        public KeyEvent getNext(int timeout) {
            long start = System.currentTimeMillis();
            while (event.isEmpty() && System.currentTimeMillis() - start < timeout)
                ;
            if (System.currentTimeMillis() - start > timeout)
                System.out.println("  Timeout for polling an KeyEvent reached");
            return event.poll();
        }
    }
}
