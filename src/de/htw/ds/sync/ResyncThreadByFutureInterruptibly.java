package de.htw.ds.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes INTERRUPTIBLY using
 * the blocking technique {@link Future#get()}. Note that any remaining active child threads should
 * be stopped when ending resynchronization prematurely (by exception)!
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByFutureInterruptibly {

	/**
	 * Performs some work INTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException if the operation is interrupted
	 * @throws ExampleCheckedException if there is a worker specific problem
	 */
	static public void main (final String[] args) throws InterruptedException, ExampleCheckedException {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Future#get()}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 * @throws InterruptedException if the operation is interrupted
	 * @throws ExampleCheckedException if there is a worker specific problem
	 */
	@SuppressWarnings("unchecked")
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException, InterruptedException, ExampleCheckedException {
		final Callable<Long> worker = () -> {
			final long timestamp = System.currentTimeMillis();
			System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
			ExampleWorker.work(10);
			System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
			return System.currentTimeMillis() - timestamp;
		};

		System.out.format("Main-Thread: Executing workers in new threads!\n");
		final RunnableFuture<Long>[] futures = new RunnableFuture[workerCount];
		for (int index = 0; index < workerCount; ++index) {
			// futures[index] = threadPool.submit(worker);
			futures[index] = new FutureTask<>(worker);
			new Thread(futures[index], "worker-thread-" + index).start();
		}

		System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final Future<Long> future : futures) {
				try {
					final long result = future.get();
					System.out.format("child thread ended after %.2fs\n", result * 0.001);
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof ExampleCheckedException) throw (ExampleCheckedException) cause;
					throw new AssertionError();
				}
			}
		} finally {
			for (final Future<Long> future : futures)
				future.cancel(true);
		}

		System.out.format("Main-Thread: All child threads are done!\n");
	}
}