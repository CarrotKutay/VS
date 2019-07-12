package de.htw.tool;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.DatatypeConverter;


/**
 * Instances model immutable RFC 2617 authentification credentials.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public interface HttpCredentials {
	
	

	/**
	 * Returns the username.
	 * @return the username
	 */
	String getUsername ();


	/**
	 * Encodes these credentials into an HTTP "authorization" header value.
	 * @return the encoded HTTP "authorization" header value
	 */
	String getAuthentication ();


	/**
	 * Returns Basic credentials decoded from the given authentication.
	 * @param authentication an HTTP "authorization" header value
	 * @return the decoded HTTP Basic credentials
	 * @throws IllegalArgumentException if the given authentication is malformed
	 * @throws AuthenticationException if the given authentication is {@code null} or illegal
	 */
	static Basic newBasicInstance (final String authentication) throws IllegalArgumentException, AuthenticationException {
		if (authentication == null) throw new AuthenticationException();

		final int blankOffset = authentication.indexOf(' ');
		if (blankOffset == -1) throw new IllegalArgumentException();

		final String mode = authentication.substring(0, blankOffset);
		final String arguments = authentication.substring(blankOffset + 1).trim();
		if (!"Basic".equalsIgnoreCase(mode)) throw new AuthenticationException();

		final byte[] binaryCredentials = DatatypeConverter.parseBase64Binary(arguments);
		final String textCredentials = new String(binaryCredentials, StandardCharsets.ISO_8859_1);
		final int colonOffset = textCredentials.indexOf(':');
		if (colonOffset == -1) throw new IllegalArgumentException();

		final String username = textCredentials.substring(0, colonOffset);
		final String password = textCredentials.substring(colonOffset + 1);
		return new Basic(username, password);
	}


	/**
	 * Returns Digest credentials decoded from the given authentication.
	 * @param authentication an HTTP "authorization" header value
	 * @return the decoded HTTP Digest credentials
	 * @throws IllegalArgumentException if the given authentication is malformed
	 * @throws AuthenticationException if the given authentication is {@code null} or illegal
	 */
	static Digest newDigestInstance (final String authentication) throws IllegalArgumentException, AuthenticationException {
		if (authentication == null) throw new AuthenticationException();

		final int blankOffset = authentication.indexOf(' ');
		if (blankOffset == -1) throw new IllegalArgumentException();

		final String mode = authentication.substring(0, blankOffset);
		final String arguments = authentication.substring(blankOffset + 1).trim();
		if (!"Digest".equalsIgnoreCase(mode)) throw new AuthenticationException();

		final Map<String,String> map = new HashMap<>();
		for (final String argument : arguments.split(", ")) {
			final String[] association = argument.split("=");
			if (association.length != 2 || !association[1].startsWith("\"") || !association[1].endsWith("\"")) throw new IllegalArgumentException();
			final String key = association[0].trim().toLowerCase();
			final String value = association[1].substring(1, association[1].length() - 1);
			map.put(key, value);
		}
		return new Digest(map.get("username"), map.get("realm"), map.get("uri"), map.get("nonce"), map.get("response"));
	}


	/**
	 * Returns a new HTTP digest response, which is calculated using the formula
	 * {@code HEX_MD5(HEX_MD5(username:realm:password):nonce:HEX_MD5(method:uri))}.
	 * @param username the username
	 * @param realm the optional realm, or {@code null} for none
	 * @param password the password
	 * @param method the method
	 * @param uri the URI
	 * @param nonce the optional nonce, or {@code null} for none
	 * @return the calculated HTTP digest response
	 * @throws NullPointerException if any of the given username, password, method, or uri is {@code null}
	 * @throws IllegalArgumentException if the given username contains a colon
	 */
	static String newDigestResponse (final String username, final String realm, final String password, final String method, final String uri, final String nonce) {
		if (username == null | password == null | method == null | uri == null) throw new NullPointerException();
		if (username.contains(":")) throw new IllegalArgumentException();

		final MessageDigest md5Digest;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException exception) {
			throw new AssertionError();
		}

		final String left = username + ":" + (realm == null ? "" : realm) + ":" + password;
		final byte[] leftHash = md5Digest.digest(left.getBytes(StandardCharsets.ISO_8859_1));
		final String right = method.toUpperCase() + ":" + uri;
		final byte[] rightHash = md5Digest.digest(right.getBytes(StandardCharsets.ISO_8859_1));
		final String union = DatatypeConverter.printHexBinary(leftHash).toLowerCase() + ":" + (nonce == null ? "" : nonce) + ":" + DatatypeConverter.printHexBinary(rightHash).toLowerCase();
		final byte[] unionHash = md5Digest.digest(union.getBytes(StandardCharsets.ISO_8859_1));
		return DatatypeConverter.printHexBinary(unionHash).toLowerCase();
	}



	/**
	 * Instances model immutable RFC 2617 {@code HTTP Basic} authentication credentials consisting of username and password.
	 */
	static class Basic implements HttpCredentials {
		private final String username;
		private final String password;


		/**
		 * Creates a new instance.
		 * @param username the username
		 * @param password the password
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 * @throws IllegalArgumentException if the given username contains a colon
		 */
		public Basic (final String username, final String password) throws NullPointerException, IllegalArgumentException {
			if (password == null) throw new NullPointerException();
			if (username.contains(":")) throw new IllegalArgumentException();

			this.username = username;
			this.password = password;
		}


		/**
		 * Returns the username.
		 * @return the username
		 */
		public String getUsername () {
			return this.username;
		}


		/**
		 * Returns the password.
		 * @return the password
		 */
		public String getPassword () {
			return this.password;
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals (final Object object) {
			if (!(object instanceof Basic)) return false;
			final Basic credentials = (Basic) object;
			return Objects.equals(this.username, credentials.username) && Objects.equals(this.password, credentials.password);
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode () {
			return this.username.hashCode() ^ this.password.hashCode();
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString () {
			return String.format("%s(username=%s)", this.getClass().getName(), this.username);
		}


		/**
		 * Encodes these credentials into an HTTP "authorization" header value.
		 * @return the encoded HTTP "authorization" header value
		 */
		public String getAuthentication () {
			final String textCredentials = this.username + ":" + this.password;
			final byte[] binaryCredentials = textCredentials.getBytes(StandardCharsets.ISO_8859_1);
			return "Basic " + DatatypeConverter.printBase64Binary(binaryCredentials);
		}
	}



	/**
	 * Instances model immutable RFC 2617 {@code HTTP Digest} authentication credentials consisting of username, uri, response,
	 * etc.
	 */
	static class Digest implements HttpCredentials {
		private final String username;
		private final String realm;
		private final String uri;
		private final String nonce;
		private final String response;


		/**
		 * Creates a new instance. Note that the given response is defined as
		 * {@code HEX_MD5(HEX_MD5(username:realm:password):nonce:HEX_MD5(method:uri))}.
		 * @param username the username
		 * @param realm the optional realm, or {@code null} for none
		 * @param uri the URI
		 * @param nonce the optional nonce, or {@code null} for none
		 * @param response the response
		 * @throws NullPointerException if any of the given username, method, uri or response is {@code null}
		 * @throws IllegalArgumentException if the given username contains a colon
		 */
		public Digest (final String username, final String realm, final String uri, final String nonce, final String response) throws NullPointerException, IllegalArgumentException {
			if (username == null | uri == null | response == null) throw new NullPointerException();
			if (username.contains(":")) throw new IllegalArgumentException();

			this.username = username;
			this.uri = uri;
			this.realm = realm;
			this.nonce = nonce;
			this.response = response;
		}


		/**
		 * Returns the username.
		 * @return the username
		 */
		public String getUsername () {
			return this.username;
		}


		/**
		 * Returns the realm.
		 * @return the realm, or {@code null} for none
		 */
		public String getRealm () {
			return this.realm;
		}


		/**
		 * Returns the URI.
		 * @return the URI
		 */
		public String getUri () {
			return this.uri;
		}


		/**
		 * Returns the nonce.
		 * @return the nonce, or {@code null} for none
		 */
		public String getNonce () {
			return this.nonce;
		}


		/**
		 * Returns the response,
		 * @return the response
		 */
		public String getResponse () {
			return this.response;
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals (final Object object) {
			if (!(object instanceof Digest)) return false;
			final Digest credentials = (Digest) object;
			return Objects.equals(this.response, credentials.response) && Objects.equals(this.username, credentials.username) && Objects.equals(this.realm, credentials.realm) && Objects.equals(this.uri, credentials.uri) && Objects.equals(this.nonce, credentials.nonce);
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode () {
			return this.response.hashCode();
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString () {
			return String.format("%s(username=%s, realm=%s, uri=%s, nonce=%s, response=%s)", this.getClass().getName(), this.username, this.realm, this.uri, this.nonce, this.response);
		}


		/**
		 * Encodes these credentials into an HTTP "authorization" header value.
		 * @return the encoded HTTP "authorization" header value
		 */
		public String getAuthentication () {
			final StringWriter writer = new StringWriter();
			writer.write("Digest username=\"");
			writer.write(this.username);
			if (this.realm != null) {
				writer.write("\", realm=\"");
				writer.write(this.realm);
			}
			if (this.nonce != null) {
				writer.write("\", nonce=\"");
				writer.write(this.nonce);
			}
			writer.write("\", uri=\"");
			writer.write(this.uri);
			writer.write("\", response=\"");
			writer.write(this.response);
			writer.write("\"");
			return writer.toString();
		}
	}


	/**
	 * This exception type indicates a failed authentication attempt.
	 */
	static class AuthenticationException extends SecurityException {
		static private final long serialVersionUID = 1L;


		/**
		 * Creates a new instance with neither detail message nor cause.
		 */
		public AuthenticationException () {
			super();
		}


		/**
		 * Creates a new instance with the specified detail message and no cause.
		 * @param message the message
		 */
		public AuthenticationException (final String message) {
			super(message);
		}


		/**
		 * Creates a new instance with the specified cause and no detail message.
		 * @param cause the cause
		 */
		public AuthenticationException (final Throwable cause) {
			super(cause);
		}


		/**
		 * Creates a new instance with the specified detail message and cause.
		 * @param message the message
		 * @param cause the cause
		 */
		public AuthenticationException (final String message, final Throwable cause) {
			super(message, cause);
		}
	}


	/**
	 * This exception type indicates a failed authorization attempt.
	 */
	class AuthorizationException extends SecurityException {
		static private final long serialVersionUID = 1L;


		/**
		 * Creates a new instance with neither detail message nor cause.
		 */
		public AuthorizationException () {
			super();
		}


		/**
		 * Creates a new instance with the specified detail message and no cause.
		 * @param message the message
		 */
		public AuthorizationException (final String message) {
			super(message);
		}


		/**
		 * Creates a new instance with the specified cause and no detail message.
		 * @param cause the cause
		 */
		public AuthorizationException (final Throwable cause) {
			super(cause);
		}


		/**
		 * Creates a new instance with the specified detail message and cause.
		 * @param message the message
		 * @param cause the cause
		 */
		public AuthorizationException (final String message, final Throwable cause) {
			super(message, cause);
		}
	}
}