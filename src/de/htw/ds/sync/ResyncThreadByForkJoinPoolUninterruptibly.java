package de.htw.ds.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the blocking technique {@link ExecutorService#awaitTermination(long, TimeUnit)}.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByForkJoinPoolUninterruptibly {

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException 
	 */
	static public void main (final String[] args) {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link ExecutorService#awaitTermination(long, TimeUnit)}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException {
		final Runnable worker = () -> {
			System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
			try { ExampleWorker.work(10); } catch (ExampleCheckedException e) {}
			System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
		};

		System.out.format("Main-Thread: Executing workers in fork/join pool!\n");
		final ExecutorService forkJoinPool = Executors.newWorkStealingPool(workerCount);
		for (int index = 0; index < workerCount; ++index) {
			forkJoinPool.execute(worker);
		}

		System.out.format("Main-Thread: Waiting for child threads to die!\n");
		forkJoinPool.shutdown();
		awaitTerminationUninterruptibly(forkJoinPool);
		System.out.format("Main-Thread: All child threads are terminated!\n");
	}


	/**
	 * Repeat the (interruptible) blocking {@link ExecutorService#awaitTermination(long, TimeUnit)}
	 * operation until it ends without being interrupted. Note that this implementation preserves
	 * the interrupt-status of it's thread, i.e. interruptions are only delayed, not ignored
	 * completely.
	 * @param executorService the executor service
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static private void awaitTerminationUninterruptibly (final ExecutorService executorService) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
					break;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}
}