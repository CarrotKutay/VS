package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * This checked exception is thrown by the example worker to indicate work related problems.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ExampleCheckedException extends Exception {
	static private final long serialVersionUID = 1L;


	/**
	 * Creates a new instance with neither message nor cause.
	 */
	public ExampleCheckedException () {}


	/**
	 * Creates a new instance with the given message.
	 * @param message the message
	 */
	public ExampleCheckedException (final String message) {
		super(message);
	}


	/**
	 * Creates a new instance with the given message and cause.
	 * @param message the message
	 * @param cause the cause
	 */
	public ExampleCheckedException (final String message, final Throwable cause) {
		super(message, cause);
	}


	/**
	 * Creates a new instance with the given cause.
	 * @param cause the cause
	 */
	public ExampleCheckedException (final Throwable cause) {
		super(cause);
	}
}