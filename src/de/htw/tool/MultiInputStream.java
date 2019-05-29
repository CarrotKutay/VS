package de.htw.tool;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.Queue;


/**
 * This class models filter-like input streams that are based on multiple byte sources. Reading from such a source appears to be
 * continuously reading one byte source after the other.
 */
@Copyright(year = 2012, holders = "Sascha Baumeister")
public class MultiInputStream extends InputStream {

	private final Queue<InputStream> byteSources;


	/**
	 * Constructs an instance from multiple byte sources. Any {@code null} source given is ignored.
	 * @param byteSources the byte sources
	 */
	public MultiInputStream (final InputStream... byteSources) {
		this.byteSources = new ArrayDeque<>();
		if (byteSources != null) {
			for (final InputStream byteSource : byteSources) {
				if (byteSource != null) this.byteSources.add(byteSource);
			}
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public synchronized void close () throws IOException {
		Exception exception = null;

		for (final InputStream byteSource : this.byteSources) {
			try {
				byteSource.close();
			} catch (final Exception e) {
				exception = e;
			}
		}
		this.byteSources.clear();

		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof IOException) throw (IOException) exception;
		assert exception == null;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized int available () {
		final InputStream byteSource = this.byteSources.peek();

		try {
			return byteSource.available();
		} catch (final Exception exception) {
			return 0;
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public synchronized int read (final byte buffer[], final int offset, final int length) throws IOException {
		final InputStream byteSource = this.byteSources.peek();
		if (byteSource == null) return -1;

		try {
			final int bytesRead = byteSource.read(buffer, offset, length);
			if (bytesRead != -1) return bytesRead;
		} catch (final SocketException exception) {
			// do nothing because an underlying socket stream has been
			// closed asynchronously while blocking!
		}

		try {
			this.byteSources.remove().close();
		} catch (final IOException exception) {}
		return this.read(buffer, offset, length);
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public int read () throws IOException {
		final byte[] buffer = new byte[1];
		final int bytesRead = this.read(buffer);
		return bytesRead == -1 ? -1 : buffer[0] & 0xFF;
	}
}