package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import de.htw.tool.Copyright;
import de.htw.tool.InetAddresses;
import de.htw.tool.Maps;


/**
 * This class models a TCP switch application. TCP switches model "spray" servers for any kind of
 * TCP oriented protocol connection. They redirect incoming client connections to one of their
 * redirect servers, either randomly selected, or determined by known session association. Note that
 * this class is declared final because it provides an application entry point, and therefore not
 * supposed to be extended.
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class TcpSwitchApp {
	static private final String PROPERTIES_FILE_NAME = "redirect-servers.properties";

	/**
	 * Prevent external instantiation.
	 */
	private TcpSwitchApp () {}


	/**
	 * Application entry point. The given runtime parameters must be an optional service port (default is
	 * 8010), and the optional session awareness (default is false).
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF], or
	 *         there are no cluster nodes
	 * @throws IOException if the given port is already in use or cannot be bound, or if there is a
	 *         problem waiting for the quit signal
	 */
	static public void main (final String[] args) throws IOException {
		final int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8010;
		final boolean sessionAware = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
		final InetSocketAddress[] redirectServerAddresses = redirectServerAddresses();

		launch(servicePort, redirectServerAddresses, sessionAware);
	}


	/**
	 * Starts the application in command mode.
	 * @param servicePort the service port
	 * @param redirectAddress the redirect address
	 * @param contextPath the context path
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static public void launch (final int servicePort, final InetSocketAddress[] redirectAddresses, final boolean sessionAware) throws IOException {
		final long timestamp = System.currentTimeMillis();

		try (TcpSwitchServer server = new TcpSwitchServer(servicePort, sessionAware, redirectAddresses)) {
			// start acceptor thread(s)
			new Thread(server, "tcp-acceptor").start();

			// print welcome message
			System.out.println("TCP switch running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Session awareness is %s.\n", server.getSessionAware());
			System.out.format("Redirect host addresses: %s.\n", Arrays.toString(server.getRedirectServerAddresses()));
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}


	/**
	 * Returns the redirect server addresses loaded from a property file. 
	 * @return the redirect server addresses
	 * @throws IOException if there is an I/O related problem
	 */
	static private InetSocketAddress[] redirectServerAddresses () throws IOException {
		final InetAddress localAddress = InetAddress.getLocalHost();
		final Collection<InetSocketAddress> serverAddresses = new ArrayList<>();

		try (InputStream byteSource = HttpRedirectServer.class.getResourceAsStream(PROPERTIES_FILE_NAME)) {
			final Map<String,String> properties = Maps.readProperties(byteSource);

			for (final Map.Entry<String,String> entry : properties.entrySet()) {
				final String addressText = entry.getValue().startsWith(":") ? localAddress.getHostName() + entry.getValue() : entry.getValue();
				final InetSocketAddress serverAddress = InetAddresses.toSocketAddress(addressText);
				serverAddresses.add(serverAddress);
			}
		}

		return serverAddresses.toArray(new InetSocketAddress[serverAddresses.size()]);
	}
}