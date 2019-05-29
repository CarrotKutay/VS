package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import de.htw.tool.Copyright;
import de.htw.tool.InetAddresses;
import de.htw.tool.Maps;


/**
 * HTTP redirect server app-entry facade.
 */
@Copyright(year=2014, holders="Sascha Baumeister")
public final class HttpRedirectServer {
	static private final String PROPERTIES_FILE_NAME = "redirect-servers.properties";
	
	/**
	 * Prevents external instantiation.
	 */
	private HttpRedirectServer () {}


	/**
	 * Application entry point. The given arguments are expected to be an optional service port
	 * (default is 8010), the optional session awareness (default is false), and an optional
	 * key store file path (default is null).
	 * @param args the runtime arguments
	 * @throws IllegalArgumentException if the given port is not a valid port number, or if
	 *         the given key store file is not a regular file path
	 * @throws CertificateException if any of the certificates in the key store could not be loaded
	 * @throws UnrecoverableKeyException if there is a key recovery problem, like incorrect passwords
	 * @throws KeyManagementException if there is a key management problem, like key expiration
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IllegalArgumentException, IOException, UnrecoverableKeyException, KeyManagementException, NullPointerException, CertificateException {
		final int servicePort = args.length > 0 ?Integer.parseInt(args[0]) : 8010;
		final boolean sessionAware = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
		final Path keyStoreFile = args.length > 2 ? Paths.get(args[2]).toAbsolutePath() : null;
		final String keyRecoveryPassword = args.length > 3 ? args[3] : "changeit";
		final String keyManagementPassword = args.length > 4 ? args[4] : keyRecoveryPassword;
		if (keyStoreFile != null && !Files.isRegularFile(keyStoreFile)) throw new IllegalArgumentException();

		final boolean transportLayerSecurity = keyStoreFile != null;
		final InetSocketAddress serviceAddress = new InetSocketAddress(InetAddress.getLocalHost(), servicePort);
		final InetSocketAddress[] redirectServerAddresses = redirectServerAddresses();

		final HttpServer server;
		if (transportLayerSecurity) {
			// Generate keystore for a given host using this JDK utility (default passwords are "changeit"):
			// keytool -genkey -alias <hostname> -keyalg RSA -validity 365 -keystore keystore.jks
			final SSLContext context = newTLSContext(keyStoreFile, keyRecoveryPassword, keyManagementPassword);

			final HttpsServer httpsServer = HttpsServer.create(serviceAddress, 0);
			httpsServer.setHttpsConfigurator(newHttpsConfigurator(context));
			server = httpsServer;
		} else {
			server = HttpServer.create(serviceAddress, 0);
		}

		final HttpRedirectHandler redirectHandler = new HttpRedirectHandler(sessionAware, redirectServerAddresses);
		server.createContext("/", redirectHandler);
		server.start();
		try {
			final String origin = String.format("%s://%s:%s/", transportLayerSecurity ? "https" : "http", serviceAddress.getHostName(), serviceAddress.getPort());
			System.out.format("Web redirect server running on origin %s, enter \"quit\" to stop.\n", origin);
			System.out.format("Redirect host addresses: %s.\n", Arrays.toString(redirectServerAddresses));
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		} finally {
			server.stop(0);
		}
	}


	/**
	 * Returns a new TLS context based on a JKS key store and the most recent supported transport layer security (TLS) version.
	 * @param keyStoreFile the key store file path, or {@code null} for none
	 * @param keyRecoveryPassword the key recovery password
	 * @param keyManagementPassword the key management password
	 * @return the SSL context created, or {@code null} if no key store is passed
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if an I/O related problem occurs during key store file access
	 * @throws CertificateException if any of the certificates in the key store could not be loaded
	 * @throws UnrecoverableKeyException if there is a key recovery problem, like incorrect passwords
	 * @throws KeyManagementException if there is a key management problem, like key expiration
	 */
	static private SSLContext newTLSContext (final Path keyStoreFile, final String keyRecoveryPassword, final String keyManagementPassword) throws NullPointerException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
		if (keyStoreFile == null) return null;

		try {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			try {
				try (InputStream byteSource = Files.newInputStream(keyStoreFile)) {
					keyStore.load(byteSource, keyRecoveryPassword.toCharArray());
				}
			} catch (final IOException exception) {
				if (exception.getCause() instanceof UnrecoverableKeyException) throw (UnrecoverableKeyException) exception.getCause();
				throw exception;
			}

			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyManagementPassword.toCharArray());

			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);

			final SSLContext context = SSLContext.getInstance("TLS");
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
			return context;
		} catch (final NoSuchAlgorithmException | KeyStoreException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Returns a new HTTPS configurator based on the given transport layer security context.
	 * @param context the TLS context
	 * @return the HTTPS configurator created
	 * @throws NullPointerException if the given context is {@code null}
	 */
	static private HttpsConfigurator newHttpsConfigurator (final SSLContext context) throws NullPointerException {
		return new HttpsConfigurator(context) {
			public void configure (final HttpsParameters params) {
				final SSLEngine engine = context.createSSLEngine();
                params.setNeedClientAuth(false);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols (engine.getEnabledProtocols());
                params.setSSLParameters(context.getDefaultSSLParameters());
			}
		};
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