package de.htw.tool;

import java.io.StringWriter;


/**
 * This facade represents a collection of logical operations on 64bit integer words which are interpreted as bit-arrays. In
 * opposition to {@link java.util.BitSet}, this class is designed as a facade for bit-parallel operations, in order to maximize
 * raw operation speed and flexibility. Note that this class is declared final because it is a facade, and therefore not
 * supposed to be extended.<br />
 * Also note that almost every operation of this facade is available in a flavor that operates on a single 64bit word, and
 * another one that operates on an array of 64bit words. The single-word operations are at least double as fast as the
 * multi-word versions once fully inlined by the JVM, even when using a single word in both cases. This is caused partly by the
 * multi-word methods requiring thorough boundary checks, while the single-word methods can do without! While the core arguments
 * boundaries for single-word operations are {@code [0, 63]} for indices and offsets ( {@code [1, 64]} for ranges), any kind of
 * value may actually be passed; the shift operators used for range masking utilize only the least significant {@code 6} bits of
 * any such argument, which automatically "fits" the arguments into their respective bounds. This implies that a single-word
 * operation with {@code index = 0} behaves the same as with {@code 64} or {@code -64} etc, while an operation with
 * {@code range = 64} behaves the same as with {@code 0} or {@code -64}, etc.
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public final class BitArrays {
	static private final byte WORD_SIZE = Long.SIZE;
	static private final byte LOG2_WORD_SIZE = 6;
	static private final long MAX_BIT_COUNT = (long) Integer.MAX_VALUE << LOG2_WORD_SIZE;


	/**
	 * Prevents instantiation
	 */
	private BitArrays () {}


	//*************************************************************//
	// Bit count to Byte count conversions.                        //
	//*************************************************************//

	/**
	 * Returns the byte count corresponding to the given bit count.
	 * @param bitCount the bit count
	 * @return the byte count
	 * @throws IllegalArgumentException if the given bit count is strictly negative
	 */
	static public long toByteCount (final long bitCount) throws IllegalArgumentException {
		if (bitCount < 0) throw new IllegalArgumentException();
		return bitCount == 0 ? 0 : ((bitCount - 1L) >>> 3) + 1;
	}


	/**
	 * Returns the byte count corresponding to the given bit count.
	 * @param bitLength the bit count
	 * @return the byte count
	 * @throws IllegalArgumentException if the given byte count is strictly negative, or exceeds the maximum number of bits in a
	 *         long[]
	 */
	static public long toBitCount (final long byteCount) throws IllegalArgumentException {
		if (byteCount < 0 | byteCount >= MAX_BIT_COUNT) throw new IllegalArgumentException();
		return byteCount << 3;
	}


	//*************************************************************//
	// Operations targeting bit-arrays consisting of single words. //
	//*************************************************************//

	/**
	 * Returns {@code true} if the bit at the specified index is {@code 1}, otherwise {@code false} for {@code 0}.
	 * @param bits the bits
	 * @param index the index
	 * @return whether or not the specified bit is set
	 */
	static public boolean get (final long bits, final byte index) {
		return ((bits >> index) & 1) == 1;
	}


	/**
	 * Returns a copy of the given bits, with the one at the given index set to the given value. The value being set is
	 * {@code 1} for {@code true}, otherwise {@code 0} for {@code false}.
	 * @param bits the bits
	 * @param index the index
	 * @param value the value
	 * @return the modified bits
	 */
	static public long set (final long bits, final byte index, final boolean value) {
		return value ? on(bits, index) : off(bits, index);
	}


	/**
	 * Returns a copy of the given bits, with the one at the given index set to {@code 0}.
	 * @param bits the bits
	 * @param index the index
	 * @return the modified bits
	 */
	static public long off (final long bits, final byte index) {
		return bits & ~(1L << index);
	}


	/**
	 * Returns a copy of the given bits, with the ones within the given range set to {@code 0}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the modified bits
	 */
	static public long off (final long bits, final byte offset, final byte range) {
		return xor(bits, bits, offset, range);
	}


	/**
	 * Returns a copy of the given bits, with the bit at the given index set to {@code 1}.
	 * @param bits the bits
	 * @param index the index
	 * @return the modified bits
	 */
	static public long on (final long bits, final byte index) {
		return bits | (1L << index);
	}


	/**
	 * Returns a copy of the given bits, with the ones within the given range set to {@code 1}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the modified bits
	 */
	static public long on (final long bits, final byte offset, final byte range) {
		return xand(bits, bits, offset, range);
	}


	/**
	 * Returns a copy of the given bits, with the one at the given index set to it's opposite value.
	 * @param bits the bits
	 * @param index the index
	 * @return the modified bits
	 */
	static public long flip (final long bits, final byte index) {
		return bits ^ (1L << index);
	}


	/**
	 * Returns a copy of the given bits, with the ones within the given range set to their respective opposite values.
	 * @param bitArray the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the modified bits
	 */
	static public long flip (final long bitArray, final byte offset, final byte range) {
		return bitArray ^ (-1L >>> -range << offset);
	}


	/**
	 * Rotates the given bits to the right by the given offset, i.e. towards higher indices.
	 * @param bits the bits
	 * @param offset the rotational shift
	 */
	static public long rotate (final long bits, final byte offset) {
		return Long.rotateLeft(bits, offset);
	}


	/**
	 * Returns the number of {@code 1}-bits within the given range, i.e. the population count. Note that the number of {@code 0}
	 * -bits can easily be derived by calculating {@code 64 - cardinality}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the cardinality within the given range
	 */
	static public byte cardinality (final long bits, final byte offset, final byte range) {
		return (byte) Long.bitCount(bits & (-1L >>> -range << offset));
	}


	/**
	 * Returns the index of the first (lowest) {@code 1}-bit within the given range, or {@code -1} if there is none. Note that
	 * the index of the first {@code 0}-bit can easily be derived by passing a flipped bit-array.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the index, or {@code -1} for none
	 */
	static public byte firstIndex (long bits, final byte offset, final byte range) {
		bits &= -1L >>> -range << offset;
		return (byte) (bits == 0 ? -1 : Long.numberOfTrailingZeros(bits));
	}


	/**
	 * Returns the index of the last (highest) {@code 1}-bit within the given range, or {@code -1} if there is none. Note that
	 * the index of the last {@code 0}-bit can easily be derived by passing a flipped bit-array.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the index, or {@code -1} for none
	 */
	static public byte lastIndex (long bits, final byte offset, final byte range) {
		bits &= -1L >>> -range << offset;
		return (byte) (bits == 0 ? -1 : WORD_SIZE - 1 - Long.numberOfLeadingZeros(bits));
	}


	/**
	 * Returns the bit-wise disjunction ({@code OR}) of the given operands, after masking out any right operand bits outside the
	 * given range.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the bit-wise disjunction
	 */
	static public long or (final long leftBits, final long rightBits, final byte offset, final byte range) {
		return leftBits | (rightBits & (-1L >>> -range << offset));
	}


	/**
	 * Returns the bit-wise conjunction ({@code AND}) of the given operands, after masking out any right operand bits outside
	 * the given range.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the bit-wise conjunction
	 */
	static public long and (final long leftBits, final long rightBits, final byte offset, final byte range) {
		return leftBits & (rightBits | ~(-1L >>> -range << offset));
	}


	/**
	 * Returns the bit-wise exclusive disjunction ({@code XOR}) of the given operands, after masking out any right operand bits
	 * outside the given range.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the bit-wise exclusive disjunction
	 */
	static public long xor (final long leftBits, final long rightBits, final byte offset, final byte range) {
		return leftBits ^ (rightBits & (-1L >>> -range << offset));
	}


	/**
	 * Returns the bit-wise exclusive conjunction ({@code XAND}) of the given operands, after masking out any right operand bits
	 * outside the given range.
	 * @param leftWord the left operand bits
	 * @param rightWord the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the bit-wise exclusive conjunction
	 */
	static public long xand (final long leftWord, final long rightWord, final byte offset, final byte range) {
		return leftWord ^ (~rightWord & (-1L >>> -range << offset));
	}


	/**
	 * Returns {@code true} if the given operands share at least a single {@code 1}-bit at the same location within the given
	 * range, {@code false} otherwise. Note that the anti-intersect can easily be derived by passing flipped bits.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return whether or not the two operands intersect each other within the given range
	 */
	static public boolean intersect (final long leftBits, final long rightBits, final byte offset, final byte range) {
		return (leftBits & rightBits & (-1L >>> -range << offset)) != 0L;
	}


	/**
	 * Returns {@code true} if either (or both) of given operands has a {@code 1}-bit in every respective location within the
	 * given range, {@code false} otherwise. Note that the anti-complement can easily be derived by passing inverted words.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return whether or not the two operands complement each other within the given range
	 */
	static public boolean complement (final long leftBits, final long rightBits, final byte offset, final byte range) {
		return (leftBits | rightBits | ~(-1L >>> -range << offset)) == -1L;
	}


	//***************************************************************//
	// Operations targeting bit-arrays consisting of multiple words. //
	//***************************************************************//

	/**
	 * Returns {@code true} if the bit at the specified index is {@code 1}, otherwise {@code false} for {@code 0}.
	 * @param bits the bits
	 * @param index the index
	 * @return whether or not the specified bit is set
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 */
	static public boolean get (final long[] bits, final long index) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (index < 0 | index >= MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		return ((bits[(int) (index >> LOG2_WORD_SIZE)] >> index) & 1) == 1;
	}


	/**
	 * Sets the bit at the specified index to the given value. The value being set is {@code 1} for {@code true}, otherwise
	 * {@code 0} for {@code false}.
	 * @param bits the bits
	 * @param index the index
	 * @param value the value
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 */
	static public void set (final long[] bits, final long index, final boolean value) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (index < 0 | index >= MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final int wordIndex = (int) (index >> LOG2_WORD_SIZE);
		final long wordMask = 1L << index;
		if (value) bits[wordIndex] |= wordMask;
		else bits[wordIndex] &= ~wordMask;
	}


	/**
	 * Sets the bit at the given index to {@code 0}.
	 * @param bits the bits
	 * @param index the index
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 */
	static public void off (final long[] bits, final long index) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (index < 0 | index >= MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		bits[(int) (index >> LOG2_WORD_SIZE)] &= ~(1L << index);
	}


	/**
	 * Sets the bits within the given range to {@code 0}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void off (final long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		xor(bits, bits, offset, range);
	}


	/**
	 * Sets the bit at the given index to {@code 1}.
	 * @param bits the bits
	 * @param index the index
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 */
	static public void on (final long[] bits, final long index) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (index < 0 | index >= MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		bits[(int) (index >> LOG2_WORD_SIZE)] |= 1L << index;
	}


	/**
	 * Sets the bits within the given range to {@code 1}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void on (final long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		xand(bits, bits, offset, range);
	}


	/**
	 * Sets the bit at the given index to it's opposite value.
	 * @param bits the bits
	 * @param index the index
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 */
	static public void flip (final long[] bits, final long index) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (index < 0 | index >= MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		bits[(int) (index >> LOG2_WORD_SIZE)] ^= 1L << index;
	}


	/**
	 * Sets the bits within the given range to their respective opposite values.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void flip (final long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			bits[firstIndex] ^= (-1L >>> -range << offset);
		} else {
			bits[firstIndex] ^= (-1L << offset);
			for (int index = firstIndex + 1; index < lastIndex; ++index) {
				bits[index] ^= -1L;
			}
			bits[lastIndex] ^= (-1L >>> -stop);
		}
	}


	/**
	 * Rotates the given bits to the right by the given offset, i.e. towards higher indices.
	 * @param bits the bits
	 * @param offset the rotational shift
	 * @throws NullPointerException if the given bits are {@code null}
	 */
	static public void rotate (final long[] bits, long offset) throws NullPointerException {
		rotateWords(bits, (int) (offset >> LOG2_WORD_SIZE));
	}


	/**
	 * Returns the number of the given {@code 1}-bits within the given range, i.e. the population count. Note that the number of
	 * {@code 0}-bits can easily be derived by calculating {@code (words.length * 64L) - cardinality}.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the cardinality within the given range
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public long cardinality (final long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			return BitArrays.cardinality(bits[firstIndex], (byte) offset, (byte) range);
		}

		long count = BitArrays.cardinality(bits[firstIndex], (byte) offset, (byte) 0);
		for (int index = firstIndex + 1; index < lastIndex; ++index) {
			count += BitArrays.cardinality(bits[index], (byte) 0, (byte) 0);
		}
		return count + BitArrays.cardinality(bits[lastIndex], (byte) 0, (byte) stop);
	}


	/**
	 * Returns the index of the first (lowest) {@code 1}-bit within the given range, or {@code -1} if there is none.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the index, or {@code -1} for none
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public long firstIndex (long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		byte result;
		if (firstIndex == lastIndex) {
			result = BitArrays.firstIndex(bits[firstIndex], (byte) offset, (byte) range);
			return result == -1 ? -1 : (firstIndex << LOG2_WORD_SIZE) + result;
		}

		result = BitArrays.firstIndex(bits[firstIndex], (byte) offset, (byte) 0);
		if (result != -1) return (firstIndex << LOG2_WORD_SIZE) + result;

		for (int index = firstIndex + 1; index < lastIndex; ++index) {
			result = BitArrays.firstIndex(bits[index], (byte) 0, (byte) 0);
			if (result != -1) return ((long) index << LOG2_WORD_SIZE) + result;
		}

		result = BitArrays.firstIndex(bits[lastIndex], (byte) 0, (byte) stop);
		return result == -1 ? -1 : (lastIndex << LOG2_WORD_SIZE) + result;
	}


	/**
	 * Returns the index of the last (highest) {@code 1}-bit within the given range, or {@code -1} if there is none.
	 * @param bits the bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return the index, or {@code -1} for none
	 * @throws NullPointerException if the given bits are {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public long lastIndex (long[] bits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		byte result;
		if (firstIndex == lastIndex) {
			result = BitArrays.lastIndex(bits[lastIndex], (byte) offset, (byte) range);
			return result == -1 ? -1 : (lastIndex << LOG2_WORD_SIZE) + result;
		}

		result = BitArrays.lastIndex(bits[(int) lastIndex], (byte) offset, (byte) 0);
		if (result != -1) return (lastIndex << LOG2_WORD_SIZE) + result;

		for (int index = lastIndex - 1; index > firstIndex; --index) {
			result = BitArrays.lastIndex(bits[index], (byte) 0, (byte) 0);
			if (result != -1) return (index << LOG2_WORD_SIZE) + result;
		}

		result = BitArrays.lastIndex(bits[firstIndex], (byte) 0, (byte) stop);
		return result == -1 ? -1 : (firstIndex << LOG2_WORD_SIZE) + result;
	}


	/**
	 * Returns the bit-wise disjunction ({@code OR}) of the given operands, after masking out any right operand bits outside the
	 * given range.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void or (final long[] leftBits, final long[] rightBits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			leftBits[firstIndex] = BitArrays.or(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		} else {
			leftBits[firstIndex] = BitArrays.or(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0);
			for (int index = firstIndex + 1; index < lastIndex; ++index) {
				leftBits[index] = BitArrays.or(leftBits[index], rightBits[index], (byte) offset, (byte) 0);
			}
			leftBits[lastIndex] = BitArrays.or(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
		}
	}


	/**
	 * Returns the bit-wise conjunction ({@code AND}) of the given operands, after masking out any right operand bits outside
	 * the given range.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void and (final long[] leftBits, final long[] rightBits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			leftBits[firstIndex] = BitArrays.and(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		} else {
			leftBits[firstIndex] = BitArrays.and(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0);
			for (int index = firstIndex + 1; index < lastIndex; ++index) {
				leftBits[index] = BitArrays.and(leftBits[index], rightBits[index], (byte) offset, (byte) 0);
			}
			leftBits[lastIndex] = BitArrays.and(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
		}
	}


	/**
	 * Returns the bit-wise exclusive disjunction ({@code XOR}) of the given operands, after masking out any right operand bits
	 * outside the given range.
	 * @param leftBits the left operands words
	 * @param rightBits the right operands words
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void xor (final long[] leftBits, final long[] rightBits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			leftBits[firstIndex] = BitArrays.xor(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		} else {
			leftBits[firstIndex] = BitArrays.xor(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0);
			for (int index = firstIndex + 1; index < lastIndex; ++index) {
				leftBits[index] = BitArrays.xor(leftBits[index], rightBits[index], (byte) offset, (byte) 0);
			}
			leftBits[lastIndex] = BitArrays.xor(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
		}
	}


	/**
	 * Returns the bit-wise exclusive conjunction ({@code XAND}) of the given operands, after masking out any right operand bits
	 * outside the given range.
	 * @param leftBits the left operands bits
	 * @param rightBits the right operands bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public void xand (final long[] leftBits, final long[] rightBits, final long offset, final long range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			leftBits[firstIndex] = BitArrays.xand(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		} else {
			leftBits[firstIndex] = BitArrays.xand(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0);
			for (int index = firstIndex + 1; index < lastIndex; ++index) {
				leftBits[index] = BitArrays.xand(leftBits[index], rightBits[index], (byte) offset, (byte) 0);
			}
			leftBits[lastIndex] = BitArrays.xand(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
		}
	}


	/**
	 * Returns {@code true} if the given operands share at least a single {@code 1}-bit at the same location within the given
	 * range, {@code false} otherwise.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return whether or not the two operands intersect within the given range
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public boolean intersect (final long[] leftBits, final long[] rightBits, final byte offset, final byte range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			return BitArrays.intersect(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		}

		if (BitArrays.intersect(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0)) return true;
		for (int index = firstIndex + 1; index < lastIndex; ++index) {
			if (BitArrays.intersect(leftBits[index], rightBits[index], (byte) offset, (byte) 0)) return true;
		}
		return BitArrays.intersect(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
	}


	/**
	 * Returns {@code true} if either (or both) of given operands has a {@code 1}-bit in every respective location within the
	 * given range, {@code false} otherwise.
	 * @param leftBits the left operand bits
	 * @param rightBits the right operand bits
	 * @param offset the offset of the first bit to process
	 * @param range the number of bits to process
	 * @return whether or not the two operands complement each other within the given range
	 * @throws NullPointerException if any of the given operands is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset or range is out of bounds
	 */
	static public boolean complement (final long[] leftBits, final long[] rightBits, final byte offset, final byte range) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (offset < 0 | offset >= MAX_BIT_COUNT | range <= 0 | range > MAX_BIT_COUNT) throw new ArrayIndexOutOfBoundsException();
		final long stop = offset + range;
		final int firstIndex = (int) (offset >> LOG2_WORD_SIZE);
		final int lastIndex = (int) ((stop - 1) >> LOG2_WORD_SIZE);

		if (firstIndex == lastIndex) {
			return BitArrays.complement(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) range);
		}

		if (!BitArrays.complement(leftBits[firstIndex], rightBits[firstIndex], (byte) offset, (byte) 0)) return false;
		for (int index = firstIndex + 1; index < lastIndex; ++index) {
			if (!BitArrays.complement(leftBits[index], rightBits[index], (byte) offset, (byte) 0)) return false;
		}
		return BitArrays.complement(leftBits[lastIndex], rightBits[lastIndex], (byte) 0, (byte) stop);
	}


	/**
	 * Rotates the words within the given array to the right (i.e. towards higher indices) by the given offset.
	 * @param words the words
	 * @param offset the rotational offset
	 * @throws NullPointerException if the given words are {@code null}
	 */
	static private void rotateWords (final long[] words, int offset) throws NullPointerException {
		offset %= words.length;
		if (offset < 0) offset += words.length;
		if (offset == 0) return;

		for (int baseIndex = 0, count = 0; count < words.length; ++count, ++baseIndex) {
			final long store = words[baseIndex];

			int index = baseIndex;
			for (int nextIndex = baseIndex + offset; nextIndex != baseIndex; ++count) {
				words[index] = words[nextIndex];
				index = nextIndex;
				if ((nextIndex += offset) >= words.length) nextIndex -= words.length;
			}

			words[index] = store;
		}
	}


	//*********************************************************************************//
	// Operations targeting bit-arrays consisting of either single and multiple words. // 
	//*********************************************************************************//

	/**
	 * Returns the given bits as a boolean array containing {@code n*64} elements.
	 * @param bits the bits
	 * @return the corresponding boolean array
	 * @throws NullPointerException if any of the given bits are {@code null}
	 * @throws IllegalArgumentException if the total number of bits exceeds the maximum array size
	 */
	static public boolean[] toBooleans (final long... bits) throws NullPointerException, IllegalArgumentException {
		if (((long) bits.length << LOG2_WORD_SIZE) > Integer.MAX_VALUE) throw new IllegalArgumentException();

		final boolean[] result = new boolean[bits.length << LOG2_WORD_SIZE];
		for (int index = 0; index < result.length; ++index) {
			result[index] = get(bits, index);
		}
		return result;
	}


	/**
	 * Returns the given bits as a text consisting of {@code n*64} binary digits.
	 * @param bits the bits
	 * @return the corresponding text
	 * @throws NullPointerException if any of the given bits are {@code null}
	 */
	static public String toString (final long... bits) throws NullPointerException {
		final StringWriter writer = new StringWriter();
		writer.write('[');
		for (int stop = bits.length << LOG2_WORD_SIZE, index = 0; index < stop; ++index) {
			if (index > 0) writer.write(", ");
			writer.write(get(bits, index) ? '1' : '0');
		}
		writer.write(']');

		return writer.toString();
	}
}