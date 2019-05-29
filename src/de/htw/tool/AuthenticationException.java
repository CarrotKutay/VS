package de.htw.tool;

/**
 * This exception indicates a failed authentication attempt.
 */
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class AuthenticationException extends SecurityException {
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