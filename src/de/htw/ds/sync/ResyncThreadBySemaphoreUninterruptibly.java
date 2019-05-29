package de.htw.ds.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the blocking technique {@link Semaphore#acquireUninterruptibly()}.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadBySemaphoreUninterruptibly {
	static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(4);

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException 
	 */
	static public void main (final String[] args) throws InterruptedException {
		try {
			workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
		} finally {
			THREAD_POOL.shutdownNow();
		}
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Semaphore#acquire()}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException {
		final Semaphore indebtedSemaphore = new Semaphore(1 - workerCount);
		final Runnable worker = () -> {
			try {
				System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
				try { ExampleWorker.work(10); } catch (ExampleCheckedException e) {}
				System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
			} finally {
				indebtedSemaphore.release();
			}
		};

		System.out.format("Main-Thread: Executing workers in thread pool!\n");
		for (int index = 0; index < workerCount; ++index) {
			THREAD_POOL.execute(worker);
		}

		System.out.format("Main-Thread: Waiting for child threads to finish work!\n");
		indebtedSemaphore.acquireUninterruptibly();

		System.out.format("Main-Thread: All child threads are terminated!\n");
	}
}