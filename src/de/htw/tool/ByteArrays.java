package de.htw.tool;


/**
 * Facade for byte array operations.
 */
@Copyright(year = 2005, holders = "Sascha Baumeister")
public class ByteArrays {

	/**
	 * Prevents external instantiation.
	 */
	private ByteArrays () {}


	/**
	 * Returns the index of the target array found within the source array, or {@code -1} for none.
	 * @param source the bytes being searched
	 * @param target the bytes being searched for
	 * @param offset the offset to begin searching from
	 * @return the index of the target within the given source, or {@code -1} for none
	 */
	static public int indexOf (final byte[] source, final byte[] target, final int offset) throws NullPointerException, IllegalArgumentException {
		if (offset < 0 | offset > source.length) throw new IllegalArgumentException();
		if (offset == source.length) return -1;
		if (target.length == 0) return offset;

		final byte first = target[0];
		for (int index = offset, stop = source.length - target.length + 1; index < stop; ++index) {
			if (source[index] != first) continue;
			if (containsAt(source, target, index)) return index;
		}

		return -1;
	}


	/**
	 * Returns whether or not the given source contains the given target at the specified offset.
	 * @param source the bytes being searched
	 * @param position the source offset to begin searching from
	 * @param target the bytes being searched for
	 * @return {@code true} if the given source contains the target at the given position, {@code false} otherwise
	 */
	static private boolean containsAt (final byte[] source, final byte[] target, final int position) throws NullPointerException, IllegalArgumentException {
		for (int index = 0; index < target.length; ++index) {
			if (source[position + index] != target[index]) return false;
		}
		return true;
	}
}
