package systems.symbol.camel.component.fingerprint;

import systems.symbol.io.Fingerprint;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Message;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.fingerprint
 * @author Symbol Systems
 * Date  : 26/06/2014
 * Time  : 11:20 PM
 */
public class FingerprintHandler {

	public FingerprintHandler() {
	}

	@Handler
	public void handle(Exchange exchange) throws IOException, NoSuchAlgorithmException {
		Message in = exchange.getIn();
		InputStream body = in.getBody(InputStream.class);
		String identify = Fingerprint.identify(body);
		in.getHeaders().put("myiq.cloud.fingerprint", identify);

	}
}
