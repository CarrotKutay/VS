package de.htw.ds.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import de.htw.tool.ByteArrays;
import de.htw.tool.Copyright;


/**
 * This class models a TCP monitor, i.e. a TCP server that redirects all incoming client connections
 * towards another host, while logging all traffic.
 */
@SuppressWarnings("unused")	// TODO: remove 
@Copyright(year=2008, holders="Sascha Baumeister")
public class TcpMonitorServerSkeleton implements Runnable, AutoCloseable {
	static private final SocketFactory TLS_SOCKET_FACTORY = SSLSocketFactory.getDefault();
	static private final byte[] HTTP_HOST_START = "Host: ".getBytes(StandardCharsets.UTF_8);
	static private final byte[] HTTP_HOST_STOP = "\n".getBytes(StandardCharsets.UTF_8);
	static private final int MAX_PACKET_SIZE = 0xffff - 20 - 20;

	private final ExecutorService threadPool;
	private final ServerSocket host;
	private final InetSocketAddress redirectHostAddress;
	private final Consumer<TcpMonitorRecord> recordConsumer;
	private final Consumer<Throwable> exceptionConsumer;


	/**
	 * Creates a new instance.
	 * @param servicePort the service port
	 * @param redirectHostAddress the redirect host address
	 * @param recordConsumer the record consumer
	 * @param exceptionConsumer the exception consumer
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given service port is already in use, or cannot be bound
	 */
	public TcpMonitorServerSkeleton (final int servicePort, final InetSocketAddress redirectHostAddress, final Consumer<TcpMonitorRecord> recordConsumer, final Consumer<Throwable> exceptionConsumer) throws IOException {
		if (redirectHostAddress == null | recordConsumer == null | exceptionConsumer == null) throw new NullPointerException();

		this.threadPool = Executors.newCachedThreadPool();
		this.host = new ServerSocket(servicePort);
		this.redirectHostAddress = redirectHostAddress;
		this.recordConsumer = recordConsumer;
		this.exceptionConsumer = exceptionConsumer;
	}


	/**
	 * Closes this server.
	 * @throws IOException {@inheritDoc}
	 */
	public void close () throws IOException {
		try {
			this.host.close();
		} finally {
			this.threadPool.shutdown();
		}
	}


	/**
	 * Returns the redirect host address.
	 * @return the redirect host address
	 */
	public InetSocketAddress getRedirectHostAddress () {
		return redirectHostAddress;
	}



	/**
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort () {
		return this.host.getLocalPort();
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run () {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.host.accept();
				this.threadPool.execute(new ConnectionHandler(this, clientConnection));
			} catch (final SocketException exception) {
				break;
			} catch (final Throwable exception) {
				try {
					clientConnection.close();
				} catch (final Throwable nestedException) {
					exception.addSuppressed(nestedException);
				}

				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}
	}



	/**
	 * Instances of this inner class handle TCP client connections accepted by a TCP monitor.
	 */
	static private class ConnectionHandler implements Runnable {
		private final TcpMonitorServerSkeleton parent;
		private final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @param parent the parent monitor
		 * @param clientConnection the connection
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final TcpMonitorServerSkeleton parent, final Socket clientConnection) {
			if (parent == null | clientConnection == null) throw new NullPointerException();

			this.parent = parent;
			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run () {
			final boolean encrypted = this.parent.redirectHostAddress.getPort() == 443;

			try (Socket clientConnection = this.clientConnection) {
				try (Socket serverConnection = newTcpConnection(this.parent.redirectHostAddress, encrypted)) {
					// TODO: Transport all content from the client connection's input stream into
					// both the server connection's output stream and a byte output stream. In
					// parallel, transport all content from the server connection's input stream
					// into both the client connection's output stream and another byte output stream.
					// Note that the existing utility class de.htw.tool.IOStreams offers a highly
					// elegant (and compact) solution, especially in conjunction with Java 8 Lambda-Operators.
	
					// Start two transporter threads, and resynchronize them before closing all
					// resources. If all goes well, use "ByteArrayOutputStream#toByteArray()" to get
					// the respective request and response data; use it to create a TcpMonitorRecord,
					// and flush it using "this.parent.recordConsumer.accept()". If anything goes
					// wrong, use "this.parent.exceptionConsumer.accept()" instead.
	
					// Note that you'll need 2 transporters in 1-2 separate threads to complete this
					// task, as you cannot foresee if the client or the server closes the connection,
					// or if the protocol communicated involves handshakes. Either case implies you'd
					// end up reading "too much" if you try to transport both communication directions
					// within a single thread, creating a deadlock scenario. The easiest solution probably
					// involves the ConnectionHandler's executor service (see Method submit()), and
					// resynchronization using the futures returned by said method.

					// Be aware that HTTP 1.1+ requires the "Host" header value to be replaced by the
					// redirect hostname - see de.htw.tool.ByteArrays.indexOf() for support. Also, HTTP
					// usually implies delayed closing of connections after transmission due to connection
					// caching. Finally, transport layer security (TLS, like in HTTPS or SFTP) requires the
					// resynchronizing Future.get() methods to specify a timeout (for example 10s), and
					// catching of the resulting TimeoutException, apart from the usual ExecutionException,
					// in order to proceed normally.

					// Note that closing one socket stream closes the underlying socket connection (and
					// therefore also the second socket stream) as well. Also note that a socket stream's
					// read() method will throw a SocketException when interrupted while blocking, which is
					// "normal" behavior and should be handled as if the read() Method returned -1!
				}
			} catch (final Throwable exception) {
				this.parent.exceptionConsumer.accept(exception);
			}
		}
	}


	/**
	 * Copies the given byte source's content to the given byte sink, replacing HTTP hostname headers along the way.
	 * @param byteSource the byte source
	 * @param byteSink the byte sink
	 * @param bufferSize the buffer size
	 * @param hostname the host name
	 * @return the number of bytes copied
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if there is an I/P related problem
	 */
	static protected long copy (final InputStream byteSource, final OutputStream byteSink, final int bufferSize, final byte[] hostname) throws NullPointerException, IOException {
		if (bufferSize <= 0) throw new IllegalArgumentException();
		final byte[] buffer = new byte[bufferSize];

		long bytesCopied = 0;
		try {
			for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
				if (hostname != null) {
					final int start = ByteArrays.indexOf(buffer, HTTP_HOST_START, 0);
					if (start != -1 & start < bytesRead) {
						final int stop = ByteArrays.indexOf(buffer, HTTP_HOST_STOP, start + HTTP_HOST_START.length);
						if (stop != -1 & stop < bytesRead) {
							byteSink.write(buffer, 0, start + HTTP_HOST_START.length);
							byteSink.write(hostname);
							byteSink.write(buffer, stop, bytesRead - stop);
							bytesCopied += start + HTTP_HOST_START.length + hostname.length + (bytesRead - stop);
							continue;
						}
					}
				}

				byteSink.write(buffer, 0, bytesRead);
				bytesCopied += bytesRead;
			}
		} catch (final SocketException exception) {
			// treat as EOF because a TCP stream has been closed by the other side
		}

		return bytesCopied;
	}


	/**
	 * Returns a new TCP connection.
	 * @param address the socket address
	 * @param encrypted whether or not the resulting connection is TLS encrypted
	 * @return the TCP connection created
	 * @throws NullPointerException if the given address is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static private Socket newTcpConnection (final InetSocketAddress address, final boolean encrypted) throws NullPointerException, IOException {
		return encrypted
			? TLS_SOCKET_FACTORY.createSocket(address.getHostName(), address.getPort())
			: new Socket(address.getHostName(), address.getPort());
	}
}