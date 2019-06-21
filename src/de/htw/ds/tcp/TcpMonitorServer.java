package de.htw.ds.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import de.htw.tool.ByteArrays;
import de.htw.tool.Copyright;
import de.htw.tool.IOStreams;
import de.htw.tool.Uninterruptibles;


/**
 * This class models a TCP monitor, i.e. a TCP server that redirects all incoming client connections
 * towards another host, while logging all traffic.
 */

@Copyright(year=2008, holders="Sascha Baumeister")
public class TcpMonitorServer implements Runnable, AutoCloseable {
	static private final byte[] HTTP_HOST_START = "Host: ".getBytes(StandardCharsets.UTF_8);
	static private final byte[] HTTP_HOST_STOP = "\n".getBytes(StandardCharsets.UTF_8);
	static private SocketFactory TLS_SOCKET_FACTORY = SSLSocketFactory.getDefault();

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
	public TcpMonitorServer (final int servicePort, final InetSocketAddress redirectHostAddress, final Consumer<TcpMonitorRecord> recordConsumer, final Consumer<Throwable> exceptionConsumer) throws IOException {
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
		private final TcpMonitorServer parent;
		private final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @param parent the parent monitor
		 * @param clientConnection the connection
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final TcpMonitorServer parent, final Socket clientConnection) {
			if (parent == null | clientConnection == null) throw new NullPointerException();

			this.parent = parent;
			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run (){
			final String serverName = this.parent.redirectHostAddress.getHostName();
			final int serverPort = this.parent.redirectHostAddress.getPort();
			final boolean transportLayerSecurity = serverPort == 443;

			try (Socket clientConnection = this.clientConnection) {
				try (Socket serverConnection = transportLayerSecurity ? TLS_SOCKET_FACTORY.createSocket(serverName, serverPort) : new Socket(serverName, serverPort)) {
					// TODO: Transport all content from the client connection's input stream into
					// both the server connection's output stream and a byte output stream. In
					// parallel, transport all content from the server connection's input stream
					// into both the client connection's output stream and another byte output stream.
					// Note that the existing utility class de.htw.tool.IOStreams offers a highly
					// elegant (and compact) solution, especially in conjunction with Java 8 Lambda-Operators.					
					final long openTimestamp = new Date().getTime();

					ByteArrayOutputStream bos_client = new ByteArrayOutputStream(), bos_server = new ByteArrayOutputStream();
					final InputStream clientIS = clientConnection.getInputStream(), serverIS = serverConnection.getInputStream();
					final OutputStream clientOS = clientConnection.getOutputStream(), serverOS = serverConnection.getOutputStream();
					
					
					try (OutputStream multiOStream2S = IOStreams.newMultiOutputStream(serverOS, bos_client)) {
						try (OutputStream multiOStream2C = IOStreams.newMultiOutputStream(bos_server, clientOS)) {	
							final Callable<Long> clientWorker = () -> copy(clientIS, multiOStream2S, 0x10000, serverName.getBytes(StandardCharsets.UTF_8));
							final Callable<Long> serverWorker = () -> IOStreams.copy(serverIS, multiOStream2C, 0x10000);	
							
							final Future<?>[] futures = new Future[2];
							futures[0] = this.parent.threadPool.submit(serverWorker);
							futures[1] = this.parent.threadPool.submit(clientWorker);
							
							//resynchronisation
							try {
								for (final Future<?> future : futures) {
									try {
										Uninterruptibles.get(future);
									} catch (final ExecutionException exception) {
										final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
										if (cause instanceof Error) throw (Error) cause;
										if (cause instanceof RuntimeException) throw (RuntimeException) cause;
										if (cause instanceof IOException) throw (IOException) cause;
										throw new AssertionError();
									}
								}
							} finally {
								for (final Future<?> future : futures) {
									future.cancel(true);
								}
							}
							
							final long closeTimestamp = new Date().getTime();
							final byte[] request = bos_client.toByteArray(), response = bos_server.toByteArray();
							final TcpMonitorRecord record = new TcpMonitorRecord(openTimestamp, closeTimestamp, request, response);
							this.parent.recordConsumer.accept(record);
						}
					}
				}
			} catch (final Throwable exception) {
				this.parent.exceptionConsumer.accept(exception);
			}
					
										
					
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
	}
}