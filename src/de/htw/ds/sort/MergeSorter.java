package de.htw.ds.sort;

import java.io.IOException;
import de.htw.tool.Copyright;


/**
 * Interface describing stream sorters that can sort tremendous amounts of objects without
 * necessarily loading them all into storage. The elements are sorted into ascending
 * natural order using the merge-sort algorithm.
 * @param <E> the element type to be sorted in naturally ascending order
 */
@Copyright(year=2010, holders="Sascha Baumeister")
public interface MergeSorter<E extends Comparable<E>> extends AutoCloseable {

	/**
	 * Describes the two states defined for stream sorters.
	 */
	static enum State {

		/**
		 * In this state a stream sorter may only be written.
		 */
		WRITE,

		/**
		 * In this state a stream sorter may only be sorted.
		 */
		SORT,

		/**
		 * In this state a stream sorter may only be read.
		 */
		READ,

		/**
		 * In this state a stream sorter cannot be used anymore.
		 */
		CLOSED
	}


	/**
	 * Closes this instance and all it's associated resources and switches the receiver
	 * into {@link State#CLOSED} state.
	 * @throws IOException if an I/O related problem occurs
	 */
	void close () throws IOException;


	/**
	 * Writes the given element into internal storage, or switches the receiver into
	 * {@link State#SORT} state if the given element is {@code null}
	 * @param element the element to be stored, or {@code null} for none
	 * @throws IllegalStateException if the sorter is not in {@link State#WRITE} state
	 * @throws IOException if there is an I/O related problem
	 */
	void write (E element) throws IllegalStateException, IOException;


	/**
	 * Sorts the elements in internal storage, and subsequently switches the receiver into
	 * {@link State#READ} state.
	 * @throws IllegalStateException if the sorter is not in {@link State#SORT} state
	 * @throws IOException if there is an I/O related problem
	 */
	void sort () throws IllegalStateException, IOException;


	/**
	 * Returns the next element from internal storage, or {@code null} if there are no
	 * more elements; the latter also clears internal storage, and switches the receiver
	 * into {@link State#WRITE} state.
	 * @return the next element in natural sort order, or {@code null} for none
	 * @throws IllegalStateException if the sorter is not in {@link State#READ} state
	 * @throws IOException if there is an I/O related problem
	 */
	E read () throws IllegalStateException, IOException;


	/**
	 * Returns the current state.
	 * @return the state
	 */
	State getState ();
}