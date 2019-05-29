package de.htw.ds.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes INTERRUPTIBLY using
 * the blocking technique {@link ExecutorService#awaitTermination(long, TimeUnit)}. Note that any
 * remaining active child threads should be stopped when ending resynchronization prematurely (by
 * exception)!
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByForkJoinPoolInterruptibly {

	/**
	 * Performs some work INTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void main (final String[] args) throws InterruptedException {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work INTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link ExecutorService#awaitTermination(long, TimeUnit)}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException, InterruptedException {
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
		try {
			forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException exception) {
			forkJoinPool.shutdownNow();
			throw exception;
		}
		System.out.format("Main-Thread: All child threads are terminated!\n");
	}
}