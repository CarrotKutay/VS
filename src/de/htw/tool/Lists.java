package de.htw.tool;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * This facade provides some basic operations for lists.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public final class Lists {

	/**
	 * Prevents external instantiation.
	 */
	private Lists () {}


	/**
	 * Adds an element to the given list at a random position. Note that this operation is not thread safe when used with a list
	 * that is shared between multiple threads.
	 * @param list the list
	 * @param element the element to be added
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws UnsupportedOperationException if the add operation is not supported by the given list
	 * @throws ClassCastException if the given element's class prevents it from being added
	 * @throws IllegalArgumentException if a property of the specified element prevents it from being added
	 */
	static public <E> void addRandom (final List<E> list, final E element) {
		try {
			list.add(ThreadLocalRandom.current().nextInt(list.size()), element);
		} catch (final IndexOutOfBoundsException | IllegalArgumentException exception) {
			list.add(element);
		}
	}


	/**
	 * Adds an element to the given list at position zero. Note that this operation is not thread safe when used with a list
	 * that is shared between multiple threads.
	 * @param list the list
	 * @param element the element to be added
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws UnsupportedOperationException if the add operation is not supported by the given list
	 * @throws ClassCastException if the given element's class prevents it from being added
	 * @throws IllegalArgumentException if a property of the specified element prevents it from being added
	 */
	static public <E> void addFirst (final List<E> list, final E element) {
		list.add(0, element);
	}


	/**
	 * Adds an element to the given list at position zero. Note that this operation is not thread safe when used with a list
	 * that is shared between multiple threads.
	 * @param list the list
	 * @param element the element to be added
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws UnsupportedOperationException if the add operation is not supported by the given list
	 * @throws ClassCastException if the given element's class prevents it from being added
	 * @throws IllegalArgumentException if a property of the specified element prevents it from being added
	 */
	static public <E> void addLast (final List<E> list, final E element) {
		list.add(element);
	}


	/**
	 * Returns the first element from the given list. Note that this operation is not thread safe when used with a list that is
	 * shared between multiple threads.
	 * @param list the list
	 * @return the element
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E getFirst (final List<E> list) {
		try {
			return list.get(0);
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Returns the last element from the given list. Note that this operation is not thread safe when used with a list that is
	 * shared between multiple threads.
	 * @param list the list
	 * @return the element
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E getLast (final List<E> list) {
		try {
			return list.get(list.size() - 1);
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Returns a random element from the given list. Note that this operation is not thread safe when used with a list that is
	 * shared between multiple threads.
	 * @param list the list
	 * @return the element
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E getRandom (final List<E> list) {
		try {
			return list.get(ThreadLocalRandom.current().nextInt(list.size()));
		} catch (final IndexOutOfBoundsException | IllegalArgumentException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Removes the first element from the given list and returns it. Note that this operation is not thread safe when used with
	 * a list that is shared between multiple threads.
	 * @param list the list
	 * @return the element that has been removed
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws UnsupportedOperationException if element removal is not supported by the given list
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E removeFirst (final List<E> list) {
		try {
			return list.remove(0);
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Removes the last element from the given list and returns it. Note that this operation is not thread safe when used with a
	 * list that is shared between multiple threads.
	 * @param list the list
	 * @return the element that has been removed
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws UnsupportedOperationException if element removal is not supported by the given list
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E removeLast (final List<E> list) {
		try {
			return list.remove(list.size() - 1);
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Removes an element from the given list at a random position and returns it. Note that this operation is not thread safe
	 * when used with a list that is shared between multiple threads.
	 * @param list the list
	 * @return the element that has been removed
	 * @throws NullPointerException if the given list is {@code null}
	 * @throws UnsupportedOperationException if element removal is not supported by the given list
	 * @throws IllegalStateException if the given list is empty or concurrently modified
	 */
	static public <E> E removeRandom (final List<E> list) {
		try {
			return list.remove(ThreadLocalRandom.current().nextInt(list.size()));
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}
}