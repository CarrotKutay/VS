package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.util.Arrays;
import de.htw.tool.Copyright;


/**
 * This class models FTP responses.
 */
@Copyright(year=2011, holders="Sascha Baumeister")
public class FtpResponse {

	/**
	 * Parses an FTP response from the given char source.
	 * @param charSource the source
	 * @return an FTP response
	 * @throws NullPointerException if the given source is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static public final FtpResponse parse (final BufferedReader charSource) throws IOException {
		final StringWriter charSink = new StringWriter();
		short code = -1;

		while (true) {
			final String line = charSource.readLine();
			if (line.length() < 4) throw new ProtocolException();

			charSink.write(line.substring(4));
			if (line.charAt(3) == ' ') {
				code = Short.parseShort(line.substring(0, 3));
				break;
			} else {
				charSink.write("\n");
			}
		}

		return new FtpResponse(code, charSink.toString());
	}


	private final short code;
	private final String message;


	/**
	 * Creates a new instance.
	 * @param code the FTP response code
	 * @param message the FTP response message
	 * @throws NullPointerException if the given message is {@code null}
	 */
	protected FtpResponse (final short code, final String message) {
		if (message == null) throw new NullPointerException();

		this.code = code;
		this.message = message;
	}


	/**
	 * Returns the FTP response code.
	 * @return the code
	 */
	public short getCode () {
		return this.code;
	}


	/**
	 * Returns the FTP response message.
	 * @return the message
	 */
	public String getMessage () {
		return this.message;
	}


	/**
	 * Returns the data port decoded from a PASV command's code 227 response. This information can
	 * be used to create TCP connection for data transport.
	 * @return the remote data port qualified with it's host address as a socket-address
	 * @throws IllegalStateException if this is not a code 227 response
	 */
	public InetSocketAddress decodeDataPort () {
		if (this.code != 227) throw new IllegalStateException(Short.toString(this.code));

		final int beginIndex = this.message.lastIndexOf('(');
		final int endIndex = this.message.lastIndexOf(')');
		assert beginIndex >= 0 & endIndex >= 0 & beginIndex < endIndex;

		final String[] binarySocketAddressElements = this.message.substring(beginIndex + 1, endIndex).split(",");
		final byte binarySocketAddress[] = new byte[binarySocketAddressElements.length];
		assert binarySocketAddressElements.length == 6;

		for (int index = 0; index < binarySocketAddress.length; ++index) {
			binarySocketAddress[index] = (byte) Short.parseShort(binarySocketAddressElements[index]);
		}

		final InetAddress address;
		try {
			address = InetAddress.getByAddress(Arrays.copyOf(binarySocketAddress, 4));
		} catch (final UnknownHostException exception) {
			throw new AssertionError(); // cannot happen because the binary address array is guaranteed to have the correct length 
		}

		final int port = ((binarySocketAddress[4] & 0xFF) << 8) | ((binarySocketAddress[5] & 0xFF) << 0);
		return new InetSocketAddress(address, port);
	}


	/**
	 * ({@inheritDoc}
	 */
	public String toString () {
		return Short.toString(this.code) + " " + this.message;
	}
}