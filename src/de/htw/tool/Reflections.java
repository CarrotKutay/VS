package de.htw.tool;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This facade provides additional methods for type reflection. Note that this class is declared final because it is a facade,
 * and therefore not supposed to be extended.
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public final class Reflections {

	/**
	 * Prevents instantiation.
	 */
	private Reflections () {

	}


	/**
	 * Returns the corresponding wrapper type for a given primitive type, or the type itself if it is not a primitive type.
	 * @param type the type
	 * @return the boxed type
	 */
	@SuppressWarnings("unchecked")
	static public <T> Class<T> boxType (final Class<T> type) {
		if (type == double.class) return (Class<T>) Double.class;
		if (type == float.class) return (Class<T>) Float.class;
		if (type == long.class) return (Class<T>) Long.class;
		if (type == int.class) return (Class<T>) Integer.class;
		if (type == short.class) return (Class<T>) Short.class;
		if (type == byte.class) return (Class<T>) Byte.class;
		if (type == char.class) return (Class<T>) Character.class;
		if (type == boolean.class) return (Class<T>) Boolean.class;
		if (type == void.class) return (Class<T>) Void.class;
		return type;
	}


	/**
	 * Returns the corresponding primitive type for a given wrapper type, or the type itself if it is not a wrapper type.
	 * @param type the type
	 * @return the unboxed type
	 */
	@SuppressWarnings("unchecked")
	static public <T> Class<T> unboxType (final Class<T> type) {
		if (type == Double.class) return (Class<T>) double.class;
		if (type == Float.class) return (Class<T>) float.class;
		if (type == Long.class) return (Class<T>) long.class;
		if (type == Integer.class) return (Class<T>) int.class;
		if (type == Short.class) return (Class<T>) short.class;
		if (type == Byte.class) return (Class<T>) byte.class;
		if (type == Character.class) return (Class<T>) char.class;
		if (type == Boolean.class) return (Class<T>) boolean.class;
		if (type == Void.class) return (Class<T>) void.class;
		return type;
	}


	/**
	 * Returns {@code true} if the given type is an integral numeric type, {@code false} otherwise.
	 * @param type the type
	 * @return whether the given type is an integral numeric type
	 */
	static public boolean isIntegralType (Class<? extends Number> type) {
		type = boxType(type);
		return type == Long.class | type == Integer.class || type == Short.class | type == Byte.class || type == BigInteger.class | type == AtomicInteger.class | type == AtomicLong.class;
	}


	/**
	 * Returns {@code true} if the given type is a decimal numeric type, {@code false} otherwise.
	 * @param type the type
	 * @return whether the given type is a decimal numeric type
	 */
	static public boolean isDecimalType (Class<? extends Number> type) {
		type = boxType(type);
		return type == Float.class | type == Double.class | type == BigDecimal.class;
	}


	/**
	 * Casts the given numeric value into the given type, and returns the resulting number. The algorithm makes sure the
	 * conversion causes minimal precision loss.
	 * @param value the numeric value
	 * @param type the target type
	 * @return the converted value
	 * @throws NullPointerException if the given type is {@code null}
	 * @throws IllegalArgumentException if the given type is not one of the standard {@link Number} types
	 */
	@SuppressWarnings("unchecked")
	static public <T extends Number> T cast (final Number value, Class<T> type) {
		if (value == null) return (T) value;
		type = boxType(type);

		if (type == value.getClass()) return (T) value;
		if (type == BigDecimal.class) {
			if (value instanceof BigInteger) return (T) new BigDecimal((BigInteger) value);
			if (value instanceof Long | value instanceof Integer || value instanceof Short | value instanceof Byte || value instanceof AtomicInteger | value instanceof AtomicLong) return (T) BigDecimal.valueOf(value.longValue());
			return (T) BigDecimal.valueOf(value.doubleValue());
		}
		if (type == BigInteger.class) return value instanceof BigDecimal ? (T) ((BigDecimal) value).toBigInteger() : (T) BigInteger.valueOf(value.longValue());

		if (type == Double.class) return (T) (Double) value.doubleValue();
		if (type == Float.class) return (T) (Float) value.floatValue();
		if (type == Long.class) return (T) (Long) value.longValue();
		if (type == Integer.class) return (T) (Integer) value.intValue();
		if (type == Short.class) return (T) (Short) value.shortValue();
		if (type == Byte.class) return (T) (Byte) value.byteValue();

		if (type == AtomicInteger.class) return (T) new AtomicInteger(value.intValue());
		if (type == AtomicLong.class) return (T) new AtomicLong(value.longValue());
		throw new IllegalArgumentException();
	}


	/**
	 * Returns a hash code value for the object. The operation returns zero if the given object is {@code null}, or the result
	 * of calling {@link Object#hashCode()} if it is not an array. Otherwise, the result is calculated using one of the
	 * {@code hashCode()} methods of class {@link Arrays}, depending on the array's component type.
	 * @param object the object
	 * @return the hash code
	 */
	static public int hashCode (final Object object) {
		if (object == null) return 0;
		final Class<?> componentType = object.getClass().getComponentType();

		if (componentType == null) return object.hashCode();
		if (!componentType.isPrimitive()) return Arrays.hashCode((Object[]) object);
		if (componentType == byte.class) return Arrays.hashCode((byte[]) object);
		if (componentType == short.class) return Arrays.hashCode((short[]) object);
		if (componentType == int.class) return Arrays.hashCode((int[]) object);
		if (componentType == long.class) return Arrays.hashCode((long[]) object);
		if (componentType == char.class) return Arrays.hashCode((char[]) object);
		if (componentType == float.class) return Arrays.hashCode((float[]) object);
		if (componentType == double.class) return Arrays.hashCode((double[]) object);
		if (componentType == boolean.class) return Arrays.hashCode((boolean[]) object);
		throw new AssertionError();
	}


	/**
	 * Determines equality for the given objects, which is {@code true} if both are identical, otherwise {@code false} if one of
	 * them is {@code null}. Otherwise, the result is determined by calling {@link Object#equals(Object)} if they are not arrays
	 * sharing the same component type. Otherwise, the result is calculated using one of the {@code equals()} methods of class
	 * {@link Arrays}, depending on their common component type.
	 * @param left the left operand
	 * @param right the right operand
	 * @return whether or not both operands are equal
	 */
	static public boolean equals (final Object left, final Object right) {
		if (left == right) return true;
		if (left == null | right == null) return false;

		final Class<?> leftComponentType = left.getClass().getComponentType();
		final Class<?> rightComponentType = right.getClass().getComponentType();
		if (leftComponentType != rightComponentType | leftComponentType == null) return left.equals(right);

		if (!leftComponentType.isPrimitive()) return Arrays.equals((Object[]) left, (Object[]) right);
		if (leftComponentType == byte.class) return Arrays.equals((byte[]) left, (byte[]) right);
		if (leftComponentType == short.class) return Arrays.equals((short[]) left, (short[]) right);
		if (leftComponentType == int.class) return Arrays.equals((int[]) left, (int[]) right);
		if (leftComponentType == long.class) return Arrays.equals((long[]) left, (long[]) right);
		if (leftComponentType == char.class) return Arrays.equals((char[]) left, (char[]) right);
		if (leftComponentType == float.class) return Arrays.equals((float[]) left, (float[]) right);
		if (leftComponentType == double.class) return Arrays.equals((double[]) left, (double[]) right);
		if (leftComponentType == boolean.class) return Arrays.equals((boolean[]) left, (boolean[]) right);
		throw new AssertionError();
	}


	/**
	 * Returns the content of the given collection as an array of the same size, and with the given element type. If the given
	 * element type is primitive, the collection's elements are wrapped before being inserted into the resulting array.
	 * @param collection the collection
	 * @param elementType the element type of the resulting array
	 * @return the resulting array
	 * @throws NullPointerException if the given collection, or any of it's elements, is {@code null}
	 * @throws IllegalArgumentException if the given element type is primitive and an unwrapping conversion fails
	 * @throws ClassCastException if the given element type is not primitive, and any of the collection's elements cannot be
	 *         assigned to it
	 */
	static public Object toArray (final Collection<?> collection, final Class<?> elementType) {
		final Object result = Array.newInstance(elementType, collection.size());

		final Iterator<?> iterator = collection.iterator();
		for (int index = 0; iterator.hasNext(); ++index) {
			Array.set(result, index, iterator.next());
		}

		return result;
	}


	/**
	 * Returns the content of the given array as a list of the same size, and with the given element type. If the given array's
	 * elements are primitive, they are wrapped before being inserted into the resulting list.
	 * @param array the array
	 * @param elementType the element type of the resulting collection
	 * @return the resulting collection
	 * @throws NullPointerException if the given array, or any of it's elements, is {@code null}
	 * @throws IllegalArgumentException if the given element type is primitive
	 * @throws ClassCastException if any of the array's elements (or it's wrapper replacement in case of primitive arrays)
	 *         cannot be assigned to the given element type
	 */
	static public <E> List<E> toList (final Object array, final Class<E> elementType) {
		if (elementType.isPrimitive()) throw new IllegalArgumentException();

		final int arrayLength = Array.getLength(array);
		final List<E> result = new ArrayList<>(arrayLength);

		for (int index = 0; index < arrayLength; ++index) {
			@SuppressWarnings("unchecked")
			final E element = (E) Array.get(array, index);

			if (!elementType.isAssignableFrom(element.getClass())) throw new ClassCastException();
			result.add(element);
		}

		return result;
	}


	/**
	 * Returns an array that is one larger than the given one, and contains the given value at the given index.
	 * @param array the array
	 * @param index the insertion index of the new element
	 * @param value the value to be inserted
	 * @return a new array containing the old elements plus an additional one
	 * @throws NullPointerException if the given array is {@code null}
	 * @throws IllegalArgumentException if the given index is outside range {@code [0, array.length]}, or if the array component
	 *         type is primitive and an unwrapping conversion fails
	 */
	static public <T> T arrayInsert (final T array, final int index, final Object value) {
		final int length = Array.getLength(array);
		if (index < 0 | index > length) throw new IllegalArgumentException();

		@SuppressWarnings("unchecked")
		final T result = (T) Array.newInstance(array.getClass().getComponentType(), length + 1);
		if (value != null) Array.set(result, index, value);
		if (index > 0) System.arraycopy(array, 0, result, 0, index);
		if (index < length) System.arraycopy(array, index, result, index + 1, length - index);
		return result;
	}


	/**
	 * Returns an array that is one smaller than the given one, and no longer contains the element at the given index.
	 * @param array the array
	 * @param index the index of the element to be removed within the given array
	 * @return a new array containing the old elements except one
	 * @throws NullPointerException if the given array is {@code null}
	 * @throws IllegalArgumentException if the given index is outside range {@code [0, array.length[}
	 */
	static public <T> T arrayRemove (final T array, final int index) {
		final int length = Array.getLength(array);
		if (index < 0 | index >= length) throw new IllegalArgumentException();

		@SuppressWarnings("unchecked")
		final T result = (T) Array.newInstance(array.getClass().getComponentType(), length - 1);
		if (index > 0) System.arraycopy(array, 0, result, 0, index);
		if (index < length - 1) System.arraycopy(array, index + 1, result, index, length - index - 1);
		return result;
	}


	/**
	 * Returns an array that consists of all the elements of both given arrays.
	 * @param leftArray the left array
	 * @param rightArray the right array
	 * @return the union of both arrays
	 * @throws NullPointerException if any of the given arrays is {@code null}
	 * @throws IllegalArgumentException if the two arrays do not share the same element class
	 */
	static public <T> T union (final T leftArray, final T rightArray) {
		final Class<?> leftComponentClass = leftArray.getClass().getComponentType();
		final Class<?> rightComponentClass = rightArray.getClass().getComponentType();
		if (leftComponentClass != rightComponentClass) throw new IllegalArgumentException();

		final int leftLength = Array.getLength(leftArray);
		final int rightLength = Array.getLength(rightArray);

		@SuppressWarnings("unchecked")
		final T union = (T) Array.newInstance(leftArray.getClass().getComponentType(), leftLength + rightLength);
		System.arraycopy(leftArray, 0, union, 0, leftLength);
		System.arraycopy(rightArray, 0, union, leftLength, rightLength);
		return union;
	}


	/**
	 * Returns the given copyright annotation as a text.
	 * @param copyright the copyright annotation
	 * @return the text
	 * @throws NullPointerException if the given copyright is {@code null}
	 */
	static public String toString (final Copyright copyright) {
		final String holders = String.join(", ", copyright.holders());
		final String licenses = String.join(", ", copyright.licenses());
		final String sentence1 = String.format("Copyright \u00A9 %d by %s.", copyright.year(), holders);
		final String sentence2 = licenses.isEmpty() ? "All rights reserved." : String.format("Licensed under %s.", licenses);
		return String.join(" ", sentence1, sentence2);
	}
}