package de.htw.ds.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.htw.tool.ByteArrays;
import de.htw.tool.Copyright;
import de.htw.tool.IOStreams;
import de.htw.tool.Uninterruptibles;


/**
 * This class models a TCP switch server, i.e. a "spray" server for all kinds of TCP oriented
 * protocol connections. It redirects incoming client connections to it's given set of redirect
 * servers, either randomly selected, or determined by known session association. Note that while
 * this implementation routes all kinds of TCP protocols, a single instance is only able to route
 * one protocol type unless it's child servers support multi-protocol requests.<br />
 * Session association is determined by receiving subsequent requests from the same client, which
 * may or may not be interpreted as being part of the same session by the protocol server selected.
 * However, two requests can never be part of the same session if they do not share the same request
 * client address! Note that this algorithm allows for protocol independence, but does not work with
 * clients that dynamically change their IP-address during a session's lifetime.
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public class TcpSwitchServer implements Runnable, Closeable {
	
	static private final byte[] HTTP_HOST_START = "Host: ".getBytes(StandardCharsets.UTF_8);
	static private final byte[] HTTP_HOST_STOP = "\n".getBytes(StandardCharsets.UTF_8);
	private final ExecutorService threadPool;
	private final ServerSocket host;
	private final boolean sessionAware;
	private final InetSocketAddress[] redirectServerAddresses;


	/**
	 * Creates a new instance.
	 * @param servicePort the service port
	 * @param sessionAware {@code true} if the server is aware of sessions, {@code false} otherwise
	 * @param redirectServerAddresses the redirect host addresses
	 * @throws NullPointerException if any of the given addresses is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF], or
	 *         the given socket-addresses array is empty
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public TcpSwitchServer (final int servicePort, final boolean sessionAware, final InetSocketAddress... redirectServerAddresses) throws IOException {
		if (redirectServerAddresses.length == 0) throw new IllegalArgumentException();

		this.threadPool = Executors.newCachedThreadPool();
		this.host = new ServerSocket(servicePort);
		this.sessionAware = sessionAware;
		this.redirectServerAddresses = redirectServerAddresses;
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
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort () {
		return this.host.getLocalPort();
	}


	/**
	 * Returns the session awareness.
	 * @return the session awareness
	 */
	public boolean getSessionAware () {
		return this.sessionAware;
	}


	/**
	 * Returns the redirect server addresses.
	 * @return the redirect server addresses
	 */
	public InetSocketAddress[] getRedirectServerAddresses () {
		return this.redirectServerAddresses;
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
	 * Instances of this inner class handle TCP client connections accepted by a TCP switch.
	 */
	static private class ConnectionHandler implements Runnable {
		private final TcpSwitchServer parent;
		private final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @parent the parent switch
		 * @param clientConnection the connection
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final TcpSwitchServer parent, final Socket clientConnection) {
			if (parent == null | clientConnection == null) throw new NullPointerException();

			this.parent = parent;
			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run () {
			//Random process of choosing a redirect server
			//get InetSocketAdresses
			final int index;
			if (this.parent.sessionAware) {
				//decide depending on IP address, which server to connect to
				final Random scrambler = new Random(Arrays.hashCode(this.clientConnection.getInetAddress().getAddress()));
				index = scrambler.nextInt(this.parent.redirectServerAddresses.length);
			} else {
				//randomly decide which server to take
				index = ThreadLocalRandom.current().nextInt(this.parent.redirectServerAddresses.length);
			}
			final InetSocketAddress server = this.parent.redirectServerAddresses[index];
			System.out.println("Server at port "+ server +" choosen, from "+(this.parent.redirectServerAddresses.length)+" Server(s)");
			
			//start connection
			try (Socket clientConnection = this.clientConnection) {
				try (Socket serverConnection = new Socket(server.getAddress(), server.getPort())) {
					
					final InputStream clientIS = clientConnection.getInputStream(), serverIS = serverConnection.getInputStream();
					final OutputStream clientOS = clientConnection.getOutputStream(), serverOS = serverConnection.getOutputStream();
																	
					final Callable<Long> clientWorker = () -> copy(clientIS, serverOS, 0x1000, serverConnection.getInetAddress().getHostName().getBytes(StandardCharsets.UTF_8));
					final Callable<Long> serverWorker = () -> IOStreams.copy(serverIS, clientOS, 0x1000);
					
					@SuppressWarnings("unchecked")
					final Future<Long>[] futures = new Future[2];
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
				}
			} catch (final Exception exception) {
					Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
				}
			}
		}
			
			// TODO implement TCP routing here, and close the connections upon completion!
			// Note that you'll need 1-2 new transporter threads to complete this tasks, as
			// you cannot foresee if the client or the server closes the connection, or if
			// the protocol communicated involves handshakes. Either case implies you'd
			// end up reading "too much" if you try to transport both communication directions
			// within this thread, creating a deadlock scenario!
			//   Especially make sure that all connections are properly closed in
			// any circumstances! Note that closing one socket stream closes the underlying
			// socket connection as well. Also note that a SocketInputStream's read() method
			// will throw a SocketException when interrupted while blocking, which is "normal"
			// behavior and should be handled as if the read() Method returned -1!
			//   Hint for sessionAware mode: The simplest solution is using a scrambler, i.e.
			// a randomizer that uses the client address as a seed, as this will repeatedly
			// create the same pseudo-random number (serverIndex) as next value. If you can't
			// realize this solution, using a cache in the form of clientAddress->serverSocketAddress
			// mappings is probably the next best alternative ...
		
	
	
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