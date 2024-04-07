package systems.symbol.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * systems.symbol (c) 2014-2015,2020-2021
 * Module: systems.symbol.util.io
 * @author Symbol Systems
 * Date  : 5/07/2014
 * Time  : 4:49 PM
 */
public class MimeTypeInputStream extends InputStream {
	String mimeType;
	InputStream in;

	public MimeTypeInputStream(InputStream in, String mimeType) {
		this.mimeType=mimeType;
		this.in=in;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}
}
