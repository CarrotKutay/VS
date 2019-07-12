package de.htw.ds.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.htw.tool.Copyright;


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
public class TcpSwitchServerSkeleton implements Runnable, Closeable {
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
	public TcpSwitchServerSkeleton (final int servicePort, final boolean sessionAware, final InetSocketAddress... redirectServerAddresses) throws IOException {
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
		@SuppressWarnings("unused")	// TODO: remove
		private final TcpSwitchServerSkeleton parent;
		@SuppressWarnings("unused")	// TODO: remove
		private final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @parent the parent switch
		 * @param clientConnection the connection
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final TcpSwitchServerSkeleton parent, final Socket clientConnection) {
			if (parent == null | clientConnection == null) throw new NullPointerException();

			this.parent = parent;
			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run () {
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
		}
	}
}