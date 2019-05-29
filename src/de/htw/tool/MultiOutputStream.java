package de.htw.tool;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * This class models filter-like output streams that are based on multiple data sinks. Writing into such a stream means writing
 * into multiple streams at once.
 */
@Copyright(year = 2012, holders = "Sascha Baumeister")
public class MultiOutputStream extends OutputStream {

	private final List<OutputStream> byteSinks;


	/**
	 * Constructs an instance from multiple byte sinks. Any {@code null} sink given is ignored.
	 * @param byteSinks the byte sinks
	 */
	public MultiOutputStream (final OutputStream... byteSinks) {
		this.byteSinks = new ArrayList<>();
		if (byteSinks != null) {
			for (final OutputStream byteSink : byteSinks) {
				if (byteSink != null) this.byteSinks.add(byteSink);
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

		for (final OutputStream byteSink : this.byteSinks) {
			try {
				byteSink.close();
			} catch (final Exception e) {
				exception = e;
			}
		}
		this.byteSinks.clear();

		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof IOException) throw (IOException) exception;
		assert exception == null;
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public void write (final byte[] buffer, final int offset, final int length) throws IOException {
		if (this.byteSinks.isEmpty()) throw new EOFException();

		for (final OutputStream byteSink : this.byteSinks) {
			byteSink.write(buffer, offset, length);
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public void write (final int value) throws IOException {
		final byte[] buffer = new byte[] { (byte) value };
		this.write(buffer);
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	public void flush () throws IOException {
		for (final OutputStream byteSink : this.byteSinks) {
			byteSink.flush();
		}
	}
}