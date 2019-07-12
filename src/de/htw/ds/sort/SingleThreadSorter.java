package de.htw.ds.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import de.htw.tool.Copyright;


/**
 * Single-threaded sorter implementation that collects elements into a list and sorts them using the
 * underlying merge sort implementation of lists. Note that this implementation implies that such a
 * sorter cannot scale its workload over more than one processor core, and additionally all elements
 * are stored within the RAM of a single process.
 * @param <E> the element type to be sorted in naturally ascending order
 */
@Copyright(year=2010, holders="Sascha Baumeister")
public class SingleThreadSorter<E extends Comparable<E>> implements MergeSorter<E> {
	private final List<E> elements;
	private int readIndex;
	private State state;


	/**
	 * Creates a new instance in {@link MergeSorter.State#WRITE} state.
	 */
	public SingleThreadSorter () {
		this.elements = new ArrayList<E>();
		this.state = State.WRITE;
	}


	@Override
	public void close () throws IOException {
		this.elements.clear();
		this.readIndex = 0;
		this.state = State.CLOSED;
	}


	/**
	 * {@inheritDoc}
	 */
	public void write (final E element) throws IllegalStateException {
		if (this.state != State.WRITE) throw new IllegalStateException(this.state.name());

		if (element == null) {
			this.state = State.SORT;
		} else {
			this.elements.add(element);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void sort () throws IllegalStateException {
		if (this.state != State.SORT) throw new IllegalStateException(this.state.name());

		this.elements.sort(Comparator.naturalOrder());
		this.state = State.READ;
	}


	/**
	 * {@inheritDoc}
	 */
	public E read () throws IllegalStateException {
		if (this.getState() != State.READ) throw new IllegalStateException();

		if (this.readIndex < this.elements.size()) return this.elements.get(this.readIndex++);

		this.elements.clear();
		this.readIndex = 0;
		this.state = State.WRITE;
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public State getState () {
		return this.state;
	}


	/**
	 * Returns a new single-thread sorter instance.
	 * @return the sorter created
	 */
	static public <T extends Comparable<T>> MergeSorter<T> newInstance () {
		return new SingleThreadSorter<T>();
	}
}