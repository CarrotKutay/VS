package de.htw.tool;

import java.sql.SQLException;


/**
 * Wraps an {@link SQLException} with an unchecked exception.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public class UncheckedSQLException extends RuntimeException {
	static private final long serialVersionUID = 6599383916530837753L;


	/**
	 * Creates a new instance.
	 * @param cause the {@code SQLException}
	 * @throws NullPointerException if the cause is {@code null}
	 */
	public UncheckedSQLException (final SQLException cause) {
		super(cause);
	}


	/**
	 * Creates a new instance.
	 * @param message the detail message, or {@code null} for none
	 * @param cause the {@code SQLException}
	 * @throws NullPointerException if the cause is {@code null}
	 */
	public UncheckedSQLException (final String message, final SQLException cause) {
		super(message, cause);
	}


	/**
	 * Returns the cause of this exception.
	 * @return the {@code SQLException} which is the cause of this exception
	 */
	@Override
	public SQLException getCause () {
		return (SQLException) super.getCause();
	}
}