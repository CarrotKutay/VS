package de.htw.tool;

/**
 * This exception indicates a failed authorization attempt.
 */
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class AuthorizationException extends SecurityException {
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