package de.htw.tool;

import static java.lang.Long.SIZE;
import static java.lang.Long.numberOfLeadingZeros;
import static java.lang.Long.numberOfTrailingZeros;
import static java.lang.Long.reverse;


/**
 * This facade adds additional mathematical operations for {@code 64-bit} integer arithmetics.
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class LongMath {

	/**
	 * An empty (and therefore both immutable and cacheable) vector.
	 */
	static public final long[] EMPTY = new long[0];

	/**
	 * The factorials contained withing range <tt>[0, 2<sup>63</sup>-1]</tt>.
	 */
	static private final long[] FACTORIALS = { 1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l, 3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l, 1307674368000l, 20922789888000l, 355687428096000l, 6402373705728000l, 121645100408832000l, 2432902008176640000l
	};


	/**
	 * Prevents external instantiation.
	 */
	private LongMath () {}


	//*******************//
	// scalar operations //
	//*******************//

	/**
	 * Returns the absolute value of the given operand {@code x}, for all values within range
	 * <tt>]-2<sup>63</sup>, +2<sup>63</sup>[</tt>. This operation computes the result in a branch-free fashion using three
	 * basic operations, in order to avoid expensive processor pipeline resets due to branch prediction failure:
	 * <ol>
	 * <li>use signed shift to calculate the value's sign, i.e. -1 if negative, else 0</li>
	 * <li>use XOR to calculate one's complement if negative</li>
	 * <li>use subtraction to calculate two's complement if negative</li>
	 * </ol>
	 * Note that the result will be incorrect for {@link Long.MIN_VALUE} due to overflow!
	 * @param x the operand value
	 * @return the value <tt>|x| = x * signum(x)</tt>
	 * @see Math#abs(long)
	 * @see #signum(long)
	 */
	static public long abs (final long x) {
		final long sign = x >> (SIZE - 1);
		return (x ^ sign) - sign;
	}


	/**
	 * Returns the signum of the given operand {@code x}, for all values within range {@code ]-2^63, +2^63[}. This operation
	 * computes the result in a branch-free fashion using four basic operations, in order to avoid expensive processor pipeline
	 * resets due to branch prediction failure:
	 * <ol>
	 * <li>use subtraction to calculate the value's two's complement</li>
	 * <li>use signed shifts to calculate sign expansion for the value and it's two-complement</li>
	 * <li>use subtraction to calculate the difference between both sign expansions</li>
	 * </ol>
	 * Note that the result will be incorrect for {@link Long.MIN_VALUE} due to overflow!
	 * @param x the operand value
	 * @return the value <tt>signum(x) = x / |x|</tt>
	 * @see Math#signum(double)
	 * @see #abs(long)
	 */
	static public long signum (final long x) {
		return (x >> (SIZE - 1)) - (-x >> (SIZE - 1));
	}


	/**
	 * Returns the square of the given operand {@code x}.
	 * @param x the operand value
	 * @return the value <tt>x<sup>2</sup></tt>
	 */
	static public long sq (final long x) {
		return x * x;
	}


	/**
	 * Returns the cube of the given operand {@code x}.
	 * @param x the operand value
	 * @return the value <tt>x<sup>3</sup></tt>
	 */
	static public long cb (final long x) {
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
	static public long mod (final long x, final long y) throws ArithmeticException {
		final long mod = x % y;
		return mod >= 0 ? mod : Math.abs(y) + mod;
	}


	/**
	 * Returns the largest (closest to positive infinity) value that is less than or equal to the {@code binary logarithm} of
	 * the given operand and is equal to a mathematical integer.
	 * @param x the operand value
	 * @return the value <tt>floor(log<sub>2</sub>(x))</tt>, with {@code -1} representing {@code negative infinity}
	 * @throws ArithmeticException if the given operand is strictly negative
	 */
	static public long floorLog2 (final long x) throws ArithmeticException {
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
	static public long ceilLog2 (final long x) throws ArithmeticException {
		if (x < 0) throw new ArithmeticException();
		final int result = SIZE - numberOfLeadingZeros(x - 1);
		return result < SIZE ? result : -1;
	}


	/**
	 * Returns two raised to the power of the given exponent <tt>x mod<sub>e</sub> 64</tt>. Note that exponents 62 and 63 cause
	 * overflows, returning {@link Long#MIN_VALUE} and zero respectively.
	 * @param x the exponent
	 * @return the value <tt>2<sup>x mod<sub>e</sub> 64</sup></tt>
	 */
	static public long exp2 (final long x) {
		return 1L << x;
	}


	/**
	 * Returns the given factor multiplied with two raised to the power of the given exponent <tt>y mod<sub>e</sub> 64</tt>.
	 * Note that exponents 62 and 63 are guaranteed to cause intermediate overflows, except for zero factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x * 2<sup>y mod<sub>e</sub> 64</sup></tt>
	 */
	static public long mulExp2 (final long x, final long y) {
		return x << y;
	}


	/**
	 * Returns the given factor divided by two raised to the power of the given exponent <tt>y mod<sub>e</sub> 64</tt>. Note
	 * that exponents 62 and 63 are guaranteed to cause intermediate underflows, except for zero factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x / 2<sup>y mod<sub>e</sub> 64</sup></tt>
	 */
	static public long divExp2 (final long x, final long y) {
		return x >> y;
	}


	/**
	 * Returns the given factor modulo two raised to the power of the given exponent <tt>y mod<sub>e</sub> 64</tt>. Note that
	 * the {@code Euclidean
	 * modulo} is calculated, and that exponents 62 and 63 are guaranteed to cause intermediate underflows, except for zero
	 * factors.
	 * @param x the factor
	 * @param y the exponent
	 * @return the value <tt>x mod<sub>e</sub> 2<sup>y mod<sub>e</sub> 64</sup></tt>
	 */
	static public long modExp2 (final long x, final long y) {
		return x & ((1L << y) - 1);
	}


	/**
	 * Returns the {@code greatest common divisor} (GCD) of the two given operands, and returns it. Note that this operation
	 * implements the fast binary GCD algorithm (Josef Stein, 1967).
	 * @param x the left operand
	 * @param y the right operand
	 * @return the greatest common divisor of both operands
	 */
	static public long gcd (long x, long y) {
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
	static public long lcm (final long x, final long y) {
		final long gcd = gcd(x, y);
		return gcd == 0 ? 0 : Math.abs(x / gcd * y);
	}


	/**
	 * Returns the swap index associated with the given shuffle index {@code x} for a collection of
	 * <tt>2<sup>magnitude mod<sub>e</sub> 64</sup></tt> elements. The resulting index pair can be used to swap the elements in
	 * a matching collection to achieve {@code perfect shuffle} order. Note that the result is undefined if {@code x} is outside
	 * it's range.
	 * @param x the shuffle index within range <tt>[0,2<sup>magnitude mod<sub>e</sub> 64</sup>[</tt>
	 * @param magnitude the magnitude
	 * @return the swap index
	 */
	static public long perfectShuffle (final long x, final long magnitude) {
		return reverse(x) >>> -magnitude;
	}


	/**
	 * Returns the factorial of x.
	 * @param x the operand {@code x}
	 * @return the value {@code x!}
	 * @throws IllegalArgumentException if the given value is outside range <tt>[0,20]</tt>
	 */
	static public long fac (final long x) {
		if (x < 0 | x > FACTORIALS.length) throw new IllegalArgumentException();
		return FACTORIALS[(int) x];
	}


	//*******************//
	// vector operations //
	//*******************//

	/**
	 * Returns the {@code smallest} coordinate of the given vector, i.e. the one with a value closest to {@link Long#MIN_VALUE}.
	 * If the vector has zero length, the result is zero.
	 * @param vector the operand vector
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public long min (final long... vector) throws NullPointerException {
		if (vector.length == 0) return 0;

		long result = vector[0];
		for (int index = 1; index < vector.length; ++index)
			result = Math.min(result, vector[index]);
		return result;
	}


	/**
	 * Returns the {@code largest} coordinate of the given vector, i.e. the one with a value closest to {@link Long#MAX_VALUE}.
	 * If the vector has zero length, the result is zero.
	 * @param vector the operand vector
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public long max (final long... vector) throws NullPointerException {
		if (vector.length == 0) return 0;

		long result = vector[0];
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
	static public long[][] wrap (final long... vector) throws NullPointerException {
		final long[][] result = new long[vector.length][1];
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
	static public long[] unwrap (final long[]... matrix) throws NullPointerException, IllegalArgumentException {
		final long[] result = new long[matrix.length];
		for (int index = 0; index < matrix.length; ++index) {
			final long[] element = matrix[index];
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
	static public boolean unique (final long... vector) throws NullPointerException {
		for (int left = 0; left < vector.length; ++left)
			for (int right = left + 1; right < vector.length; ++right)
				if (vector[left] == vector[right]) return false;
		return true;
	}
}