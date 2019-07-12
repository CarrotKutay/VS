package de.htw.tool;

/**
 * Reusable class referencing a single object, or {@code null}. It is useful for shared read/write accessto objects across
 * multiple threads, or in order to wrap arrays for extended {@link #hashCode()} and {@link #equals(Object)} semantics provided
 * by class {@link java.util.Arrays}.
 * @param <T> the object type
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public class Reference<T> {
	private T object = null;


	/**
	 * Returns the referenced object.
	 * @return the object
	 */
	public T get () {
		return this.object;
	}


	/**
	 * Sets the referenced object.
	 * @param object the object
	 */
	public void put (final T object) {
		this.object = object;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Reflections.hashCode(this.object);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object object) {
		if (!(object instanceof Reference)) return false;
		return Reflections.equals(this.object, ((Reference<?>) object).object);
	}
}