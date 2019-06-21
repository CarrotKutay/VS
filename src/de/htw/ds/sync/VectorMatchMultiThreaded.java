package de.htw.ds.sync;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import de.htw.tool.Uninterruptibles;


public class VectorMatchMultiThreaded {
	static private final int DEFAULT_SIZE = 100;
	static private final int WARMUP_LOOPS = 25000;
	static private final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	static public final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(PROCESSOR_COUNT);



//	static private double[] doPartition (double[] x, int start, int end) {
//		double[] result = new double[end-start];
//		int index = 0;
//		for (int i = start; i < end; i++) {
//			result[index] = x[i]; 
//			index++;
//		}
//		return result;
//	}

	/**
	 * Sums two vectors within a single thread.
	 * 
	 * @param left  the first operand
	 * @param right the second operand
	 * @return the resulting vector
	 * @throws InterruptedException
	 * @throws NullPointerException     if one of the given parameters is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if the given parameters do not share the
	 *                                  same length
	 */	 
	 static public double[] add (final double[] left, final double[] right) {
		if (left.length != right.length) throw new IllegalArgumentException();
		final double[] result = new double[left.length];
		for (int x = 0; x < left.length; ++x) {
			result[x] = left[x] + right[x];
		}
		return result;
	}

	static public double[] addMulti(final double[] left, final double[] right) throws InterruptedException, ExampleCheckedException{
		if (left.length != right.length) throw new IllegalArgumentException();
		final double[] result = new double[left.length];


		//System.out.format("Main-Thread: Executing workers in new threads!\n");
		final Future<?>[] futures = new Future[PROCESSOR_COUNT];
		for (int threadIndex = 0; threadIndex < PROCESSOR_COUNT; ++threadIndex) {
			// futures[index] = threadPool.submit(worker);
			final int offset = threadIndex;
			final Callable<?> worker = () -> {
				for (int index = offset; index <result.length; index+=PROCESSOR_COUNT) {
					result[index] = left[index] + right[index];
				}
				return null;
			};
			
			futures[threadIndex] = THREAD_POOL.submit(worker);
			
		}

		//System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final Future<?> future : futures) {
				try {
					Uninterruptibles.get(future);
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					throw new AssertionError();
				}
			}
		} finally {
			for (final Future<?> future : futures) {
				future.cancel(true);
			}
			
		}

		//System.out.format("Main-Thread: All child threads are done!\n");
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


	static public double[][] muxMulti(final double[] left, final double[] right) throws InterruptedException, ExampleCheckedException{
		if (left.length != right.length) throw new IllegalArgumentException();
		double[][] result = new double[left.length][right.length];

		final Future<?>[] futures = new Future[PROCESSOR_COUNT];
		for (int threadIndex = 0; threadIndex < PROCESSOR_COUNT; ++threadIndex) {
			// futures[index] = threadPool.submit(worker);
			final int offset = threadIndex;
			final Callable<?> worker = () -> {
				for (int indexX = offset; indexX <result.length; indexX+=PROCESSOR_COUNT) {
					for (int indexY = 0; indexY < result[indexX].length; indexY++) {
						result[indexX][indexY] = left[indexX] * right[indexY];
					}
				}
				return null;
			};
			
			futures[threadIndex] = THREAD_POOL.submit(worker);			
		}

		//System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final Future<?> future : futures) {
				try {
					Uninterruptibles.get(future);
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					throw new AssertionError();
				}
			}
		} finally {
			for (final Future<?> future : futures) {
				future.cancel(true);
			}
			
		}
		
		return result;
	}
	

	
	/**
	 * Runs both vector summation and vector multiplexing for demo purposes.
	 * 
	 * @param args the argument array
	 * @throws ExampleCheckedException
	 * @throws InterruptedException
	 */
	static public void main(final String[] args) throws InterruptedException, ExampleCheckedException {
		final int size = args.length == 0 ? DEFAULT_SIZE : Integer.parseInt(args[0]);
		System.out.format("Computation is performed using a single thread for operand size %d.\n", size);

		// initialize operand vectors
		final double[] a = new double[size], b = new double[size];
		for (int index = 0; index < size; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}
		int resultHash = 0;
		for (int loop = 0; loop < WARMUP_LOOPS; ++loop) {
			double[] c = addMulti(a, b);
			resultHash ^= c.hashCode();

			double[][] d = muxMulti(a, b);
			resultHash ^= d.hashCode();
		}
		System.out.format("warm-up phase ended with result hash %d.\n", resultHash);
	
		final long timestamp0 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[] sum = addMulti(a, b);
			resultHash ^= sum.hashCode();
		}

		final long timestamp1 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[][] mux = muxMulti(a, b);
			resultHash ^= mux.hashCode();
		}
		final long timestamp2 = System.currentTimeMillis();
		System.out.format("timing phase ended with result hash %d.\n", resultHash);
		System.out.format("a + b computed in %.4fms.\n", (timestamp1 - timestamp0) * 0.0001);
		System.out.format("a x b computed in %.4fms.\n", (timestamp2 - timestamp1) * 0.0001);

		if (size <= 100) {
			final double[] sum = addMulti(a, b);
			final double[][] mux = muxMulti(a, b);
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
		
		THREAD_POOL.shutdown();
	}
}
