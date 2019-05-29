package de.htw.tool;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.SocketException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;


/**
 * This facade offers stream related convenience methods.Note that this class is declared final because it is a facade, and
 * therefore not supposed to be extended.
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class IOStreams {

	/**
	 * Prevents instantiation.
	 */
	private IOStreams () {}


	/**
	 * Reads all remaining bytes from the given byte source, and writes them to the given byte sink. Returns the number of bytes
	 * copied, and closes neither source nor sink. Note that large copy buffers speed up processing, but consume more memory.
	 * Also note that {@link SocketException} is treated as a kind of EOF due to to other side terminating the stream.
	 * @param byteSource the byte source
	 * @param byteSink the byte sink
	 * @param bufferSize the buffer size, in number of bytes
	 * @return the number of bytes copied
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given buffer size is negative
	 * @throws IOException if there is an I/O related problem
	 */
	static public long copy (final InputStream byteSource, final OutputStream byteSink, final int bufferSize) throws IOException {
		if (bufferSize <= 0) throw new IllegalArgumentException();
		final byte[] buffer = new byte[bufferSize];

		long bytesCopied = 0;
		try {
			for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
				byteSink.write(buffer, 0, bytesRead);
				bytesCopied += bytesRead;
			}
		} catch (final SocketException exception) {
			// treat as EOF because a TCP stream has been closed by the other side
		}
		return bytesCopied;
	}


	/**
	 * Reads all remaining characters from the given char source, and writes them to the given char sink. Returns the number of
	 * characters copied, and closes neither source nor sink. Note that large copy buffers speed up processing, but consume more
	 * memory. Also note that {@link SocketException} is treated as a kind of EOF due to to other side terminating the stream.
	 * @param charSource the byte source
	 * @param charSink the byte sink
	 * @param bufferSize the buffer size, in number of characters
	 * @return the number of characters copied
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given buffer size is negative
	 * @throws IOException if there is an I/O related problem
	 */
	static public long copy (final Reader charSource, final Writer charSink, final int bufferSize) throws IOException {
		if (bufferSize <= 0) throw new IllegalArgumentException();
		final char[] buffer = new char[bufferSize];

		long charsCopied = 0;
		try {
			for (int charsRead = charSource.read(buffer); charsRead != -1; charsRead = charSource.read(buffer)) {
				charSink.write(buffer, 0, charsRead);
				charsCopied += charsRead;
			}
		} catch (final SocketException exception) {
			// treat as EOF because a TCP stream has been closed by the other side
		}
		return charsCopied;
	}


	/**
	 * Reads all remaining bytes from the given byte source, and returns them as a byte array.
	 * @param byteSource the byte source
	 * @return the bytes
	 * @throws NullPointerException if the given byte source is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static public byte[] read (final InputStream byteSource) throws IOException {
		try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
			copy(byteSource, byteSink, 0x10000);
			return byteSink.toByteArray();
		}
	}


	/**
	 * Reads all remaining characters from the given char source, and returns them as a String.
	 * @param charSource the char source
	 * @return the characters
	 * @throws NullPointerException if the given char source is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static public String read (final Reader charSource) throws IOException {
		try (StringWriter charSink = new StringWriter()) {
			copy(charSource, charSink, 0x10000);
			return charSink.toString();
		}
	}


	/**
	 * Returns the path and binary content of all the files within the given file system. Note that this operation is designed
	 * to work with virtual file systems.
	 * @param fileSystem the (virtual) file system
	 * @return the file names and their respective binary content as a map
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static public SortedMap<String,byte[]> read (final FileSystem fileSystem) throws IOException {
		final SortedMap<String,byte[]> result = new TreeMap<>();

		for (final Path directory : fileSystem.getRootDirectories()) {
			try (Stream<Path> stream = Files.walk(directory)) {
				for (final Iterator<Path> iterator = stream.iterator(); iterator.hasNext();) {
					final Path path = iterator.next();
					if (Files.isRegularFile(path) & Files.isReadable(path)) result.put(path.toString(), Files.readAllBytes(path));
				}
			}
		}

		return result;
	}


	/**
	 * Returns a new input stream based on a sequence of byte sources, each read subsequently.
	 * @param byteSources the byte sources
	 * @return the multi input stream created
	 */
	static public InputStream newMultiInputStream (final InputStream... byteSources) {
		final Queue<InputStream> queue = new ArrayDeque<InputStream>(Arrays.asList(byteSources));

		return new InputStream() {
			@Override
			public synchronized void close () throws IOException {
				Throwable exception = null;

				for (final InputStream byteSource : queue) {
					try {
						byteSource.close();
					} catch (final Throwable e) {
						exception = e;
					}
				}
				queue.clear();

				if (exception instanceof Error) throw (Error) exception;
				if (exception instanceof RuntimeException) throw (RuntimeException) exception;
				if (exception instanceof IOException) throw (IOException) exception;
				throw new AssertionError();
			}

			@Override
			public synchronized int available () {
				final InputStream byteSource = queue.peek();

				try {
					return byteSource.available();
				} catch (final Exception exception) {
					return 0;
				}
			}

			@Override
			public synchronized int read (final byte buffer[], final int offset, final int length) throws IOException {
				final InputStream byteSource = queue.peek();
				if (byteSource == null) return -1;

				try {
					final int bytesRead = byteSource.read(buffer, offset, length);
					if (bytesRead != -1) return bytesRead;
				} catch (final SocketException exception) {
					// do nothing because an underlying socket stream
					// has been closed asynchronously while blocking!
				}

				try {
					queue.remove().close();
				} catch (final IOException exception) {}
				return this.read(buffer, offset, length);
			}

			@Override
			public int read () throws IOException {
				final byte[] buffer = new byte[1];
				final int bytesRead = this.read(buffer);
				return bytesRead == -1 ? -1 : buffer[0] & 0xFF;
			}
		};
	}


	/**
	 * Returns a new output stream based on a collection of byte sinks, each written in parallel.
	 * @param byteSinks the byte sinks
	 * @return the multi output stream created
	 */
	static public OutputStream newMultiOutputStream (final OutputStream... byteSinks) {
		return new OutputStream() {
			@Override
			public synchronized void close () throws IOException {
				Throwable exception = null;

				for (final OutputStream byteSink : byteSinks) {
					try {
						byteSink.close();
					} catch (final Throwable e) {
						exception = e;
					}
				}

				if (exception instanceof Error) throw (Error) exception;
				if (exception instanceof RuntimeException) throw (RuntimeException) exception;
				if (exception instanceof IOException) throw (IOException) exception;
				throw new AssertionError();
			}

			@Override
			public void write (final byte[] buffer, final int offset, final int length) throws IOException {
				if (byteSinks.length == 0) throw new EOFException();

				for (final OutputStream byteSink : byteSinks) {
					byteSink.write(buffer, offset, length);
				}
			}

			@Override
			public void write (final int value) throws IOException {
				this.write(new byte[] { (byte) value });
			}

			@Override
			public void flush () throws IOException {
				for (final OutputStream byteSink : byteSinks) {
					byteSink.flush();
				}
			}
		};
	}
}