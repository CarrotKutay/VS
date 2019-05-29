package de.htw.tool;

import static java.lang.Integer.SIZE;
import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Integer.numberOfTrailingZeros;
import static java.lang.Integer.reverse;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * This facade adds additional mathematical operations for {@code 32-bit} integer arithmetics.
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class IntMath {

	/**
	 * An empty (and therefore both immutable and cacheable) vector.
	 */
	static public final int[] EMPTY = new int[0];


	/**
	 * Prevents external instantiation.
	 */
	private IntMath () {}


	//*******************//
	// scalar operations //
	//*******************//

	/**
	 * Returns the absolute value of the given operand {@code x}, for all values within range
	 * <tt>]-2<sup>31</sup>, +2<sup>31</sup>[</tt>. This operation computes the result in a branch-free fashion using three
	 * basic operations, in order to avoid expensive processor pipeline resets due to branch prediction failure:
	 * <ol>
	 * <li>use signed shift to calculate the value's sign, i.e. -1 if negative, else 0</li>
	 * <li>use XOR to calculate one's complement if negative</li>
	 * <li>use subtraction to calculate two's complement if negative</li>
	 * </ol>
	 * Note that the result will be incorrect for {@link Integer.MIN_VALUE} due to overflow!
	 * @param x the operand value
	 * @return the value <tt>|x| = x * signum(x)</tt>
	 * @see Math#abs(int)
	 * @see #signum(int)
	 */
	static public int abs (final int x) {
		final int sign = x >> (SIZE - 1);
		return (x ^ sign) - sign;
	}


	/**
	 * Returns the signum of the given operand {@code x}, for all values within range {@code ]-2^31, +2^31[}. This operation
	 * computes the result in a branch-free fashion using four basic operations, in order to avoid expensive processor pipeline
	 * resets due to branch prediction failure:
	 * <ol>
	 * <li>use subtraction to calculate the value's two's complement</li>
	 * <li>use signed shifts to calculate sign expansion for the value and it's two-complement</li>
	 * <li>use subtraction to calculate the difference between both sign expansions</li>
	 * </ol>
	 * Note that the result will be incorrect for {@link Integer.MIN_VALUE} due to overflow!
	 * @param x the operand value
	 * @return the value <tt>signum(x) = x / |x|</tt>
	 * @see Math#signum(float)
	 * @see #abs(int)
	 */
	static public int signum (final int x) {
		return (x >> (SIZE - 1)) - (-x >> (SIZE - 1));
	}


	/**
	 * Returns the square of the given operand {@code x}.
	 * @param x the operand value
	 * @return the value <tt>x<sup>2</sup></tt>
	 */
	static public int sq (final int x) {
		return x * x;
	}


	/**
	 * Returns the cube of the given operand {@code x}.
	 * @param x the operand value
	 * @return the value <tt>x<sup>3</sup></tt>
	 */
	static public int cb (final int x) {
		return x * x * x;
	}


	/**
	 * Returns the {@code Euclidean modulo} of the given dividend and divisor, based on the {@code truncated modulo} returned by
	 * the {@code %} operator.
	 * @param x the dividend value
	 * @param y the divisor value
	 * @return the value <tt>x mod<sub>e</sub> y</tt> within range {@code [0,|y|[}
	 * @throws ArithmeticException if the given divisor is zero
	 */
	static public int mod (final int x, final int y) throws ArithmeticException {
		return x >= 0 ? x % y : Math.abs(y) + x % y;
	}


	/**
	 * Returns the largest (closest to positive infinity) value that is less than or equal to the {@code binary logarithm} of
	 * the given operand and is equal to a mathematical integer.
	 * @param x the operand value
	 * @return the value <tt>floor(log<sub>2</sub>(x))</tt>, with {@code -1} representing {@code negative infinity}
	 * @throws ArithmeticException if the given operand is strictly negative
	 */
	static public int floorLog2 (final int x) throws ArithmeticException {
		if (x < 0) throw new ArithmeticException();
		return (SIZE - 1) - numberOfLeadingZeros(x);
	}


	/**
	 * Returns the smallest (closest to negative infinity) value that is greater than or equal to the {@code binary logarithm}
	 * of the given operand and is equal to a mathematical integer.
	 * @param operand the operand
	 * @return the value <tt>ceil(log<sub>2</sub>(x))</tt>, with {@code -1} representing {@code negative infinity}
	 * @throws ArithmeticException if the given operand is strictly negative
	 */
	static public int ceilLog2 (final int x) throws ArithmeticException {
		if (x < 0) throw new ArithmeticException();
		final int result = SIZE - numberOfLeadingZeros(x - 1);
		return result < SIZE ? result : -1;
	}


	/**
	 * Returns two raised to the power of the given exponent <tt>x mod<sub>e</sub> 32</tt>. Note that exponents 30 and 31 cause
	 * overflows, returning {@link Integer#MIN_VALUE} and zero respectively.
	 * @param x the exponent
	 * @return the value <tt>2<sup>x mod<sub>e</sub> 32</sup></tt>
	 */
	static public int exp2 (final int x) {
		return 1 << x;
	}


	/**
	 * Returns the given factor multiplied with two raised to the power of the given exponent <tt>y mod<sub>e</sub> 32</tt>.
	 * Note that exponents 30 and 31 are guaranteed to cause intermediate overflows, except for zero factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x * 2<sup>y mod<sub>e</sub> 32</sup></tt>
	 */
	static public long mulExp2 (final int x, final int y) {
		return x << y;
	}


	/**
	 * Returns the given factor divided by two raised to the power of the given exponent <tt>y mod<sub>e</sub> 32</tt>. Note
	 * that exponents 30 and 31 are guaranteed to cause intermediate underflows, except for zero factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x / 2<sup>y mod<sub>e</sub> 32</sup></tt>
	 */
	static public int divExp2 (final int x, final int y) {
		return x >> y;
	}


	/**
	 * Returns the given factor modulo two raised to the power of the given exponent <tt>y mod<sub>e</sub> 32</tt>. Note that
	 * the {@code Euclidean modulo} is calculated, and that exponents 30 and 31 are guaranteed to cause intermediate underflows,
	 * except for zero factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x mod<sub>e</sub> 2<sup>y mod<sub>e</sub> 32</sup></tt>
	 */
	static public int modExp2 (final int x, final int y) {
		return x & ((1 << y) - 1);
	}


	/**
	 * Returns the {@code greatest common divisor} (GCD) of the two given operands, and returns it. Note that this operation
	 * implements the fast binary GCD algorithm (Josef Stein, 1967).
	 * @param x the left operand
	 * @param y the right operand
	 * @return the greatest common divisor of both operands
	 */
	static public int gcd (int x, int y) {
		if (x < 0) x = -x;
		if (y < 0) y = -y;
		if (x == 0) return y;
		if (y == 0) return x;

		final int shift = Math.min(numberOfTrailingZeros(x), numberOfTrailingZeros(y));
		x >>= shift;
		y >>= shift;

		for (x >>= numberOfTrailingZeros(x); y != 0; y -= x) {
			y >>= numberOfTrailingZeros(y);
			if (x > y) {
				x ^= y;
				y ^= x;
				x ^= y;
			}
		}

		return x << shift;
	}


	/**
	 * Calculates the {@code least common multiple} (LCM) of the two given operands, and returns it. Note that this operation
	 * divides by the GCD before multiplying, therefore avoiding intermediate over- or underflow.
	 * @param x the left operand
	 * @param y the right operand
	 * @return the least common multiple of both values
	 */
	static public int lcm (final int x, final int y) {
		final int gcd = gcd(x, y);
		return gcd == 0 ? 0 : Math.abs(x / gcd * y);
	}


	/**
	 * Returns the binominal coefficient <i>(n choose k)</i>, which is the number of unique subsets of size k elements,
	 * disregarding their order, from a set of {@code n} elements.
	 * @param n the set size
	 * @param k the subset size
	 * @return the value <tt>(n choose k) = n!/(k!&middot;(n-k)!)</tt>
	 * @throws IllegalArgumentException if the given subset size is outside range <tt>[0,n]</tt>
	 */
	static public long binomial (final int n, int k) throws IllegalArgumentException {
		if (k < 0 | k > n) throw new IllegalArgumentException();
		if (k > n - k) k = n - k;

		long result = 1;
		for (int left = 1, right = n; left <= k; ++left, --right)
			result = result * right / left;
		return result;
	}


	/**
	 * Returns the swap index associated with the given shuffle index {@code x} for a collection of
	 * <tt>2<sup>magnitude mod<sub>e</sub> 32</sup></tt> elements. The resulting index pair can be used to swap the elements in
	 * a matching collection to achieve {@code perfect shuffle} order. Note that the result is undefined if {@code x} is outside
	 * it's range.
	 * @param x the shuffle index within range <tt>[0,2<sup>magnitude mod<sub>e</sub> 32</sup>[</tt>
	 * @param magnitude the magnitude
	 * @return the swap index
	 */
	static public int perfectShuffle (final int x, final int magnitude) {
		return reverse(x) >>> -magnitude;
	}


	//*******************//
	// vector operations //
	//*******************//

	/**
	 * Returns the {@code smallest} coordinate of the given vector, i.e. the one with a value closest to
	 * {@link Integer#MIN_VALUE}. If the vector has zero length, the result is zero.
	 * @param vector the operand vector
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public int min (final int... vector) throws NullPointerException {
		if (vector.length == 0) return 0;

		int result = vector[0];
		for (int index = 1; index < vector.length; ++index)
			result = Math.min(result, vector[index]);
		return result;
	}


	/**
	 * Returns the {@code largest} coordinate of the given vector, i.e. the one with a value closest to
	 * {@link Integer#MAX_VALUE}. If the vector has zero length, the result is zero.
	 * @param vector the operand vector
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public int max (final int... vector) throws NullPointerException {
		if (vector.length == 0) return 0;

		int result = vector[0];
		for (int index = 1; index < vector.length; ++index)
			result = Math.max(result, vector[index]);
		return result;
	}


	/**
	 * Wraps each coordinate of the given vector into it's own single-coordinate vector, and returns the resulting matrix.
	 * @param vector the operand vector
	 * @return the matrix created
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public int[][] wrap (final int... vector) throws NullPointerException {
		final int[][] result = new int[vector.length][1];
		for (int index = 0; index < vector.length; ++index)
			result[index][0] = vector[index];
		return result;
	}


	/**
	 * Unwraps each single-coordinate element of the given matrix, and returns the resulting vector.
	 * @param matrix the operand matrix
	 * @return the vector created
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if any of the given matrix elements has a non-unary length
	 */
	static public int[] unwrap (final int[]... matrix) throws NullPointerException, IllegalArgumentException {
		final int[] result = new int[matrix.length];
		for (int index = 0; index < matrix.length; ++index) {
			final int[] element = matrix[index];
			if (element.length != 1) throw new IllegalArgumentException();
			result[index] = element[0];
		}
		return result;
	}


	/**
	 * Returns {@code true} if the given vector consists solely of unique coordinates, {@code false} otherwise. Note that both
	 * empty and single-coordinate vectors are intrinsically unique.
	 * @param vector the vector
	 * @return whether or not the given vector consists solely of unique coordinates
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public boolean unique (final int... vector) throws NullPointerException {
		for (int left = 0; left < vector.length; ++left)
			for (int right = left + 1; right < vector.length; ++right)
				if (vector[left] == vector[right]) return false;
		return true;
	}


	/**
	 * Returns the <i>(n choose k)</i> unique subsets of size k elements, disregarding their order, from a set of {@code n}
	 * elements within integer range <tt>[0,n[</tt>.
	 * @param n the set size, implying an element range of <tt>[0,n[</tt>
	 * @param k the subset size, implying <tt>n!/(k!&middot;(n-k)!)</tt> possible combinations
	 * @return a stream of <tt>n!/(k!&middot;(n-k)!)</tt> unique k-size combinations of integers withing range <tt>[0,n[</tt>
	 * @throws IllegalArgumentException if the given subset size is outside range <tt>[0,n]</tt>
	 */
	static public Stream<int[]> choose (final int n, final int k) throws IllegalArgumentException {
		if (k < 0 | k > n) throw new IllegalArgumentException();
		if (k == 0) return Stream.of(EMPTY);

		final int[] state = new int[k];
		for (int index = 0; index < state.length; ++index)
			state[index] = index;
		state[state.length - 1] -= 1;// decrement last element to allow initial increment!

		final Iterator<int[]> iterator = new Iterator<int[]>() {
			private final int delta = n - state.length;
			private int current = state.length - 1;


			// set current element to the rightmost increasable element
			public boolean hasNext () {
				for (int index = state.length - 1; index >= 0; --index) {
					if (state[index] != index + this.delta) {
						this.current = index;
						return true;
					}
				}
				this.current = -1;
				return false;
			}


			// alter current element and any to the right of it
			public int[] next () throws NoSuchElementException {
				if (this.current == -1 && !this.hasNext()) throw new NoSuchElementException();

				state[this.current] += 1;
				for (int index = this.current + 1; index < state.length; ++index) {
					state[index] = state[index - 1] + 1;
				}

				this.current = -1;
				return state.clone();
			}
		};

		final Iterable<int[]> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), false);
	}
}