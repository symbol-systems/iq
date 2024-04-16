package systems.symbol.rdf4j.io;
/*
 *  symbol.systems - see license
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * symbol.systems (c) 2010-2013
 * @author Symbol Systems
 * Date: 12/02/13
 * Time: 6:27 PM
 * 
 * Common IO
 * 
 */
public class IOCopier {
	private static final Logger log = LoggerFactory.getLogger(IOCopier.class);

	public IOCopier(InputStream input, OutputStream output) throws IOException {
		log.debug("Copying binary stream ...");
		copy(input, output);
		input.close();
	}

	public IOCopier(InputStream input, File output) throws IOException {
		log.debug("binary to: ${output.absolutePath}");
		FileOutputStream fos = new FileOutputStream(output);
		copy(input, fos);
		fos.close();
		input.close();
	}

	public IOCopier(File input, OutputStream output) throws IOException {
		log.debug("file to: ${input.absolutePath}");
		InputStream inStream = new FileInputStream(input);
		if (input.exists()) copy(inStream, output);
		inStream.close();
	}

	public static void copy(InputStream input, Writer output) throws IOException {
		IOUtils.copy(input, output, "UTF-8");
		output.flush();
	}


	public static int copy(Reader input, Writer output, int buffer_size) throws IOException {
		char[] buffer = new char[buffer_size + 16];
		int n = 0, c = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			output.flush();
			c += n;
		}
		output.flush();
		return c;
	}

	public static int copy(InputStream input, OutputStream output) throws IOException {
		int copied = IOUtils.copy(input, output);
		output.flush();
		return copied;
	}

	public void process(InputStream input, OutputStream output) throws IOException {
		IOUtils.copy(input, output);
		output.flush();
	}

	public static String load(File file) throws IOException {
		return toString(new FileInputStream(file), "UTF-8");
	}

	public static String toString(File file)  {
		try {
			return toString(new FileInputStream(file), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toString(InputStream input) throws IOException {
		return toString(input, "UTF-8");
	}

	public static String toString(InputStream inputStream, String encoding) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, encoding);
		writer.flush();
		writer.close();
		return writer.toString();
	}

}
