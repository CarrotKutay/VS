package de.htw.tool;

/**
 * This exception indicates a failed resource location attempt.
 */
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class ResourceNotFoundException extends RuntimeException {
	static private final long serialVersionUID = 1L;


	/**
	 * Creates a new instance with neither detail message nor cause.
	 */
	public ResourceNotFoundException () {
		super();
	}


	/**
	 * Creates a new instance with the specified detail message and no cause.
	 * @param message the message
	 */
	public ResourceNotFoundException (final String message) {
		super(message);
	}


	/**
	 * Creates a new instance with the specified cause and no detail message.
	 * @param cause the cause
	 */
	public ResourceNotFoundException (final Throwable cause) {
		super(cause);
	}


	/**
	 * Creates a new instance with the specified detail message and cause.
	 * @param message the message
	 * @param cause the cause
	 */
	public ResourceNotFoundException (final String message, final Throwable cause) {
		super(message, cause);
	}
}