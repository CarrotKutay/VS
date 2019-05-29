package de.htw.ds.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the blocking technique {@link Future#get()}. Note that any remaining active child threads should
 * be stopped when ending resynchronization prematurely (by exception)!
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByFutureUninterruptibly {
	static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(4);

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws ExampleCheckedException if there is a worker specific problem
	 */
	static public void main (final String[] args) throws ExampleCheckedException {
		try {
			workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
		} finally {
			THREAD_POOL.shutdownNow();
		}
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Future#get()}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 * @throws ExampleCheckedException if there is a worker specific problem
	 */
	@SuppressWarnings("unchecked")
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException, ExampleCheckedException {
		final Callable<Long> worker = () -> {
			final long timestamp = System.currentTimeMillis();
			System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
			ExampleWorker.work(10);
			System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
			return System.currentTimeMillis() - timestamp;
		};

		System.out.format("Main-Thread: Executing workers in thread pool!\n");
		final Future<Long>[] futures = new Future[workerCount];
		for (int index = 0; index < workerCount; ++index) {
			futures[index] = THREAD_POOL.submit(worker);
		}

		System.out.format("Main-Thread: Waiting for child threads to finish!\n");
		try {
			for (final Future<Long> future : futures) {
				try {
					final long result = getUninterruptibly(future);
					System.out.format("child thread ended after %.2fs\n", result * 0.001);
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();	// manual precise rethrow for cause!
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof ExampleCheckedException) throw (ExampleCheckedException) cause;
					throw new AssertionError();
				}
			}
		} catch (final Throwable exception) {
			for (final Future<Long> future : futures) {
				future.cancel(true);
			}
		}

		System.out.format("Main-Thread: All child threads are done!\n");
	}


	/**
	 * Repeat the (interruptible) blocking {@link Future#get()} operation until it ends without
	 * being interrupted. Note that this implementation preserves the interrupt-status of it's
	 * thread, i.e. interruptions are only delayed, not ignored completely.
	 * @param future the future
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws ExecutionException if there is a problem during the given future's execution
	 */
	static public <T> T getUninterruptibly (final Future<T> future) throws NullPointerException, ExecutionException {
		T result;

		boolean interrupted = false;
		try {
			while (true) {
				try {
					result = future.get();
					break;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}

		return result;
	}
}