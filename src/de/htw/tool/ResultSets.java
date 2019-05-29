package de.htw.tool;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * This facade provides common conversion operations for JDBC result sets.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public final class ResultSets {
	static private final Object[][] EMPTY = new Object[0][];


	/**
	 * Prevents external instantiation.
	 */
	private ResultSets () {}


	/**
	 * Returns the column labels of the given result set.
	 * @param resultSet the result set
	 * @return the column labels
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public String[] columnLabels (final ResultSet resultSet) throws NullPointerException, SQLException {
		final ResultSetMetaData metadata = resultSet.getMetaData();
		final String[] columnLabels = new String[metadata.getColumnCount()];
		for (int index = 0; index < columnLabels.length; ++index) {
			columnLabels[index] = metadata.getColumnLabel(index + 1);
		}
		return columnLabels;
	}


	/**
	 * Returns the given result set's remaining rows as an array row stream.
	 * @param resultSet the result set
	 * @return the array row stream
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public Stream<Object[]> arrayRowStream (final ResultSet resultSet) throws NullPointerException, SQLException {
		final Iterator<Object[]> iterator = RowIterator.newArrayBasedInstance(resultSet);
		final Iterable<Object[]> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), false);
	}


	/**
	 * Returns the given result set's remaining rows as a matrix array.
	 * @param resultSet the result set
	 * @return the matrix array
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public Object[][] arrayRows (final ResultSet resultSet) throws NullPointerException, SQLException {
		final int columnCount = resultSet.getMetaData().getColumnCount();

		final List<Object[]> rows = new ArrayList<>();
		while (resultSet.next()) {
			rows.add(arrayRow(resultSet, columnCount));
		}
		return rows.toArray(EMPTY);
	}


	/**
	 * Returns the given result set's current row as an array.
	 * @param resultSet the result set
	 * @return the array
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public Object[] arrayRow (final ResultSet resultSet) throws NullPointerException, SQLException {
		final int columnCount = resultSet.getMetaData().getColumnCount();
		return arrayRow(resultSet, columnCount);
	}


	/**
	 * Returns the given result set's current row as an array.
	 * @param resultSet the result set
	 * @param columnCount the number of columns
	 * @return the array
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws IllegalArgumentException if the given count is strictly negative
	 * @throws SQLException if there is a database related problem
	 */
	static protected Object[] arrayRow (final ResultSet resultSet, final int columnCount) throws NullPointerException, IllegalArgumentException, SQLException {
		if (columnCount < 0) throw new IllegalArgumentException();

		final Object[] row = new Object[columnCount];
		for (int index = 0; index < row.length; ++index) {
			row[index] = resultSet.getObject(index + 1);
		}
		return row;
	}


	/**
	 * Returns the given result set's remaining rows as a map row stream.
	 * @param resultSet the result set
	 * @param sorted whether or not the columns shall be sorted
	 * @return the map row stream
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public Stream<Map<String,Object>> mapRowStream (final ResultSet resultSet, final boolean sorted) throws NullPointerException, SQLException {
		final Iterator<Map<String,Object>> iterator = RowIterator.newMapBasedInstance(resultSet, sorted);
		final Iterable<Map<String,Object>> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), false);
	}


	/**
	 * Returns the given result set's remaining rows as a list of row maps.
	 * @param resultSet the result set
	 * @param sorted whether or not the columns shall be sorted
	 * @return the map row list
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public List<Map<String,Object>> mapRows (final ResultSet resultSet, final boolean sorted) throws NullPointerException, SQLException {
		final String[] columnLabels = columnLabels(resultSet);

		final List<Map<String,Object>> rows = new ArrayList<>();
		while (resultSet.next()) {
			rows.add(mapRow(resultSet, columnLabels, sorted));
		}
		return rows;
	}


	/**
	 * Returns the given result set's current row as a map.
	 * @param resultSet the result set
	 * @param sorted whether or not the columns shall be sorted
	 * @return the map
	 * @throws NullPointerException if the given result set is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static public Map<String,Object> mapRow (final ResultSet resultSet, final boolean sorted) throws NullPointerException, SQLException {
		final String[] columnLabels = columnLabels(resultSet);
		return mapRow(resultSet, columnLabels, sorted);
	}


	/**
	 * Returns the given result set's current row as a map.
	 * @param resultSet the result set
	 * @param columnLabels the column labels
	 * @param sorted whether or not the columns shall be sorted
	 * @return the map
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws SQLException if there is a database related problem
	 */
	static protected Map<String,Object> mapRow (final ResultSet resultSet, final String[] columnLabels, final boolean sorted) throws NullPointerException, SQLException {
		if (columnLabels == null) throw new NullPointerException();

		final Map<String,Object> row = sorted ? new TreeMap<>() : new HashMap<>();
		for (int index = 0; index < columnLabels.length; ++index) {
			row.put(columnLabels[index], resultSet.getObject(index + 1));
		}
		return row;
	}



	/**
	 * Instances of subtypes of this class adapt a JDBC result set into an {@link Iterator} of row elements. Subclasses are free to
	 * choose a suitable element type for the rows; the default implementations model rows as either maps associating column labels
	 * with cell values, or as arrays of cell values.
	 */
	static private final class RowIterator<T> implements Iterator<T> {
		private final ResultSet resultSet;
		private final String[] columnLabels;
		private final BiFunction<ResultSet,String[],T> rowMapper;
		private Boolean hasNext;


		/**
		 * Returns a new result set iterator that exposes rows as maps associating column labels with cell values.
		 * @param resultSet the result set
		 * @param sorted whether or not the columns shall be sorted
		 * @return the map based result set iterator
		 * @throws NullPointerException if the given result set is {@code null}
		 * @throws SQLException if a database access error occurs or the given result set is closed
		 */
		static public Iterator<Map<String,Object>> newMapBasedInstance (final ResultSet resultSet, final boolean sorted) throws NullPointerException, SQLException {
			final BiFunction<ResultSet,String[],Map<String,Object>> rowMapper = (rows,columnLabels) -> {
				try {
					return ResultSets.mapRow(rows, columnLabels, sorted);
				} catch (final SQLException exception) {
					throw new UncheckedSQLException(exception);
				}
			};

			return new RowIterator<Map<String,Object>>(resultSet, rowMapper);
		}


		/**
		 * Returns a new result set iterator that exposes rows as arrays of cell values.
		 * @param resultSet the result set
		 * @return the array based result set iterator
		 * @throws NullPointerException if the given result set is {@code null}
		 * @throws SQLException if a database access error occurs or the given cursor is closed
		 */
		static public Iterator<Object[]> newArrayBasedInstance (final ResultSet resultSet) throws NullPointerException, SQLException {
			final BiFunction<ResultSet,String[],Object[]> rowMapper = (rows,columnLabels) -> {
				try {
					return ResultSets.arrayRow(rows, columnLabels.length);
				} catch (final SQLException exception) {
					throw new UncheckedSQLException(exception);
				}
			};

			return new RowIterator<Object[]>(resultSet, rowMapper);
		}


		/**
		 * Creates a new instance based on the remaining rows of the given result set.
		 * @param resultSet the result set
		 * @param rowMapper the row mapper function
		 * @throws NullPointerException if the given result set is {@code null}
		 * @throws SQLException if there is a database related problem
		 */
		public RowIterator (final ResultSet resultSet, final BiFunction<ResultSet,String[],T> rowMapper) throws NullPointerException, SQLException {
			this.resultSet = resultSet;
			this.columnLabels = ResultSets.columnLabels(resultSet);
			this.rowMapper = rowMapper;
		}


		/**
		 * Returns the current row.
		 * @return the result set row
		 * @throws UncheckedSQLException if a database access error occurs or this method is called on a closed result set
		 */
		protected T current () throws UncheckedSQLException {
			return this.rowMapper.apply(this.resultSet, this.columnLabels);
		}


		/**
		 * {@inheritDoc}
		 * @throws IllegalStateException if there is a database related problem
		 * @throws UnsupportedOperationException if the {@code hasNext} operation is not supported by this list iterator
		 */
		public final boolean hasNext () throws UncheckedSQLException {
			if (this.hasNext == Boolean.TRUE) return true;

			try {
				return this.hasNext = this.resultSet.next();
			} catch (final SQLException exception) {
				throw new UncheckedSQLException(exception);
			}
		}


		/**
		 * {@inheritDoc}
		 * @throws NoSuchElementException {@inheritDoc}
		 * @throws UncheckedSQLException if there is a database related problem
		 */
		public final T next () throws NoSuchElementException, UncheckedSQLException {
			if (!this.hasNext()) throw new NoSuchElementException();

			this.hasNext = null;
			return this.current();
		}


		/**
		 * {@inheritDoc}
		 * @throws UncheckedSQLException if there is a database related problem
		 * @throws UnsupportedOperationException {@inheritDoc}
		 */
		public final void remove () throws UnsupportedOperationException, UncheckedSQLException {
			try {
				this.resultSet.deleteRow();
			} catch (final SQLFeatureNotSupportedException exception) {
				throw new UnsupportedOperationException(exception);
			} catch (final SQLException exception) {
				throw new UncheckedSQLException(exception);
			}
		}
	}
}