package de.htw.ds.sync;

import java.util.Arrays;
import de.htw.tool.Copyright;


/**
 * Demonstrator for single threading vector arithmetics based on double arrays. Note that of all
 * available processor cores within a system, this implementation is only capable of using one! Also
 * note that this class is declared final because it provides an application entry point, and
 * therefore not supposed to be extended.
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class VectorMathSingleThreaded {
	static private final int DEFAULT_SIZE = 100;
	static private final int WARMUP_LOOPS = 25000;

	/**
	 * Sums two vectors within a single thread.
	 * @param left the first operand
	 * @param right the second operand
	 * @return the resulting vector
	 * @throws NullPointerException if one of the given parameters is {@code null}
	 * @throws IllegalArgumentException if the given parameters do not share the same length
	 */
	static public double[] add (final double[] left, final double[] right) {
		if (left.length != right.length) throw new IllegalArgumentException();
		final double[] result = new double[left.length];
		for (int x = 0; x < left.length; ++x) {
			result[x] = left[x] + right[x];
		}
		return result;
	}


	/**
	 * Multiplexes two vectors within a single thread.
	 * @param left the first operand
	 * @param right the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is {@code null}
	 */
	static public double[][] mux (final double[] left, final double[] right) {
		final double[][] result = new double[left.length][right.length];
		for (int x = 0; x < left.length; ++x) {
			for (int rightIndex = 0; rightIndex < right.length; ++rightIndex) {
				result[x][rightIndex] = left[x] * right[rightIndex];
			}
		}
		return result;
	}


	/**
	 * Runs both vector summation and vector multiplexing for demo purposes.
	 * @param args the argument array
	 */
	static public void main (final String[] args) {
		final int size = args.length == 0 ? DEFAULT_SIZE : Integer.parseInt(args[0]);
		System.out.format("Computation is performed using a single thread for operand size %d.\n", size);
	
		// initialize operand vectors
		final double[] a = new double[size], b = new double[size];
		for (int index = 0; index < size; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}

		// Warm-up phase to force hot-spot translation of byte-code into machine code, code-optimization, etc!
		// Computation of resultHash prevents JIT from over-optimizing the warmup-phase (by complete removal),
		// which would happen if the loop does not compute something that is used outside of it.
		int resultHash = 0;
		for (int loop = 0; loop < WARMUP_LOOPS; ++loop) {
			double[] c = add(a, b);
			resultHash ^= c.hashCode();

			double[][] d = mux(a, b);
			resultHash ^= d.hashCode();
		}
		System.out.format("warm-up phase ended with result hash %d.\n", resultHash);
	
		final long timestamp0 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[] sum = add(a, b);
			resultHash ^= sum.hashCode();
		}

		final long timestamp1 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[][] mux = mux(a, b);
			resultHash ^= mux.hashCode();
		}
		final long timestamp2 = System.currentTimeMillis();
		System.out.format("timing phase ended with result hash %d.\n", resultHash);
		System.out.format("a + b computed in %.4fms.\n", (timestamp1 - timestamp0) * 0.0001);
		System.out.format("a x b computed in %.4fms.\n", (timestamp2 - timestamp1) * 0.0001);

		if (size <= 100) {
			final double[] sum = add(a, b);
			final double[][] mux = mux(a, b);
			System.out.print("a = ");
			System.out.println(Arrays.toString(a));
			System.out.print("b = ");
			System.out.println(Arrays.toString(b));
			System.out.print("a + b = ");
			System.out.println(Arrays.toString(sum));
			System.out.print("a x b = [");
			for (int index = 0; index < mux.length; ++index) {
				System.out.print(Arrays.toString(mux[index]));
			}
			System.out.println("]");
		}
	}
}