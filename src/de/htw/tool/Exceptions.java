package de.htw.tool;

/**
 * This facade provides exception related operations.
 */
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class Exceptions {

	/**
	 * Prevents external instantiation.
	 */
	private Exceptions () {}


	/**
	 * Returns the root cause of the given exception, which may be the exception itself. Root causes can be instrumental in
	 * analyzing a given problem, especially when using libraries that throw exception wrappers to avoid compilation problems
	 * related to checked exceptions and inheritance.
	 * @param exception the exception
	 * @return the exception's root cause
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public Throwable rootCause (Throwable exception) throws NullPointerException {
		while (exception.getCause() != null)
			exception = exception.getCause();
		return exception;
	}
}