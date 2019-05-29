package de.htw.ds.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class VectorMathMultiThreaded {

	static private final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	static private final int DEFAULT_SIZE = 100;
	static private final int WARMUP_LOOPS = 25000;

	static double[] a = null;
	static double[] b = null;
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
	 * @throws ExampleCheckedException 
	 * @throws InterruptedException 
	 * @throws NegativeArraySizeException 
	 */
	static public void main (final String[] args) throws NegativeArraySizeException, InterruptedException, ExampleCheckedException {
		
		final int size = args.length == 0 ? DEFAULT_SIZE : Integer.parseInt(args[0]);
		System.out.format("Computation is performed using a single thread for operand size %d.\n", size);

		a = new double[size];
		b = new double[size];
		
		// initialize operand vectors
		for (int index = 0; index < size; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}

		// Warm-up phase to force hot-spot translation of byte-code into machine code, code-optimization, etc!
		// Computation of resultHash prevents JIT from over-optimizing the warmup-phase (by complete removal),
		// which would happen if the loop does not compute something that is used outside of it.
		int resultHash = 0;
		System.out.format("Main-Thread: Executing workers in new threads for Warmup!\n");
		resultHash = addDistributed(WARMUP_LOOPS);
		resultHash = muxDistributed(WARMUP_LOOPS);
		System.out.format("warm-up phase ended with result hash %d.\n", resultHash);
	
		final long timestamp0 = System.currentTimeMillis();
		resultHash = addDistributed(10000);
		final long timestamp1 = System.currentTimeMillis();
		resultHash ^= muxDistributed(10000);
		final long timestamp2 = System.currentTimeMillis();
		System.out.format("timing phase ended with result hash %d.\n", resultHash);
		System.out.format("a + b computed in %.4fms.\n", (timestamp1 - timestamp0) * 0.0001);
		System.out.format("a x b computed in %.4fms.\n", (timestamp2 - timestamp1) * 0.0001);

//		if (size <= 100) {
//			final double[] sum = addDistributed(a, b);
//			final double[][] mux = muxDistributed(a, b);
//			System.out.print("a = ");
//			System.out.println(Arrays.toString(a));
//			System.out.print("b = ");
//			System.out.println(Arrays.toString(b));
//			System.out.print("a + b = ");
//			System.out.println(Arrays.toString(sum));
//			System.out.print("a x b = [");
//			for (int index = 0; index < mux.length; ++index) {
//				System.out.print(Arrays.toString(mux[index]));
//			}
//			System.out.println("]");
//		}
	}
	
	@SuppressWarnings("unchecked")
	static public int addDistributed (int loopSize) throws NegativeArraySizeException, InterruptedException, ExampleCheckedException {
		final Callable<double[]> addWorker = () -> {
			double[] result = null;
			result = add(a, b);
			return result;
		};
		
		int result = 0;
		
		System.out.format("Main-Thread: Executing workers in new threads!\n");
		final RunnableFuture<double[]>[] futures = new RunnableFuture[PROCESSOR_COUNT];
		ExecutorService threadPool = Executors.newFixedThreadPool(PROCESSOR_COUNT);
		
		for (int i = 0; i<PROCESSOR_COUNT; i++) {
			futures[i] = (RunnableFuture<double[]>) threadPool.submit(addWorker);
			//futures[index] = new FutureTask<>(addWorker);
			//new Thread(futures[index], "worker-thread-" + index).start();
		}
		threadPool.shutdown();
		
		System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final RunnableFuture<double[]> future : futures) {
				try {
					result ^= future.get().hashCode();
					System.out.format("child thread ended\n");
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof ExampleCheckedException) throw (ExampleCheckedException) cause;
					throw new AssertionError();
				}
			}
		} catch (Error | RuntimeException exception) {
			for (final RunnableFuture<double[]> future : futures) {
				future.cancel(true);
			}
			throw exception;
		}

		System.out.format("Main-Thread: All child threads are done!\n");
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	static public int muxDistributed (int loopSize) throws NegativeArraySizeException, InterruptedException, ExampleCheckedException {
		
		AtomicInteger loopMux = new AtomicInteger(loopSize);
		
		final Callable<double[][]> muxWorker = () -> {
			double[][] result = null;
			System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
			while (loopMux.get()>0) {
				result = mux(a, b);
				loopMux.set(loopMux.decrementAndGet());
			}
			System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
			return result;
		};

		int totalResult = 0;
		System.out.format("Main-Thread: Executing workers in new threads!\n");
		final RunnableFuture<double[][]>[] futures = new RunnableFuture[PROCESSOR_COUNT];
		ExecutorService threadPool = Executors.newFixedThreadPool(PROCESSOR_COUNT);
		for (int index = 0; index < PROCESSOR_COUNT; ++index) {
			futures[index] = (RunnableFuture<double[][]>) threadPool.submit(muxWorker);
			//futures[index] = new FutureTask<>(addWorker);
			//new Thread(futures[index], "worker-thread-" + index).start();
		}
		threadPool.shutdown();
		System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final RunnableFuture<double[][]> future : futures) {
				try {
					totalResult ^= future.get().hashCode();
					System.out.format("child thread ended\n");
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof ExampleCheckedException) throw (ExampleCheckedException) cause;
					throw new AssertionError();
				}
			}
		} catch (Error | RuntimeException exception) {
			for (final RunnableFuture<double[][]> future : futures) {
				future.cancel(true);
			}
			throw exception;
		}

		System.out.format("Main-Thread: All child threads are done!\n");
		return totalResult;
	}

}